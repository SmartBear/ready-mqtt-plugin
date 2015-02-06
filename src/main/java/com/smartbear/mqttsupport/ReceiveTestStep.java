package com.smartbear.mqttsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.TestAssertionRegistry;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionsListener;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;


import java.util.List;
import java.util.Map;

@PluginTestStep(typeName = "MQTTReceiveTestStep", name = "Receive MQTT Message", description = "Waits for a MQTT message of a specific topic.")
public class ReceiveTestStep extends MqttConnectedTestStep implements Assertable {

    public enum UnexpectedTopicBehavior implements MqttConnectedTestStepPanel.UIOption {
        Ignore("Ignore unexpected messages"), Fail("Fail");
        private String title;
        @Override
        public String getTitle(){return title;}
        UnexpectedTopicBehavior(String title){
            this.title = title;
        }
        public static UnexpectedTopicBehavior fromString(String str){
            if(str == null) return null;
            for (UnexpectedTopicBehavior m : UnexpectedTopicBehavior.values()) {
                if (m.toString().equals(str)) {
                    return m;
                }
            }
            return null;
        }
    }

    enum MessageType {
        Utf8Text("Text (UTF8)"), Utf16Text("Text (UTF16)"), BinaryData("Raw binary data"), IntegerNumber("Integer number"), FloatNumber("Float number");
        private String title;

        @Override
        public String toString(){return title;}

        MessageType(String title){
            this.title = title;
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

    private final static String QOS_PROP_NAME = "QoS";
    private final static String LISTENED_TOPICS_PROP_NAME = "ListenedTopics";
    private final static String EXPECTED_MESSAGE_TYPE_PROP_NAME = "ExpectedMessageType";
    private final static String ON_UNEXPECTED_TOPIC_PROP_NAME = "OnUnexpectedTopic";

    private static boolean actionGroupAdded = false;

    private String listenedTopics;
    private int qos;
    private UnexpectedTopicBehavior onUnexpectedTopic;
    private MessageType expectedMessageType;

    public ReceiveTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
        super(testCase, config, true, forLoadTest);
        if (!actionGroupAdded) {
            SoapUI.getActionRegistry().addActionGroup(new ReceiveTestStepActionGroup());
            actionGroupAdded = true;
        }
        if (config != null && config.getConfig() != null) {
            XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(config.getConfig());
            readData(reader);
        }

        addProperty(new TestStepBeanProperty(LISTENED_TOPICS_PROP_NAME, false, this, "listenedTopics", this));
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

    }

    @Override
    protected void readData(XmlObjectConfigurationReader reader) {
        super.readData(reader);
        listenedTopics = reader.readString(LISTENED_TOPICS_PROP_NAME, "");
        qos = reader.readInt(QOS_PROP_NAME, 0);
        try {
            expectedMessageType = MessageType.valueOf(reader.readString(EXPECTED_MESSAGE_TYPE_PROP_NAME, MessageType.Utf8Text.toString()));
        } catch (IllegalArgumentException | NullPointerException e){
            expectedMessageType = MessageType.Utf8Text;
        }
        try{
            onUnexpectedTopic = UnexpectedTopicBehavior.valueOf(reader.readString(ON_UNEXPECTED_TOPIC_PROP_NAME, UnexpectedTopicBehavior.Ignore.toString()));
        }
        catch (IllegalArgumentException | NullPointerException e){
            onUnexpectedTopic = UnexpectedTopicBehavior.Ignore;
        }

    }


    @Override
    protected void writeData(XmlObjectConfigurationBuilder builder) {
        super.writeData(builder);
        builder.add(LISTENED_TOPICS_PROP_NAME, listenedTopics);
        builder.add(QOS_PROP_NAME, qos);
        builder.add(EXPECTED_MESSAGE_TYPE_PROP_NAME, expectedMessageType.toString());
        builder.add(ON_UNEXPECTED_TOPIC_PROP_NAME, onUnexpectedTopic.toString());
    }

    public int getQos(){return qos;}
    public void setQos(int newValue){setIntProperty("qos", QOS_PROP_NAME, newValue, 0, 2);}

    public String getListenedTopics(){return listenedTopics;}
    public void setListenedTopics(String newValue){
        setProperty("listenedTopics", LISTENED_TOPICS_PROP_NAME, newValue);
    }

    public UnexpectedTopicBehavior getOnUnexpectedTopic(){
        return onUnexpectedTopic;
    }

    public void setOnUnexpectedTopic(UnexpectedTopicBehavior value){
        setProperty("onUnexpectedTopic", null, value);
    }

    public MessageType getExpectedMessageType(){return expectedMessageType;}
    public void setExpectedMessageType(MessageType value){
        setProperty("expectedMessageType", null, value);
//        if(expectedMessageType == value) return;
//        MessageType old = expectedMessageType;
//        expectedMessageType = value;
//        updateData();
//        notifyPropertyChanged("expectedMessageType", old, value);
//        //firePropertyValueChanged(MESSAGE_TYPE_PROP_NAME, old.toString(), value.toString());
    }

    @Override
    public TestStepResult run(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        return null;
    }

    @Override
    public TestAssertion addAssertion(String selection) {
        return null;
    }

    @Override
    public void addAssertionsListener(AssertionsListener listener) {

    }

    @Override
    public int getAssertionCount() {
        return 0;
    }

    @Override
    public TestAssertion getAssertionAt(int c) {
        return null;
    }

    @Override
    public void removeAssertionsListener(AssertionsListener listener) {

    }

    @Override
    public void removeAssertion(TestAssertion assertion) {

    }

    @Override
    public AssertionStatus getAssertionStatus() {
        return AssertionStatus.UNKNOWN;
    }

    @Override
    public String getAssertableContentAsXml() {
        return null;
    }

    @Override
    public String getAssertableContent() {
        return null;
    }

    @Override
    public String getDefaultAssertableContent() {
        return null;
    }

    @Override
    public TestAssertionRegistry.AssertableType getAssertableType() {
        return null;
    }

    @Override
    public List<TestAssertion> getAssertionList() {
        return null;
    }

    @Override
    public TestAssertion getAssertionByName(String name) {
        return null;
    }

    @Override
    public TestStep getTestStep() {
        return null;
    }

    @Override
    public Interface getInterface() {
        return null;
    }

    @Override
    public TestAssertion cloneAssertion(TestAssertion source, String name) {
        return null;
    }

    @Override
    public Map<String, TestAssertion> getAssertions() {
        return null;
    }

    @Override
    public TestAssertion moveAssertion(int ix, int offset) {
        return null;
    }
}
