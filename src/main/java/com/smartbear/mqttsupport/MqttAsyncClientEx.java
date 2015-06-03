package com.smartbear.mqttsupport;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.MqttToken;

public class MqttAsyncClientEx extends MqttAsyncClient {
    public MqttAsyncClientEx(String serverURI, String clientId) throws MqttException {
        super(serverURI, clientId);
    }

    public MqttAsyncClientEx(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
        super(serverURI, clientId, persistence);
    }

    public MqttAsyncClientEx(String serverURI, String clientId, MqttClientPersistence persistence, MqttPingSender pingSender) throws MqttException {
        super(serverURI, clientId, persistence, pingSender);
    }
    public void closeConnection(){
        MqttToken token = new MqttToken(comms.getClient().getClientId());
        comms.shutdownConnection(token, null);

    }
}