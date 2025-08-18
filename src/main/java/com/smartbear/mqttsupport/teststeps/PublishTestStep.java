package com.smartbear.mqttsupport.teststeps;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.support.IconAnimator;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.model.mock.MockRunner;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.LoadTestRunner;
import com.eviware.soapui.model.testsuite.ProjectRunner;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.model.testsuite.TestSuiteRunner;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.monitor.TestMonitorListener;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.security.SecurityTestRunner;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.smartbear.mqttsupport.CancellationToken;
import com.smartbear.mqttsupport.Messages;
import com.smartbear.mqttsupport.PluginConfig;
import com.smartbear.mqttsupport.Utils;
import com.smartbear.mqttsupport.XmlObjectBuilder;
import com.smartbear.mqttsupport.connection.Client;
import com.smartbear.mqttsupport.teststeps.actions.groups.PublishTestStepActionGroup;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.util.ArrayList;

@PluginTestStep(typeName = "MQTTPublishTestStep", name = "Publish using MQTT", description = "Publishes a specified message through MQTT protocol.", iconPath = "com/smartbear/mqttsupport/publish_step.png")
public class PublishTestStep extends MqttConnectedTestStep implements TestMonitorListener, ExecutableTestStep {
    private final static String MESSAGE_KIND_SETTING_NAME = "MessageKind";
    private final static String TOPIC_SETTING_NAME = "Topic";
    private final static String MESSAGE_SETTING_NAME = "Message";
    private final static String QOS_SETTING_NAME = "QoS";
    private final static String RETAINED_SETTING_NAME = "Retained";

    private final static String MESSAGE_TYPE_PROP_NAME = "MessageType";
    private final static String TOPIC_PROP_NAME = "Topic";
    private final static String MESSAGE_PROP_NAME = "Message";
    private final static String QOS_PROP_NAME = "QoS";
    private final static String RETAINED_PROP_NAME = "Retained";
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(PluginConfig.LOGGER_NAME);
    public final static PublishedMessageType DEFAULT_MESSAGE_TYPE = PublishedMessageType.Json;
    public final static int DEFAULT_QOS = 0;

    private PublishedMessageType messageKind = DEFAULT_MESSAGE_TYPE;
    private String message;
    private String topic;

    private int qos = DEFAULT_QOS;
    private boolean retained;

    private static boolean actionGroupAdded = false;

    private ImageIcon disabledStepIcon;
    private ImageIcon unknownStepIcon;
    private IconAnimator<PublishTestStep> iconAnimator;
    private ArrayList<ExecutionListener> executionListeners;

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

