package com.smartbear.mqttsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansion;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionUtils;
import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.security.boundary.IntegerBoundary;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

import com.google.common.base.Charsets;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.validation.Payload;
import javax.xml.transform.Result;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@PluginTestStep(typeName = "MQTTPublishTestStep", name = "Publish using MQTT", description = "Publishes a specified message through MQTT protocol.")
public class PublishTestStep extends MqttConnectedTestStep {
    private final static String MESSAGE_KIND_PROP_NAME = "MessageKind";
    private final static String TOPIC_PROP_NAME = "Topic";
    private final static String MESSAGE_PROP_NAME = "Message";
    private final static String QOS_PROP_NAME = "QoS";
    private final static String RETAINED_PROP_NAME = "Retained";

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

        addProperty(new DefaultTestStepProperty(TIMEOUT_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler(){
            @Override
            public String getValue(DefaultTestStepProperty property) {
                return Integer.toString(getTimeout());
            }

            @Override
            public void setValue(DefaultTestStepProperty property, String value) {
                int newTimeout;
                try{
                    newTimeout = Integer.parseInt(value);
                }
                catch (NumberFormatException e){
                    return;
                }
                setTimeout(newTimeout);
            }

        }, this));
        addProperty(new DefaultTestStepProperty(QOS_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty property) {
                return Integer.toString(qos);
            }

            @Override
            public void setValue(DefaultTestStepProperty property, String value) {
                int newQos;
                try{
                    newQos = Integer.parseInt(value);
                }
                catch (NumberFormatException e){
                    return;
                }
                setQos(newQos);
            }
        }, this));

    }


    private boolean checkProperties(WsdlTestStepResult result, String serverUri, String topicToCheck, MessageType messageTypeToCheck, String messageToCheck) {
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
        if(StringUtils.isNullOrEmpty(topicToCheck)){
            result.addMessage("The topic of message is not specified");
            ok = false;
        }
        if(messageTypeToCheck == null){
            result.addMessage("The message format is not specified.");
            ok = false;
        }
        if(StringUtils.isNullOrEmpty(messageToCheck) && (messageTypeToCheck != MessageType.Utf16Text) && (messageTypeToCheck != MessageType.Utf8Text)){
            if(messageTypeToCheck == MessageType.BinaryFile) result.addMessage("A file which contains a message is not specified"); else result.addMessage("A message content is not specified.");
            ok = false;
        }

        return ok;
    }


    private byte[] formPayload(WsdlTestStepResult errorsStorage, MessageType messageType, String msg){
        byte[] buf;
        switch(messageType){
            case Utf8Text:
                if(msg == null) return new byte[0]; else return msg.getBytes(Charsets.UTF_8);
            case Utf16Text:
                if(msg == null) return new byte[0]; else return msg.getBytes(Charsets.UTF_16);
            case IntegerValue:
                int iv;
                try{
                    iv = Integer.parseInt(msg);
                }
                catch (NumberFormatException e){
                    errorsStorage.addMessage(String.format("The specified text (\"%s\") cannot be published as an integer value.", msg));
                    return null;
                }
                buf = new byte[4];
                for(int i = 0; i < 4; ++i){
                    buf[i] = (byte)((iv >> ((3 - i) * 8)) & 0xff);
                }
                return buf;
            case LongValue:
                long lv;
                try{
                    lv = Long.parseLong(msg);
                }
                catch (NumberFormatException e){
                    errorsStorage.addMessage(String.format("The specified text (\"%s\") cannot be published as a long value.", msg));
                    return null;
                }
                buf = new byte[8];
                for(int i = 0; i < 8; ++i){
                    buf[i] = (byte)((lv >> ((7 - i) * 8)) & 0xff);
                }
                return buf;
            case DoubleValue :
                buf = new byte[8];
                double dv;
                try{
                    dv = Double.parseDouble(msg);
                }
                catch(NumberFormatException e){
                    errorsStorage.addMessage(String.format("The specified text (\"%s\") cannot be published as a double value.", msg));
                    return null;
                }
                long rawD = Double.doubleToLongBits(dv);
                for(int i = 0; i < 8; ++i){
                    buf[i] = (byte)((rawD >> ((7 - i) * 8)) & 0xff);
                }
                return buf;

            case FloatValue:
                buf = new byte[4];
                float fv;
                try{
                    fv = Float.parseFloat(msg);
                }
                catch(NumberFormatException e){
                    errorsStorage.addMessage(String.format("The specified text (\"%s\") cannot be published as a float value.", msg));
                    return null;
                }
                int rawF = Float.floatToIntBits(fv);
                for(int i = 0; i < 4; ++i){
                    buf[i] = (byte)((rawF >> ((3 - i) * 8)) & 0xff);
                }
                return buf;
            case BinaryFile:
                File file = null;
                try {
                    file = new File(msg);
                    if (!file.isAbsolute()) {
                        file = new File(new File(getProject().getPath()).getParent(), file.getPath());
                    }
                    if (!file.exists()) {
                        errorsStorage.addMessage(String.format("Unable to find \"%s\" file which contains a published message", file.getPath()));
                        return null;
                    }
                    int fileLen = (int) file.length();
                    buf = new byte[fileLen];
                    FileInputStream stream = new FileInputStream(file);
                    stream.read(buf);
                    return buf;
                }
                catch(RuntimeException | IOException e){
                    errorsStorage.addMessage(String.format("Attempt of access to \"%s\" file with a published message has failed.", file.getPath()));
                    errorsStorage.setError(e);
                    return null;
                }

        }
        errorsStorage.addMessage("The format of the published message is not specified or unknown."); //We won't be here
        return null;

    }


    @Override
    public TestStepResult run(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        WsdlTestStepResult result = new WsdlTestStepResult(this);
        result.startTimer();
        boolean success = false;

        try {
            try {
                String expandedUri = testRunContext.expand(getServerUri());
                String expandedMessage = testRunContext.expand(message);
                String expandedTopic = testRunContext.expand(topic);

                if (!checkProperties(result, expandedUri, expandedTopic, messageKind, expandedMessage)) {
                    return result;
                }
                long starTime = System.nanoTime();
                long maxTime = getTimeout() == 0 ? Long.MAX_VALUE : starTime + (long)getTimeout() * 1000 * 1000;

                byte[] payload = formPayload(result, messageKind, expandedMessage);
                if(payload == null) return result;

                MqttAsyncClient client = waitForMqttClient(testRunner, testRunContext, result, maxTime);
                if(client == null) return result;

                MqttMessage message = new MqttMessage();
                message.setRetained(retained);
                message.setQos(qos);
                message.setPayload(payload);
                if(!waitForMqttOperation(client.publish(expandedTopic, message), testRunner, result, maxTime, "Attempt to publish the message failed.")) return result;

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
        setProperty("topic", TOPIC_PROP_NAME, newValue);
    }

    public String getMessage(){return message;}

    public void setMessage(String value){
        try {
            switch (messageKind) {
                case IntegerValue:
                    Integer.parseInt(value);
                    break;
                case LongValue:
                    Long.parseLong(value);
                    break;
            }
        }
        catch(NumberFormatException e){
                return;
        }
        setProperty("message", MESSAGE_PROP_NAME, value);
    }

    public int getQos(){return qos;}
    public void setQos(int newValue){setIntProperty("qos", QOS_PROP_NAME, newValue, 0, 2);}

    public boolean getRetained(){return retained;}
    public void setRetained(boolean value){setBooleanProperty("retained", RETAINED_PROP_NAME, value);}

    @Override
    protected void readData(XmlObjectConfigurationReader reader){
        super.readData(reader);
        int messageKindNo = reader.readInt(MESSAGE_KIND_PROP_NAME, messageKind.ordinal());
        if(messageKindNo >= 0 && messageKindNo < MessageType.values().length) messageKind = MessageType.values()[messageKindNo];
        topic = reader.readString(TOPIC_PROP_NAME, "");
        message = reader.readString(MESSAGE_PROP_NAME, "");
        qos = reader.readInt(QOS_PROP_NAME, 0);
        retained = reader.readBoolean(RETAINED_PROP_NAME, false);
    }


    @Override
    protected void writeData(XmlObjectConfigurationBuilder builder){
        super.writeData(builder);
        if(messageKind != null) builder.add(MESSAGE_KIND_PROP_NAME, messageKind.ordinal());
        builder.add(TOPIC_PROP_NAME, topic);
        builder.add(MESSAGE_PROP_NAME, message);
        builder.add(QOS_PROP_NAME, qos);
        builder.add(RETAINED_PROP_NAME, retained);
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
