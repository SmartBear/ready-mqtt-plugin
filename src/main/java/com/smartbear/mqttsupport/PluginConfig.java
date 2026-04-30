package com.smartbear.mqttsupport;

import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;
import com.eviware.soapui.support.UISupport;
@PluginConfiguration(groupId = "com.smartbear.plugins", name = "MQTT Support Plugin", version = "2.1.0-SNAPSHOT",
        autoDetect = true, description = "Adds MQTT TestSteps to ReadyAPI", minimumReadyApiVersion = "4.0.0-SNAPSHOT",
        infoUrl = "https://support.smartbear.com/readyapi/docs/integrations/mqtt.html")
public class PluginConfig extends PluginAdapter {

    public static final int DEFAULT_TCP_PORT = 1883;
    public static final int DEFAULT_SSL_PORT = 8883;
    public static final String LOGGER_NAME = "MQTT Plugin";

    public PluginConfig(){
        super();
        UISupport.addResourceClassLoader(getClass().getClassLoader());
    }
}