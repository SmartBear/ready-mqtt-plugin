package com.smartbear.mqttsupport;

import org.apache.commons.lang.ArrayUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class ExpandedConnectionParams {
    private String originalServerUri;
    public String fixedId;
    public String login;
    public String password;
    public String willTopic;
    public byte[] willMessage;
    public int willQos;
    public boolean willRetained;

    public ExpandedConnectionParams(){}

    public ExpandedConnectionParams(String serverUri, String clientId, String login, String password){
        this.originalServerUri = serverUri;
        this.fixedId = clientId;
        this.login = login;
        this.password = password;
    }

    public ExpandedConnectionParams(String serverUri, String clientId, String login, String password, String willTopic, byte[] willMessage, int willQos, boolean willRetained){
        this.originalServerUri = serverUri;
        this.fixedId = clientId;
        this.login = login;
        this.password = password;
        this.willTopic = willTopic;
        this.willMessage = willMessage;
        this.willQos = willQos;
        this.willRetained = willRetained;
    }

    public String getServerUri(){
        return this.originalServerUri;
    }

    public void setServerUri(String value){
        this.originalServerUri = value;
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

    public boolean isGeneratedId(){return fixedId == null || fixedId.equals("");}
//    public String getFixedId(){return fixedId;}

    public boolean hasCredentials(){return login != null && !"".equals(login);}
    //    public String getLogin(){return login;}
//    public String getPassword(){return password;}
    public void setCredentials(String login, String password){
        if(login == null || login.length() == 0){
            this.login = login;
            password = null;
        }
        else{
            this.login = login;
            this.password = password;
        }
    }


    @Override
    public boolean equals(Object arg){
        if(arg == null || !(arg instanceof ExpandedConnectionParams))return false;
        ExpandedConnectionParams params2 = (ExpandedConnectionParams)arg;
        return Utils.areStringsEqual(getNormalizedServerUri(), params2.getNormalizedServerUri())
                && Utils.areStringsEqual(fixedId, params2.fixedId, false, true)
                && Utils.areStringsEqual(login, params2.login, false, true)
                && (login == null || login.length() == 0 || Utils.areStringsEqual(password, params2.password, false, true))
                && Utils.areStringsEqual(willTopic, params2.willTopic, false, true)
                && (willTopic == null || willTopic.length() == 0 ||
                (
                        willRetained == params2.willRetained
                        && willQos == params2.willQos
                        && ArrayUtils.isEquals(willMessage, params2.willMessage)
                )
        );

    }

    @Override
    public int hashCode(){
        return String.format("%s\n%s\n%s\n%s", getNormalizedServerUri(), fixedId, login, password).hashCode();
    }
}
