package com.smartbear.mqttsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
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
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import org.apache.bcel.generic.RETURN;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;



import javax.xml.transform.Result;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@PluginTestStep(typeName = "MQTTReceiveTestStep", name = "Receive MQTT Message", description = "Waits for a MQTT message of a specific topic.")
public class ReceiveTestStep extends MqttConnectedTestStep implements Assertable {

    public enum UnexpectedTopicBehavior implements MqttConnectedTestStepPanel.UIOption {
        Discard("Discard unexpected messages"), Ignore("Ignore (defer) unexpected messages"), Fail("Fail");
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
    private UnexpectedTopicBehavior onUnexpectedTopic = UnexpectedTopicBehavior.Ignore;
    private MessageType expectedMessageType = MessageType.Utf8Text;

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
        builder.add(EXPECTED_MESSAGE_TYPE_PROP_NAME, expectedMessageType.name());
        builder.add(ON_UNEXPECTED_TOPIC_PROP_NAME, onUnexpectedTopic.name());
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

    private boolean topicCorrespondsFilters(String topic, String[] filters){
        return false;
    }

    private boolean storeMessage(Client.Message message){
        return false;
    }


    private String[] diffOfStringSets(String[] minuend, ArrayList<String> subtrahend){
        ArrayList<String> resultList = new ArrayList<String>();
        for(String s: minuend){
            boolean presentsEverywhere = false;
            for(String s2: subtrahend) {
                if (Utils.areStringsEqual(s, s2)) {
                    presentsEverywhere = true;
                    break;
                }
            }
            if(!presentsEverywhere) resultList.add(s);
        }
        String[] result = new String[resultList.size()];
        resultList.toArray(result);
        return result;
    }

    @Override
    public TestStepResult run(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        WsdlTestStepResult result = new WsdlTestStepResult(this);
        result.startTimer();
        boolean success = false;

        try {
            try {
                String actualBrokerUri = testRunContext.expand(getServerUri());
                ConnectionParams actualConnectionParams =  getConnectionParams(testRunContext);
                Client client = getCache(testRunContext).get(actualBrokerUri, actualConnectionParams);

                String[] neededTopics = listenedTopics.split("[\\r\\n]+");
                if(neededTopics == null || neededTopics.length == 0){
                    result.addMessage("The specified listened topic list is empty.");
                    return result;
                }

                for(int i = 0; i < neededTopics.length; ++i){
                    neededTopics[i] = testRunContext.expand(neededTopics[i].trim());
                }

                long starTime = System.nanoTime();
                long maxTime = getTimeout() == 0 ? Long.MAX_VALUE : starTime + (long)getTimeout() * 1000 * 1000;

                int connectAttemptCount = 0;
                MqttAsyncClient clientObj = client.getClientObject();
                ArrayList<String> activeSubscriptions = client.getCachedSubscriptions();
                String[] requiredSubscriptions = diffOfStringSets(neededTopics, activeSubscriptions);
                if(requiredSubscriptions.length > 0) {
                    if(!client.isConnected()) {
                        ++connectAttemptCount;
                        if(!waitForMqttConnection(actualBrokerUri, actualConnectionParams, testRunner, testRunContext, result, maxTime)) return result;
                        activeSubscriptions = client.getCachedSubscriptions();
                        requiredSubscriptions = diffOfStringSets(neededTopics, activeSubscriptions);
                    }
                    int[] qosArray = new int[requiredSubscriptions.length];
                    Arrays.fill(qosArray, qos);
                    if (!waitForMqttOperation(clientObj.subscribe(requiredSubscriptions, qosArray), testRunner, result, maxTime, "Attempt to subscribe on the specified topics failed.")) {
                        return result;
                    }
                }

                Client.Message suitableMsg = null;
                MessageQueue messageQueue = client.getMessageQueue();
                messageQueue.setCurrentMessageToHead();
                while (System.nanoTime() <= maxTime && testRunner.isRunning()){
                    Client.Message msg = messageQueue.getMessage();
                    if(msg != null){
                        if(topicCorrespondsFilters(msg.topic, neededTopics)){
                            suitableMsg = msg;
                            messageQueue.removeCurrentMessage();
                            break;
                        }
                        switch(onUnexpectedTopic){
                            case Fail:
                                result.addMessage(String.format("\"%s\" topic of the received message does not correspond to any filter", msg.topic));
                                return result;
                            case Discard:
                                messageQueue.removeCurrentMessage();
                                messageQueue.setCurrentMessageToHead();
                                break;
                        }
                    }
                    else{
                        if(!client.isConnected() && connectAttemptCount == 0){
                            if(!waitForMqttConnection(actualBrokerUri, actualConnectionParams, testRunner, testRunContext, result, maxTime)) return result;
                            ++connectAttemptCount;
                        }
                    }
                }
                if(!testRunner.isRunning()){
                    return result;
                }
                if(suitableMsg == null) {
                    result.addMessage("The test step's timeout has expired");
                    return result;
                }
                else{
                    if (!storeMessage(suitableMsg)) return result;
                }
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
