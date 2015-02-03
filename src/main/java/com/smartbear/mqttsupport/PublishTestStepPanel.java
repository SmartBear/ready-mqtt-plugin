package com.smartbear.mqttsupport;

import com.eviware.soapui.config.DataGeneratorPropertyConfig;
import com.eviware.soapui.impl.wsdl.WsdlTestSuite;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.PropertyComponent;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;
import com.eviware.x.form.ValidationMessage;
import com.eviware.x.form.XForm;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;
import com.eviware.x.form.XFormFieldListener;
import com.eviware.x.form.XFormFieldValidator;
import com.eviware.x.form.XFormOptionsField;
import com.eviware.x.form.support.ADialogBuilder;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.adapter.SpinnerAdapterFactory;
import com.jgoodies.binding.adapter.SpinnerToValueModelConnector;
import com.smartbear.ready.core.module.support.PrivateReadyApiModule;
import org.junit.Test;

import javax.naming.Binding;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class PublishTestStepPanel extends ModelItemDesktopPanel<PublishTestStep> {

    private JTextField numberEdit;
    private JTextArea textMemo;
    private JTextField fileNameEdit;
    private JButton chooseFileButton;
    private JPasswordField hiddenPasswordEdit;
    private JTextField visiblePasswordEdit;
    private char passwordChar;

    public PublishTestStepPanel(PublishTestStep modelItem) {
        super(modelItem);
        buildUI();
    }


    private static class CaseComboItem{
        public TestCase testCase;
        public TestSuite testSuite;

        public CaseComboItem(TestCase testCase, TestSuite testSuite){
            this.testCase = testCase;
            this.testSuite = testSuite;
        }

        @Override
        public String toString(){
            return String.format("%s [%s]", testCase.getName(), testSuite.getName());
        }
    }

    private static class StepComboItem{
        public MqttConnectedTestStep testStep;
        public StepComboItem(MqttConnectedTestStep testStep){
            this.testStep = testStep;
        }
        @Override
        public String toString(){
            return testStep.getName();
        }
    }

    private ArrayList<CaseComboItem> formCaseList(TestStep excludedTestStep){
        List<WsdlTestSuite> testSuites = getModelItem().getProject().getTestSuiteList();
        ArrayList<CaseComboItem> allowedTestCases = new ArrayList<CaseComboItem>();
        if(testSuites != null) {
            for (TestSuite testSuite : testSuites) {
                List<? extends TestCase> allTestCases = testSuite.getTestCaseList();
                if(allTestCases != null) {
                    for (TestCase testCase : allTestCases) {
                        List<TestStep> allTestSteps = testCase.getTestStepList();
                        if(allTestSteps != null) {
                            for (TestStep testStep : allTestSteps) {
                                if(testStep instanceof MqttConnectedTestStep && testStep != excludedTestStep){
                                    allowedTestCases.add(new CaseComboItem(testCase, testSuite));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return allowedTestCases;
    }

    private ArrayList<StepComboItem> formStepList(TestCase testCase, TestStep excludedTestStep){
        ArrayList<StepComboItem> result = new ArrayList<>();
        List<TestStep> testSteps = testCase.getTestStepList();
        for(TestStep testStep : testSteps){
            if(testStep instanceof MqttConnectedTestStep && testStep != excludedTestStep){
                result.add(new StepComboItem((MqttConnectedTestStep)testStep));
            }
        }
        return result;
    }

    private void buildUI() {
        PresentationModel<PublishTestStep> pm = new PresentationModel<PublishTestStep>(getModelItem());
        SimpleBindingForm form = new SimpleBindingForm(pm);
        form.appendHeading("Connection to MQTT Server");
        form.appendTextField("serverUri", "MQTT Server URI", "The MQTT server URI");
        form.appendTextField("clientId", "Client ID (optional)", "Fill this field if you want to connect with fixed Client ID or leave it empty so a unique ID will be generated");
        JTextField loginEdit = form.appendTextField("login", "Login (optional)", "Login to MQTT server. Fill this if the server requires authentication.");

        final String PASSWORD_TOOLTIP = "Password to MQTT server. Fill this if the server requires authentication.";


        final JPanel passwordPanel = new JPanel();
        passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.X_AXIS));
        final JPasswordField passwordEdit = new JPasswordField(loginEdit.getColumns());
        passwordEdit.setToolTipText(PASSWORD_TOOLTIP);
        passwordEdit.getAccessibleContext().setAccessibleDescription(PASSWORD_TOOLTIP);
        Bindings.bind(passwordEdit, pm.getModel("password"));
        passwordPanel.add(passwordEdit);
        final JCheckBox hidePasswordCheckBox = new JCheckBox("Hide");
        hidePasswordCheckBox.setSelected(true);
        passwordPanel.add(hidePasswordCheckBox);
        hidePasswordCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(hidePasswordCheckBox.isSelected()){
                    passwordEdit.setEchoChar(passwordChar);
                }
                else{
                    passwordChar = passwordEdit.getEchoChar();
                    passwordEdit.setEchoChar('\0');
                }
            }
        });


        form.append("Password (optional)", passwordPanel);
        //form.appendPasswordField("password", "Password (optional)", "Password to MQTT server. Fill this if the server requires authentication.");

        form.addButtonWithoutLabelToTheRight("Use Connection of Another Test Step...", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<CaseComboItem> caseComboItems = formCaseList(getModelItem());
                if (caseComboItems.size() == 0) {
                    UISupport.showErrorMessage("There are no other test steps which connect using MQTT in this project at this moment.");
                    return;
                }
                XFormDialog dialog = ADialogBuilder.buildDialog(SelectSourceTestStepForm.class);
                final XFormOptionsField caseCombo = (XFormOptionsField) dialog.getFormField(SelectSourceTestStepForm.TEST_CASE);
                final XFormOptionsField stepCombo = (XFormOptionsField) dialog.getFormField(SelectSourceTestStepForm.TEST_STEP);
                final XFormField infoField = dialog.getFormField(SelectSourceTestStepForm.STEP_INFO);
                final XFormField propertyExpansionsCheckBox = dialog.getFormField(SelectSourceTestStepForm.USE_PROPERTY_EXPANSIONS);

                XFormFieldListener caseComboListener = new XFormFieldListener() {
                    @Override
                    public void valueChanged(XFormField sourceField, String newValue, String oldValue) {
                        Object[] selectedCases = caseCombo.getSelectedOptions();
                        if (selectedCases == null || selectedCases.length == 0) {
                            stepCombo.setOptions(new Object[0]);
                            return;
                        }
                        TestCase selectedCase = ((CaseComboItem) selectedCases[0]).testCase;
                        stepCombo.setOptions(formStepList(selectedCase, getModelItem()).toArray());
                        if (selectedCase == getModelItem().getTestCase()) {
                            propertyExpansionsCheckBox.setEnabled(true);
                        } else {
                            propertyExpansionsCheckBox.setEnabled(false);
                            propertyExpansionsCheckBox.setValue("false");
                        }
                    }
                };
                caseCombo.addFormFieldListener(caseComboListener);

                XFormFieldListener stepComboListener = new XFormFieldListener() {
                    @Override
                    public void valueChanged(XFormField sourceField, String newValue, String oldValue) {
                        Object[] selectedSteps = stepCombo.getSelectedOptions();
                        if (selectedSteps == null || selectedSteps.length == 0) {
                            infoField.setValue(null);
                        } else {
                            StepComboItem item = (StepComboItem) stepCombo.getSelectedOptions()[0];
                            infoField.setValue(String.format(
                                    "MQTT server URI: %s\nClient ID: %s\nLogin: %s",
                                    StringUtils.isNullOrEmpty(item.testStep.getServerUri()) ? "{not specified yet}" : item.testStep.getServerUri(),
                                    Utils.areStringsEqual(item.testStep.getClientId(), "", false, true) ? "{generated}" : item.testStep.getClientId(),
                                    Utils.areStringsEqual(item.testStep.getLogin(), "", false, true) ? "{Doesn\'t use authentication}" : item.testStep.getLogin()
                            ));
                        }
                    }
                };
                stepCombo.addFormFieldListener(stepComboListener);
                stepCombo.addFormFieldValidator(new XFormFieldValidator() {
                    @Override
                    public ValidationMessage[] validateField(XFormField formField) {
                        if (stepCombo.getSelectedIndexes() == null || stepCombo.getSelectedIndexes().length == 0) {
                            return new ValidationMessage[]{new ValidationMessage("Please select a test step as a connection source", stepCombo)};
                        } else {
                            return new ValidationMessage[0];
                        }
                    }
                });

                caseCombo.setOptions(caseComboItems.toArray());
                for (CaseComboItem item : caseComboItems) {
                    if (item.testCase == getModelItem().getTestCase()) caseCombo.setSelectedOptions(new Object[]{item});
                }
                caseComboListener.valueChanged(caseCombo, caseCombo.getValue(), null);
                stepComboListener.valueChanged(stepCombo, stepCombo.getValue(), null);
                if (dialog.show()) {
                    MqttConnectedTestStep selectedTestStep = ((StepComboItem) stepCombo.getSelectedOptions()[0]).testStep;
                    if (Boolean.parseBoolean(dialog.getFormField(SelectSourceTestStepForm.USE_PROPERTY_EXPANSIONS).getValue())) {
                        getModelItem().setServerUri(String.format("${%s#%s}", selectedTestStep.getName(), MqttConnectedTestStep.SERVER_URI_PROP_NAME));
                        getModelItem().setLogin(String.format("${%s#%s}", selectedTestStep.getName(), MqttConnectedTestStep.LOGIN_PROP_NAME));
                        getModelItem().setPassword(String.format("${%s#%s}", selectedTestStep.getName(), MqttConnectedTestStep.PASSWORD_PROP_NAME));
                        getModelItem().setClientId(String.format("${%s#%s}", selectedTestStep.getName(), MqttConnectedTestStep.CLIENT_ID_PROP_NAME));
                    } else {
                        getModelItem().setServerUri(selectedTestStep.getServerUri());
                        getModelItem().setLogin(selectedTestStep.getLogin());
                        getModelItem().setPassword(selectedTestStep.getPassword());
                        getModelItem().setClientId(selectedTestStep.getClientId());
                    }
                }

            }
        });
        form.appendSeparator();
        form.appendHeading("Published Message");
        form.appendTextField("topic", "Topic", "Message Topic");
        form.appendComboBox("messageKind", "Message kind", PublishTestStep.MessageType.values(), "");
        numberEdit = form.appendTextField("message", "Message", "The number which will be published.");
        textMemo = form.appendTextArea("message", "Message", "The text which will be published.");
        fileNameEdit = form.appendTextField("message", "File name", "The file which content will be used as payload");
        chooseFileButton = form.addRightButton(new SelectFileAction());

        form.appendSeparator();
        form.appendHeading("Message Delivering Settings");
        JPanel qosPanel = new JPanel();
        qosPanel.setLayout(new BoxLayout(qosPanel, BoxLayout.X_AXIS));
        JRadioButton qos0Radio = new JRadioButton("At most once (0)"), qos1Radio = new JRadioButton("At least once (1)"), qos2Radio = new JRadioButton("Exactly once (2)");
        Bindings.bind(qos0Radio, pm.getModel("qos"), 0);
        Bindings.bind(qos1Radio, pm.getModel("qos"), 1);
        Bindings.bind(qos2Radio, pm.getModel("qos"), 2);
        qosPanel.add(qos0Radio);
        qosPanel.add(qos1Radio);
        qosPanel.add(qos2Radio);
        form.append("Quality of service", qosPanel);
        form.appendCheckBox("retained", "Retained", "");

        JPanel timeoutPanel = new JPanel();
        timeoutPanel.setLayout(new BoxLayout(timeoutPanel, BoxLayout.X_AXIS));
        timeoutPanel.add(Utils.createBoundSpinEdit(pm, "timeout", 0, 1000 * 1000, 1000));
        timeoutPanel.add(new JLabel(" ms"));
        form.append("Timeout", timeoutPanel);

        add(new JScrollPane(form.getPanel()));
        setPreferredSize(new Dimension(500, 300));

        propertyChange(new PropertyChangeEvent(getModelItem(), "messageKind", null, getModelItem().getMessageKind()));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);

        if (evt.getPropertyName().equals("messageKind")) {
            PublishTestStep.MessageType newMessageType = (PublishTestStep.MessageType)evt.getNewValue();
            boolean isNumber = newMessageType == PublishTestStep.MessageType.DoubleValue || newMessageType == PublishTestStep.MessageType.FloatValue || newMessageType == PublishTestStep.MessageType.IntegerValue || newMessageType == PublishTestStep.MessageType.LongValue;
            boolean isFile = newMessageType == PublishTestStep.MessageType.BinaryFile;
            boolean isText = newMessageType == PublishTestStep.MessageType.Utf8Text || newMessageType == PublishTestStep.MessageType.Utf16Text;
            numberEdit.setVisible(isNumber);
            textMemo.setVisible(isText);
            if(textMemo.getParent() instanceof JScrollPane) {
                textMemo.getParent().setVisible(isText);
            }
            else if(textMemo.getParent().getParent() instanceof JScrollPane){
                textMemo.getParent().getParent().setVisible(isText);
            }
            fileNameEdit.setVisible(isFile);
            chooseFileButton.setVisible(isFile);
        }
    }

    public class SelectFileAction extends AbstractAction {
        private JFileChooser fileChooser;

        public SelectFileAction() {
            super("Browse...");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
            }

            int returnVal = fileChooser.showOpenDialog(UISupport.getMainFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
//                String projectDir = new File(getModelItem().getProject().getPath()).getParent();
//                File selectedFile = new File(projectDir, fileChooser.getSelectedFile().getAbsolutePath());
//                fileNameEdit.setText(selectedFile.getPath());
                fileNameEdit.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

}
