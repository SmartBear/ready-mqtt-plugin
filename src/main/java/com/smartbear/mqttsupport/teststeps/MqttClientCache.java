package com.smartbear.mqttsupport.teststeps;

import com.smartbear.mqttsupport.connection.ClientCache;

import java.util.concurrent.ConcurrentHashMap;

public enum MqttClientCache {
    INSTANCE;
    private final ConcurrentHashMap<String, ClientCache> clientCache = new ConcurrentHashMap<>();

    public ClientCache getOrCreate(String key) {
        return clientCache.computeIfAbsent(key, a -> new ClientCache());
    }

    public void purge() {
        clientCache.values().forEach(ClientCache::assureFinalized);
    }
}
