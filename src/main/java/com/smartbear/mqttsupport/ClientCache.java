package com.smartbear.mqttsupport;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ClientCache {

    public Client getLegacy(ConnectionParams connectionParams) throws MqttException {
        Client result = map.get(connectionParams);
        if(result == null){
            result = register(connectionParams);
        }
        return result;
    }

    public Client get(String connectionName){
        return mapByName.get(connectionName);
    }

    public Client add(ConnectionParams connectionParams) throws MqttException{
        if(connectionParams.isLegacy()) throw new IllegalArgumentException("This caching mechanism is not appropriate for the legacy mode.");
        Client result = get(connectionParams.getName());
        if(result == null){
            if(!connectionParams.isGeneratedId()){
                result = map.get(connectionParams);
                if(result != null){
                    mapByName.put(connectionParams.getName(), result);
                    return result;
                }
            }
            register(connectionParams);
            result = get(connectionParams.getName());
        }
        return result;
    }

    private Client register(ConnectionParams connectionParams) throws MqttException {
        String clientId;
        if (connectionParams.isGeneratedId()) {
            clientId = MqttAsyncClient.generateClientId();
        }
        else{
            clientId = connectionParams.getFixedId();
        }

        MqttAsyncClient clientObj = new MqttAsyncClient(connectionParams.getActualServerUri(), clientId, new MemoryPersistence());
        Client newClient = new Client(clientObj, createConnectionOptions(connectionParams));
        if(connectionParams.isLegacy() || !connectionParams.isGeneratedId()) {
            map.put(connectionParams, newClient);
        }
        if(!connectionParams.isLegacy()){
            mapByName.put(connectionParams.getName(), newClient);
        }
        return newClient;

    }

    private MqttConnectOptions createConnectionOptions(ConnectionParams connectionParams) {
        MqttConnectOptions connectOptions;
        if (connectionParams == null) {
            connectOptions = getDefaultConnectOptions();
        } else {
            connectOptions = new MqttConnectOptions();
            if (connectionParams.hasCredentials()) {
                connectOptions.setUserName(connectionParams.getLogin());
                connectOptions.setPassword(connectionParams.getPassword().toCharArray());
            }
        }
        return connectOptions;
    }


    private MqttConnectOptions getDefaultConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        return options;
    }

    public void assureFinalized() {
        ArrayList<String> duplicates = new ArrayList<>();
        for(Map.Entry<ConnectionParams, Client> pair : map.entrySet()){
            if(!pair.getKey().isLegacy()) duplicates.add(pair.getKey().getName());
        }
        map.clear();
        for (Map.Entry<String, Client> pair : mapByName.entrySet()) {
            if(!duplicates.contains(pair.getKey())) pair.getValue().dispose();
        }
        mapByName.clear();
    }

    private HashMap<ConnectionParams, Client> map = new HashMap<>();
    private HashMap<String, Client> mapByName = new HashMap<>();

}
