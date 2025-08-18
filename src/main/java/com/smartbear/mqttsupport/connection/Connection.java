package com.smartbear.mqttsupport.connection;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.PropertyChangeNotifier;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.smartbear.mqttsupport.Utils;
import com.smartbear.mqttsupport.teststeps.PublishTestStep;
import com.smartbear.mqttsupport.XmlObjectBuilder;
import com.smartbear.mqttsupport.teststeps.PublishedMessageType;
import org.apache.commons.ssl.OpenSSL;
import org.apache.xmlbeans.XmlObject;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;

import static com.smartbear.mqttsupport.Utils.bytesToHexString;
import static com.smartbear.mqttsupport.Utils.hexStringToBytes;

public class Connection implements PropertyChangeNotifier {
    public final static boolean ARE_NAMES_CASE_INSENSITIVE = true;

    private final static String NAME_SETTING_NAME = "Name";
    private static final String SERVER_URI_SETTING_NAME = "ServerURI";

    public static final String CERT_CA_CERT_SETTING_NAME = "caCrtFile";
    public static final String CERT_CLIENT_CERT_SETTING_NAME = "crtFile";
    public static final String CERT_KEY_SETTING_NAME = "keyFile";
    public static final String CERT_KEY_PASSWORD_SETTING_NAME = "keysPassword";
    public static final String CERT_SNI_SERVER_SETTING_NAME = "sniHost";

    private static final String CLIENT_ID_SETTING_NAME = "ClientID";
    private static final String LOGIN_SETTING_NAME = "Login";
    private static final String ENCR_PASSWORD_SETTING_NAME = "EncrPassword";
    private static final String CLEAN_SESSION_SETTING_NAME = "CleanSession";
    private static final String WILL_TOPIC_SETTING_NAME = "WillTopic";
    private static final String WILL_MESSAGE_TYPE_SETTING_NAME = "WillMessageType";
    private static final String WILL_MESSAGE_SETTING_NAME = "WillMessage";
    private static final String WILL_QOS_SETTING_NAME = "WillQos";
    private final static String WILL_RETAINED_SETTING_NAME = "WillRetained";
    private static final String PASSWORD_FOR_ENCODING = "{CB012CCB-6D9C-4c3d-8A82-06B54D546512}";
    public static final String ENCRYPTION_METHOD = "des3";

    private String name;
    private String originalServerUri;

    private String caCrtFile;
    private String crtFile;
    private String keyFile;
    private String keysPassword;
    private String sniHost;

    private String fixedId;
    private String login;
    private String password;
    private boolean cleanSession;
    private String willTopic;
    private PublishedMessageType willMessageType = PublishTestStep.DEFAULT_MESSAGE_TYPE;
    private String willMessage;
    private int willQos = PublishTestStep.DEFAULT_QOS;
    private boolean willRetained;

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public Connection() {
    }

    public Connection(String name, ConnectionParams params) {
        this();
        this.name = name;
        setParams(params);
    }


    public void load(XmlObject xml) {
        XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(xml);
        name = reader.readString(NAME_SETTING_NAME, null);
        originalServerUri = reader.readString(SERVER_URI_SETTING_NAME, null);

        caCrtFile = reader.readString(CERT_CA_CERT_SETTING_NAME, null);
        crtFile = reader.readString(CERT_CLIENT_CERT_SETTING_NAME, null);
        keyFile = reader.readString(CERT_KEY_SETTING_NAME, null);
        keysPassword = reader.readString(CERT_KEY_PASSWORD_SETTING_NAME, null);
        sniHost = reader.readString(CERT_SNI_SERVER_SETTING_NAME, null);

        fixedId = reader.readString(CLIENT_ID_SETTING_NAME, null);
        login = reader.readString(LOGIN_SETTING_NAME, null);
        password = null;
        String encodedPasswordString = reader.readString(ENCR_PASSWORD_SETTING_NAME, null);
        if (encodedPasswordString != null && encodedPasswordString.length() != 0) {
            byte[] encodedPassword = hexStringToBytes(encodedPasswordString);
            try {
                password = new String(OpenSSL.decrypt(ENCRYPTION_METHOD, PASSWORD_FOR_ENCODING.toCharArray(), encodedPassword));
            } catch (Throwable e) {
                SoapUI.logError(e);
            }
        }

        cleanSession = reader.readBoolean(CLEAN_SESSION_SETTING_NAME, false);
        willTopic = reader.readString(WILL_TOPIC_SETTING_NAME, null);
        if (willTopic != null && willTopic.length() != 0) {
            willMessageType = PublishedMessageType.fromString(reader.readString(WILL_MESSAGE_TYPE_SETTING_NAME, null));
            if (willMessageType == null) {
                willMessageType = PublishTestStep.DEFAULT_MESSAGE_TYPE;
            }
            willMessage = reader.readString(WILL_MESSAGE_SETTING_NAME, null);
            willQos = reader.readInt(WILL_QOS_SETTING_NAME, PublishTestStep.DEFAULT_QOS);
            willRetained = reader.readBoolean(WILL_RETAINED_SETTING_NAME, false);
        }
    }

