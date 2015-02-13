package com.smartbear.mqttsupport;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;

public class Client implements MqttCallback {
    public static class Message{
        public String topic;
        public MqttMessage message;

        public Message(String topic, MqttMessage msg){
            this.topic = topic;
            this.message = msg;
        }
    }


    private MqttAsyncClient clientObj;
    private IMqttToken connectionToken;
    private MqttConnectOptions connectionOptions;
    private ArrayList<String> subscribedTopics = new ArrayList<String>();
    private MessageQueue messageQueue = new MessageQueue();

    public Client(MqttAsyncClient client, MqttConnectOptions connectionOptions){
        this.clientObj = client;
        this.connectionOptions = connectionOptions;
        client.setCallback(this);
    }

    public boolean isConnected(){
        return clientObj.isConnected();
    }

    public IMqttToken getConnectingStatus() throws MqttException {
        if(connectionToken == null){
            if(connectionOptions.isCleanSession()) subscribedTopics = new ArrayList<String>();
            connectionToken = clientObj.connect(connectionOptions);
        }
        return connectionToken;
    }


    public void connectionLost(Throwable throwable){
        connectionToken = null;
    }

    public void messageArrived(String topic, MqttMessage mqttMessage) throws java.lang.Exception{
        messageQueue.addMessage(new Message(topic, mqttMessage));
    }

    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken){

    }

    public MqttAsyncClient getClientObject(){
        return clientObj;
    }

    public ArrayList<String> getCachedSubscriptions(){
        return subscribedTopics;
    }


    public MessageQueue getMessageQueue(){return messageQueue;}

}
