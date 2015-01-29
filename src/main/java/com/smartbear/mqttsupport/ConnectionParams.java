package com.smartbear.mqttsupport;

import com.eviware.soapui.support.StringUtils;

public class ConnectionParams {
    private String fixedId;
    private String login;
    private String password;

    public ConnectionParams(){
    }

    public boolean hasGeneratedId(){return fixedId == null || fixedId.equals("");}
    public String getFixedId(){return fixedId;}
    public void setId(String value) {
        fixedId = value;
    }

    public String getLogin(){return login;}
    public String getPassword(){return password;}
    public boolean hasCredentials(){return login != null && !"".equals(login);}
    public void setCredentials(String login, String password){
        if(login == null || login.length() == 0){
            this.login = login;
            this.password = null;
        }
        else{
            this.login = login;
            this.password = password;
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

    public String getKey(){
        return String.format("%s\n%s", fixedId, login);
    }

    @Override
    public boolean equals(Object arg){
        if(arg == null || !(arg instanceof ConnectionParams))return false;
        ConnectionParams params2 = (ConnectionParams)arg;
        return Utils.areStringsEqual(fixedId, params2.fixedId, false, true) && Utils.areStringsEqual(login, params2.login, false, true) && (login == null || login.length() == 0 || Utils.areStringsEqual(password, params2.password, false, true));
    }

    @Override
    public int hashCode(){
        return String.format("%s\n%s\n%s", fixedId, login, password).hashCode();
    }
}
