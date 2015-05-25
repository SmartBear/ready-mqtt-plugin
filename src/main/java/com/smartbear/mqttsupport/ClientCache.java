package com.smartbear.mqttsupport;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ClientCache {
    private static final int LEGACY_UNTITLED_CLIENT = -1;
    private static final int FIRST_UNTITLED_CLIENT = 0;

    private static class CacheKey{
        public ConnectionParams params;
        public int generatedClientNo;

        public CacheKey(ConnectionParams params, int generatedClientNo){
            this.params = params;
            this.generatedClientNo = generatedClientNo;
        }

        public CacheKey(ConnectionParams params){
            this(params, LEGACY_UNTITLED_CLIENT);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CacheKey cacheKey = (CacheKey) o;

            if (!params.equals(cacheKey.params)) {
                return false;
            }

            if (params.isGeneratedId() && cacheKey.params.isGeneratedId() && generatedClientNo != cacheKey.generatedClientNo) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return String.format("%s\n%s\n%s", params.getNormalizedServerUri(), params.fixedId, params.login).hashCode();
        }
    }


    public Client getLegacy(ConnectionParams connectionParams) throws MqttException {
        Client result = map.get(new CacheKey(connectionParams, LEGACY_UNTITLED_CLIENT));
        if(result == null){
            result = register(connectionParams, LEGACY_UNTITLED_CLIENT);
        }
        return result;
    }

    public Client get(String connectionName){
        CacheKey key = mapByName.get(connectionName);
        if(key == null) return null;
        return map.get(key);
    }

    public Client add(String connectionName, ConnectionParams params) throws MqttException{
        Client result = get(connectionName);
        if(result == null){
            if(!params.isGeneratedId()){
                result = getLegacy(params);
                mapByName.put(connectionName, new CacheKey(params));
            }
            else{
                int clientNo = FIRST_UNTITLED_CLIENT;
                while(map.get(new CacheKey(params, clientNo)) != null){
                    clientNo++;
                }
                mapByName.put(connectionName, new CacheKey(params, clientNo));
                result = register(params,  clientNo);
            }
        }
        return result;
    }

    private Client register(ConnectionParams connectionParams, int generatedClientNo) throws MqttException {
        CacheKey cacheKey = new CacheKey(connectionParams, generatedClientNo);
        String clientId;
        if (connectionParams.isGeneratedId()) {
            clientId = MqttAsyncClient.generateClientId();
        }
        else{
            clientId = connectionParams.fixedId;
        }

        MqttAsyncClient clientObj = new MqttAsyncClient(connectionParams.getActualServerUri(), clientId, new MemoryPersistence());
        Client newClient = new Client(clientObj, createConnectionOptions(connectionParams));
        map.put(cacheKey, newClient);
        return newClient;
    }

    private MqttConnectOptions createConnectionOptions(ConnectionParams connectionParams) {
        MqttConnectOptions connectOptions;
        if (connectionParams == null) {
            connectOptions = getDefaultConnectOptions();
        } else {
            connectOptions = new MqttConnectOptions();
            if (connectionParams.hasCredentials()) {
                connectOptions.setUserName(connectionParams.login);
                connectOptions.setPassword(connectionParams.password.toCharArray());
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
        for(Client client: map.values()){
            client.dispose();
        }
        map.clear();
        mapByName.clear();
    }

    private HashMap<CacheKey, Client> map = new HashMap<>();
    private HashMap<String, CacheKey> mapByName = new HashMap<>();

}
