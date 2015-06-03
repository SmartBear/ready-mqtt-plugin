package com.smartbear.mqttsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.PropertyChangeNotifier;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import org.apache.commons.ssl.OpenSSL;
import org.apache.xmlbeans.XmlObject;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;
import static com.smartbear.mqttsupport.Utils.*;

public class Connection implements PropertyChangeNotifier {
    final static boolean ARE_NAMES_CASE_INSENSITIVE = true;

    private final static String NAME_PROP_NAME = "Name";
    private static final String SERVER_URI_SETTING_NAME = "ServerURI";
    private static final String CLIENT_ID_SETTING_NAME = "ClientID";
    private static final String LOGIN_SETTING_NAME = "Login";
    private static final String ENCR_PASSWORD_SETTING_NAME = "EncrPassword";
    private static final String WILL_TOPIC_SETTING_NAME = "WillTopic";
    private static final String WILL_MESSAGE_TYPE_SETTING_NAME = "WillMessageType";
    private static final String WILL_MESSAGE_SETTING_NAME = "WillMessage";
    private static final String WILL_QOS_SETTING_NAME = "WillQos";
    private final static String WILL_RETAINED_SETTING_NAME = "WillRetained";
    private static final String PASSWORD_FOR_ENCODING = "{CB012CCB-6D9C-4c3d-8A82-06B54D546512}";
    public static final String ENCRYPTION_METHOD = "des3";

    private String name;
    private String originalServerUri;
    private String fixedId;
    private String login;
    private String password;
    private String willTopic;
    private PublishedMessageType willMessageType = PublishTestStep.DEFAULT_MESSAGE_TYPE;
    private String willMessage;
    private int willQos = PublishTestStep.DEFAULT_QOS;
    private boolean willRetained;

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public Connection(){
    }

    public Connection(String name, ConnectionParams params){
        this();
        this.name = name;
        setParams(params);
    }


