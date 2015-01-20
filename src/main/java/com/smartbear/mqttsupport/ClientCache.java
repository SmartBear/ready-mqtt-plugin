package com.smartbear.mqttsupport;

import com.eviware.soapui.support.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;


public class ClientCache {
    private static class CacheKey {
        private String serverUri;
        private String clientId;

        public CacheKey(String serverUri, String clientId) {
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
            this.clientId = clientId;
        }

        public String getServerUri() {
            return serverUri;
        }

        public String getClientId() {
            return clientId;
        }

        @Override
        public int hashCode() {
            if (clientId == null) {
                return serverUri.hashCode();
            } else {
                return (serverUri + "@" + clientId).hashCode();
            }
        }

        @Override
        public boolean equals(Object arg) {
            if (arg == null || !(arg instanceof ClientCache)) {
                return false;
            }
            CacheKey compared = (CacheKey) arg;
            if (!serverUri.equals(compared.getServerUri())) {
                return false;
            }
            if (clientId == null) {
                return compared.getClientId() == null;
            }
            return clientId.equals(compared);
        }
    }
    private static class CacheValue{
        MqttAsyncClient client;
        IMqttToken connectionToken;
        public CacheValue(MqttAsyncClient client, IMqttToken connectionToken){
            this.client = client;
            this.connectionToken = connectionToken;
        }
    }


    public MqttAsyncClient get(String serverUri, String clientId, boolean onlyConnected) throws MqttException {
        CacheKey key = new CacheKey(serverUri, clientId);
        CacheValue info = map.get(key);
        if(info == null){
            register(serverUri, clientId, getDefaultConnectOptions());
            info = map.get(key);
        }
        if(onlyConnected){
            if(!info.connectionToken.isComplete()) return null;
            MqttException exception = info.connectionToken.getException();
            if(exception != null) throw exception;
        }
        return info.client;
    }

    public MqttAsyncClient get(String serverUri, boolean onlyConnected) throws MqttException {
        return get(serverUri, null, onlyConnected);
    }

    private MqttAsyncClient register(String serverUri, String clientId, MqttConnectOptions connectOptions) throws MqttException {
        CacheKey key = new CacheKey(serverUri, clientId);
        if (StringUtils.isNullOrEmpty(clientId)) {
            clientId = MqttAsyncClient.generateClientId();
        }
        MqttAsyncClient newClient = new MqttAsyncClient(serverUri, clientId);
        IMqttToken token = newClient.connect(connectOptions);
        map.put(key, new CacheValue(newClient, token));
        return newClient;

    }

    private MqttConnectOptions getDefaultConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        return options;
    }

    public void assureFinalized() {
        for (CacheValue info : map.values()) {
            try {
                info.client.disconnect();
            } catch (MqttException e) {
            }
        }
        map.clear();
    }

    private HashMap<CacheKey, CacheValue> map = new HashMap<>();

}
