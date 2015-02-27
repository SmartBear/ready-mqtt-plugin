package com.smartbear.mqttsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.support.IconAnimator;
import com.eviware.soapui.impl.wsdl.support.assertions.AssertableConfig;
import com.eviware.soapui.impl.wsdl.support.assertions.AssertionsSupport;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestRunContext;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.TestAssertionRegistry;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.iface.Attachment;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.iface.Operation;
import com.eviware.soapui.model.iface.Response;
import com.eviware.soapui.model.mock.MockRunner;import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionsListener;
import com.eviware.soapui.model.testsuite.LoadTestRunner;import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.monitor.TestMonitorListener;import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.security.SecurityTestRunner;import com.eviware.soapui.support.StringUtils;import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.soapui.support.types.StringToStringsMap;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.google.common.base.Charsets;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import javax.swing.ImageIcon;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;import java.net.URISyntaxException;import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@PluginTestStep(typeName = "MQTTReceiveTestStep", name = "Receive MQTT Message", description = "Waits for a MQTT message of a specific topic.", iconPath = "com/smartbear/mqttsupport/receive_step.png")
public class ReceiveTestStep extends MqttConnectedTestStep implements Assertable, PropertyChangeListener, TestMonitorListener {

public enum UnexpectedTopicBehavior implements MqttConnectedTestStepPanel.UIOption {
        Discard("Discard unexpected messages"), Ignore("Ignore (defer) unexpected messages"), Fail("Fail");
        private String title;

        @Override
        public String getTitle() {
            return title;
        }

        UnexpectedTopicBehavior(String title) {
            this.title = title;
        }

