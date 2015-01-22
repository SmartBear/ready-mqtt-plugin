package com.smartbear.mqttsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStep;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.opensaml.xml.encryption.OriginatorKeyInfo;
import org.xmlsoap.schemas.wsdl.soap.THeader;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;

@PluginTestStep(typeName = "MQTTPublishTestStep", name = "Publish using MQTT", description = "Publishes a specified message through MQTT protocol.")
public class PublishTestStep extends MqttConnectedTestStep {



    private int timeout;
    private int qos;
    private String topic;

    private static boolean actionGroupAdded = false;

    public PublishTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
        super(testCase, config, true, forLoadTest);
        if (!actionGroupAdded) {
            SoapUI.getActionRegistry().addActionGroup(new PublishTestStepActionGroup());
            actionGroupAdded = true;
        }
    }


    private boolean checkProperties(WsdlTestStepResult result) {
        boolean ok = true;
        if (StringUtils.isNullOrEmpty(getServerUri())) {
            result.addMessage("The Server URI is not specified for the test step.");
            ok = false;
        } else {
            URI uri;
            try {
                uri = new URI(getServerUri());
            } catch (URISyntaxException e) {
                result.addMessage("The string specified as Server URI is not a valid URI.");
                ok = false;
            }
        }
//        if (useFixedClientId && StringUtils.isNullOrEmpty(fixedClientId)) {
//            result.addMessage("The Client ID is not specified in the test step properties.");
//            ok = false;
//        }
        return ok;
    }

    private boolean waitForMqttOperation(IMqttToken token, TestCaseRunner testRunner, WsdlTestStepResult testStepResult, long startTime) {
        while (!token.isComplete() && token.getException() == null) {
            if (!testRunner.isRunning() || (timeout != 0 && (System.nanoTime() - startTime) / 1000000 >= timeout)) {
                if (testRunner.isRunning()) {
                    testStepResult.addMessage("The test step's timeout has expired");
                }
                return false;
            }
        }
        if (token.getException() != null) {
            testStepResult.setError(token.getException());
            return false;
        }
        return true;
    }

    private ClientCache getCache(TestCaseRunContext testRunContext){
        final String CLIENT_CACHE_PROPNAME = "client_cache";
        ClientCache cache = (ClientCache)(testRunContext.getProperty(CLIENT_CACHE_PROPNAME));
        if(cache == null){
            cache = new ClientCache();
            testRunContext.setProperty(CLIENT_CACHE_PROPNAME, cache);
        }
        return cache;
    }

    private MqttAsyncClient waitForMqttClient(TestCaseRunner testRunner, TestCaseRunContext testRunContext, WsdlTestStepResult testStepResult, long startTime) throws MqttException{
        ClientCache cache = getCache(testRunContext);
        if(waitForMqttOperation(cache.getConnectionStatus(getServerUri(), getConnectionParams()), testRunner, testStepResult, startTime)){
            return cache.get(getServerUri(), getConnectionParams());
        }
        else{
            return null;
        }
    }

    @Override
    public TestStepResult run(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        WsdlTestStepResult result = new WsdlTestStepResult(this);
        result.startTimer();
        boolean success = false;

        try {
            try {
                if (!checkProperties(result)) {
                    return result;
                }
                long starTime = System.nanoTime();
                MqttAsyncClient client = waitForMqttClient(testRunner, testRunContext, result, starTime);
                if(client == null) return result;

                MqttMessage message = new MqttMessage();
                if(!waitForMqttOperation(client.publish(topic, message), testRunner, result, starTime)) return result;

                success = true;
            } catch (MqttException e) {
                result.setError(e);
            }
            return result;
        } finally {
            result.stopTimer();
            result.setStatus(success ? TestStepResult.TestStepStatus.OK : TestStepResult.TestStepStatus.FAILED);
        }
    }

    @Override
    public void finish(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        getCache(testRunContext).assureFinalized();
        super.finish(testRunner, testRunContext);
    }


//    @Override
//    public void onSuccess(IMqttToken asyncActionToken) {
//
//    }
//
//    @Override
//    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//
//    }
//

}
