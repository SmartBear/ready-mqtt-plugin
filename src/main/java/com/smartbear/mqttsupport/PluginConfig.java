package com.smartbear.mqttsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;
import com.eviware.soapui.support.UISupport;

@PluginConfiguration(groupId = "com.smartbear.plugins", name = "MQTT Support Plugin", version = "1.0.1",
        autoDetect = true, description = "Adds MQTT TestSteps to SoapUI NG",
        infoUrl = "")
public class PluginConfig extends PluginAdapter {

    public final static int DEFAULT_TCP_PORT = 1883;
    public final static int DEFAULT_SSL_PORT = 8883;
    public final static String LOGGER_NAME = "MQTT Plugin";
    public PluginConfig(){
        super();
        UISupport.addResourceClassLoader(getClass().getClassLoader());
    }
}
