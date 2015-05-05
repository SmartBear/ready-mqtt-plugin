package com.smartbear.mqttsupport;

import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.model.support.TestStepBeanProperty;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestRunContext;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;


import java.lang.reflect.Field;


public abstract class MqttConnectedTestStep extends WsdlTestStepWithProperties {
    private String serverUri;
    private String clientId;
    private String login, password;
    final static String SERVER_URI_PROP_NAME = "ServerURI";
    final static String CLIENT_ID_PROP_NAME = "ClientID";
    final static String LOGIN_PROP_NAME = "Login";
    final static String PASSWORD_PROP_NAME = "Password";
    protected final static String TIMEOUT_PROP_NAME = "Timeout";
    private final static String TIMEOUT_MEASURE_PROP_NAME = "TimeoutMeasure";


    public enum TimeMeasure{
        Milliseconds("milliseconds"), Seconds("seconds");
        private String title;
        TimeMeasure(String title){this.title = title;}
        @Override
        public String toString(){return title;}
    }

    private int timeout = 30000;
    private TimeMeasure timeoutMeasure = TimeMeasure.Seconds;


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
        timeout = reader.readInt(TIMEOUT_PROP_NAME, 30000);
        try {
            timeoutMeasure = TimeMeasure.valueOf(reader.readString(TIMEOUT_MEASURE_PROP_NAME, TimeMeasure.Milliseconds.toString()));
        } catch(NumberFormatException | NullPointerException e) {
            timeoutMeasure = TimeMeasure.Milliseconds;
        }
    }

    protected void writeData(XmlObjectBuilder builder){
        builder.add(SERVER_URI_PROP_NAME, serverUri);
        if(clientId != null) builder.add(CLIENT_ID_PROP_NAME, login);
        if(login != null){
            builder.add(LOGIN_PROP_NAME, getLogin());
            builder.add(PASSWORD_PROP_NAME, getPassword());
        }
        builder.add(TIMEOUT_PROP_NAME, timeout);
        builder.add(TIMEOUT_MEASURE_PROP_NAME, timeoutMeasure.name());
    }

    protected void updateData(TestStepConfig config) {
        XmlObjectBuilder builder = new XmlObjectBuilder();
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
        setProperty("serverUri", SERVER_URI_PROP_NAME, value);
    }

    public String getClientId(){
        return clientId;
    }

    public void setClientId(String value){
        setProperty("clientId", CLIENT_ID_PROP_NAME, value);
    }

    public String getLogin(){
        return login;
    }

    public void setLogin(String value){
        setProperty("login", LOGIN_PROP_NAME, value);
    }

    public String getPassword(){
        return password;
    }

    public void setPassword(String value){
        setProperty("password", PASSWORD_PROP_NAME, value);
    }

    public int getTimeout(){return timeout;}
    public void setTimeout(int newValue){
        int oldShownTimeout = getShownTimeout();
        if(setIntProperty("timeout", TIMEOUT_PROP_NAME, newValue, 0, Integer.MAX_VALUE)) {
            if(timeoutMeasure == TimeMeasure.Seconds && (newValue % 1000) != 0){
                timeoutMeasure = TimeMeasure.Milliseconds;
                notifyPropertyChanged("timeoutMeasure", TimeMeasure.Seconds, TimeMeasure.Milliseconds);
            }
            int newShownTimeout = getShownTimeout();
            if(oldShownTimeout != newShownTimeout) notifyPropertyChanged("shownTimeout", oldShownTimeout, newShownTimeout);
        }
    }

    public int getShownTimeout(){
        if(timeoutMeasure == TimeMeasure.Milliseconds) return timeout; else return timeout / 1000;
    }

    public void setShownTimeout(int newValue){
        if(timeoutMeasure == TimeMeasure.Milliseconds){
            setTimeout(newValue);
        }
        else if (timeoutMeasure == TimeMeasure.Seconds){
            if((long)newValue * 1000 > Integer.MAX_VALUE){
                setTimeout(Integer.MAX_VALUE / 1000 * 1000);
            }
            else {
                setTimeout(newValue * 1000);
            }
        }
    }

    public TimeMeasure getTimeoutMeasure(){
        return timeoutMeasure;
    }

    public void setTimeoutMeasure(TimeMeasure newValue){
        if(newValue == null) return;
        TimeMeasure oldValue = timeoutMeasure;
        if(oldValue == newValue) return;
        int oldShownTimeout = getShownTimeout();
        timeoutMeasure = newValue;
        notifyPropertyChanged("timeoutMeasure", oldValue, newValue);
        if(newValue == TimeMeasure.Milliseconds){
            setIntProperty("timeout", TIMEOUT_PROP_NAME, oldShownTimeout, 0, Integer.MAX_VALUE);
        }
        else if(newValue == TimeMeasure.Seconds){
            if((long)oldShownTimeout * 1000 > Integer.MAX_VALUE){
                setIntProperty("timeout", TIMEOUT_PROP_NAME, Integer.MAX_VALUE / 1000 * 1000);
                notifyPropertyChanged("shownTimeout", oldShownTimeout, getShownTimeout());
            }
            else {
                setIntProperty("timeout", TIMEOUT_PROP_NAME, oldShownTimeout * 1000, 0, Integer.MAX_VALUE);
            }
        }
    }

