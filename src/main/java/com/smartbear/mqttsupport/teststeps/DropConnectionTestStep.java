package com.smartbear.mqttsupport.teststeps;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.support.IconAnimator;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.model.mock.MockRunner;
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
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.smartbear.mqttsupport.Messages;
import com.smartbear.mqttsupport.connection.Client;
import com.smartbear.mqttsupport.teststeps.panels.MqttConnectedTestStepPanel;
import com.smartbear.mqttsupport.PluginConfig;
import com.smartbear.mqttsupport.XmlObjectBuilder;
import com.smartbear.mqttsupport.teststeps.actions.groups.DropConnectionTestStepActionGroup;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;

@PluginTestStep(typeName = "MQTTDropConnectionTestStep", name = "Drop MQTT Connection", description = "Disconnects from the MQTT server", iconPath = "com/smartbear/mqttsupport/drop_step.png")
public class DropConnectionTestStep extends MqttConnectedTestStep implements TestMonitorListener {
    private static final String DROP_METHOD_SETTING_NAME = "DropMethod";

    private static boolean actionGroupAdded = false;
    private ImageIcon disabledStepIcon;
    private ImageIcon unknownStepIcon;
    private IconAnimator<DropConnectionTestStep> iconAnimator;
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(PluginConfig.LOGGER_NAME);


    private DropMethod dropMethod = DropMethod.SendDisconnect;

    public enum DropMethod implements MqttConnectedTestStepPanel.UIOption {
        SendDisconnect("Send Disconnect message to MQTT server"), Drop("Close network connection");
        private String title;
        DropMethod(String title){this.title = title;}
        @Override
        public String getTitle(){
            return title;
        }
        public static DropMethod fromString(String str) {
            if (str == null) {
                return null;
            }
            for (DropMethod m : DropMethod.values()) {
                if (m.toString().equals(str)) {
                    return m;
                }
            }
            return null;
        }
    }

    public DropConnectionTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
        super(testCase, config, true, forLoadTest);
        if (!actionGroupAdded) {
            SoapUI.getActionRegistry().addActionGroup(new DropConnectionTestStepActionGroup());
            actionGroupAdded = true;
        }
        if (config != null && config.getConfig() != null) {
            XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(config.getConfig());
            readData(reader);
        }

        if (!forLoadTest) {
            initIcons();
        }
        setIcon(unknownStepIcon);
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null){
            testMonitor.addTestMonitorListener(this);
        }

    }

    protected void initIcons() {
        unknownStepIcon = UISupport.createImageIcon("com/smartbear/mqttsupport/unknown_drop_step.png");
        disabledStepIcon = UISupport.createImageIcon("com/smartbear/mqttsupport/disabled_drop_step.png");

        iconAnimator =  new IconAnimator<DropConnectionTestStep>(this, "com/smartbear/mqttsupport/unknown_drop_step.png", "com/smartbear/mqttsupport/drop_step.png", 5);
    }

    @Override
    public void release(){
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null) testMonitor.removeTestMonitorListener(this);
        super.release();
    }

    @Override
    protected void readData(XmlObjectConfigurationReader reader){
        super.readData(reader);
        try {
            dropMethod = DropMethod.valueOf(reader.readString(DROP_METHOD_SETTING_NAME, DropMethod.SendDisconnect.toString()));
        } catch (IllegalArgumentException | NullPointerException e) {
            dropMethod = DropMethod.SendDisconnect;
        }
    }


    @Override
    protected void writeData(XmlObjectBuilder builder){
        super.writeData(builder);
        builder.add(DROP_METHOD_SETTING_NAME, dropMethod.name());
    }

    public final static String DROP_METHOD_BEAN_PROP_NAME = "dropMethod";
    public DropMethod getDropMethod(){return dropMethod;}
    public void setDropMethod(DropMethod newValue){setProperty(DROP_METHOD_BEAN_PROP_NAME, null, newValue);}

    private void updateState() {
        if(iconAnimator == null) return;
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null
                && (testMonitor.hasRunningLoadTest(getTestCase()) || testMonitor.hasRunningSecurityTest(getTestCase()))) {
            setIcon(disabledStepIcon);
        }
        else {
            setIcon(unknownStepIcon);
        }
    }

    @Override
    public TestStepResult run(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        WsdlTestStepResult result = new WsdlTestStepResult(this);
        result.startTimer();
        result.setStatus(TestStepResult.TestStepStatus.UNKNOWN);
        if(iconAnimator != null) iconAnimator.start();
        try{
            Client client = getClient(testRunContext, result);
            if(client == null) return result;
            if(client.isConnected()) {
                try {
                    switch (dropMethod) {
                        case SendDisconnect:
                            client.disconnect(true);
                            break;
                        case Drop:
                            client.disconnect(false);
                            break;
                    }
                }
                catch (MqttException e){
                    result.setStatus(TestStepResult.TestStepStatus.FAILED);
                    result.setError(e);
                }
            }
            else{
                result.addMessage(Messages.ALREADY_DISCONNECTED_FROM_THE_MQTT_SERVER);
                result.setStatus(TestStepResult.TestStepStatus.FAILED);
            }

            return result;

        }
        finally {
            result.stopTimer();
            if(iconAnimator != null) iconAnimator.stop();
            //log.info(String.format("%s - [%s test step]", result.getOutcome(), getName()));

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
