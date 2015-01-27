package com.smartbear.mqttsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.net.URI;
import java.net.URISyntaxException;

@PluginTestStep(typeName = "MQTTPublishTestStep", name = "Publish using MQTT", description = "Publishes a specified message through MQTT protocol.")
public class PublishTestStep extends MqttConnectedTestStep {
    private final static String MESSAGE_KIND_PROP_NAME = "MessageKind";
    private final static String TOPIC_PROP_NAME = "Topic";
    private final static String MESSAGE_PROP_NAME = "Message";
    private final static String QOS_PROP_NAME = "QoS";
    private final static String RETAINED_PROP_NAME = "Retained";
    private final static String TIMEOUT_PROP_NAME = "Timeout";

    enum MessageType{
        Utf8Text("Text (UTF8)"), Utf16Text("Text (UTF16)"), BinaryFile("Content of file"), IntegerValue("Integer (4 bytes)"), LongValue("Long (8 bytes)"), FloatValue("Float"), DoubleValue("Double");
        private String name;
        private MessageType(String name){this.name = name;}
        @Override
        public String toString(){
            return name;
        }
        public static MessageType fromString(String s){
            if(s == null) return null;
            for (MessageType m : MessageType.values()) {
                if (m.toString().equals(s)) {
                    return m;
                }
            }
            return null;

        }
    }

    private MessageType messageKind = MessageType.Utf8Text;
    private String message;
    private String topic;

    private int timeout;
    private int qos;
    private boolean retained;

    private static boolean actionGroupAdded = false;

    public PublishTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
        super(testCase, config, true, forLoadTest);
        if (!actionGroupAdded) {
            SoapUI.getActionRegistry().addActionGroup(new PublishTestStepActionGroup());
            actionGroupAdded = true;
        }
        if (config != null && config.getConfig() != null) {
            XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(config.getConfig());
            readData(reader);
        }

        addProperty(new DefaultTestStepProperty(MESSAGE_KIND_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty property) {
                return messageKind.toString();
            }

            @Override
            public void setValue(DefaultTestStepProperty property, String value) {
                MessageType messageType = MessageType.fromString(value);
                if(messageType != null) messageKind = messageType;
            }
        }, this));
        addProperty(new TestStepBeanProperty(TOPIC_PROP_NAME, false, this, "topic", this));
        addProperty(new TestStepBeanProperty(MESSAGE_PROP_NAME, false, this, "message", this));

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
                message.setRetained(retained);
                message.setQos(qos);
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


    public MessageType getMessageKind(){return messageKind;}
    public void setMessageKind(MessageType newValue){
        if(messageKind == newValue) return;
        MessageType old = messageKind;
        messageKind = newValue;
        updateData();
        notifyPropertyChanged("messageKind", old, newValue);
        firePropertyValueChanged(MESSAGE_KIND_PROP_NAME, old.toString(), newValue.toString());
        String oldMessage = getMessage();
        if(oldMessage == null) oldMessage = "";
        try {
            switch (messageKind) {
                case IntegerValue:
                    Integer.parseInt(oldMessage);
                    break;
                case LongValue:
                    Long.parseLong(oldMessage);
                    break;
                case FloatValue:
                    Float.parseFloat(oldMessage);
                    break;
                case DoubleValue:
                    Double.parseDouble(oldMessage);
                    break;
            }
        }
        catch(NumberFormatException e){
            setMessage("0");
        }
    }

    public String getTopic(){
        return topic;
    }

    public void setTopic(String newValue){
        setStringProperty("topic", TOPIC_PROP_NAME, newValue);
    }

    public String getMessage(){return message;}

    public void setMessage(String value){setStringProperty("message", MESSAGE_PROP_NAME, value);}

    public int getQos(){return qos;}
    public void setQos(int newValue){setIntProperty("qos", QOS_PROP_NAME, newValue, 0, 2);}

    public boolean getRetained(){return retained;}
    public void setRetained(boolean value){setBooleanProperty("retained", RETAINED_PROP_NAME, value);}

    public int getTimeout(){return timeout;}
    public void setTimeout(int value){setIntProperty("timeout", TIMEOUT_PROP_NAME, value);}

    @Override
    protected void readData(XmlObjectConfigurationReader reader){
        super.readData(reader);
        int messageKindNo = reader.readInt(MESSAGE_KIND_PROP_NAME, messageKind.ordinal());
        if(messageKindNo >= 0 && messageKindNo < MessageType.values().length) messageKind = MessageType.values()[messageKindNo];
        topic = reader.readString(TOPIC_PROP_NAME, "");
        message = reader.readString(MESSAGE_PROP_NAME, "");
        qos = reader.readInt(QOS_PROP_NAME, 0);
        retained = reader.readBoolean(RETAINED_PROP_NAME, false);
        timeout = reader.readInt(TIMEOUT_PROP_NAME, 30000);
    }


    @Override
    protected void writeData(XmlObjectConfigurationBuilder builder){
        super.writeData(builder);
        if(messageKind != null) builder.add(MESSAGE_KIND_PROP_NAME, messageKind.ordinal());
        builder.add(TOPIC_PROP_NAME, topic);
        builder.add(MESSAGE_PROP_NAME, message);
        builder.add(QOS_PROP_NAME, qos);
        builder.add(RETAINED_PROP_NAME, retained);
        builder.add(TIMEOUT_PROP_NAME, timeout);
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
