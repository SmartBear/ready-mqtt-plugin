package com.smartbear.mqttsupport;

import com.eviware.soapui.support.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashMap;


public class ClientCache {

    private final static Logger log = Logger.getLogger(PluginConfig.LOGGER_NAME);

    private static final int LEGACY_UNTITLED_CLIENT = -1;
    private static final int FIRST_UNTITLED_CLIENT = 0;

    private static class CacheKey {
        public ExpandedConnectionParams params;
        public int generatedClientNo;

        public CacheKey(ExpandedConnectionParams params, int generatedClientNo) {
            this.params = params;
            this.generatedClientNo = generatedClientNo;
        }

        public CacheKey(ExpandedConnectionParams params) {
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


    public Client getLegacy(ExpandedConnectionParams connectionParams) throws MqttException {
        Client result = map.get(new CacheKey(connectionParams, LEGACY_UNTITLED_CLIENT));
        if (result == null) {
            result = register(connectionParams, LEGACY_UNTITLED_CLIENT);
        }
        return result;
    }

    public Client get(String connectionName) {
        CacheKey key = mapByName.get(connectionName);
        if (key == null) {
            return null;
        }
        return map.get(key);
    }

    public Client add(String connectionName, ExpandedConnectionParams params) throws MqttException {
        Client result = get(connectionName);
        if (result == null) {
            if (!params.isGeneratedId()) {
                result = getLegacy(params);
                mapByName.put(connectionName, new CacheKey(params));
            } else {
                int clientNo = FIRST_UNTITLED_CLIENT;
                while (map.get(new CacheKey(params, clientNo)) != null) {
                    clientNo++;
                }
                mapByName.put(connectionName, new CacheKey(params, clientNo));
                result = register(params, clientNo);
            }
        }
        return result;
    }

    private Client register(ExpandedConnectionParams connectionParams, int generatedClientNo) throws MqttException {
        CacheKey cacheKey = new CacheKey(connectionParams, generatedClientNo);
        String clientId;
        if (connectionParams.isGeneratedId()) {
            clientId = MqttAsyncClient.generateClientId();
        } else {
            clientId = connectionParams.fixedId;
        }

        MqttAsyncClientEx clientObj = new MqttAsyncClientEx(connectionParams.getActualServerUri(), clientId, new MemoryPersistence());
        Client newClient = new Client(clientObj, createConnectionOptions(connectionParams), !connectionParams.cleanSession && !connectionParams.isGeneratedId());
        map.put(cacheKey, newClient);
        return newClient;
    }

    private MqttConnectOptions createConnectionOptions(ExpandedConnectionParams connectionParams) {
        MqttConnectOptions connectOptions;
        if (connectionParams == null) {
            connectOptions = getDefaultConnectOptions();
        } else {
            connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(connectionParams.cleanSession);
            if (connectionParams.hasCredentials()) {
                connectOptions.setUserName(connectionParams.login);
                connectOptions.setPassword(connectionParams.password.toCharArray());
            }
            if (connectionParams.willTopic != null && connectionParams.willTopic.length() != 0) {
                connectOptions.setWill(connectionParams.willTopic, connectionParams.willMessage, connectionParams.willQos, connectionParams.willRetained);
            }
        }

        if (!StringUtils.isNullOrEmpty(connectionParams.getCaCrtFile())
                && !StringUtils.isNullOrEmpty(connectionParams.getCrtFile())
                && !StringUtils.isNullOrEmpty(connectionParams.getKeyFile())
                && !StringUtils.isNullOrEmpty(connectionParams.getKeysPassword())) {
            try {
                connectOptions.setSocketFactory(SSLCertsHelper.getSocketFactory(connectionParams.getCaCrtFile(),
                        connectionParams.getCrtFile(), connectionParams.getKeyFile(), connectionParams.getKeysPassword(),
                        connectionParams.getSniHost()));
            } catch (Exception e) {
                log.error("Unable to initialize SSL connection.", e);
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
        for (Client client : map.values()) {
            client.dispose();
        }
        map.clear();
        mapByName.clear();
    }

    private HashMap<CacheKey, Client> map = new HashMap<>();
    private HashMap<String, CacheKey> mapByName = new HashMap<>();

}