        addProperty(new DefaultTestStepProperty(MESSAGE_TYPE_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty property) {
                return messageKind.toString();
            }

            @Override
            public void setValue(DefaultTestStepProperty property, String value) {
                PublishedMessageType messageType = PublishedMessageType.fromString(value);
                if (messageType != null) {
                    setMessageKind(messageType);
                }
            }
        }, this));
        addProperty(new TestStepBeanProperty(TOPIC_PROP_NAME, false, this, "topic", this));
        addProperty(new TestStepBeanProperty(MESSAGE_PROP_NAME, false, this, "message", this));

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

        if (!forLoadTest) {
            initIcons();
        }
        setIcon(unknownStepIcon);
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null) {
            testMonitor.addTestMonitorListener(this);
        }
    }

    protected void initIcons() {
        unknownStepIcon = UISupport.createImageIcon("com/smartbear/mqttsupport/unknown_publish_step.png");
        disabledStepIcon = UISupport.createImageIcon("com/smartbear/mqttsupport/disabled_publish_step.png");

        iconAnimator = new IconAnimator<PublishTestStep>(this, "com/smartbear/mqttsupport/unknown_publish_step.png", "com/smartbear/mqttsupport/publish_step.png", 5);
    }

    @Override
    public void release() {
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null) {
            testMonitor.removeTestMonitorListener(this);
        }
        super.release();
    }

    private boolean checkProperties(WsdlTestStepResult result, String topicToCheck, PublishedMessageType messageTypeToCheck, String messageToCheck) {
        boolean ok = true;
        if (StringUtils.isNullOrEmpty(topicToCheck)) {
            result.addMessage(Messages.THE_TOPIC_OF_MESSAGE_IS_NOT_SPECIFIED);
            ok = false;
        }
        if (messageTypeToCheck == null) {
            result.addMessage(Messages.THE_MESSAGE_FORMAT_IS_NOT_SPECIFIED);
            ok = false;
        }
        if (StringUtils.isNullOrEmpty(messageToCheck) && (messageTypeToCheck != PublishedMessageType.Utf16Text) && (messageTypeToCheck != PublishedMessageType.Utf8Text)) {
            if (messageTypeToCheck == PublishedMessageType.BinaryFile) {
                result.addMessage(Messages.A_FILE_WHICH_CONTAINS_A_MESSAGE_IS_NOT_SPECIFIED);
            } else {
                result.addMessage(Messages.A_MESSAGE_CONTENT_IS_NOT_SPECIFIED);
            }
            ok = false;
        }

        return ok;
    }


    @Override
    public TestStepResult run(final TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        return doExecute(testRunContext, new CancellationToken() {
            @Override
            public boolean cancelled() {
                return !testRunner.isRunning();
            }

            @Override
            public String cancellationReason() {
                return null;
            }
        });
    }

    private ExecutableTestStepResult doExecute(PropertyExpansionContext testRunContext, CancellationToken cancellationToken) {

        ExecutableTestStepResult result = new ExecutableTestStepResult(this);
        result.startTimer();
        result.setStatus(TestStepResult.TestStepStatus.OK);
        if (iconAnimator != null) {
            iconAnimator.start();
        }
        try {
            try {
                Client client = getClient(testRunContext, result);
                if (client == null) {
                    return reportError(result, Messages.ATTEMPT_TO_PUBLISH_THE_MESSAGE_FAILED, cancellationToken);
                }
                String expandedMessage = testRunContext.expand(message);
                String expandedTopic = testRunContext.expand(topic);

                if (!checkProperties(result, expandedTopic, messageKind, expandedMessage)) {
                    return reportError(result, null, cancellationToken);
                }
                long starTime = System.nanoTime();
                long maxTime = getTimeout() == 0 ? Long.MAX_VALUE : starTime + (long) getTimeout() * 1000_000;

                byte[] payload = null;
                try {
                    payload = messageKind.toPayload(expandedMessage, getOwningProject());
                } catch (RuntimeException e) {
                    return reportError(result, e.getMessage(), cancellationToken);
                }
                if (!client.isConnected()) {
                    if (!waitForMqttConnection(client, cancellationToken, result, maxTime)) {
                        return reportError(result, Messages.UNABLE_TO_CONNECT_TO_THE_SERVER, cancellationToken);
                    }
                }
                if (!client.isConnected()) {
                    return reportError(result, Messages.UNABLE_TO_PUBLISH_THE_MESSAGE_DUE_TO_LOST_CONNECTION, cancellationToken);
                }
                MqttMessage message = new MqttMessage();
                message.setRetained(retained);
                message.setQos(qos);
                message.setPayload(payload);
                if (!waitForMqttOperation(client.getClientObject().publish(expandedTopic, message),
                        cancellationToken, result, maxTime,
                        Messages.ATTEMPT_TO_PUBLISH_THE_MESSAGE_FAILED)) {
                    client.disconnect(false);
                    return result;
                }

            } catch (MqttException e) {
                return reportError(result, Messages.UNABLE_TO_PUBLISH_THE_MESSAGE_DUE_TO_THE_FOLLOWING_ERROR + e.getMessage(), cancellationToken);
            }
            return result;
        } finally {
            result.stopTimer();
            if (iconAnimator != null) {
                iconAnimator.stop();
            }
            result.setOutcome(formOutcome(result));
            log.info(String.format(Messages.S_S_TEST_STEP, result.getOutcome(), getName()));
            notifyExecutionListeners(result);
        }
    }

    private String formOutcome(WsdlTestStepResult executionResult) {
        switch (executionResult.getStatus()) {
            case CANCELED:
                return Messages.CANCELED;
            case FAILED:
                if (executionResult.getError() == null) {
                    return Messages.UNABLE_TO_PUBLISH_THE_MESSAGE + StringUtils.join(executionResult.getMessages(), " ") + ")";
                } else {
                    return Messages.ERROR_DURING_MESSAGE_PUBLISHING + Utils.getExceptionMessage(executionResult.getError());
                }
            default:
                return String.format(Messages.THE_MESSAGE_HAS_BEEN_PUBLISHED_WITHIN_D_MS, executionResult.getTimeTaken());

        }

    }

    private void notifyExecutionListeners(final ExecutableTestStepResult stepRunResult) {
        if (SwingUtilities.isEventDispatchThread()) {
            if (executionListeners != null) {
                for (ExecutionListener listener : executionListeners) {
                    try {
                        listener.afterExecution(this, stepRunResult);
                    } catch (Throwable e) {
                        SoapUI.logError(e);
                    }
                }
            }
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    notifyExecutionListeners(stepRunResult);
                }
            });
        }
    }

    @Override
    public void addExecutionListener(ExecutionListener listener) {
        if (executionListeners == null) {
            executionListeners = new ArrayList<ExecutionListener>();
        }
        executionListeners.add(listener);
    }

    @Override
    public void removeExecutionListener(ExecutionListener listener) {
        executionListeners.remove(listener);
    }

    public PublishedMessageType getMessageKind() {
        return messageKind;
    }

    public void setMessageKind(PublishedMessageType newValue) {
        if (messageKind == newValue) {
            return;
        }
        PublishedMessageType old = messageKind;
        messageKind = newValue;
        updateData();
        notifyPropertyChanged("messageKind", old, newValue);
        firePropertyValueChanged(MESSAGE_TYPE_PROP_NAME, old.toString(), newValue.toString());
        String oldMessage = getMessage();
        if (oldMessage == null) {
            oldMessage = "";
        }
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
        } catch (NumberFormatException e) {
            setMessage("0");
        }
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String newValue) {
        setProperty("topic", TOPIC_PROP_NAME, newValue);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String value) {
        try {
            switch (messageKind) {
                case IntegerValue:
                    Integer.parseInt(value);
                    break;
                case LongValue:
                    Long.parseLong(value);
                    break;
            }
        } catch (NumberFormatException e) {
            return;
        }
        setProperty("message", MESSAGE_PROP_NAME, value);
    }

    @Override
    public ExecutableTestStepResult execute(PropertyExpansionContext runContext, CancellationToken cancellationToken) {
        return doExecute(runContext, cancellationToken);
    }


    public int getQos() {
        return qos;
    }

    public void setQos(int newValue) {
        setIntProperty("qos", QOS_PROP_NAME, newValue, 0, 2);
    }

    public boolean getRetained() {
        return retained;
    }

    public void setRetained(boolean value) {
        setBooleanProperty("retained", RETAINED_PROP_NAME, value);
    }

    @Override
    protected void readData(XmlObjectConfigurationReader reader) {
        super.readData(reader);
        try {
            messageKind = PublishedMessageType.valueOf(reader.readString(MESSAGE_KIND_SETTING_NAME, DEFAULT_MESSAGE_TYPE.name()));
        } catch (IllegalArgumentException | NullPointerException e) {
            messageKind = DEFAULT_MESSAGE_TYPE;
        }
        topic = reader.readString(TOPIC_SETTING_NAME, "");
        message = reader.readString(MESSAGE_SETTING_NAME, "");
        qos = reader.readInt(QOS_SETTING_NAME, DEFAULT_QOS);
        retained = reader.readBoolean(RETAINED_SETTING_NAME, false);
    }


    @Override
    protected void writeData(XmlObjectBuilder builder) {
        super.writeData(builder);
        if (messageKind != null) {
            builder.add(MESSAGE_KIND_SETTING_NAME, messageKind.name());
        }
        builder.add(TOPIC_SETTING_NAME, topic);
        builder.add(MESSAGE_SETTING_NAME, message);
        builder.add(QOS_SETTING_NAME, qos);
        builder.add(RETAINED_SETTING_NAME, retained);
    }

    private void updateState() {
        if (iconAnimator == null) {
            return;
        }
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null
                && (testMonitor.hasRunningLoadTest(getTestCase()) || testMonitor.hasRunningSecurityTest(getTestCase()))) {
            setIcon(disabledStepIcon);
        } else {
            setIcon(unknownStepIcon);
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

    }

    @Override
    public void testCaseFinished(TestCaseRunner runner) {

    }

    @Override
    public void mockServiceStarted(MockRunner runner) {

    }

    @Override
    public void mockServiceStopped(MockRunner runner) {

    }

    @Override
    public void projectStarted(ProjectRunner projectRunner) {

    }

    @Override
    public void projectFinished(ProjectRunner projectRunner) {

    }

    @Override
    public void testSuiteStarted(TestSuiteRunner testSuiteRunner) {

    }

    @Override
    public void testSuiteFinished(TestSuiteRunner testSuiteRunner) {

    }

}
