package com.smartbear.mqttsupport;

import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

import java.lang.reflect.Field;

public abstract class MqttConnectedTestStep extends WsdlTestStepWithProperties {
    private ConnectionParams connectionParams;
    private String serverUri;
    private final static String SERVER_URI_PROP_NAME = "ServerURI";

    public MqttConnectedTestStep(WsdlTestCase testCase, TestStepConfig config, boolean hasEditor, boolean forLoadTest){
        super(testCase, config, hasEditor, forLoadTest);
        if (config != null && config.getConfig() != null) {
            readData(config);
        }
        addProperty(new TestStepBeanProperty(SERVER_URI_PROP_NAME, false, this, "serverUri", this));
    }

    private void readData(TestStepConfig config) {
        XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(config.getConfig());
        serverUri = reader.readString(SERVER_URI_PROP_NAME, "");
    }


    private void updateData(TestStepConfig config) {
        XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
        builder.add(SERVER_URI_PROP_NAME, serverUri);
        if (config.getConfig() != null) {
            config.setConfig(builder.finish());
        }

    }

    private void updateData() {
        updateData(getConfig());
    }

    public ConnectionParams getConnectionParams(){return connectionParams;}
    public void setConnectionParams(ConnectionParams params){connectionParams = params;}
    public String getServerUri() {
        return serverUri;
    }

    public void setServerUri(String value) {
//        String old = getServerUri();
//        if(old == null && value == null) return;
//        if(old.equals(value)) return;
//        serverUri = value;
//        updateData();
//        notifyPropertyChanged("serverUri", old, value);
//        firePropertyValueChanged(SERVER_URI_PROP_NAME, old, value);
        setStringProperty("serverUri", SERVER_URI_PROP_NAME, value);
    }

    protected void setStringProperty(String propName, String publishedPropName, String value) {
        String old;
        try {
            Field field = getClass().getField(propName);
            field.setAccessible(true);
            old = (String) (field.get(this));

            if (old == null && value == null) {
                return;
            }
            if (old.equals(value)) {
                return;
            }
            field.set(this, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)", propName, e.getMessage() + ")")); //We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        firePropertyValueChanged(publishedPropName, old, value);

    }
}
