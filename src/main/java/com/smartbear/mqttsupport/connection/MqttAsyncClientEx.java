package com.smartbear.mqttsupport.connection;

import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;

import java.util.UUID;

public class MqttAsyncClientEx extends MqttAsyncClient {

    private static final String INSTANCE_ID = UUID.randomUUID().toString();
    private static final String READY_API_CLIENT_ID = "ReadyAPI_MQTT_client";
    private int clientIndex;

    public MqttAsyncClientEx(String serverURI, int clientIndex) throws MqttException {
        super(serverURI, getClientId(clientIndex));
        this.clientIndex = clientIndex;
    }

    public MqttAsyncClientEx(String serverURI, int clientIndex, MqttClientPersistence persistence) throws MqttException {
        super(serverURI, getClientId(clientIndex), persistence);
        this.clientIndex = clientIndex;
    }

    public MqttAsyncClientEx(String actualServerUri, String clientId, MemoryPersistence persistence) throws MqttException {
        super(actualServerUri, clientId, persistence);
        this.clientIndex = -1;
    }

    private static String getClientId(int clientIndex) {
        return READY_API_CLIENT_ID + "_" + INSTANCE_ID + "_" + clientIndex;
    }

    public void closeConnection() {
        MqttToken token = new MqttToken(comms.getClient().getClientId());
        comms.shutdownConnection(token, null, null);
    }

    public boolean isConnecting() {
        return comms.isConnecting();
    }


    public boolean isClosed() {
        return comms.isClosed();
    }

    public boolean isDisconnecting() {
        return comms.isDisconnecting();
    }

    public boolean isDisconnected() {
        return comms.isDisconnected();
    }

    public int getClientIndex() {
        return clientIndex;
    }
}