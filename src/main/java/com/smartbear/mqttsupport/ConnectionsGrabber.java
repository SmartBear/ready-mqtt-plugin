package com.smartbear.mqttsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.environment.Environment;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.load.LoadTestModelItem;
import com.eviware.soapui.model.mock.MockService;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.project.ProjectListener;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.plugins.ListenerConfiguration;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@ListenerConfiguration
public class ConnectionsGrabber implements ProjectListener {


    public ConnectionsGrabber(){

    }


    @Override
    public void interfaceAdded(Interface anInterface) {

    }

    @Override
    public void interfaceRemoved(Interface anInterface) {

    }

    @Override
    public void interfaceUpdated(Interface anInterface) {

    }

    @Override
    public void testSuiteAdded(TestSuite testSuite) {

    }

    @Override
    public void testSuiteRemoved(TestSuite testSuite) {

    }

    @Override
    public void testSuiteMoved(TestSuite testSuite, int i, int i1) {

    }

    @Override
    public void mockServiceAdded(MockService mockService) {

    }

    @Override
    public void mockServiceRemoved(MockService mockService) {

    }

    @Override
    public void loadUITestAdded(LoadTestModelItem loadUiTest) {

    }

    @Override
    public void loadUITestRemoved(LoadTestModelItem loadUiTest) {

    }


    @Override
    public void afterLoad(Project project) {
        ConnectionsManager.onProjectLoaded(project);
    }

    @Override
    public void beforeSave(Project project) {
        ConnectionsManager.beforeProjectSaved(project);
    }

    @Override
    public void environmentAdded(Environment environment) {

    }

    @Override
    public void environmentRemoved(Environment environment, int i) {

    }

    @Override
    public void environmentSwitched(Environment environment) {

    }

    @Override
    public void environmentRenamed(Environment environment, String s, String s1) {

    }
}