    public void load(XmlObject xml){
        XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(xml);
        name = reader.readString(NAME_PROP_NAME, null);
        originalServerUri = reader.readString(SERVER_URI_SETTING_NAME, null);
        fixedId = reader.readString(CLIENT_ID_SETTING_NAME, null);
        login = reader.readString(LOGIN_SETTING_NAME, null);
        password = null;
        String encodedPasswordString = reader.readString(ENCR_PASSWORD_SETTING_NAME, null);
        if(encodedPasswordString != null && encodedPasswordString.length() != 0) {
            byte[] encodedPassword = hexStringToBytes(encodedPasswordString);
            try {
                password = new String(OpenSSL.decrypt(ENCRYPTION_METHOD, PASSWORD_FOR_ENCODING.toCharArray(), encodedPassword));
            }
            catch (Throwable e){
                SoapUI.logError(e);
            }
        }
        willTopic = reader.readString(WILL_TOPIC_SETTING_NAME, null);
        if(willTopic != null && willTopic.length() != 0){
            willMessageType = PublishedMessageType.fromString(reader.readString(WILL_MESSAGE_TYPE_SETTING_NAME, null));
            if(willMessageType == null) willMessageType = PublishTestStep.DEFAULT_MESSAGE_TYPE;
            willMessage = reader.readString(WILL_MESSAGE_SETTING_NAME, null);
            willQos = reader.readInt(WILL_QOS_SETTING_NAME, PublishTestStep.DEFAULT_QOS);
            willRetained = reader.readBoolean(WILL_RETAINED_SETTING_NAME, false);
        }
    }
    public XmlObject save(){
        XmlObjectBuilder builder = new XmlObjectBuilder();
        builder.add(NAME_PROP_NAME, name);
        builder.add(SERVER_URI_SETTING_NAME, originalServerUri);
        builder.add(CLIENT_ID_SETTING_NAME, fixedId);
        if(login != null) {
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
        if(willTopic != null && willTopic.length() != 0){
            builder.add(WILL_TOPIC_SETTING_NAME, willTopic);
            builder.add(WILL_MESSAGE_TYPE_SETTING_NAME, willMessageType.name());
            builder.add(WILL_MESSAGE_SETTING_NAME, willMessage);
            builder.add(WILL_QOS_SETTING_NAME, willQos);
            builder.add(WILL_RETAINED_SETTING_NAME, willRetained);
        }
        return builder.finish();
    }

    public String getServerUri(){return originalServerUri;}
    public void setServerUri(String serverUri){
        String oldServerUri = getServerUri();
        if (serverUri == null) {
            serverUri = "";
        }
        this.originalServerUri = serverUri;
        if(!Utils.areStringsEqual(oldServerUri, this.originalServerUri)) notifyPropertyChanged("serverUri", oldServerUri, this.originalServerUri);
    }

    public String getName(){return name;}
    public void setName(String newName){
        String oldName = name;
        if(!Utils.areStringsEqual(oldName, newName, ARE_NAMES_CASE_INSENSITIVE, true)){
            boolean oldLegacy = isLegacy();
            name = newName;
            notifyPropertyChanged("name", oldName,  newName);
            if(isLegacy() != oldLegacy) notifyPropertyChanged("legacy", oldLegacy, isLegacy());
        }
    }

    public boolean isLegacy(){
        return StringUtils.isNullOrEmpty(name);
    }


    public final static String CLIENT_ID_BEAN_PROP = "fixedId";
    public String getFixedId(){return fixedId;}
    public void setFixedId(String value) {
        String oldId = getFixedId();
        if(!Utils.areStringsEqual(oldId, value, false, true)){
            boolean oldGeneratedId = isGeneratedId();
            fixedId = value;
            notifyPropertyChanged(CLIENT_ID_BEAN_PROP, oldId,  value);
            if(isGeneratedId() != oldGeneratedId) notifyPropertyChanged(CLIENT_ID_BEAN_PROP, oldGeneratedId, value);
        }
    }
    public boolean isGeneratedId(){return fixedId == null || fixedId.equals("");}


    public String getLogin(){return login;}
    public String getPassword(){return password;}

    public final static String LOGIN_BEAN_PROP = "login";
    public void setLogin(String newValue){
        String old = getLogin();
        if(!Utils.areStringsEqual(old, newValue, false, true)){
            login = newValue;
            notifyPropertyChanged(LOGIN_BEAN_PROP, old, newValue);
        }
    }

    public final static String PASSWORD_BEAN_PROP = "password";
    public void setPassword(String newValue){
        String old = getPassword();
        if(!Utils.areStringsEqual(old, newValue, false, true)){
            password = newValue;
            notifyPropertyChanged(PASSWORD_BEAN_PROP, old, newValue);
        }
    }

    public boolean hasCredentials(){return login != null && !"".equals(login);}

    public void setCredentials(String login, String password){
        if(login == null || login.length() == 0){
            setLogin(login);
            setPassword(null);
        }
        else{
            setLogin(login);
            setPassword(password);
        }
    }

    public final static String WILL_TOPIC_BEAN_PROP = "willTopic";
    public String getWillTopic(){return willTopic;}
    public void setWillTopic(String newValue){
        String old = getWillTopic();
        if(!Utils.areStringsEqual(old, newValue, false, true)){
            willTopic = newValue;
            notifyPropertyChanged(WILL_TOPIC_BEAN_PROP, old, newValue);
        }
    }

    public final static String WILL_MESSAGE_TYPE_BEAN_PROP = "willMessageType";
    public PublishedMessageType getWillMessageType(){return willMessageType;}
    public void setWillMessageType(PublishedMessageType newValue){
        PublishedMessageType old = getWillMessageType();
        if(old != newValue){
            willMessageType = newValue;
            notifyPropertyChanged(WILL_MESSAGE_TYPE_BEAN_PROP, old, newValue);
        }
    }

    public final static String WILL_MESSAGE_BEAN_PROP = "willMessage";
    public String getWillMessage(){return willMessage;}
    public void setWillMessage(String newValue){
        String old = getWillMessage();
        if(!Utils.areStringsEqual(old, newValue, false, true)){
            willMessage = newValue;
            notifyPropertyChanged(WILL_MESSAGE_BEAN_PROP, old, newValue);
        }
    }

    public final static String WILL_QOS_BEAN_PROP = "willQos";
    public int getWillQos(){return willQos;}
    public void setWillQos(int newValue){
        int old = getWillQos();
        if(old != newValue){
            willQos = newValue;
            notifyPropertyChanged(WILL_QOS_BEAN_PROP, old, newValue);
        }
    }

    public final static String WILL_RETAINED_BEAN_PROP = "willRetained";
    public boolean isWillRetained(){return willRetained;}
    public void setWillRetained(boolean newValue){
        boolean old = isWillRetained();
        if(old != newValue){
            willRetained = newValue;
            notifyPropertyChanged(WILL_RETAINED_BEAN_PROP, old, newValue);
        }

    }


    public ConnectionParams getParams(){
        return new ConnectionParams(getServerUri(), getFixedId(), getLogin(), getPassword(), getWillTopic(), getWillMessageType(), getWillMessage(), getWillQos(), isWillRetained());
    }

    public void setParams(ConnectionParams params){
        setServerUri(params.getServerUri());
        setFixedId(params.fixedId);
        setCredentials(params.login, params.password);
        setWillTopic(params.willTopic);
        if(params.willTopic != null && params.willTopic.length() != 0){
            setWillMessageType(params.willMessageType);
            setWillMessage(params.willMessage);
            setWillQos(params.willQos);
            setWillRetained(params.willRetained);
        }
    }

    public ExpandedConnectionParams expand(PropertyExpansionContext context){
        ExpandedConnectionParams result = new ExpandedConnectionParams();
        result.setServerUri(context.expand(getServerUri()));
        result.fixedId = context.expand(getFixedId());
        result.setCredentials(context.expand(getLogin()), context.expand(getPassword()));
        result.willTopic = context.expand(getWillTopic());
        String willMessageStr = context.expand(getWillMessage());
        if(getWillMessageType() == null) throw new IllegalArgumentException("The message type is not specified.");
        result.willMessage = getWillMessageType().toPayload(willMessageStr, ModelSupport.getModelItemProject(context.getModelItem()));
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