        public static UnexpectedTopicBehavior fromString(String str) {
            if (str == null) {
                return null;
            }
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
        public String toString() {
            return title;
        }

        MessageType(String title) {
            this.title = title;
        }

        public static MessageType fromString(String s) {
            if (s == null) {
                return null;
            }
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
    private final static String RECEIVED_MESSAGE_PROP_NAME = "ReceivedMessage";
    private final static String RECEIVED_TOPIC_PROP_NAME = "ReceivedMessageTopic";

    private final static String ASSERTION_SECTION = "assertion";

    private static boolean actionGroupAdded = false;

    private String listenedTopics;
    private int qos;
    private UnexpectedTopicBehavior onUnexpectedTopic = UnexpectedTopicBehavior.Ignore;
    private MessageType expectedMessageType = MessageType.Utf8Text;

    private String receivedMessage = null;
    private String receivedMessageTopic = null;
    private AssertionsSupport assertionsSupport;
    private AssertionStatus assertionStatus = AssertionStatus.UNKNOWN;
    private ArrayList<TestAssertionConfig> assertionConfigs = new ArrayList<TestAssertionConfig>();

    private ImageIcon validStepIcon;
    private ImageIcon failedStepIcon;
    private ImageIcon disabledStepIcon;
    private ImageIcon unknownStepIcon;
    private IconAnimator<ReceiveTestStep> iconAnimator;


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
        initAssertions(config);

        addProperty(new TestStepBeanProperty(LISTENED_TOPICS_PROP_NAME, false, this, "listenedTopics", this));
        addProperty(new DefaultTestStepProperty(QOS_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty property) {
                return Integer.toString(qos);
            }

            @Override
            public void setValue(DefaultTestStepProperty property, String value) {
                int newQos;
                try {
                    newQos = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return;
                }
                setQos(newQos);
            }
        }, this));
        addProperty(new DefaultTestStepProperty(TIMEOUT_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty property) {
                return Integer.toString(getTimeout());
            }

            @Override
            public void setValue(DefaultTestStepProperty property, String value) {
                int newTimeout;
                try {
                    newTimeout = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return;
                }
                setTimeout(newTimeout);
            }

        }, this));

        addProperty(new TestStepBeanProperty(RECEIVED_MESSAGE_PROP_NAME, true, this, "receivedMessage", this));
        addProperty(new TestStepBeanProperty(RECEIVED_TOPIC_PROP_NAME, true, this, "receivedMessageTopic", this));

        if (!forLoadTest) {
            initIcons();
        }
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null){
            testMonitor.addTestMonitorListener(this);
        }
        updateState();
    }

    protected void initIcons() {
        validStepIcon = UISupport.createImageIcon("com/smartbear/mqttsupport/valid_receive_step.png");
        failedStepIcon = UISupport.createImageIcon("com/smartbear/mqttsupport/invalid_receive_step.png");
        unknownStepIcon = UISupport.createImageIcon("com/smartbear/mqttsupport/unknown_receive_step.png");
        disabledStepIcon = UISupport.createImageIcon("com/smartbear/mqttsupport/disabled_receive_step.png");

        iconAnimator =  new IconAnimator<ReceiveTestStep>(this, "com/smartbear/mqttsupport/receive_step_base.png", "com/smartbear/mqttsupport/receive_step.png", 5);
    }

    private void initAssertions(TestStepConfig testStepData) {
        if(testStepData != null && testStepData.getConfig() != null){
            XmlObject config = testStepData.getConfig();
            XmlObject[] assertionsSections = config.selectPath("$this/" + ASSERTION_SECTION);
            for(XmlObject assertionSection: assertionsSections){
//                //TestAssertionConfig assertionConfig = TestAssertionConfig.Factory.newInstance();
//                //assertionConfig.set(assertionSection);
//                TestAssertionConfig assertionConfig = (TestAssertionConfig)( assertionSection.changeType(TestAssertionConfig.type));
                TestAssertionConfig assertionConfig;
                try {
                    assertionConfig = TestAssertionConfig.Factory.parse(assertionSection.toString());
                }
                catch(XmlException e){
                    SoapUI.logError(e);
                    continue;
                }
                assertionConfigs.add(assertionConfig);
            }
        }
        assertionsSupport = new AssertionsSupport(this, new AssertableConfigImpl());
    }

    @Override
    public void setIcon(ImageIcon newIcon){
        if(iconAnimator != null && newIcon == iconAnimator.getBaseIcon()) return;
        super.setIcon(newIcon);
    }

    @Override
    public void release(){
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null) testMonitor.removeTestMonitorListener(this);
        super.release();
    }

    @Override
    protected void readData(XmlObjectConfigurationReader reader) {
        super.readData(reader);
        listenedTopics = reader.readString(LISTENED_TOPICS_PROP_NAME, "");
        qos = reader.readInt(QOS_PROP_NAME, 0);
        try {
            expectedMessageType = MessageType.valueOf(reader.readString(EXPECTED_MESSAGE_TYPE_PROP_NAME, MessageType.Utf8Text.toString()));
        } catch (IllegalArgumentException | NullPointerException e) {
            expectedMessageType = MessageType.Utf8Text;
        }
        try {
            onUnexpectedTopic = UnexpectedTopicBehavior.valueOf(reader.readString(ON_UNEXPECTED_TOPIC_PROP_NAME, UnexpectedTopicBehavior.Ignore.toString()));
        } catch (IllegalArgumentException | NullPointerException e) {
            onUnexpectedTopic = UnexpectedTopicBehavior.Ignore;
        }

    }


    @Override
    protected void writeData(XmlObjectBuilder builder) {
        super.writeData(builder);
        builder.add(LISTENED_TOPICS_PROP_NAME, listenedTopics);
        builder.add(QOS_PROP_NAME, qos);
        builder.add(EXPECTED_MESSAGE_TYPE_PROP_NAME, expectedMessageType.name());
        builder.add(ON_UNEXPECTED_TOPIC_PROP_NAME, onUnexpectedTopic.name());
        for(TestAssertionConfig assertionConfig: assertionConfigs){
            builder.addSection(ASSERTION_SECTION, assertionConfig);
        }
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int newValue) {
        setIntProperty("qos", QOS_PROP_NAME, newValue, 0, 2);
    }

    public String getListenedTopics() {
        return listenedTopics;
    }

    public void setListenedTopics(String newValue) {
        setProperty("listenedTopics", LISTENED_TOPICS_PROP_NAME, newValue);
    }

    public UnexpectedTopicBehavior getOnUnexpectedTopic() {
        return onUnexpectedTopic;
    }

    public void setOnUnexpectedTopic(UnexpectedTopicBehavior value) {
        setProperty("onUnexpectedTopic", null, value);
    }

    public MessageType getExpectedMessageType() {
        return expectedMessageType;
    }

    public void setExpectedMessageType(MessageType value) {
        setProperty("expectedMessageType", null, value);
    }

    public String getReceivedMessage() {
        return receivedMessage;
    }

    public void setReceivedMessage(String value) {
        setProperty("receivedMessage", RECEIVED_MESSAGE_PROP_NAME, value);
    }

    public String getReceivedMessageTopic() {
        return receivedMessageTopic;
    }

    public void setReceivedMessageTopic(String value) {
        setProperty("receivedMessageTopic", RECEIVED_TOPIC_PROP_NAME, value);
    }

    static boolean topicCorrespondsFilters(String topic, String[] filters) {
        String[] topicParts = topic.split("/", -1);
        for (String filter : filters) {
            String[] filterParts = filter.split("/", -1);
            int checkedLen = filterParts.length;
            if("#".equals(filterParts[filterParts.length - 1])){
                checkedLen = filterParts.length - 1;
                if(checkedLen > topicParts.length) continue;
            }
            else{
                if(filterParts.length != topicParts.length) continue;
            }
            if(checkedLen == 0){
                if(topicParts[0].length() > 0 && topicParts[0].charAt(0) == '$') continue; else return true;
            }
            if(!Utils.areStringsEqual(filterParts[0], topicParts[0]) && (!"+".equals(filterParts[0]) || (topicParts[0].length() > 0 && topicParts[0].charAt(0) == '$'))){
                continue;
            }
            int partNo = 1;
            for(; partNo < checkedLen; ++partNo){
                if(!Utils.areStringsEqual(filterParts[partNo], topicParts[partNo]) && !"+".equals(filterParts[partNo])){
                    break;
                }
            }
            if(partNo == checkedLen) return true;
        }
        return false;
    }

    private String bytesToString(byte[] buf, int startPos, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        ByteBuffer byteBuf = ByteBuffer.wrap(buf, startPos, buf.length - startPos);
        try {
            CharBuffer charBuf = decoder.decode(byteBuf);
            return charBuf.toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private String bytesToHexString(byte[] buf){
        final String decimals = "0123456789ABCDEF";
        if(buf == null) return null;
        char[] r = new char[buf.length * 2];
        for(int i = 0; i < buf.length; ++i){
            r[i * 2] = decimals.charAt((buf[i] & 0xf0) >> 4);
            r[i * 2 + 1] = decimals.charAt(buf[i] & 0x0f);
        }
        return new String(r);
    }

    private void onInvalidPayload(byte[] payload, WsdlTestStepResult errors){
        if(payload == null || payload.length == 0) {
            setReceivedMessage(null);
            errors.addMessage(String.format("Unable to extract a content of \"%s\" type from the message, because its payload is empty.", expectedMessageType));
            return;
        }
        String text;
        String actualFormat = "hexadecimal digits sequence";
        if(payload.length >= 3 && payload[0] == 0xef && payload[1] == 0xbb & payload[2] == 0xbf){
            text = bytesToString(payload, 3, Charsets.UTF_8);
            if(text == null) text = bytesToHexString(payload); else actualFormat = "UTF-8 text";
        }
        else if(payload.length >= 2 && ( ((payload[1] & 0xff)* 256 + (payload[0] & 0xff)) == 0xfffe || ((payload[1] & 0xff)* 256 + (payload[0] & 0xff)) == 0xfeff) ){
            text = bytesToString(payload, 2, Charsets.UTF_16);
            if(text == null) text = bytesToHexString(payload); else actualFormat = "UTF-16 text";
        }
        else{
            text = bytesToHexString(payload);
        }
        setReceivedMessage(text);
        errors.addMessage(String.format("Unable to extract a content of \"%s\" type from the message. It is stored as %s.", expectedMessageType, actualFormat));
    }


    private boolean storeMessage(Client.Message message, WsdlTestStepResult errors) {
        setReceivedMessageTopic(message.topic);
        byte[] payload = message.message.getPayload();
        String msgText = null;
        switch (expectedMessageType) {
            case Utf8Text:
                if (payload == null || payload.length == 0) {
                    setReceivedMessage("");
                    return true;
                }
                if(payload.length >= 3 && payload[0] == 0xef && payload[1] == 0xbb & payload[2] == 0xbf){
                    if(payload.length == 3) {
                        setReceivedMessage("");
                        return true;
                    } else {
                        msgText = bytesToString(payload, 3, Charsets.UTF_8);
                        if (msgText != null) {
                            setReceivedMessage(msgText);
                            return true;
                        }
                    }
                }
                else {
                    msgText = bytesToString(payload, 0, Charsets.UTF_8);
                    if (msgText != null) {
                        setReceivedMessage(msgText);
                        return true;
                    }
                }
                break;
            case Utf16Text:
                if (payload == null || payload.length == 0) {
                    setReceivedMessage("");
                    return true;
                }
                int bom = payload.length >= 2 ? payload[1] * 256 + payload[0] : 0;
                if(bom == 0xfffe || bom == 0xfeff){
                    if(payload.length == 2) {
                        setReceivedMessage("");
                        return true;
                    } else {
                        msgText = bytesToString(payload, 2, Charsets.UTF_16);
                        if (msgText != null) {
                            setReceivedMessage(msgText);
                            return true;
                        }
                    }
                }
                else {
                    msgText = bytesToString(payload, 0, Charsets.UTF_16LE);
                    if (msgText != null) {
                        setReceivedMessage(msgText);
                        return true;
                    }
                }
                break;
            case IntegerNumber:
                switch(payload.length){
                    case 1:
                        setReceivedMessage(String.valueOf(payload[0]));
                        return true;
                    case 2:
                        setReceivedMessage(String.valueOf((payload[0] & 0xff) + ((short)payload[1] << 8)));
                        return true;
                    case 4:
                        int ir = 0;
                        for(int i = 0; i < 4; ++i){
                            ir += (payload[i] & 0xff) << (8 * i);
                        }
                        setReceivedMessage(String.valueOf(ir));
                        return true;
                    case 8:
                        long lr = 0;
                        for(int i = 0; i < 8; ++i){
                            lr += (long)(payload[i] & 0xff) << (8 * i);
                        }
                        setReceivedMessage(String.valueOf(lr));
                        return true;

                }
                break;
            case FloatNumber:
                switch (payload.length){
                    case 4:
                        setReceivedMessage(String.valueOf(ByteBuffer.wrap(payload).getFloat()));
                        return true;
                    case 8:
                        setReceivedMessage(String.valueOf(ByteBuffer.wrap(payload).getDouble()));
                        return true;
                }

            case BinaryData:
                setReceivedMessage(bytesToHexString(payload));
                return true;
        }

        onInvalidPayload(payload, errors);
        return true;
    }


    private String[] diffOfStringSets(ArrayList<String> minuend, ArrayList<String> subtrahend) {
        ArrayList<String> resultList = new ArrayList<String>();
        for (String s : minuend) {
            boolean presentsEverywhere = false;
            for (String s2 : subtrahend) {
                if (Utils.areStringsEqual(s, s2)) {
                    presentsEverywhere = true;
                    break;
                }
            }
            if (!presentsEverywhere) {
                resultList.add(s);
            }
        }
        String[] result = new String[resultList.size()];
        resultList.toArray(result);
        return result;
    }

    @Override
    public void prepare(TestCaseRunner testRunner, TestCaseRunContext testRunContext) throws Exception {
        super.prepare(testRunner, testRunContext);
        setReceivedMessage(null);
        setReceivedMessageTopic(null);
        for (TestAssertion assertion : assertionsSupport.getAssertionList()) {
            assertion.prepare(testRunner, testRunContext);
        }
        updateState();
    }




    @Override
    public TestStepResult run(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        WsdlTestStepResult result = new WsdlTestStepResult(this);
        result.startTimer();
        result.setStatus(TestStepResult.TestStepStatus.UNKNOWN);
        if(iconAnimator != null) iconAnimator.start();
        try {
            try {
                String actualBrokerUri = testRunContext.expand(getServerUri());
                String uriCheckResult = Utils.checkServerUri(actualBrokerUri);
                if(uriCheckResult != null){
                    result.addMessage(uriCheckResult);
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    return result;
                }
                ConnectionParams actualConnectionParams = getConnectionParams(testRunContext);
                Client client = getCache(testRunContext).get(actualBrokerUri, actualConnectionParams);

                String[] splitTopics = testRunContext.expand(listenedTopics).split("[\\r\\n]+");

                ArrayList<String> neededTopics = new ArrayList<String>();
                for (String t : splitTopics) {
                    if(StringUtils.hasContent(t)) neededTopics.add(t.trim());
                }
                if (neededTopics.size() == 0) {
                    result.addMessage("The specified listened topic list is empty.");
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    return result;
                }

                long starTime = System.nanoTime();
                long maxTime = getTimeout() == 0 ? Long.MAX_VALUE : starTime + (long) getTimeout() * 1000 * 1000;

                int connectAttemptCount = 0;
                MqttAsyncClient clientObj = client.getClientObject();
                ArrayList<String> activeSubscriptions = client.getCachedSubscriptions();
                String[] requiredSubscriptions = diffOfStringSets(neededTopics, activeSubscriptions);
                if (requiredSubscriptions.length > 0) {
                    if (!client.isConnected()) {
                        ++connectAttemptCount;
                        if (!waitForMqttConnection(client, testRunner, result, maxTime)) {
                            return result;
                        }
                        activeSubscriptions = client.getCachedSubscriptions();
                        requiredSubscriptions = diffOfStringSets(neededTopics, activeSubscriptions);
                    }
                    int[] qosArray = new int[requiredSubscriptions.length];
                    Arrays.fill(qosArray, qos);
                    if (!waitForMqttOperation(clientObj.subscribe(requiredSubscriptions, qosArray), testRunner, result, maxTime, "Attempt to subscribe on the specified topics failed.")) {
                        return result;
                    }
                    Collections.addAll(activeSubscriptions, requiredSubscriptions);
                }

                Client.Message suitableMsg = null;
                MessageQueue messageQueue = client.getMessageQueue();
                messageQueue.setCurrentMessageToHead();
                while (System.nanoTime() <= maxTime && testRunner.isRunning()) {
                    Client.Message msg = messageQueue.getMessage();
                    if (msg != null) {
                        if (topicCorrespondsFilters(msg.topic, splitTopics)) {
                            suitableMsg = msg;
                            messageQueue.removeCurrentMessage();
                            break;
                        }
                        switch (onUnexpectedTopic) {
                            case Fail:
                                result.addMessage(String.format("\"%s\" topic of the received message does not correspond to any filter", msg.topic));
                                result.setStatus(TestStepResult.TestStepStatus.FAILED);
                                return result;
                            case Discard:
                                messageQueue.removeCurrentMessage();
                                messageQueue.setCurrentMessageToHead();
                                break;
                        }
                    } else {
                        if (!client.isConnected() && connectAttemptCount == 0) {
                            if (!waitForMqttConnection(client, testRunner, result, maxTime)) {
                                return result;
                            }
                            ++connectAttemptCount;
                        }
                    }
                }
                if (suitableMsg == null) {
                    if (!testRunner.isRunning()) {
                        result.setStatus(TestStepResult.TestStepStatus.CANCELED);
                    }
                    else {
                        result.addMessage("The test step's timeout has expired");
                        result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    }
                    return result;
                } else {
                    if (!storeMessage(suitableMsg, result)) {
                        result.setStatus(TestStepResult.TestStepStatus.FAILED);
                        return result;
                    }
                }

                for(WsdlMessageAssertion assertion: assertionsSupport.getAssertionList()){
                    applyAssertion(assertion);
                    AssertionError[] errors = assertion.getErrors();
                    if (errors != null) {
                        for (AssertionError error : errors) {
                            result.addMessage("[" + assertion.getName() + "] " + error.getMessage());
                        }
                    }
                }

            } catch (MqttException e) {
                result.setError(e);
                result.setStatus(TestStepResult.TestStepStatus.FAILED);
            }
            return result;
        } finally {
            result.stopTimer();
            if(iconAnimator != null) iconAnimator.stop();
            updateState();
            if(result.getStatus() == TestStepResult.TestStepStatus.UNKNOWN) {
                switch(getAssertionStatus()){
                    case FAILED:
                        result.setStatus(TestStepResult.TestStepStatus.FAILED);
                        break;
                    case VALID:
                        result.setStatus(TestStepResult.TestStepStatus.OK);
                        break;
                }
            }
        }

    }


    private void updateState() {
        AssertionStatus oldAssertionStatus = assertionStatus;
        if(getReceivedMessageTopic() != null){
            int cnt = getAssertionCount();
            if (cnt == 0) {
                assertionStatus = AssertionStatus.UNKNOWN;
            } else {
                assertionStatus = AssertionStatus.VALID;
                for (int c = 0; c < cnt; c++) {
                    if (getAssertionAt(c).getStatus() == AssertionStatus.FAILED) {
                        assertionStatus = AssertionStatus.FAILED;
                        break;
                    }
                }
            }
        }
        else{
            assertionStatus = AssertionStatus.UNKNOWN;
        }
        if(oldAssertionStatus != assertionStatus){
            notifyPropertyChanged("assertionStatus", oldAssertionStatus, assertionStatus);
        }
        if(iconAnimator == null) return;
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null
                && (testMonitor.hasRunningLoadTest(getTestStep().getTestCase()) || testMonitor.hasRunningSecurityTest(getTestStep().getTestCase()))) {
            setIcon(disabledStepIcon);
        }
        else{
            ImageIcon icon = iconAnimator.getIcon();
            if(icon == iconAnimator.getBaseIcon()){
                switch(assertionStatus){
                    case VALID:
                        setIcon(validStepIcon);
                        break;
                    case FAILED:
                        setIcon(failedStepIcon);
                        break;
                    case UNKNOWN:
                        setIcon(unknownStepIcon);
                        break;
                }
            }
        }
    }


    private void applyAssertion(WsdlMessageAssertion assertion){
        assertion.assertProperty(this, RECEIVED_MESSAGE_PROP_NAME, new MessageExchangeImpl(), new WsdlTestRunContext(this));
    }

    private void assertReceivedMessage(){
        if(getReceivedMessageTopic() != null){
            for(WsdlMessageAssertion assertion: assertionsSupport.getAssertionList()){
                applyAssertion(assertion);
            }
        }
        updateState();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(TestAssertion.CONFIGURATION_PROPERTY)
                || event.getPropertyName().equals(TestAssertion.DISABLED_PROPERTY)) {
            updateData();
            assertReceivedMessage();
        }

    }

    @Override
    public TestAssertion addAssertion(String selection) {

        try {
            WsdlMessageAssertion assertion = assertionsSupport.addWsdlAssertion(selection);
            if (assertion == null) {
                return null;
            }

            if (receivedMessageTopic != null) {
                applyAssertion(assertion);
                updateState();
            }

            return assertion;
        } catch (Exception e) {
            SoapUI.logError(e);
            throw e;
        }
    }

    @Override
    public void addAssertionsListener(AssertionsListener listener) {
        assertionsSupport.addAssertionsListener(listener);
    }

    @Override
    public int getAssertionCount() {
        return assertionsSupport.getAssertionCount();
    }

    @Override
    public TestAssertion getAssertionAt(int c) {
        return assertionsSupport.getAssertionAt(c);
    }

    @Override
    public void removeAssertionsListener(AssertionsListener listener) {
        assertionsSupport.removeAssertionsListener(listener);
    }

    @Override
    public void removeAssertion(TestAssertion assertion) {
        try {
            assertionsSupport.removeAssertion((WsdlMessageAssertion) assertion);

        } finally {
            ((WsdlMessageAssertion) assertion).release();
        }
        updateState();
    }

    @Override
    public AssertionStatus getAssertionStatus() {
        return assertionStatus;
    }

    @Override
    public String getAssertableContentAsXml() {
        //XmlObject.Factory.parse(receivedMessage)
        return getReceivedMessage();
    }

    @Override
    public String getAssertableContent() {
        return getReceivedMessage();
    }

    @Override
    public String getDefaultAssertableContent() {
        return "";
    }

    @Override
    public TestAssertionRegistry.AssertableType getAssertableType() {
        return TestAssertionRegistry.AssertableType.BOTH;
    }

    @Override
    public List<TestAssertion> getAssertionList() {
        return new ArrayList<TestAssertion>(assertionsSupport.getAssertionList());
    }

    @Override
    public TestAssertion getAssertionByName(String name) {
        return assertionsSupport.getAssertionByName(name);
    }

    @Override
    public TestStep getTestStep() {
        return this;
    }

    @Override
    public Interface getInterface() {
        return null;
    }

    @Override
    public TestAssertion cloneAssertion(TestAssertion source, String name) {
        return assertionsSupport.cloneAssertion(source, name);
    }

    @Override
    public Map<String, TestAssertion> getAssertions() {
        return assertionsSupport.getAssertions();
    }

    @Override
    public TestAssertion moveAssertion(int ix, int offset) {
        WsdlMessageAssertion assertion = assertionsSupport.getAssertionAt(ix);
        try {
            return assertionsSupport.moveAssertion(ix, offset);
        } finally {
            ((WsdlMessageAssertion) assertion).release();
            updateState();
        }
    }

    private class AssertableConfigImpl implements AssertableConfig {

        public TestAssertionConfig addNewAssertion()
        {
            TestAssertionConfig newConfig = TestAssertionConfig.Factory.newInstance();
            assertionConfigs.add(newConfig);
            return newConfig;
        }

        public List<TestAssertionConfig> getAssertionList() {
            return assertionConfigs;
        }

        public void removeAssertion(int ix) {
            assertionConfigs.remove(ix);
            updateData();
        }

        public TestAssertionConfig insertAssertion(TestAssertionConfig source, int ix) {
            TestAssertionConfig conf = TestAssertionConfig.Factory.newInstance();
            conf.set(source);
            assertionConfigs.add(ix, conf);
            updateData();
            return conf;
        }
    }

    private class MessageExchangeImpl implements MessageExchange {

        @Override
        public Operation getOperation() {
            return null;
        }

        @Override
        public ModelItem getModelItem() {
            return ReceiveTestStep.this;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }

        @Override
        public long getTimeTaken() {
            return 0;
        }

        @Override
        public String getEndpoint() {
            return null;
        }

        @Override
        public StringToStringMap getProperties() {
            return null;
        }

        @Override
        public String getRequestContent() {
            return null;
        }

        @Override
        public String getResponseContent() {
            return null;
        }

        @Override
        public String getRequestContentAsXml() {
            return null;
        }

        @Override
        public String getResponseContentAsXml() {
            return null;
        }

        @Override
        public StringToStringsMap getRequestHeaders() {
            return null;
        }

        @Override
        public StringToStringsMap getResponseHeaders() {
            return null;
        }

        @Override
        public Attachment[] getRequestAttachments() {
            return new Attachment[0];
        }

        @Override
        public Attachment[] getResponseAttachments() {
            return new Attachment[0];
        }

        @Override
        public String[] getMessages() {
            return new String[0];
        }

        @Override
        public boolean isDiscarded() {
            return false;
        }

        @Override
        public boolean hasRawData() {
            return false;
        }

        @Override
        public byte[] getRawRequestData() {
            return new byte[0];
        }

        @Override
        public byte[] getRawResponseData() {
            return new byte[0];
        }

        @Override
        public Attachment[] getRequestAttachmentsForPart(String partName) {
            return new Attachment[0];
        }

        @Override
        public Attachment[] getResponseAttachmentsForPart(String partName) {
            return new Attachment[0];
        }

        @Override
        public boolean hasRequest(boolean ignoreEmpty) {
            return false;
        }

        @Override
        public boolean hasResponse() {
            return false;
        }

        @Override
        public Response getResponse() {
            return null;
        }

        @Override
        public String getProperty(String name) {
            return null;
        }
    }


    @Override
    public void loadTestStarted(LoadTestRunner runner) {
        updateState();
    }

    @Override
    public void loadTestFinished(LoadTestRunner runner) {
        updateState();
    }

    @Override
    public void securityTestStarted(SecurityTestRunner runner) {
        updateState();
    }

    @Override
    public void securityTestFinished(SecurityTestRunner runner) {
        updateState();
    }

    @Override
    public void testCaseStarted(TestCaseRunner runner) {

    }@Override
     public void testCaseFinished(TestCaseRunner runner) {

    }@Override
     public void mockServiceStarted(MockRunner runner) {

    }@Override
     public void mockServiceStopped(MockRunner runner) {

    }}