    public XmlObject save() {
        XmlObjectBuilder builder = new XmlObjectBuilder();
        builder.add(NAME_SETTING_NAME, name);
        builder.add(SERVER_URI_SETTING_NAME, originalServerUri);

        builder.add(CERT_CA_CERT_SETTING_NAME, caCrtFile);
        builder.add(CERT_CLIENT_CERT_SETTING_NAME, crtFile);
        builder.add(CERT_KEY_SETTING_NAME, keyFile);
        builder.add(CERT_KEY_PASSWORD_SETTING_NAME, keysPassword);
        builder.add(CERT_SNI_SERVER_SETTING_NAME, sniHost);

        builder.add(CLIENT_ID_SETTING_NAME, fixedId);
        builder.add(CLEAN_SESSION_SETTING_NAME, cleanSession);
        if (login != null) {
            builder.add(LOGIN_SETTING_NAME, login);
            if (password != null && password.length() != 0) {
                byte[] encodedPassword = null;
                try {
                    encodedPassword = OpenSSL.encrypt(ENCRYPTION_METHOD, PASSWORD_FOR_ENCODING.toCharArray(), password.getBytes());
                } catch (IOException | GeneralSecurityException e) {
                    SoapUI.logError(e);
                }
                builder.add(ENCR_PASSWORD_SETTING_NAME, bytesToHexString(encodedPassword));
            }
        }
        if (willTopic != null && willTopic.length() != 0) {
            builder.add(WILL_TOPIC_SETTING_NAME, willTopic);
            builder.add(WILL_MESSAGE_TYPE_SETTING_NAME, willMessageType.name());
            builder.add(WILL_MESSAGE_SETTING_NAME, willMessage);
            builder.add(WILL_QOS_SETTING_NAME, willQos);
            builder.add(WILL_RETAINED_SETTING_NAME, willRetained);
        }
        return builder.finish();
    }

    public String getServerUri() {
        return originalServerUri;
    }

    public void setServerUri(String serverUri) {
        String oldServerUri = getServerUri();
        if (serverUri == null) {
            serverUri = "";
        }
        this.originalServerUri = serverUri;
        if (!Utils.areStringsEqual(oldServerUri, this.originalServerUri)) {
            notifyPropertyChanged("serverUri", oldServerUri, this.originalServerUri);
        }
    }

    public String getCaCrtFile() {
        return caCrtFile;
    }

    public void setCaCrtFile(String caCrtFile) {
        String oldcCACrtFile = getCaCrtFile();
        if (caCrtFile == null) {
            caCrtFile = "";
        }
        this.caCrtFile = caCrtFile;
        if (!Utils.areStringsEqual(oldcCACrtFile, this.caCrtFile)) {
            notifyPropertyChanged(CERT_CA_CERT_SETTING_NAME, oldcCACrtFile, this.caCrtFile);
        }
    }

    public String getCrtFile() {
        return crtFile;
    }

    public void setCrtFile(String crtFile) {
        String oldCrtFile = getCrtFile();
        if (crtFile == null) {
            crtFile = "";
        }
        this.crtFile = crtFile;
        if (!Utils.areStringsEqual(oldCrtFile, this.crtFile)) {
            notifyPropertyChanged(CERT_CLIENT_CERT_SETTING_NAME, oldCrtFile, this.crtFile);
        }
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        String oldKeyFile = getKeyFile();
        if (keyFile == null) {
            keyFile = "";
        }
        this.keyFile = keyFile;
        if (!Utils.areStringsEqual(oldKeyFile, this.keyFile)) {
            notifyPropertyChanged(CERT_KEY_SETTING_NAME, oldKeyFile, this.keyFile);
        }
    }

