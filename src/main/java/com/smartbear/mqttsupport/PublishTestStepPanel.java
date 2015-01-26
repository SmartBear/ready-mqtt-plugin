package com.smartbear.mqttsupport;

import com.eviware.soapui.config.DataGeneratorPropertyConfig;
import com.eviware.soapui.impl.wsdl.WsdlTestSuite;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestSuite;
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
import org.junit.Test;

import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class PublishTestStepPanel extends ModelItemDesktopPanel<PublishTestStep> {
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
        SimpleBindingForm form = new SimpleBindingForm(new PresentationModel<PublishTestStep>(getModelItem()));
        form.appendHeading("Connection to MQTT Server");
        form.appendTextField("serverUri", "MQTT Server URI", "The MQTT server URI");
        form.appendTextField("clientId", "Client ID (optional)", "Fill this field if you want to connect with fixed Client ID or leave it empty so a unique ID will be generated");
        form.appendTextField("login", "Login (optional)", "Login to MQTT server. Fill this if the server requires authentication.");
        form.appendPasswordField("password", "Password (optional)", "Password to MQTT server. Fill this if the server requires authentication.");
        form.addButtonWithoutLabelToTheRight("Use Connection of Another Test Step...", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<CaseComboItem> caseComboItems = formCaseList(getModelItem());
                if(caseComboItems.size() == 0){
                    UISupport.showErrorMessage("There are no other test steps which connect using MQTT in this project at this moment.");
                    return;
                }
                XFormDialog dialog = ADialogBuilder.buildDialog(SelectSourceTestStepForm.class);
                final XFormOptionsField caseCombo = (XFormOptionsField)dialog.getFormField(SelectSourceTestStepForm.TEST_CASE);
                final XFormOptionsField stepCombo = (XFormOptionsField)dialog.getFormField(SelectSourceTestStepForm.TEST_STEP);
                final XFormField serverField = dialog.getFormField(SelectSourceTestStepForm.SERVER);
                final XFormField clientIdField = dialog.getFormField(SelectSourceTestStepForm.CLIENT_ID);
                final XFormField loginField = dialog.getFormField(SelectSourceTestStepForm.LOGIN);
                final XFormField propertyExpansionsCheckBox = dialog.getFormField(SelectSourceTestStepForm.USE_PROPERTY_EXPANSIONS);
                serverField.setEnabled(false);
                clientIdField.setEnabled(false);
                loginField.setEnabled(false);

                XFormFieldListener caseComboListener = new XFormFieldListener() {
                    @Override
                    public void valueChanged(XFormField sourceField, String newValue, String oldValue) {
                        Object[] selectedCases = caseCombo.getSelectedOptions();
                        if(selectedCases == null || selectedCases.length == 0){
                            stepCombo.setOptions(new Object[0]);
                            return;
                        }
                        TestCase selectedCase = ((CaseComboItem) selectedCases[0]).testCase;
                        stepCombo.setOptions(formStepList(selectedCase, getModelItem()).toArray());
                        if(selectedCase == getModelItem().getTestCase()){
                            propertyExpansionsCheckBox.setEnabled(true);
                        }
                        else{
                            propertyExpansionsCheckBox.setEnabled(false);
                            propertyExpansionsCheckBox.setValue("false");
                        }
                    }
                };
                caseCombo.addFormFieldListener(caseComboListener);

                stepCombo.addFormFieldListener(new XFormFieldListener() {
                    @Override
                    public void valueChanged(XFormField sourceField, String newValue, String oldValue) {
                        Object[] selectedSteps = stepCombo.getSelectedOptions();
                        if(selectedSteps == null || selectedSteps.length == 0){
                            serverField.setValue(null);
                            clientIdField.setValue(null);
                            loginField.setValue(null);
                        }
                        else{
                            StepComboItem item = (StepComboItem) stepCombo.getSelectedOptions()[0];
                            serverField.setValue(item.testStep.getServerUri());
                            clientIdField.setValue(item.testStep.getConnectionParams().hasGeneratedId() ? "{generated}" : item.testStep.getConnectionParams().getFixedId());
                            loginField.setValue(item.testStep.getConnectionParams().hasCredentials() ? item.testStep.getConnectionParams().getLogin() : "{Doesn\'t use authentication}");
                        }
                    }
                });
                stepCombo.addFormFieldValidator(new XFormFieldValidator() {
                    @Override
                    public ValidationMessage[] validateField(XFormField formField) {
                        if(stepCombo.getSelectedIndexes() == null || stepCombo.getSelectedIndexes().length == 0) {
                            return new ValidationMessage[]{new ValidationMessage("Please select a test step as a connection source", stepCombo)};
                        }
                        else{
                            return new ValidationMessage[0];
                        }
                    }
                });

                caseCombo.setOptions(caseComboItems.toArray());
                for(CaseComboItem item: caseComboItems){
                    if(item.testCase == getModelItem().getTestCase()) caseCombo.setSelectedOptions(new Object[]{item});
                }
                caseComboListener.valueChanged(caseCombo, caseCombo.getValue(), null);
                if(dialog.show()){
                    MqttConnectedTestStep selectedTestStep = ((StepComboItem) stepCombo.getSelectedOptions()[0]).testStep;
                    if(Boolean.parseBoolean(dialog.getFormField(SelectSourceTestStepForm.USE_PROPERTY_EXPANSIONS).getValue())){
                        getModelItem().setServerUri(String.format("${%s#%s}", selectedTestStep.getName(), MqttConnectedTestStep.SERVER_URI_PROP_NAME));
                        getModelItem().setLogin(String.format("${%s#%s}", selectedTestStep.getName(), MqttConnectedTestStep.LOGIN_PROP_NAME));
                        getModelItem().setPassword(String.format("${%s#%s}", selectedTestStep.getName(), MqttConnectedTestStep.PASSWORD_PROP_NAME));
                        getModelItem().setClientId(String.format("${%s#%s}", selectedTestStep.getName(), MqttConnectedTestStep.CLIENT_ID_PROP_NAME));
                    }
                    else{
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

        add(new JScrollPane(form.getPanel()));
        setPreferredSize(new Dimension(500, 300));
    }


}
