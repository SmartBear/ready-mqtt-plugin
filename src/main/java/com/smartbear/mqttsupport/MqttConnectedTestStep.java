package com.smartbear.mqttsupport;

import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import org.apache.xmlbeans.XmlObject;

import java.lang.reflect.Field;

public abstract class MqttConnectedTestStep extends WsdlTestStepWithProperties {
    private ConnectionParams connectionParams = new ConnectionParams();
    private String serverUri;
    private final static String SERVER_URI_PROP_NAME = "ServerURI";
    private final static String CLIENT_ID_PROP_NAME = "ClientID";
    private final static String LOGIN_PROP_NAME = "Login";
    private final static String PASSWORD_PROP_NAME = "Password";
private String fileName;
    public MqttConnectedTestStep(WsdlTestCase testCase, TestStepConfig config, boolean hasEditor, boolean forLoadTest){
        super(testCase, config, hasEditor, forLoadTest);
        addProperty(new TestStepBeanProperty(SERVER_URI_PROP_NAME, false, this, "serverUri", this));
        addProperty(new TestStepBeanProperty(CLIENT_ID_PROP_NAME, false, this, "clientId", this));
        addProperty(new TestStepBeanProperty(LOGIN_PROP_NAME, false, this, "login", this));
        addProperty(new TestStepBeanProperty(PASSWORD_PROP_NAME, false, this, "password", this));
    }


    protected void readData(XmlObjectConfigurationReader reader){
        serverUri = reader.readString(SERVER_URI_PROP_NAME, "");
        connectionParams.setId(reader.readString(CLIENT_ID_PROP_NAME, ""));
        connectionParams.setCredentials(reader.readString(LOGIN_PROP_NAME, ""), reader.readString(PASSWORD_PROP_NAME, ""));

    }

    protected void writeData(XmlObjectConfigurationBuilder builder){
        builder.add(SERVER_URI_PROP_NAME, serverUri);
        if(!connectionParams.hasGeneratedId()) builder.add(CLIENT_ID_PROP_NAME, connectionParams.getFixedId());
        if(!connectionParams.hasCredentials()){
            builder.add(LOGIN_PROP_NAME, connectionParams.getLogin());
            builder.add(PASSWORD_PROP_NAME, connectionParams.getPassword());
        }
    }

    protected void updateData(TestStepConfig config) {
        XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
        writeData(builder);
        if (config.getConfig() != null) {
            config.setConfig(builder.finish());
        }

    }

    protected void updateData() {
        updateData(getConfig());
    }

    public ConnectionParams getConnectionParams(){return connectionParams;}
    public void setConnectionParams(ConnectionParams params){connectionParams = params;}

    public String getServerUri() {
        return serverUri;
    }

    public void setServerUri(String value) {
        setStringProperty("serverUri", SERVER_URI_PROP_NAME, value);
    }

    public String getClientId(){
        return connectionParams.getFixedId();
    }

    public void setClientId(String value){
        setStringConnectionProperty("fixedId", CLIENT_ID_PROP_NAME, value);
    }

    public String getLogin(){
        return connectionParams.getLogin();
    }

    public void setLogin(String value){
        setStringConnectionProperty("login", LOGIN_PROP_NAME, value);
    }

    public String getPassword(){
        return connectionParams.getPassword();
    }

    public void setPassword(String value){
        setStringConnectionProperty("password", PASSWORD_PROP_NAME, value);
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

    protected void setStringConnectionProperty(String propName, String publishedPropName, String value) {
        String old;
        try {
            Field field = ConnectionParams.class.getField(propName);
            field.setAccessible(true);
            old = (String) (field.get(connectionParams));

            if (old == null && value == null) {
                return;
            }
            if (old.equals(value)) {
                return;
            }
            field.set(this, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(String.format("Error during access to connectionParams.%s bean property (details: %s)", propName, e.getMessage() + ")")); //We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        firePropertyValueChanged(publishedPropName, old, value);

    }

}