    public String getKeysPassword() {
        return keysPassword;
    }

    public void setKeysPassword(String keysPassword) {
        String oldKeysPassword = getKeysPassword();
        if (keysPassword == null) {
            keysPassword = "";
        }
        this.keysPassword = keysPassword;
        if (!Utils.areStringsEqual(oldKeysPassword, this.keysPassword)) {
            notifyPropertyChanged(CERT_KEY_PASSWORD_SETTING_NAME, oldKeysPassword, this.keysPassword);
        }
    }

    public String getSniHost() {
        return sniHost;
    }

    public void setSniHost(String sniHost) {
        String oldSniHost = getSniHost();
        if (sniHost == null) {
            sniHost = "";
        }
        this.sniHost = sniHost;
        if (!Utils.areStringsEqual(oldSniHost, this.sniHost)) {
            notifyPropertyChanged(CERT_SNI_SERVER_SETTING_NAME, oldSniHost, this.sniHost);
        }
    }

    public final static String NAME_BEAN_PROP = "name";

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        String oldName = name;
        if (!Utils.areStringsEqual(oldName, newName, ARE_NAMES_CASE_INSENSITIVE, true)) {
            boolean oldLegacy = isLegacy();
            name = newName;
            notifyPropertyChanged(NAME_BEAN_PROP, oldName, newName);
            if (isLegacy() != oldLegacy) {
                notifyPropertyChanged("legacy", oldLegacy, isLegacy());
            }
        }
    }

    public boolean isLegacy() {
        return StringUtils.isNullOrEmpty(name);
    }


    public final static String CLIENT_ID_BEAN_PROP = "fixedId";

    public String getFixedId() {
        return fixedId;
    }

    public void setFixedId(String value) {
        String oldId = getFixedId();
        if (!Utils.areStringsEqual(oldId, value, false, true)) {
            boolean oldGeneratedId = isGeneratedId();
            fixedId = value;
            notifyPropertyChanged(CLIENT_ID_BEAN_PROP, oldId, value);
            if (isGeneratedId() != oldGeneratedId) {
                notifyPropertyChanged(IS_GENERATED_ID_BEAN_PROP, oldGeneratedId, isGeneratedId());
            }
        }
    }

    public final static String IS_GENERATED_ID_BEAN_PROP = "generatedId";

    public boolean isGeneratedId() {
        return fixedId == null || fixedId.equals("");
    }


    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public final static String LOGIN_BEAN_PROP = "login";

    public void setLogin(String newValue) {
        String old = getLogin();
        if (!Utils.areStringsEqual(old, newValue, false, true)) {
            login = newValue;
            notifyPropertyChanged(LOGIN_BEAN_PROP, old, newValue);
        }
    }

    public final static String PASSWORD_BEAN_PROP = "password";

    public void setPassword(String newValue) {
        String old = getPassword();
        if (!Utils.areStringsEqual(old, newValue, false, true)) {
            password = newValue;
            notifyPropertyChanged(PASSWORD_BEAN_PROP, old, newValue);
        }
    }

    public void setCredentials(String login, String password) {
        if (login == null || login.isEmpty()) {
            setLogin(login);
            setPassword(null);
        } else {
            setLogin(login);
            setPassword(password);
        }
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public final static String CLEAN_SESSION_BEAN_PROP = "cleanSession";

    public void setCleanSession(boolean newValue) {
        boolean old = isCleanSession();
        if (old != newValue) {
            cleanSession = newValue;
            notifyPropertyChanged(CLEAN_SESSION_BEAN_PROP, old, newValue);
        }
    }

    public final static String WILL_TOPIC_BEAN_PROP = "willTopic";

    public String getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(String newValue) {
        String old = getWillTopic();
        if (!Utils.areStringsEqual(old, newValue, false, true)) {
            willTopic = newValue;
            notifyPropertyChanged(WILL_TOPIC_BEAN_PROP, old, newValue);
        }
    }

    public final static String WILL_MESSAGE_TYPE_BEAN_PROP = "willMessageType";

    public PublishedMessageType getWillMessageType() {
        return willMessageType;
    }

    public void setWillMessageType(PublishedMessageType newValue) {
        PublishedMessageType old = getWillMessageType();
        if (old != newValue) {
            willMessageType = newValue;
            notifyPropertyChanged(WILL_MESSAGE_TYPE_BEAN_PROP, old, newValue);
        }
    }

    public final static String WILL_MESSAGE_BEAN_PROP = "willMessage";

    public String getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(String newValue) {
        String old = getWillMessage();
        if (!Utils.areStringsEqual(old, newValue, false, true)) {
            willMessage = newValue;
            notifyPropertyChanged(WILL_MESSAGE_BEAN_PROP, old, newValue);
        }
    }

    public final static String WILL_QOS_BEAN_PROP = "willQos";

    public int getWillQos() {
        return willQos;
    }

    public void setWillQos(int newValue) {
        int old = getWillQos();
        if (old != newValue) {
            willQos = newValue;
            notifyPropertyChanged(WILL_QOS_BEAN_PROP, old, newValue);
        }
    }

    public final static String WILL_RETAINED_BEAN_PROP = "willRetained";

    public boolean isWillRetained() {
        return willRetained;
    }

    public void setWillRetained(boolean newValue) {
        boolean old = isWillRetained();
        if (old != newValue) {
            willRetained = newValue;
            notifyPropertyChanged(WILL_RETAINED_BEAN_PROP, old, newValue);
        }
    }

    public ConnectionParams getParams() {
        return new ConnectionParams(getServerUri(),
                getCaCrtFile(), getCrtFile(), getKeyFile(), getKeysPassword(), getSniHost(),
                getFixedId(), getLogin(), getPassword(), isCleanSession(), getWillTopic(), getWillMessageType(), getWillMessage(), getWillQos(), isWillRetained());
    }

    public void setParams(ConnectionParams params) {
        setServerUri(params.getServerUri());

        setCaCrtFile(params.getCaCrtFile());
        setCrtFile(params.getCrtFile());
        setKeyFile(params.getKeyFile());
        setKeysPassword(params.getKeysPassword());
        setSniHost(params.getSniHost());

        setFixedId(params.fixedId);
        setCredentials(params.login, params.password);
        setWillTopic(params.willTopic);
        setCleanSession(params.cleanSession);
        if (params.willTopic != null && !params.willTopic.isEmpty()) {
            setWillMessageType(params.willMessageType);
            setWillMessage(params.willMessage);
            setWillQos(params.willQos);
            setWillRetained(params.willRetained);
        }
    }

    public ExpandedConnectionParams expand(PropertyExpansionContext context) {
        ExpandedConnectionParams result = new ExpandedConnectionParams();
        result.setServerUri(context.expand(getServerUri()));

        result.setCaCrtFile(context.expand(getCaCrtFile()));
        result.setCrtFile(context.expand(getCrtFile()));
        result.setKeyFile(context.expand(getKeyFile()));
        result.setKeysPassword(context.expand(getKeysPassword()));
        result.setSniHost(context.expand(getSniHost()));

        result.fixedId = context.expand(getFixedId());
        result.setCredentials(context.expand(getLogin()), context.expand(getPassword()));
        result.cleanSession = isCleanSession();
        result.willTopic = context.expand(getWillTopic());
        String willMessageStr = context.expand(getWillMessage());
        if (getWillMessageType() == null) {
            throw new IllegalArgumentException("The message type is not specified.");
        }
        result.willMessage = new MqttMessage(getWillMessageType().toPayload(willMessageStr, ModelSupport.getModelItemProject(context.getModelItem())));
        result.willQos = getWillQos();
        result.willRetained = isWillRetained();

        return result;
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        try {
            propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
        } catch (Throwable t) {
            SoapUI.logError(t);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        try {
            propertyChangeSupport.addPropertyChangeListener(listener);
        } catch (Throwable t) {
            SoapUI.logError(t);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        try {
            propertyChangeSupport.removePropertyChangeListener(listener);
        } catch (Throwable t) {
            SoapUI.logError(t);
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        try {
            propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
        } catch (Throwable t) {
            SoapUI.logError(t);
        }
    }

    public void notifyPropertyChanged(String name, Object oldValue, Object newValue) {
        try {
            if (!Objects.equals(oldValue, newValue)) {
                propertyChangeSupport.firePropertyChange(name, oldValue, newValue);
            }
        } catch (Throwable t) {
            SoapUI.logError(t);
        }
    }
}
