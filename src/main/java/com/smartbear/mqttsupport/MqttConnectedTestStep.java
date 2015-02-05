package com.smartbear.mqttsupport;

import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import org.apache.xmlbeans.XmlObject;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.lang.reflect.Field;

public abstract class MqttConnectedTestStep extends WsdlTestStepWithProperties {
    //private ConnectionParams connectionParams = new ConnectionParams();
    private String serverUri;
    private String clientId;
    private String login, password;
    final static String SERVER_URI_PROP_NAME = "ServerURI";
    final static String CLIENT_ID_PROP_NAME = "ClientID";
    final static String LOGIN_PROP_NAME = "Login";
    final static String PASSWORD_PROP_NAME = "Password";

    public MqttConnectedTestStep(WsdlTestCase testCase, TestStepConfig config, boolean hasEditor, boolean forLoadTest){
        super(testCase, config, hasEditor, forLoadTest);
        addProperty(new TestStepBeanProperty(SERVER_URI_PROP_NAME, false, this, "serverUri", this));
        addProperty(new TestStepBeanProperty(CLIENT_ID_PROP_NAME, false, this, "clientId", this));
        addProperty(new TestStepBeanProperty(LOGIN_PROP_NAME, false, this, "login", this));
        addProperty(new TestStepBeanProperty(PASSWORD_PROP_NAME, false, this, "password", this));
    }


    protected void readData(XmlObjectConfigurationReader reader){
        serverUri = reader.readString(SERVER_URI_PROP_NAME, "");
        clientId = reader.readString(CLIENT_ID_PROP_NAME, "");
        login = reader.readString(LOGIN_PROP_NAME, "");
        password = reader.readString(PASSWORD_PROP_NAME, "");

    }

    protected void writeData(XmlObjectConfigurationBuilder builder){
        builder.add(SERVER_URI_PROP_NAME, serverUri);
        if(clientId != null) builder.add(CLIENT_ID_PROP_NAME, login);
        if(login != null){
            builder.add(LOGIN_PROP_NAME, getLogin());
            builder.add(PASSWORD_PROP_NAME, getPassword());
        }
    }

    protected void updateData(TestStepConfig config) {
        XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
        writeData(builder);
        config.setConfig(builder.finish());
    }

    protected void updateData() {
        if(getConfig() == null) return;
        updateData(getConfig());
    }

    protected ConnectionParams getConnectionParams(PropertyExpansionContext context){
        ConnectionParams result = new ConnectionParams();
        result.setId(PropertyExpander.expandProperties(context, clientId));
        result.setCredentials(PropertyExpander.expandProperties(context, login), PropertyExpander.expandProperties(context, password));
        return result;
    }

    public String getServerUri() {
        return serverUri;
    }

    public void setServerUri(String value) {
        setStringProperty("serverUri", SERVER_URI_PROP_NAME, value);
    }

    public String getClientId(){
        return clientId;
    }

    public void setClientId(String value){
        setStringProperty("clientId", CLIENT_ID_PROP_NAME, value);
    }

    public String getLogin(){
        return login;
    }

    public void setLogin(String value){
        setStringProperty("login", LOGIN_PROP_NAME, value);
    }

    public String getPassword(){
        return password;
    }

    public void setPassword(String value){
        setStringProperty("password", PASSWORD_PROP_NAME, value);
    }

    protected void setStringProperty(String propName, String publishedPropName, String value) {
        String old;
        try {
            Field field = null;
            Class curClass = getClass();
            while (field == null && curClass != null){
                try {
                    field = curClass.getDeclaredField(propName);
                } catch (NoSuchFieldException e) {
                    curClass = curClass.getSuperclass();
                }
            }
            if(field == null) throw new RuntimeException(String.format("Error during access to %s bean property (details: unable to find the underlying field)", propName)); //We may not get here
            field.setAccessible(true);
            old = (String) (field.get(this));

            if (old == null && value == null) {
                return;
            }
            if (Utils.areStringsEqual(old, value)) {
                return;
            }
            field.set(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)", propName, e.getMessage() + ")")); //We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        firePropertyValueChanged(publishedPropName, old, value);

    }

//    protected void setStringConnectionProperty(String propName, String publishedPropName, String value) {
//        String old;
//        try {
//            Field field = ConnectionParams.class.getDeclaredField(propName);
//            field.setAccessible(true);
//            old = (String) (field.get(connectionParams));
//
//            if (Utils.areStringsEqual(old, value)) {
//                return;
//            }
//            field.set(connectionParams, value);
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            throw new RuntimeException(String.format("Error during access to connectionParams.%s bean property (details: %s)", propName, e.getMessage() + ")")); //We may not get here
//        }
//        updateData();
//        notifyPropertyChanged(propName, old, value);
//        if(StringUtils.hasContent(publishedPropName)) firePropertyValueChanged(publishedPropName, old, value);
//
//    }

