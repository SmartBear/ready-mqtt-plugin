package com.smartbear.mqttsupport.connection;

import com.smartbear.mqttsupport.MessageQueue;
import com.smartbear.mqttsupport.Messages;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClientException;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Client implements MqttCallback, MqttActionListener {
    private static final Logger log = LoggerFactory.getLogger(Client.class);

    private final long SECOND_IN_NANOSECONDS = 1000_000_000;
    private static ArrayList<Integer> clientIndexPool = new ArrayList<>();
    private static AtomicInteger maxClientIndex = new AtomicInteger();

    public static class Message {
        public String topic;
        public MqttMessage message;

        public Message(String topic, MqttMessage msg) {
            this.topic = topic;
            this.message = msg;
        }
    }


    private MqttAsyncClientEx clientObj;
    private IMqttToken connectionToken;
    private MqttConnectionOptions connectionOptions;
    private volatile ArrayList<String> subscribedTopics = new ArrayList<>();
    private MessageQueue messageQueue = new MessageQueue();
    private boolean resetSession;

    public Client(MqttAsyncClientEx client, MqttConnectionOptions connectionOptions, boolean resetSession) {
        this.clientObj = client;
        this.connectionOptions = connectionOptions;
        this.resetSession = resetSession;
        client.setCallback(this);
    }

    public boolean isConnected() {
        return clientObj.isConnected();
    }

    public boolean isConnecting() {
        return clientObj.isConnecting();
    }

    public boolean isClosed() {
        return clientObj.isClosed();
    }

    public boolean isDisconnecting() {
        return clientObj.isDisconnecting();
    }

    public boolean isDisconnected() {
        return clientObj.isDisconnected();
    }

    public MqttConnectionOptions getConnectionOptions() {
        return connectionOptions;
    }

    public IMqttToken getConnectingStatus(long maxTimeout) throws MqttException { // 0 - no timeout
        if (connectionToken == null) {
            if (connectionOptions.isCleanStart()) {
                subscribedTopics = new ArrayList<>();
            }
            if (resetSession) {
                connectionToken = new ResetSessionToken(maxTimeout / 1000);
            } else {
                connectionOptions.setKeepAliveInterval((int) (maxTimeout / SECOND_IN_NANOSECONDS));
                connectionOptions.setConnectionTimeout((int) (maxTimeout / SECOND_IN_NANOSECONDS));
                connectionToken = clientObj.connect(connectionOptions, null, this);
                if (!clientObj.isConnecting()) {
                    log.error(Messages.CLIENT_IS_NOT_ATTEPTING_TO_CONNECT);
                }
            }
        } else {
            return connectionToken;
        }
        return connectionToken;
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {

    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {

    }

    public void messageArrived(String topic, MqttMessage mqttMessage) throws java.lang.Exception {
        messageQueue.addMessage(new Message(topic, mqttMessage));
    }

    @Override
    public void deliveryComplete(IMqttToken token) {

    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {

    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {

    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        if (!asyncActionToken.getSessionPresent()) {
            subscribedTopics = new ArrayList<>();
        }
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        log.error(Messages.INTERNAL_ERROR, exception);
    }


    private void onDisconnected() {
        connectionToken = null;
    }

    public MqttAsyncClient getClientObject() {
        return clientObj;
    }

    public ArrayList<String> getCachedSubscriptions() {
        if (connectionToken == null && connectionOptions.isCleanStart()) {
            subscribedTopics = new ArrayList<>();
        }
        return subscribedTopics;
    }


    public MessageQueue getMessageQueue() {
        return messageQueue;
    }


    public void disconnect(boolean sendDisconnectMessage) throws MqttException {
        messageQueue = new MessageQueue();
        if (sendDisconnectMessage) {
            clientObj.disconnectForcibly();
        } else {
            clientObj.closeConnection();
        }
        onDisconnected(); //we may not receive connectionLost notification (especially if sendDisconnectMessage = false)
    }

    public void dispose() {
        if (!connectionOptions.isCleanStart()) {
            ArrayList<String> topicList = subscribedTopics;
            String[] topics = new String[topicList.size()];
            topicList.toArray(topics);
            if (getClientObject().isConnected()) {
                try {
                    getClientObject().unsubscribe(topics);
                } catch (MqttException e) {
                }
            }
        }
        try {
            if (getClientObject().isConnected()) {
                clientObj.disconnectForcibly();
            }
            clientObj.close();

        } catch (MqttException e) {
        }
        freeClientIndex(clientObj.getClientIndex());
    }

    private static void freeClientIndex(int clientIndex) {
        synchronized (clientIndexPool) {
            clientIndexPool.add(clientIndex);
        }
    }

    public static int getClientIndex() {
        synchronized (clientIndexPool) {
            if (clientIndexPool.size() > 0) {
                Integer value = clientIndexPool.get(0);
                clientIndexPool.remove(value);
                return value;
            } else {
                return maxClientIndex.getAndIncrement();
            }
        }
    }


    private static class TokenState {
        boolean completed;
        MqttException exception;

        public TokenState(boolean completed) {
            this.completed = completed;
        }

        public TokenState(MqttException exception) {
            this.exception = exception;
            this.completed = true;
        }

        public TokenState(IMqttToken token) {
            this.completed = token.isComplete();
            this.exception = token.getException();
        }
    }

    class ResetSessionToken implements IMqttToken {
        private IMqttToken connect1Token;
        private IMqttToken disconnectToken, connect2Token;
        private MqttException initiationException = null;
        private long maxTime;


        public ResetSessionToken(long maxTimeout) throws MqttException {
            if (maxTimeout == 0) {
                maxTime = Long.MAX_VALUE;
            } else {
                maxTime = System.nanoTime() + maxTimeout;
            }
            MqttConnectionOptions resetConnectionOptions = new MqttConnectionOptions();
            resetConnectionOptions.setCleanStart(true);
            resetConnectionOptions.setConnectionTimeout(getTimeout());
            resetConnectionOptions.setAutomaticReconnect(connectionOptions.isAutomaticReconnect());
            resetConnectionOptions.setUserName(connectionOptions.getUserName());
            resetConnectionOptions.setPassword(connectionOptions.getPassword());
            resetConnectionOptions.setKeepAliveInterval(connectionOptions.getKeepAliveInterval());
            resetConnectionOptions.setSocketFactory(connectionOptions.getSocketFactory());
            this.connect1Token = clientObj.connect(resetConnectionOptions);
        }

        @Override
        public boolean isComplete() {
            return getState().completed;
        }

        private int getTimeout() throws MqttException {
            final int unitsPerSecond = 1000 * 1000 * 1000;
            if (maxTime == Long.MAX_VALUE) {
                return 0;
            }
            long timeout = maxTime - System.nanoTime();
            if (timeout <= 0) {
                throw new MqttException(MqttClientException.REASON_CODE_CLIENT_TIMEOUT);
            } else {
                return (int) ((timeout + unitsPerSecond - 1) / unitsPerSecond);
            }
        }

        private TokenState getState() {
            if (initiationException != null) {
                return new TokenState(initiationException);
            }
            if (connect2Token != null) {
                return new TokenState(connect2Token);
            }
            if (disconnectToken != null) {
                if (disconnectToken.isComplete()) {
                    if (disconnectToken.getException() == null) {
                        try {
                            connectionOptions.setConnectionTimeout(getTimeout());
                            connect2Token = clientObj.connect(connectionOptions);
                            return new TokenState(connect2Token);
                        } catch (MqttException e) {
                            initiationException = e;
                            return new TokenState(initiationException);
                        }
                    } else {
                        return new TokenState(disconnectToken.getException());
                    }
                } else {
                    return new TokenState(false);
                }
            } else {
                if (connect1Token.isComplete()) {
                    if (connect1Token.getException() == null) {
                        resetSession = false;
                        try {
                            disconnectToken = clientObj.disconnect(0);
                        } catch (MqttException e) {
                            initiationException = e;
                            return new TokenState(initiationException);
                        }
                        return getState();
                    } else {
                        return new TokenState(connect1Token.getException());
                    }
                } else {
                    return new TokenState(false);
                }
            }
        }

        @Override
        public void waitForCompletion() throws MqttException {
            waitForCompletion(Long.MAX_VALUE);
        }

        @Override
        public void waitForCompletion(long timeout) throws MqttException {
            long endTime = System.currentTimeMillis() + timeout;
            while (System.currentTimeMillis() < endTime) {
                if (connectionToken == null || connectionToken.isComplete()) {
                    return;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        @Override
        public MqttException getException() {
            return getState().exception;
        }

        @Override
        public void setActionCallback(MqttActionListener listener) {
            throw new UnsupportedOperationException(Messages.NOT_IMPLEMENTED_YET);
        }

        @Override
        public MqttActionListener getActionCallback() {
            return null;
        }

        @Override
        public MqttAsyncClient getClient() {
            return clientObj;
        }

        @Override
        public String[] getTopics() {
            return new String[0];
        }

        @Override
        public void setUserContext(Object userContext) {
            throw new UnsupportedOperationException(Messages.NOT_IMPLEMENTED_YET);
        }

        @Override
        public Object getUserContext() {
            return null;
        }

        @Override
        public int getMessageId() {
            return 0;
        }

        @Override
        public int[] getGrantedQos() {
            return new int[0];
        }

        @Override
        public int[] getReasonCodes() {
            return new int[0];
        }

        @Override
        public boolean getSessionPresent() {
            if (connect2Token != null) {
                return connect2Token.getSessionPresent();
            } else {
                return false;
            }
        }

        @Override
        public MqttWireMessage getResponse() {
            if (connect2Token != null) {
                return connect2Token.getResponse();
            }
            if (disconnectToken != null) {
                return disconnectToken.getResponse();
            }
            if (connect1Token != null) {
                return connect1Token.getResponse();
            }
            return null;
        }

        @Override
        public MqttProperties getResponseProperties() {
            return null;
        }

        @Override
        public MqttMessage getMessage() {
            return null;
        }

        @Override
        public MqttWireMessage getRequestMessage() {
            return null;
        }

        @Override
        public MqttProperties getRequestProperties() {
            return null;
        }
    }

}
