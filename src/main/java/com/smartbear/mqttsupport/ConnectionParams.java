package com.smartbear.mqttsupport;

import com.eviware.soapui.support.StringUtils;

public class ConnectionParams {
    private String fixedId;
    private String login;
    private String password;

    public ConnectionParams(){
        fixedId = "";
        login = "";
        password = "";
    }
    public boolean hasGeneratedId(){return fixedId.equals("");}
    public String getFixedId(){return fixedId;}
    public void setId(String value) {
        fixedId = value == null ? "" : value;
    }

    public String getLogin(){return login;}
    public String getPassword(){return password;}
    public boolean hasCredentials(){return "".equals(login);}
    public void setCredentials(String login, String password){
        if(StringUtils.isNullOrEmpty(login)){
            login = "";
            password = "";
        }
        else{
            login = login;
            password = password == null ? "" : password;
        }
    }

    public String getName(){
        if(hasGeneratedId()) {
            if (!hasCredentials()) return "Default (without authentication)"; else return String.format("Login as %s", login);
        }
        else {
            if (!hasCredentials()) return String.format("Client ID is %s", fixedId); else return String.format("Client ID: %s; Login: %s");
        }
    }

    public String getKey(){
        return String.format("%s\n%s\n%s", fixedId, login);
    }

    @Override
    public boolean equals(Object arg){
        if(arg == null || !(arg instanceof ConnectionParams))return false;
        ConnectionParams params2 = (ConnectionParams)arg;
        return fixedId.equals(params2.fixedId) && login.equals(params2.login) && password.equals(params2.password);
    }

    @Override
    public int hashCode(){
        return String.format("%s\n%s\n%s", fixedId, login, password).hashCode();
    }
}