    protected void setIntProperty(String propName, String publishedPropName, int value, int minAllowed, int maxAllowed) {
        if(value < minAllowed || value > maxAllowed) return;
        setIntProperty(propName, publishedPropName, value);
    }

    protected void setIntProperty(String propName, String publishedPropName, int value) {
        int old;
        try {
            Field field = null;
            Class curClass = getClass();
            while (field == null && curClass != null){
                try {
                    field = curClass.getDeclaredField(propName);
                } catch (NoSuchFieldException e) {
                    curClass = curClass.getSuperclass();
                }
            }
            if(field == null) throw new RuntimeException(String.format("Error during access to %s bean property (details: unable to find the underlying field)", propName)); //We may not get here
            field.setAccessible(true);
            old = (int) (field.get(this));

            if (old == value) return;
            field.set(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)", propName, e.getMessage() + ")")); //We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        if(StringUtils.hasContent(publishedPropName)) firePropertyValueChanged(publishedPropName, Integer.toString(old), Integer.toString(value));

    }

    protected void setBooleanProperty(String propName, String publishedPropName, boolean value) {
        boolean old;
        try {
            Field field = null;
            Class curClass = getClass();
            while (field == null && curClass != null){
                try {
                    field = curClass.getDeclaredField(propName);
                } catch (NoSuchFieldException e) {
                    curClass = curClass.getSuperclass();
                }
            }
            if(field == null) throw new RuntimeException(String.format("Error during access to %s bean property (details: unable to find the underlying field)", propName)); //We may not get here
            field.setAccessible(true);
            old = (boolean) (field.get(this));

            if (old == value) return;
            field.set(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)", propName, e.getMessage() + ")")); //We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        if(StringUtils.hasContent(publishedPropName)) firePropertyValueChanged(publishedPropName, Boolean.toString(old), Boolean.toString(value));

    }

    protected boolean waitForMqttOperation(IMqttToken token, TestCaseRunner testRunner, WsdlTestStepResult testStepResult, long maxTime, String errorText) {
        while (!token.isComplete() && token.getException() == null) {
            if (!testRunner.isRunning() || (maxTime != Long.MAX_VALUE && System.nanoTime() > maxTime)) {
                if (testRunner.isRunning()) {
                    testStepResult.addMessage("The test step's timeout has expired");
                }
                return false;
            }
        }
        if (token.getException() != null) {
            testStepResult.addMessage(errorText);
            testStepResult.setError(token.getException());
            return false;
        }
        return true;
    }

    protected ClientCache getCache(TestCaseRunContext testRunContext){
        final String CLIENT_CACHE_PROPNAME = "client_cache";
        ClientCache cache = (ClientCache)(testRunContext.getProperty(CLIENT_CACHE_PROPNAME));
        if(cache == null){
            cache = new ClientCache();
            testRunContext.setProperty(CLIENT_CACHE_PROPNAME, cache);
        }
        return cache;
    }

    protected MqttAsyncClient waitForMqttClient(TestCaseRunner testRunner, TestCaseRunContext testRunContext, WsdlTestStepResult testStepResult, long maxTime) throws MqttException {
        ClientCache cache = getCache(testRunContext);
        if(waitForMqttOperation(cache.getConnectionStatus(testRunContext.expand(getServerUri()), getConnectionParams(testRunContext)), testRunner, testStepResult, maxTime, "Unable connect to the MQTT broker.")){
            return cache.get(testRunContext.expand(getServerUri()), getConnectionParams(testRunContext));
        }
        else{
            return null;
        }
    }

}
