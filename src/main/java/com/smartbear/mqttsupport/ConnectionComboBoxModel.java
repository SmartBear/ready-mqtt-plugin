package com.smartbear.mqttsupport;

import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.support.StringUtils;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;

class ConnectionComboBoxModel extends AbstractListModel<ConnectionComboBoxItem> implements ComboBoxModel<ConnectionComboBoxItem>{
    private Project project;
    private MqttConnectedTestStep step;
    private ConnectionParams[] items = null;

    public ConnectionComboBoxModel(MqttConnectedTestStep step, Project project){
        this.project = project;
        this.step = step;
    }

    private void fillItems(){
        HashSet<ConnectionParams> set = new HashSet<>();
        List<? extends TestSuite> testSuites = project.getTestSuiteList();
        for(TestSuite suite: testSuites) {
            List<? extends TestCase> testCases = suite.getTestCaseList();
            for (TestCase testCase : testCases) {
                List<TestStep> steps = testCase.getTestStepList();
                for (TestStep step : steps) {
                    if (step instanceof MqttConnectedTestStep) {
                        MqttConnectedTestStep mqttStep = (MqttConnectedTestStep) step;
                        if(Utils.areStringsEqual(mqttStep.getServerUri(), this.step.getServerUri())){
                            if (mqttStep.getConnectionParams() != null) set.add(mqttStep.getConnectionParams());
                        }
                    }
                }
            }
        }
        items = new ConnectionParams[set.size()];
        int i = 0;
        for(ConnectionParams params: set){
            items[i] = params;
            ++i;
        }
    }

    void setSelectedItem(ConnectionComboBoxItem anItem){

    }

    @Override
    public void setSelectedItem(Object anItem) {

    }

    @Override
    public ConnectionComboBoxItem getSelectedItem(){
        return null;
    }

    @Override
    public int getSize() {
        fillItems();
        return items.length + 2;
    }

    @Override
    public ConnectionComboBoxItem getElementAt(int index) {
        if(items == null) fillItems();
        if(index == 0) return new ConnectionComboBoxItem(null);
        if(index == items.length + 1) return ConnectionComboBoxItem.newConnectionItem;
        return new ConnectionComboBoxItem(items[index + 1]);
    }
}
