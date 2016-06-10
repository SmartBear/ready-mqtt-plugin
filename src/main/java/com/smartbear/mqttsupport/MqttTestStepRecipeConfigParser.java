package com.smartbear.mqttsupport;

import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStep;
import com.eviware.soapui.plugins.recipe.PluginRecipeConfigParser;
import com.eviware.soapui.plugins.recipe.RecipeConfigParser;

import java.util.Map;

@PluginRecipeConfigParser(typeName = "MQTTPublishTestStep", description = "")
public class MqttTestStepRecipeConfigParser implements RecipeConfigParser {
    @Override
    public void parseConfig(WsdlTestStep wsdlTestStep, Map<String, String> map) {
        MqttConnectedTestStep mqttConnectedTestStep = (MqttConnectedTestStep) wsdlTestStep;
        mqttConnectedTestStep.setConnection(new Connection("Local", new ConnectionParams()));
        mqttConnectedTestStep.setServerUri(map.get(MqttConnectedTestStep.SERVER_URI_PROP_NAME));
        mqttConnectedTestStep.setClientId(map.get(MqttConnectedTestStep.CLIENT_ID_PROP_NAME));
    }
}
