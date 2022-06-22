package com.smartbear.mqttsupport.connection;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;

public class MqttAsyncClientEx extends MqttAsyncClient {

    private final static String INSTANCE_ID = UUID.randomUUID().toString();
    private final static String READY_API_CLIENT_ID = "ReadyAPI_MQTT_client";
    private int clientIndex;

    public MqttAsyncClientEx(String serverURI, int clientIndex) throws MqttException {
        super(serverURI, getClientId(clientIndex));
        this.clientIndex = clientIndex;
    }

    public MqttAsyncClientEx(String serverURI, int clientIndex, MqttClientPersistence persistence) throws MqttException {
        super(serverURI, getClientId(clientIndex), persistence);
        this.clientIndex = clientIndex;
    }

    public MqttAsyncClientEx(String serverURI, int clientIndex, MqttClientPersistence persistence, MqttPingSender pingSender) throws MqttException {
        super(serverURI, getClientId(clientIndex), persistence, pingSender);
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
        comms.shutdownConnection(token, null);

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