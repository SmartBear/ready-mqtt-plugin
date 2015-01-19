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
import org.opensaml.xml.encryption.OriginatorKeyInfo;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;

@PluginTestStep(typeName = "MQTTPublishTestStep", name = "Publish using MQTT", description = "Publishes a specified message through MQTT protocol.")
public class PublishTestStep extends WsdlTestStepWithProperties {

    private final static String SERVER_URI_PROP_NAME = "ServerURI";

    private String serverUri;
    private boolean useFixedClientId;
    private String fixedClientId;
    private int timeout;
    private int qos;

    private static boolean actionGroupAdded = false;

    public PublishTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
        super(testCase, config, true, forLoadTest);
        if (!actionGroupAdded) {
            SoapUI.getActionRegistry().addActionGroup(new PublishTestStepActionGroup());
            actionGroupAdded = true;
        }
        if (config != null && config.getConfig() != null) {
            readData(config);
        }
        addProperty(new TestStepBeanProperty(SERVER_URI_PROP_NAME, false, this, "serverUri", this));
    }

    private void readData(TestStepConfig config) {
        XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(config.getConfig());
        serverUri = reader.readString(SERVER_URI_PROP_NAME, "");
    }


    private void updateData(TestStepConfig config) {
        XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
        builder.add(SERVER_URI_PROP_NAME, serverUri);
        if (config.getConfig() != null) {
            config.setConfig(builder.finish());
        }

    }

    private void updateData() {
        updateData(getConfig());
    }

    private boolean checkProperties(WsdlTestStepResult result) {
        boolean ok = true;
        if (StringUtils.isNullOrEmpty(serverUri)) {
            result.addMessage("The Server URI is not specified for the test step.");
            ok = false;
        } else {
            URI uri;
            try {
                uri = new URI(serverUri);
            } catch (URISyntaxException e) {
                result.addMessage("The string specified as Server URI is not a valid URI.");
                ok = false;
            }
        }
        if (useFixedClientId && StringUtils.isNullOrEmpty(fixedClientId)) {
            result.addMessage("The Client ID is not specified in the test step properties.");
            ok = false;
        }
        return ok;
    }

    private boolean waitForMqttOperation(IMqttToken token, TestCaseRunner testRunner, WsdlTestStepResult testStepResult, long startTime) {
        while (!token.isComplete() && token.getException() == null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {

            }
            if (!testRunner.isRunning() || (timeout != 0 && (System.nanoTime() - startTime) / 1000000 >= timeout)) {
                if (testRunner.isRunning()) {
                    testStepResult.addMessage("The test step's timeout has expired");
                }
                try {
                    token.getClient().disconnect();
                } catch (MqttException e) {
                }
                return false;
            }
        }
        if (token.getException() != null) {
            testStepResult.setError(token.getException());
            try {
                token.getClient().disconnect();
            } catch (MqttException e) {
            }
            return false;
        }
        return true;
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
                String clientId;
                if (useFixedClientId) {
                    clientId = fixedClientId;
                } else {
                    clientId = MqttAsyncClient.generateClientId();
                }
                long starTime = System.nanoTime();
                MqttAsyncClient client = new MqttAsyncClient(serverUri, clientId);
                MqttConnectOptions connectionOptions = new MqttConnectOptions();
                connectionOptions.setConnectionTimeout(timeout);
                connectionOptions.setCleanSession(true); //???

                if (!waitForMqttOperation(client.connect(connectionOptions), testRunner, result, starTime)) {
                    return result;
                }
                //if(!waitForMqttOperation(client.publish(topic, ))) return result;

                success = true;
            } catch (Throwable e) {
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
        ClientCache.assureFinalized();
        super.finish(testRunner, testRunContext);
    }

    public String getServerUri() {
        return serverUri;
    }

    public void setServerUri(String value) {
//        String old = getServerUri();
//        if(old == null && value == null) return;
//        if(old.equals(value)) return;
//        serverUri = value;
//        updateData();
//        notifyPropertyChanged("serverUri", old, value);
//        firePropertyValueChanged(SERVER_URI_PROP_NAME, old, value);
        setStringProperty("serverUri", SERVER_URI_PROP_NAME, value);
    }

    private void setStringProperty(String propName, String publishedPropName, String value) {
        String old;
        try {
            Field field = getClass().getField(propName);
            field.setAccessible(true);
            old = (String) (field.get(this));

            if (old == null && value == null) {
                return;
            }
            if (old.equals(value)) {
                return;
            }
            field.set(this, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)", propName, e.getMessage() + ")")); //We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        firePropertyValueChanged(publishedPropName, old, value);

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
