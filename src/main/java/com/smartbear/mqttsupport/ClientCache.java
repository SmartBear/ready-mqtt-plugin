package com.smartbear.mqttsupport;

import com.eviware.soapui.support.StringUtils;
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

    public static MqttAsyncClient get(String serverUri, String clientId) throws MqttException {
        CacheKey key = new CacheKey(serverUri, clientId);
        if (map.containsKey(key)) {
            return map.get(key);
        }
        return register(serverUri, clientId, getDefaultConnectOptions());
    }

    public static MqttAsyncClient get(String serverUri) throws MqttException {
        return get(serverUri, null);
    }

    public static MqttAsyncClient register(String serverUri, String clientId, MqttConnectOptions connectOptions) throws MqttException {
        CacheKey key = new CacheKey(serverUri, clientId);
        if (StringUtils.isNullOrEmpty(clientId)) {
            clientId = MqttAsyncClient.generateClientId();
        }
        MqttAsyncClient newClient = new MqttAsyncClient(serverUri, clientId);
        map.put(key, newClient);
        return newClient;

    }

    private static MqttConnectOptions getDefaultConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        return options;
    }

    public static void assureFinalized() {
        for (MqttAsyncClient client : map.values()) {
            try {
                client.disconnect();
            } catch (MqttException e) {
            }
        }
        map.clear();
    }

    private static HashMap<CacheKey, MqttAsyncClient> map = new HashMap<>();

}
