package com.smartbear.mqttsupport;

import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;


import javax.swing.SwingUtilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;


public abstract class MqttConnectedTestStep extends WsdlTestStepWithProperties implements PropertyChangeListener {
    private Connection connection;
    private Connection legacyConnection;

    final static String SERVER_URI_PROP_NAME = "ServerURI";
    final static String CLIENT_ID_PROP_NAME = "ClientID";
    final static String LOGIN_PROP_NAME = "Login";
    final static String PASSWORD_PROP_NAME = "Password";
    final static String CONNECTION_NAME_PROP_NAME = "ConnectionName";
    final static String LEGACY_CONNECTION_PROP_NAME = "LegacyConnection";
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
        addProperty(new DefaultTestStepProperty(SERVER_URI_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if (connection == null) return ""; else return connection.getServerUri();
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if (connection == null) return;
                connection.setServerUri(s);
            }
        }, this));
        addProperty(new DefaultTestStepProperty(CLIENT_ID_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if(connection == null) return ""; else return connection.getFixedId();
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if(connection == null) return;
                connection.setFixedId(s);
            }
        }, this));
        addProperty(new DefaultTestStepProperty(LOGIN_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if(connection == null) return ""; else return connection.getLogin();
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if(connection == null) return;
                connection.setLogin(s);
            }
        }, this));
        addProperty(new DefaultTestStepProperty(PASSWORD_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if(connection == null) return ""; else return connection.getPassword();
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if(connection == null) return;
                connection.setPassword(s);
            }
        }, this));
    }


    protected void readData(XmlObjectConfigurationReader reader){
        if(connection != null) {
            connection.removePropertyChangeListener(this);
            connection = null;
        }
        if(reader.readBoolean(LEGACY_CONNECTION_PROP_NAME, true)){
            connection = new Connection();
            connection.setServerUri(reader.readString(SERVER_URI_PROP_NAME, ""));
            connection.setFixedId(reader.readString(CLIENT_ID_PROP_NAME, ""));
            connection.setLogin(reader.readString(LOGIN_PROP_NAME, ""));
            connection.setPassword(reader.readString(PASSWORD_PROP_NAME, ""));
            legacyConnection = connection;
        }
        else{
            String connectionName = reader.readString(CONNECTION_NAME_PROP_NAME, "");
            if(StringUtils.hasContent(connectionName)) {
                connection = ConnectionsManager.getConnection(this, connectionName);
            }
            if(connection != null) connection.addPropertyChangeListener(this);
        }
        timeout = reader.readInt(TIMEOUT_PROP_NAME, 30000);
        try {
            timeoutMeasure = TimeMeasure.valueOf(reader.readString(TIMEOUT_MEASURE_PROP_NAME, TimeMeasure.Milliseconds.toString()));
        } catch(NumberFormatException | NullPointerException e) {
            timeoutMeasure = TimeMeasure.Milliseconds;
        }
    }

    protected void writeData(XmlObjectBuilder builder){
        if(connection == null){
            builder.add(LEGACY_CONNECTION_PROP_NAME, false);
            builder.add(CONNECTION_NAME_PROP_NAME, "");
        }
        else if(connection.isLegacy()){
            builder.add(LEGACY_CONNECTION_PROP_NAME, true);
            builder.add(SERVER_URI_PROP_NAME, connection.getServerUri());
            if (connection.getFixedId() != null) builder.add(CLIENT_ID_PROP_NAME, connection.getFixedId());
            if (connection.getLogin() != null) {
                builder.add(LOGIN_PROP_NAME, connection.getLogin());
                builder.add(PASSWORD_PROP_NAME, connection.getPassword());
            }
        }
        else {
            builder.add(LEGACY_CONNECTION_PROP_NAME, false);
            builder.add(CONNECTION_NAME_PROP_NAME, connection.getName());
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


    public Connection getConnection(){return connection;}
    public void setConnection(Connection value){
        if(connection == value) return;
        String oldServerUri = null, oldClientId = null, oldLogin = null, oldPassword = null;
        if(connection != null){
            oldServerUri = connection.getServerUri();
            oldClientId = connection.getFixedId();
            oldLogin = connection.getLogin();
            oldPassword = connection.getPassword();
            connection.removePropertyChangeListener(this);
        }
        Connection oldConnection = null;
        connection = value;
        String newServerUri = null, newClientId = null, newLogin = null, newPassword = null;
        if(value != null){
            value.addPropertyChangeListener(this);
            newServerUri = value.getServerUri();
            newClientId = value.getFixedId();
            newPassword = value.getPassword();
            newLogin = value.getLogin();
        }
        notifyPropertyChanged("connection", oldConnection, value);
        if(!Utils.areStringsEqual(newServerUri, oldServerUri, false, true)){
            notifyPropertyChanged("serverUri", oldServerUri, newServerUri);
            firePropertyValueChanged(SERVER_URI_PROP_NAME, oldServerUri, newServerUri);
        }
        if(!Utils.areStringsEqual(newClientId, oldClientId, false, true)){
            notifyPropertyChanged("clientId", oldClientId, newClientId);
            firePropertyValueChanged(CLIENT_ID_PROP_NAME, oldClientId, newClientId);
        }
        if(!Utils.areStringsEqual(newLogin, oldLogin, false, true)) firePropertyValueChanged(LOGIN_PROP_NAME, oldLogin, newLogin);
        if(!Utils.areStringsEqual(newPassword, oldPassword, false, true)) firePropertyValueChanged(PASSWORD_PROP_NAME, oldPassword, newPassword);
    }

    public Connection getLegacyConnection(){return legacyConnection;}

    public String getServerUri() {
        return connection == null ? null: connection.getServerUri();
    }

    public void setServerUri(String value) {
        if(connection != null) connection.setServerUri(value);
    }

    public String getClientId(){
        return connection == null? null: connection.getFixedId();
    }

    public void setClientId(String value){
        if(connection != null) connection.setFixedId(value);
    }

    public String getLogin(){
        return connection == null ? null : connection.getLogin();
    }
    public void setLogin(String value){
        if(connection != null) connection.setLogin(value);
    }

    public String getPassword(){
        return connection == null ? null : connection.getPassword();
    }
    public void setPassword(String value){
        if(connection != null) connection.setPassword(value);
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

    protected boolean setProperty(final String propName, final String publishedPropName, final Object value) {
        Object old;
        synchronized (this) {
            try {
                Field field = null;
                Class curClass = getClass();
                while (field == null && curClass != null) {
                    try {
                        field = curClass.getDeclaredField(propName);
                    } catch (NoSuchFieldException e) {
                        curClass = curClass.getSuperclass();
                    }
                }
                if (field == null)
                    throw new RuntimeException(String.format("Error during access to %s bean property (details: unable to find the underlying field)", propName)); //We may not get here
                field.setAccessible(true);
                old = field.get(this);

                if (value == null) {
                    if (old == null) return false;
                } else {
                    if (value.equals(old)) return false;
                }
                field.set(this, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format("Error during access to %s bean property (details: %s)", propName, e.getMessage() + ")")); //We may not get here
            }
            updateData();
        }
        final Object oldValue = old;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                notifyPropertyChanged(propName, oldValue, value);
                if(publishedPropName != null) firePropertyValueChanged(publishedPropName, oldValue == null ? null : oldValue.toString(), value == null ? null : value.toString());
            }
        });
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getSource() == connection){
            if(Utils.areStringsEqual(evt.getPropertyName(), "serverUri")){
                notifyPropertyChanged("serverUri", (String)evt.getOldValue(), (String)evt.getNewValue());
                firePropertyValueChanged(SERVER_URI_PROP_NAME, (String)evt.getOldValue(), (String)evt.getNewValue());
            }
            else if(Utils.areStringsEqual(evt.getPropertyName(), "fixedId")){
                notifyPropertyChanged("clientId", (String)evt.getOldValue(), (String)evt.getNewValue());
                firePropertyValueChanged(CLIENT_ID_PROP_NAME, (String)evt.getOldValue(), (String)evt.getNewValue());
            }
            else if(Utils.areStringsEqual(evt.getPropertyName(), "login")){
                notifyPropertyChanged("login", (String)evt.getOldValue(), (String)evt.getNewValue());
                firePropertyValueChanged(LOGIN_PROP_NAME, (String)evt.getOldValue(), (String)evt.getNewValue());
            }
            else if(Utils.areStringsEqual(evt.getPropertyName(), "password")){
                notifyPropertyChanged("password", (String)evt.getOldValue(), (String)evt.getNewValue());
                firePropertyValueChanged(PASSWORD_PROP_NAME, (String)evt.getOldValue(), (String)evt.getNewValue());
            }
        }

    }

    @Override
    public void release(){
        if(connection != null) connection.removePropertyChangeListener(this);
        super.release();
    }

    protected boolean waitForMqttOperation(IMqttToken token, CancellationToken cancellationToken, WsdlTestStepResult testStepResult, long maxTime, String errorText) {
        while (!token.isComplete() && token.getException() == null) {
            boolean stopped = cancellationToken.cancelled();
            if (stopped || (maxTime != Long.MAX_VALUE && System.nanoTime() > maxTime)) {
                if (stopped) {
                    testStepResult.setStatus(TestStepResult.TestStepStatus.CANCELED);
                }
                else{
                    testStepResult.addMessage("The test step's timeout has expired.");
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

    private ClientCache getCache(PropertyExpansionContext testRunContext){
        final String CLIENT_CACHE_PROPNAME = "client_cache";
        ClientCache cache = (ClientCache)(testRunContext.getProperty(CLIENT_CACHE_PROPNAME));
        if(cache == null){
            cache = new ClientCache();
            testRunContext.setProperty(CLIENT_CACHE_PROPNAME, cache);
        }
        return cache;
    }

    protected Client getClient(PropertyExpansionContext runContext, WsdlTestStepResult log){
        if(connection == null){
            log.addMessage("Connection for this test step is not selected or is broken.");
            log.setStatus(TestStepResult.TestStepStatus.FAILED);
            return null;
        }
        ConnectionParams actualConnectionParams;
        if(connection.isLegacy()){
            actualConnectionParams = connection.expand(runContext);
            if(!checkConnectionParams(actualConnectionParams, log)) return null;
            try {
                return getCache(runContext).getLegacy(actualConnectionParams);
            }
            catch (MqttException e){
                log.setError(e);
                log.setStatus(TestStepResult.TestStepStatus.FAILED);
                return null;
            }
        }
        else{
            ClientCache cache = getCache(runContext);
            Client result = cache.get(connection.getName());
            if(result == null){
                actualConnectionParams = connection.expand(runContext);
                if(!checkConnectionParams(actualConnectionParams, log)) return null;
                try {
                    result = cache.add(connection.getName(), actualConnectionParams);
                }
                catch (MqttException e){
                    log.setError(e);
                    log.setStatus(TestStepResult.TestStepStatus.FAILED);
                    return null;
                }
            }
            return result;
        }
    }

    private boolean checkConnectionParams(ConnectionParams connectionParams, WsdlTestStepResult log) {
        String uriCheckResult = Utils.checkServerUri(connectionParams.getServerUri());
        if (uriCheckResult == null) return true;
        log.addMessage(uriCheckResult);
        log.setStatus(TestStepResult.TestStepStatus.FAILED);
        return false;
    }

    protected boolean waitForMqttConnection(Client client, CancellationToken cancellationToken, WsdlTestStepResult testStepResult, long maxTime) throws MqttException {
        return waitForMqttOperation(client.getConnectingStatus(), cancellationToken, testStepResult, maxTime, "Unable connect to the MQTT broker.");
    }


    protected void cleanAfterExecution(PropertyExpansionContext runContext) {
        getCache(runContext).assureFinalized();
    }

    @Override
    public void finish(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        cleanAfterExecution(testRunContext);
        super.finish(testRunner, testRunContext);
    }

    public Project getOwningProject(){
        return ModelSupport.getModelItemProject(this);
    }

}
