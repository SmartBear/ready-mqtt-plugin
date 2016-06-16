package com.smartbear.mqttsupport;

import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStep;
import com.eviware.soapui.plugins.recipe.PluginTestStepConfigurator;
import com.eviware.soapui.plugins.recipe.TestStepConfigurator;

import java.util.Map;

@PluginTestStepConfigurator(typeName = "MQTTPublishTestStep", description = "")
public class MqttTestStepRecipeConfigParser implements TestStepConfigurator {
    @Override
    public void applyConfig(WsdlTestStep wsdlTestStep, Map<String, Object> map) {
        MqttConnectedTestStep mqttConnectedTestStep = (MqttConnectedTestStep) wsdlTestStep;
        mqttConnectedTestStep.setConnection(new Connection("Local", new ConnectionParams()));
        mqttConnectedTestStep.setServerUri((String) map.get(MqttConnectedTestStep.SERVER_URI_PROP_NAME));
        mqttConnectedTestStep.setClientId((String) map.get(MqttConnectedTestStep.CLIENT_ID_PROP_NAME));
    }
}
