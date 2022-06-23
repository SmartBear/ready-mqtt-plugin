package com.smartbear.mqttsupport.teststeps;

import com.smartbear.mqttsupport.connection.ClientCache;

import java.util.HashMap;

public enum MqttClientCache {
    INSTANCE;
    private final HashMap<String, ClientCache> clientCache = new HashMap<>();

    public synchronized ClientCache getOrCreate(String key) {
        return clientCache.computeIfAbsent(key, a -> new ClientCache());
    }

    public synchronized void purge() {
        clientCache.values().forEach(ClientCache::assureFinalized);
        clientCache.clear();
    }
}
