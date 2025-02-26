package com.smartbear.mqttsupport.teststeps;

import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.support.DefaultTestStepProperty;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.types.StringToObjectMap;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.smartbear.mqttsupport.CancellationToken;
import com.smartbear.mqttsupport.Messages;
import com.smartbear.mqttsupport.Utils;
import com.smartbear.mqttsupport.XmlObjectBuilder;
import com.smartbear.mqttsupport.connection.Client;
import com.smartbear.mqttsupport.connection.ClientCache;
import com.smartbear.mqttsupport.connection.Connection;
import com.smartbear.mqttsupport.connection.ConnectionsManager;
import com.smartbear.mqttsupport.connection.ExpandedConnectionParams;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.Optional;


public abstract class MqttConnectedTestStep extends WsdlTestStepWithProperties implements PropertyChangeListener {
    Logger log = LoggerFactory.getLogger(MqttConnectedTestStep.class);


    public static final int MAX_CONNECTION_ATTEMPTS = 100_000;
    private Connection connection;
    private Connection legacyConnection;

    final static String SERVER_URI_PROP_NAME = "ServerURI";

    final static String CERT_CA_CERT_SETTING_NAME = "CertCAPEM";
    static final String CERT_CLIENT_CERT_SETTING_NAME = "CertClientPEM";
    static final String CERT_KEY_SETTING_NAME = "CertKeyPEM";
    static final String CERT_KEY_PASSWORD_SETTING_NAME = "CertKeyPassword";
    static final String CERT_SNI_SERVER_SETTING_NAME = "CertSniServer";

    final static String CLIENT_ID_PROP_NAME = "ClientID";
    final static String LOGIN_PROP_NAME = "Login";
    final static String PASSWORD_PROP_NAME = "Password";
    final static String CONNECTION_NAME_PROP_NAME = "ConnectionName";
    final static String LEGACY_CONNECTION_PROP_NAME = "LegacyConnection";
    protected final static String TIMEOUT_PROP_NAME = "Timeout";
    private final static String TIMEOUT_MEASURE_PROP_NAME = "TimeoutMeasure";

    public enum TimeMeasure {
        Milliseconds("milliseconds"), Seconds("seconds");
        private String title;

        TimeMeasure(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private int timeout = 30000;
    private TimeMeasure timeoutMeasure = TimeMeasure.Seconds;


    public MqttConnectedTestStep(WsdlTestCase testCase, TestStepConfig config, boolean hasEditor, boolean forLoadTest) {
        super(testCase, config, hasEditor, forLoadTest);
        addProperty(new DefaultTestStepProperty(SERVER_URI_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if (connection == null) {
                    return Messages.EMPTY_STRING;
                } else {
                    return connection.getServerUri();
                }
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if (connection == null) {
                    return;
                }
                connection.setServerUri(s);
            }
        }, this));

