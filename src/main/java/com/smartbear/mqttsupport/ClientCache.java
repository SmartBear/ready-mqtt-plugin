package com.smartbear.mqttsupport;

import com.eviware.soapui.support.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;


public class ClientCache {
    private static class CacheKey {
        private String serverUri;
        private ConnectionParams connectionParams;

        public CacheKey(String serverUri, ConnectionParams connectionParams) {
            if (serverUri == null) {
                serverUri = "";
            }

            URI uri = null;
            try {
                uri = new URI(serverUri);
                if (StringUtils.isNullOrEmpty(uri.getScheme())) {
                    uri = new URI("tcp", uri.getSchemeSpecificPart(), uri.getFragment());
                }
                if (uri.getPort() == -1) {
                    if ("tcp".equals(uri.getScheme().toLowerCase(Locale.ENGLISH))) {
                        uri = new URI("tcp", uri.getUserInfo(), uri.getHost(), PluginConfig.DEFAULT_TCP_PORT, uri.getPath(), uri.getQuery(), uri.getFragment());
                    } else if ("ssl".equals(uri.getScheme().toLowerCase(Locale.ENGLISH))) {
                        uri = new URI("ssl", uri.getUserInfo(), uri.getHost(), PluginConfig.DEFAULT_SSL_PORT, uri.getPath(), uri.getQuery(), uri.getFragment());
                    }
                }
                this.serverUri = uri.toString().toLowerCase(Locale.ENGLISH);
            } catch (URISyntaxException e) {
                this.serverUri = serverUri;
            }
            this.connectionParams = connectionParams;
        }

        public String getServerUri() {
            return serverUri;
        }

        public ConnectionParams getConnectionParams() {
            return connectionParams;
        }

        @Override
        public int hashCode() {
            if (connectionParams == null) {
                return serverUri.hashCode();
            } else {
                return (serverUri + "@" + connectionParams.getKey()).hashCode();
            }
        }

        @Override
        public boolean equals(Object arg) {
            if (arg == null || !(arg instanceof CacheKey)) {
                return false;
            }
            CacheKey compared = (CacheKey) arg;
            if (!serverUri.equals(compared.getServerUri())) {
                return false;
            }
            if (connectionParams == null) {
                return compared.getConnectionParams() == null;
            }
            return connectionParams.equals(compared.getConnectionParams());
        }
    }



    public Client get(String serverUri, ConnectionParams connectionParams) throws MqttException {
        CacheKey key = new CacheKey(serverUri, connectionParams);
        Client result = map.get(key);
        if(result == null){
            register(serverUri, connectionParams);
            result = map.get(key);
        }
        return result;
    }


    public Client get(String serverUri) throws MqttException {
        return get(serverUri, null);
    }

    private MqttAsyncClient register(String serverUri, ConnectionParams connectionParams) throws MqttException {

        CacheKey key = new CacheKey(serverUri, connectionParams);
        String clientId;
        if (connectionParams == null || connectionParams.hasGeneratedId()) {
            clientId = MqttAsyncClient.generateClientId();
        }
        else{
            clientId = connectionParams.getFixedId();
        }

        MqttAsyncClient newClient = new MqttAsyncClient(serverUri, clientId, new MemoryPersistence());

        //IMqttToken token = newClient.connect(createConnectionOptions(connectionParams));
        map.put(key, new Client(newClient, createConnectionOptions(connectionParams)));
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
        for (Client client : map.values()) {
            client.dispose();
        }
        map.clear();
    }

    private HashMap<CacheKey, Client> map = new HashMap<>();

}
