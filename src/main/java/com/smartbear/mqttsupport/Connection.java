package com.smartbear.mqttsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.support.PropertyChangeNotifier;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import org.apache.commons.ssl.OpenSSL;
import org.apache.xmlbeans.XmlObject;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.Objects;
import static com.smartbear.mqttsupport.Utils.*;

public class Connection implements PropertyChangeNotifier {
    final static boolean ARE_NAMES_CASE_INSENSITIVE = true;

    private final static String NAME_PROP_NAME = "Name";
    private static final String SERVER_URI_PROP_NAME = "ServerURI";
    private static final String CLIENT_ID_PROP_NAME = "ClientID";
    private static final String LOGIN_PROP_NAME = "Login";
    private static final String ENCR_PASSWORD_PROP_NAME = "EncrPassword";
    private static final String PASSWORD_FOR_ENCODING = "{CB012CCB-6D9C-4c3d-8A82-06B54D546512}";
    public static final String ENCRYPTION_METHOD = "des3";

    private String name;
    private String originalServerUri;
    private String fixedId;
    private String login;
    private String password;
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
        originalServerUri = reader.readString(SERVER_URI_PROP_NAME, null);
        fixedId = reader.readString(CLIENT_ID_PROP_NAME, null);
        login = reader.readString(LOGIN_PROP_NAME, null);
        password = null;
        String encodedPasswordString = reader.readString(ENCR_PASSWORD_PROP_NAME, null);
        if(encodedPasswordString != null && encodedPasswordString.length() != 0) {
            byte[] encodedPassword = hexStringToBytes(encodedPasswordString);
            try {
                password = new String(OpenSSL.decrypt(ENCRYPTION_METHOD, PASSWORD_FOR_ENCODING.toCharArray(), encodedPassword));
            }
            catch (Throwable e){
                SoapUI.logError(e);
            }
        }

    }
    public XmlObject save(){
        XmlObjectBuilder builder = new XmlObjectBuilder();
        builder.add(NAME_PROP_NAME, name);
        builder.add(SERVER_URI_PROP_NAME, originalServerUri);
        builder.add(CLIENT_ID_PROP_NAME, fixedId);
        if(login != null) {
            builder.add(LOGIN_PROP_NAME, login);
            if (password != null && password.length() != 0) {
                byte[] encodedPassword = null;
                try {
                    encodedPassword = OpenSSL.encrypt(ENCRYPTION_METHOD, PASSWORD_FOR_ENCODING.toCharArray(), password.getBytes());
                } catch (IOException | GeneralSecurityException e) {
                    SoapUI.logError(e);
                }
                builder.add(ENCR_PASSWORD_PROP_NAME, bytesToHexString(encodedPassword));
            }
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


    public String getFixedId(){return fixedId;}
    public void setFixedId(String value) {
        String oldId = getFixedId();
        if(!Utils.areStringsEqual(oldId, value, false, true)){
            boolean oldGeneratedId = isGeneratedId();
            fixedId = value;
            notifyPropertyChanged("fixedId", oldId,  value);
            if(isGeneratedId() != oldGeneratedId) notifyPropertyChanged("fixedId", oldGeneratedId, value);
        }
    }
    public boolean isGeneratedId(){return fixedId == null || fixedId.equals("");}


    public String getLogin(){return login;}
    public String getPassword(){return password;}
    public void setLogin(String newValue){
        String old = getLogin();
        if(!Utils.areStringsEqual(old, newValue, false, true)){
            login = newValue;
            notifyPropertyChanged("login", old, newValue);
        }
    }
    public void setPassword(String newValue){
        String old = getPassword();
        if(!Utils.areStringsEqual(old, newValue, false, true)){
            password = newValue;
            notifyPropertyChanged("password", old, newValue);
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

    public ConnectionParams getParams(){
        return new ConnectionParams(getServerUri(), getFixedId(), getLogin(), getPassword());
    }

    public void setParams(ConnectionParams params){
        setServerUri(params.getServerUri());
        setFixedId(params.fixedId);
        setCredentials(params.login, params.password);
    }

    public ConnectionParams expand(PropertyExpansionContext context){
        ConnectionParams result = new ConnectionParams();
        result.setServerUri(context.expand(getServerUri()));
        result.fixedId = context.expand(getFixedId());
        result.setCredentials(context.expand(getLogin()), context.expand(getPassword()));
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
