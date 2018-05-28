package com.smartbear.mqttsupport.connection;

public interface ConnectionsListener {
    public void connectionListChanged();
    public void connectionChanged(Connection connection, String propertyName, Object oldPropertyValue, Object newPropertyValue);
}
