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

public class ConnectionParams implements PropertyChangeNotifier {
    private final static boolean ARE_NAMES_CASE_INSENSITIVE = true;

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

    public ConnectionParams(){
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

    public String getActualServerUri(){
        URI uri = null;
        try {
            uri = new URI(originalServerUri);
            if (uri.getAuthority() == null) {
                uri = new URI("tcp://" + originalServerUri);
            }
            if (uri.getPort() == -1) {
                if ("tcp".equals(uri.getScheme().toLowerCase(Locale.ENGLISH))) {
                    uri = new URI("tcp", uri.getUserInfo(), uri.getHost(), PluginConfig.DEFAULT_TCP_PORT, uri.getPath(), uri.getQuery(), uri.getFragment());
                } else if ("ssl".equals(uri.getScheme().toLowerCase(Locale.ENGLISH))) {
                    uri = new URI("ssl", uri.getUserInfo(), uri.getHost(), PluginConfig.DEFAULT_SSL_PORT, uri.getPath(), uri.getQuery(), uri.getFragment());
                }
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            return this.originalServerUri;
        }
    }

    public String getNormalizedServerUri(){
        return getActualServerUri().toLowerCase(Locale.ENGLISH);
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

//    public String getName(){
//        if(hasGeneratedId()) {
//            if (!hasCredentials()) return "Default (without authentication)"; else return String.format("Login as %s", login);
//        }
//        else {
//            if (!hasCredentials()) return String.format("Client ID is %s", fixedId); else return String.format("Client ID: %s; Login: %s");
//        }
//    }

    public ConnectionParams expand(PropertyExpansionContext context){
        ConnectionParams result = new ConnectionParams();
        result.setName(getName());
        result.setServerUri(context.expand(getServerUri()));
        result.setFixedId(context.expand(getFixedId()));
        result.setCredentials(context.expand(getLogin()), context.expand(getPassword()));
        return result;
    }


    @Override
    public boolean equals(Object arg){
        if(arg == null || !(arg instanceof ConnectionParams))return false;
        ConnectionParams params2 = (ConnectionParams)arg;
        //if(Utils.areStringsEqual(name, params2.name, ARE_NAMES_CASE_INSENSITIVE, true) && name != null && name.length() != 0) return true;
        return Utils.areStringsEqual(getNormalizedServerUri(),  params2.getNormalizedServerUri())
            && Utils.areStringsEqual(fixedId, params2.fixedId, false, true)
            && Utils.areStringsEqual(login, params2.login, false, true)
            && (login == null || login.length() == 0 || Utils.areStringsEqual(password, params2.password, false, true));
    }

    @Override
    public int hashCode(){
        if(name == null || name.length() == 0) {
            return String.format("%s\n%s\n%s\n%s", getNormalizedServerUri(), fixedId, login, password).hashCode();
        }
        else {
            return name.toLowerCase().hashCode();
        }
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