        addProperty(new DefaultTestStepProperty(CERT_CA_CERT_SETTING_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if (connection == null) {
                    return Messages.EMPTY_STRING;
                } else {
                    return connection.getCaCrtFile();
                }
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if (connection == null) {
                    return;
                }
                connection.setCaCrtFile(s);
            }
        }, this));
        addProperty(new DefaultTestStepProperty(CERT_CLIENT_CERT_SETTING_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if (connection == null) {
                    return Messages.EMPTY_STRING;
                } else {
                    return connection.getCrtFile();
                }
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if (connection == null) {
                    return;
                }
                connection.setCrtFile(s);
            }
        }, this));
        addProperty(new DefaultTestStepProperty(CERT_KEY_SETTING_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if (connection == null) {
                    return Messages.EMPTY_STRING;
                } else {
                    return connection.getKeyFile();
                }
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if (connection == null) {
                    return;
                }
                connection.setKeyFile(s);
            }
        }, this));
        addProperty(new DefaultTestStepProperty(CERT_KEY_PASSWORD_SETTING_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if (connection == null) {
                    return Messages.EMPTY_STRING;
                } else {
                    return connection.getKeysPassword();
                }
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if (connection == null) {
                    return;
                }
                connection.setKeysPassword(s);
            }
        }, this));
        addProperty(new DefaultTestStepProperty(CERT_SNI_SERVER_SETTING_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if (connection == null) {
                    return Messages.EMPTY_STRING;
                } else {
                    return connection.getSniHost();
                }
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if (connection == null) {
                    return;
                }
                connection.setSniHost(s);
            }
        }, this));

        addProperty(new DefaultTestStepProperty(CLIENT_ID_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if (connection == null) {
                    return Messages.EMPTY_STRING;
                } else {
                    return connection.getFixedId();
                }
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if (connection == null) {
                    return;
                }
                connection.setFixedId(s);
            }
        }, this));
        addProperty(new DefaultTestStepProperty(LOGIN_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if (connection == null) {
                    return Messages.EMPTY_STRING;
                } else {
                    return connection.getLogin();
                }
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if (connection == null) {
                    return;
                }
                connection.setLogin(s);
            }
        }, this));
        addProperty(new DefaultTestStepProperty(PASSWORD_PROP_NAME, false, new DefaultTestStepProperty.PropertyHandler() {
            @Override
            public String getValue(DefaultTestStepProperty defaultTestStepProperty) {
                if (connection == null) {
                    return Messages.EMPTY_STRING;
                } else {
                    return connection.getPassword();
                }
            }

            @Override
            public void setValue(DefaultTestStepProperty defaultTestStepProperty, String s) {
                if (connection == null) {
                    return;
                }
                connection.setPassword(s);
            }
        }, this));
    }


    protected void readData(XmlObjectConfigurationReader reader) {
        if (connection != null) {
            connection.removePropertyChangeListener(this);
            connection = null;
        }
        if (reader.readBoolean(LEGACY_CONNECTION_PROP_NAME, true)) {
            connection = new Connection();
            connection.setServerUri(reader.readString(SERVER_URI_PROP_NAME, Messages.EMPTY_STRING));

            connection.setCaCrtFile(reader.readString(CERT_CA_CERT_SETTING_NAME, Messages.EMPTY_STRING));
            connection.setCrtFile(reader.readString(CERT_CLIENT_CERT_SETTING_NAME, Messages.EMPTY_STRING));
            connection.setKeyFile(reader.readString(CERT_KEY_SETTING_NAME, Messages.EMPTY_STRING));
            connection.setKeysPassword(reader.readString(CERT_KEY_PASSWORD_SETTING_NAME, Messages.EMPTY_STRING));
            connection.setSniHost(reader.readString(CERT_SNI_SERVER_SETTING_NAME, Messages.EMPTY_STRING));

            connection.setFixedId(reader.readString(CLIENT_ID_PROP_NAME, Messages.EMPTY_STRING));
            connection.setLogin(reader.readString(LOGIN_PROP_NAME, Messages.EMPTY_STRING));
            connection.setPassword(reader.readString(PASSWORD_PROP_NAME, Messages.EMPTY_STRING));
            connection.setCleanSession(true);
            connection.addPropertyChangeListener(this);
            legacyConnection = connection;
        } else {
            String connectionName = reader.readString(CONNECTION_NAME_PROP_NAME, Messages.EMPTY_STRING);
            if (StringUtils.hasContent(connectionName)) {
                connection = ConnectionsManager.getConnection(this, connectionName);
            }
            if (connection != null) {
                connection.addPropertyChangeListener(this);
            }
        }
        timeout = reader.readInt(TIMEOUT_PROP_NAME, 30000);
        try {
            timeoutMeasure = TimeMeasure.valueOf(reader.readString(TIMEOUT_MEASURE_PROP_NAME, TimeMeasure.Milliseconds.toString()));
        } catch (NumberFormatException | NullPointerException e) {
            timeoutMeasure = TimeMeasure.Milliseconds;
        }
    }

    protected void writeData(XmlObjectBuilder builder) {
        if (connection == null) {
            builder.add(LEGACY_CONNECTION_PROP_NAME, false);
            builder.add(CONNECTION_NAME_PROP_NAME, Messages.EMPTY_STRING);
        } else if (connection.isLegacy()) {
            builder.add(LEGACY_CONNECTION_PROP_NAME, true);
            builder.add(SERVER_URI_PROP_NAME, connection.getServerUri());
            if (connection.getFixedId() != null) {
                builder.add(CLIENT_ID_PROP_NAME, connection.getFixedId());
            }
            if (connection.getLogin() != null) {
                builder.add(LOGIN_PROP_NAME, connection.getLogin());
                builder.add(PASSWORD_PROP_NAME, connection.getPassword());
            }
        } else {
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
        if (getConfig() == null) {
            return;
        }
        updateData(getConfig());
    }


    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection value) {
        if (connection == value) {
            return;
        }
        String oldServerUri = null, oldClientId = null, oldLogin = null, oldPassword = null;
        Connection oldConnection = connection;
        if (oldConnection != null) {
            oldServerUri = oldConnection.getServerUri();
            oldClientId = oldConnection.getFixedId();
            oldLogin = oldConnection.getLogin();
            oldPassword = oldConnection.getPassword();
            oldConnection.removePropertyChangeListener(this);
        }
        connection = value;
        String newServerUri = null, newClientId = null, newLogin = null, newPassword = null;
        if (value != null) {
            value.addPropertyChangeListener(this);
            newServerUri = value.getServerUri();
            newClientId = value.getFixedId();
            newPassword = value.getPassword();
            newLogin = value.getLogin();
        }
        updateData();
        notifyPropertyChanged("connection", oldConnection, value);
        if (!Utils.areStringsEqual(newServerUri, oldServerUri, false, true)) {
            notifyPropertyChanged("serverUri", oldServerUri, newServerUri);
            firePropertyValueChanged(SERVER_URI_PROP_NAME, oldServerUri, newServerUri);
        }
        if (!Utils.areStringsEqual(newClientId, oldClientId, false, true)) {
            notifyPropertyChanged("clientId", oldClientId, newClientId);
            firePropertyValueChanged(CLIENT_ID_PROP_NAME, oldClientId, newClientId);
        }
        if (!Utils.areStringsEqual(newLogin, oldLogin, false, true)) {
            firePropertyValueChanged(LOGIN_PROP_NAME, oldLogin, newLogin);
        }
        if (!Utils.areStringsEqual(newPassword, oldPassword, false, true)) {
            firePropertyValueChanged(PASSWORD_PROP_NAME, oldPassword, newPassword);
        }
    }

    public Connection getLegacyConnection() {
        return legacyConnection;
    }

    public String getServerUri() {
        return connection == null ? null : connection.getServerUri();
    }

    public void setServerUri(String value) {
        if (connection != null) {
            connection.setServerUri(value);
        }
    }

    public String getClientId() {
        return connection == null ? null : connection.getFixedId();
    }

    public void setClientId(String value) {
        if (connection != null) {
            connection.setFixedId(value);
        }
    }

    public String getLogin() {
        return connection == null ? null : connection.getLogin();
    }

    public void setLogin(String value) {
        if (connection != null) {
            connection.setLogin(value);
        }
    }

    public String getPassword() {
        return connection == null ? null : connection.getPassword();
    }

    public void setPassword(String value) {
        if (connection != null) {
            connection.setPassword(value);
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int newValue) {
        int oldShownTimeout = getShownTimeout();
        if (setIntProperty("timeout", TIMEOUT_PROP_NAME, newValue, 0, Integer.MAX_VALUE)) {
            if (timeoutMeasure == TimeMeasure.Seconds && (newValue % 1000) != 0) {
                timeoutMeasure = TimeMeasure.Milliseconds;
                notifyPropertyChanged("timeoutMeasure", TimeMeasure.Seconds, TimeMeasure.Milliseconds);
            }
            int newShownTimeout = getShownTimeout();
            if (oldShownTimeout != newShownTimeout) {
                notifyPropertyChanged("shownTimeout", oldShownTimeout, newShownTimeout);
            }
        }
    }

    public int getShownTimeout() {
        if (timeoutMeasure == TimeMeasure.Milliseconds) {
            return timeout;
        } else {
            return timeout / 1000;
        }
    }

    public void setShownTimeout(int newValue) {
        if (timeoutMeasure == TimeMeasure.Milliseconds) {
            setTimeout(newValue);
        } else if (timeoutMeasure == TimeMeasure.Seconds) {
            if ((long) newValue * 1000 > Integer.MAX_VALUE) {
                setTimeout(Integer.MAX_VALUE / 1000 * 1000);
            } else {
                setTimeout(newValue * 1000);
            }
        }
    }

    public TimeMeasure getTimeoutMeasure() {
        return timeoutMeasure;
    }

    public void setTimeoutMeasure(TimeMeasure newValue) {
        if (newValue == null) {
            return;
        }
        TimeMeasure oldValue = timeoutMeasure;
        if (oldValue == newValue) {
            return;
        }
        int oldShownTimeout = getShownTimeout();
        timeoutMeasure = newValue;
        notifyPropertyChanged("timeoutMeasure", oldValue, newValue);
        if (newValue == TimeMeasure.Milliseconds) {
            setIntProperty("timeout", TIMEOUT_PROP_NAME, oldShownTimeout, 0, Integer.MAX_VALUE);
        } else if (newValue == TimeMeasure.Seconds) {
            if ((long) oldShownTimeout * 1000 > Integer.MAX_VALUE) {
                setIntProperty("timeout", TIMEOUT_PROP_NAME, Integer.MAX_VALUE / 1000 * 1000);
                notifyPropertyChanged("shownTimeout", oldShownTimeout, getShownTimeout());
            } else {
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
                if (field == null) {
                    throw new RuntimeException(String.format(Messages.ERROR_DURING_ACCESS_TO_S_BEAN_PROPERTY_DETAILS_UNABLE_TO_FIND_THE_UNDERLYING_FIELD, propName)); //We may not get here
                }
                field.setAccessible(true);
                old = field.get(this);

                if (value == null) {
                    if (old == null) {
                        return false;
                    }
                } else {
                    if (value.equals(old)) {
                        return false;
                    }
                }
                field.set(this, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format(Messages.ERROR_DURING_ACCESS_TO_S_BEAN_PROPERTY_DETAILS_S, propName, e.getMessage() + Messages.CLOSE_BRACKET)); //We may not get here
            }
            updateData();
        }
        final Object oldValue = old;
        if (SwingUtilities.isEventDispatchThread()) {
            notifyPropertyChanged(propName, oldValue, value);
            if (publishedPropName != null) {
                firePropertyValueChanged(publishedPropName, oldValue == null ? null : oldValue.toString(), value == null ? null : value.toString());
            }
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    notifyPropertyChanged(propName, oldValue, value);
                    if (publishedPropName != null) {
                        firePropertyValueChanged(publishedPropName, oldValue == null ? null : oldValue.toString(), value == null ? null : value.toString());
                    }
                }
            });
        }
        ;
        return true;
    }


    protected boolean setIntProperty(String propName, String publishedPropName, int value, int minAllowed, int maxAllowed) {
        if (value < minAllowed || value > maxAllowed) {
            return false;
        }
        return setIntProperty(propName, publishedPropName, value);
    }

    protected boolean setIntProperty(String propName, String publishedPropName, int value) {
        int old;
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
            if (field == null) {
                throw new RuntimeException(String.format(Messages.ERROR_DURING_ACCESS_TO_S_BEAN_PROPERTY_DETAILS_UNABLE_TO_FIND_THE_UNDERLYING_FIELD, propName)); //We may not get here
            }
            field.setAccessible(true);
            old = (int) (field.get(this));

            if (old == value) {
                return false;
            }
            field.set(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format(Messages.ERROR_DURING_ACCESS_TO_S_BEAN_PROPERTY_DETAILS_S, propName, e.getMessage() + Messages.CLOSE_BRACKET)); //We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        if (StringUtils.hasContent(publishedPropName)) {
            firePropertyValueChanged(publishedPropName, Integer.toString(old), Integer.toString(value));
        }
        return true;
    }

    protected void setBooleanProperty(String propName, String publishedPropName, boolean value) {
        boolean old;
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
            if (field == null) {
                throw new RuntimeException(String.format(Messages.ERROR_DURING_ACCESS_TO_S_BEAN_PROPERTY_DETAILS_UNABLE_TO_FIND_THE_UNDERLYING_FIELD, propName)); //We may not get here
            }
            field.setAccessible(true);
            old = (boolean) (field.get(this));

            if (old == value) {
                return;
            }
            field.set(this, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format(Messages.ERROR_DURING_ACCESS_TO_S_BEAN_PROPERTY_DETAILS_S, propName, e.getMessage() + Messages.CLOSE_BRACKET)); //We may not get here
        }
        updateData();
        notifyPropertyChanged(propName, old, value);
        if (StringUtils.hasContent(publishedPropName)) {
            firePropertyValueChanged(publishedPropName, Boolean.toString(old), Boolean.toString(value));
        }

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == connection) {
            if (Utils.areStringsEqual(evt.getPropertyName(), "serverUri")) {
                if (connection.isLegacy()) {
                    updateData();
                }
                notifyPropertyChanged("serverUri", (String) evt.getOldValue(), (String) evt.getNewValue());
                firePropertyValueChanged(SERVER_URI_PROP_NAME, (String) evt.getOldValue(), (String) evt.getNewValue());
            } else if (Utils.areStringsEqual(evt.getPropertyName(), Connection.CLIENT_ID_BEAN_PROP)) {
                if (connection.isLegacy()) {
                    updateData();
                }
                notifyPropertyChanged("clientId", (String) evt.getOldValue(), (String) evt.getNewValue());
                firePropertyValueChanged(CLIENT_ID_PROP_NAME, (String) evt.getOldValue(), (String) evt.getNewValue());
            } else if (Utils.areStringsEqual(evt.getPropertyName(), Connection.LOGIN_BEAN_PROP)) {
                if (connection.isLegacy()) {
                    updateData();
                }
                notifyPropertyChanged("login", (String) evt.getOldValue(), (String) evt.getNewValue());
                firePropertyValueChanged(LOGIN_PROP_NAME, (String) evt.getOldValue(), (String) evt.getNewValue());
            } else if (Utils.areStringsEqual(evt.getPropertyName(), Connection.PASSWORD_BEAN_PROP)) {
                if (connection.isLegacy()) {
                    updateData();
                }
                notifyPropertyChanged("password", (String) evt.getOldValue(), (String) evt.getNewValue());
                firePropertyValueChanged(PASSWORD_PROP_NAME, (String) evt.getOldValue(), (String) evt.getNewValue());
            } else if (Utils.areStringsEqual(evt.getPropertyName(), Connection.NAME_BEAN_PROP)) {
                updateData();
            }
        }

    }

    @Override
    public void release() {
        if (connection != null) {
            connection.removePropertyChangeListener(this);
        }
        MqttClientCache.INSTANCE.purge();
        super.release();
    }

    protected boolean waitForMqttOperation(IMqttToken token, CancellationToken cancellationToken, WsdlTestStepResult testStepResult, long maxTime, String errorText) {
        if (token == null) {
            log.error(Messages.CONNECTION_TOKEN_IS_NULL);
            return false;
        }
        long timeout = (maxTime - System.nanoTime()) / 1000_000_000;
        while (!token.isComplete()) {
            if (cancellationToken.cancelled()) {
                testStepResult.setStatus(TestStepResult.TestStepStatus.CANCELED);
                //log.error(Messages.CANCELED_ON_CONNECT);
                return false;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                //log.error(Messages.INTERRUPTED_ON_CONNECT);
                break;
            }
            if (token.getException() != null) {
                //log.error(Messages.EXCEPTION_ON_CONNECT);
                break;
            }
            if (maxTime != Long.MAX_VALUE && System.nanoTime() > maxTime) {
                testStepResult.addMessage(Messages.THE_TEST_STEP_S_TIMEOUT_HAS_EXPIRED);
                testStepResult.setStatus(TestStepResult.TestStepStatus.FAILED);
                //log.error(Messages.TIMEOUT_ON_CONNECT);
                return false;
            }
        }
        if (token.getException() != null) {
            testStepResult.addMessage(errorText);
            testStepResult.addMessage(Messages.EXCEPTION_TEXT + token.getException());
            testStepResult.setError(token.getException());
            testStepResult.setStatus(TestStepResult.TestStepStatus.FAILED);

            //log.error(Messages.EXCEPTION_TEXT + token.getException());
            return false;
        }

        if (!token.isComplete()) {
            //log.warn(Messages.TOKEN_NOT_COMPLETED);
        }
        return true;
    }

    private ClientCache getCache(PropertyExpansionContext testRunContext) {
        StringToObjectMap vuc = (StringToObjectMap) testRunContext.getProperty("VirtualUserContext");
        if (vuc != null) {
            //in a performance test
            Object vuId = vuc.get("VirtualUserId");
            if (vuId != null) {
                return MqttClientCache.INSTANCE.getOrCreate(vuId.toString());
            }
        }
        //regular case (api, functional test)
        return MqttClientCache.INSTANCE.getOrCreate("DefaultConnection");
    }

    protected Client getClient(PropertyExpansionContext runContext, WsdlTestStepResult log) {
        if (connection == null) {
            log.addMessage(Messages.CONNECTION_FOR_THIS_TEST_STEP_IS_NOT_SELECTED_OR_IS_BROKEN);
            log.setStatus(TestStepResult.TestStepStatus.FAILED);
            return null;
        }
        ExpandedConnectionParams actualConnectionParams;
        try {
            actualConnectionParams = connection.expand(runContext);
        } catch (Exception e) {
            log.addMessage(e.getMessage());
            log.setStatus(TestStepResult.TestStepStatus.FAILED);
            return null;
        }
        if (connection.isLegacy()) {
            if (!checkConnectionParams(actualConnectionParams, log)) {
                return null;
            }
            try {
                return getCache(runContext).getLegacy(actualConnectionParams);
            } catch (MqttException e) {
                log.setError(e);
                log.setStatus(TestStepResult.TestStepStatus.FAILED);
                return null;
            }
        } else {
            ClientCache cache = getCache(runContext);
            Client result = cache.get(connection.getName());
            if (isDisconnectedAndNotConnecting(result) || credentialsChanged(result, actualConnectionParams)) {
                cache.invalidate(connection.getName());
                //this.log.error(Messages.INVALID_CLIENT_IN_CACHE + result.toString());
                result = null;
            }
            if (result == null) {
                try {
                    actualConnectionParams = connection.expand(runContext);
                } catch (Exception e) {
                    log.addMessage(Messages.UNABLE_TO_EXPAND_CONTEXT);
                    log.addMessage(e.getMessage());
                    log.setStatus(TestStepResult.TestStepStatus.FAILED);
                    return null;
                }
                if (!checkConnectionParams(actualConnectionParams, log)) {
                    log.addMessage(Messages.UNABLE_TO_CHECK_CONNECTION);
                    return null;
                }
                try {
                    result = cache.add(connection.getName(), actualConnectionParams);
                } catch (MqttException e) {
                    log.addMessage(Messages.UNABLE_TO_STORE_CONNECTION_IN_CACHE);
                    log.setError(e);
                    log.setStatus(TestStepResult.TestStepStatus.FAILED);
                    return null;
                }
            } else {
            }
            return result;
        }
    }
    private boolean isDisconnectedAndNotConnecting(Client result) {
        return Optional.ofNullable(result)
                .map(r -> !r.isConnected() && !r.isConnecting())
                .orElse(false);
    }

    private boolean credentialsChanged(Client result, ExpandedConnectionParams connectionParams) {
        return Optional.ofNullable(result)
                .map(r -> Optional.ofNullable(r.getConnectionOptions())
                    .map(connectOptions -> connectOptions.getUserName().equals(connectionParams.getLogin())
                            && String.valueOf(connectOptions.getPassword()).equals(connectionParams.getPassword()))
                    .orElse(false))
                .orElse(false);
   }

    private boolean checkConnectionParams(ExpandedConnectionParams connectionParams, WsdlTestStepResult log) {
        String uriCheckResult = Utils.checkServerUri(connectionParams.getServerUri());
        if (uriCheckResult == null) {
            return true;
        }
        log.addMessage(uriCheckResult);
        log.setStatus(TestStepResult.TestStepStatus.FAILED);
        return false;
    }

    protected boolean waitForMqttConnection(Client client, CancellationToken cancellationToken, WsdlTestStepResult testStepResult, long maxTime) throws MqttException {
        if (client != null && client.isConnected()) {
            return true;
        }

        long timeout;
        if (maxTime == Long.MAX_VALUE) {
            timeout = 0;
        } else {
            timeout = maxTime - System.nanoTime();
            if (timeout <= 0) {
                testStepResult.addMessage(Messages.UNABLE_TO_CONNECT_TO_THE_SERVER_DUE_TO_TIMEOUT);
                testStepResult.setStatus(TestStepResult.TestStepStatus.FAILED);
                return false;
            }
        }
        for (int i = 0; i < MAX_CONNECTION_ATTEMPTS; i++) {
            IMqttToken token = client.getConnectingStatus(timeout);
            /*
            if (!client.isConnecting() && !client.isConnected()) {
                if (client.isClosed()) {
                    log.error(Messages.CLIENT_IS_CLOSED);
                } else {
                    log.error(Messages.CLIENT_IN_INVALID_INITIAL_STATE);
                }
            }
            */
            if (token == null) {
                //log.error(Messages.NULL_CONNECTION_TOKEN);
                continue;
            }
            if (client.isDisconnected()) {
                //log.error(Messages.CLIENT_IS_DISCONNECTED);
            }
            if (!waitForMqttOperation(token, cancellationToken, testStepResult, maxTime,
                    Messages.UNABLE_CONNECT_TO_THE_MQTT_BROKER)) {
                return false;
            }
            /*
            if (client.isDisconnected()) {
                log.error(Messages.CLIENT_BECAME_DISCONNECTED + client.getClientObject().getClientId());
            }
            */
            if (cancellationToken.cancelled()) {
                return false;
            }
            if (!client.isConnected()) {
                /*
                if (token.isComplete()) {
                    if (token.getClient() != client.getClientObject()) {
                        log.error(Messages.INVALID_CLIENT_OF_THE_TOKEN);
                    } else {
                        log.warn(String.format(Messages.NOT_CONNECTED_WITH_COMPLETED_TOKEN_TO_CONNECT_FROM_D_ATTEMPT_S,
                                i, token.getResponse().toString()));
                    }
                } else {
                    log.warn(String.format(Messages.UNABLE_TO_CONNECT_FROM_D_ATTEMPT, i));
                }
                */
                continue;
            }
            return true;
        }
        return false;
    }

    public Project getOwningProject() {
        return ModelSupport.getModelItemProject(this);
    }

    protected ExecutableTestStepResult reportError(ExecutableTestStepResult result,
                                                   String message, CancellationToken cancellationToken) {
        if (cancellationToken.cancelled()) {
            result.setStatus(TestStepResult.TestStepStatus.CANCELED);
        } else {
            if (message != null) {
                result.addMessage(message);
            }
            result.setStatus(TestStepResult.TestStepStatus.FAILED);
        }
        return result;
    }

}