//    protected void setStringProperty(String propName, String publishedPropName, String value) {
//        String old;
//        try {
//            Field field = null;
//            Class curClass = getClass();
//            while (field == null && curClass != null){
//                try {
//                    field = curClass.getDeclaredField(propName);
//                } catch (NoSuchFieldException e) {
//                    curClass = curClass.getSuperclass();
//                }
//            }
//            if(field == null) throw new RuntimeException(String.format("Error during access to %s bean property (details: unable to find the underlying field)", propName)); //We may not get here
//            field.setAccessible(true);
//            old = (String) (field.get(this));
//
//            if (old == null && value == null) {
//                return;
//            }
//            if (Utils.areStringsEqual(old, value)) {
//                return;
//            }
//            field.set(this, value);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)", propName, e.getMessage() + ")")); //We may not get here
//        }
//        updateData();
//        notifyPropertyChanged(propName, old, value);
//        firePropertyValueChanged(publishedPropName, old, value);
//
//    }

    protected boolean setProperty(String propName, String publishedPropName, Object value) {
        Object old;
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
            old = field.get(this);

            if (value == null) {
                if(old == null) return false;
            }
            else{
                if(value.equals(old)) return false;
            }
            field.set(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)", propName, e.getMessage() + ")")); //We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        if(publishedPropName != null) firePropertyValueChanged(publishedPropName, old == null ? null : old.toString(), value == null ? null : value.toString());
        return true;
    }



    protected boolean setIntProperty(String propName, String publishedPropName, int value, int minAllowed, int maxAllowed) {
        if(value < minAllowed || value > maxAllowed) return false;
        return setIntProperty(propName, publishedPropName, value);
    }

    protected boolean setIntProperty(String propName, String publishedPropName, int value) {
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

            if (old == value) return false;
            field.set(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)", propName, e.getMessage() + ")")); //We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        if(StringUtils.hasContent(publishedPropName)) firePropertyValueChanged(publishedPropName, Integer.toString(old), Integer.toString(value));
        return true;
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

    protected boolean waitForMqttOperation(IMqttToken token, CancellationToken cancellationToken, WsdlTestStepResult testStepResult, long maxTime, String errorText) {
        while (!token.isComplete() && token.getException() == null) {
            boolean stopped = cancellationToken.cancelled();
            if (stopped || (maxTime != Long.MAX_VALUE && System.nanoTime() > maxTime)) {
                if (stopped) {
                    testStepResult.setStatus(TestStepResult.TestStepStatus.CANCELED);
                }
                else{
                    testStepResult.addMessage("The test step's timeout has expired");
                    testStepResult.setStatus(TestStepResult.TestStepStatus.FAILED);

                }
                return false;
            }
        }
        if (token.getException() != null) {
            testStepResult.addMessage(errorText);
            testStepResult.setError(token.getException());
            testStepResult.setStatus(TestStepResult.TestStepStatus.FAILED);
            return false;
        }
        return true;
    }

    protected ClientCache getCache(PropertyExpansionContext testRunContext){
        final String CLIENT_CACHE_PROPNAME = "client_cache";
        ClientCache cache = (ClientCache)(testRunContext.getProperty(CLIENT_CACHE_PROPNAME));
        if(cache == null){
            cache = new ClientCache();
            testRunContext.setProperty(CLIENT_CACHE_PROPNAME, cache);
        }
        return cache;
    }

    protected boolean waitForMqttConnection(Client client, CancellationToken cancellationToken, WsdlTestStepResult testStepResult, long maxTime) throws MqttException {
        return waitForMqttOperation(client.getConnectingStatus(), cancellationToken, testStepResult, maxTime, "Unable connect to the MQTT broker.");
    }


    protected void afterExecution(PropertyExpansionContext runContext) {
        getCache(runContext).assureFinalized();
    }

    @Override
    public void finish(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        afterExecution(testRunContext);
        super.finish(testRunner, testRunContext);
    }

    public Project getOwningProject(){
        return ModelSupport.getModelItemProject(this);
    }

}
