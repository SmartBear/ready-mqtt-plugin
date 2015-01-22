package com.smartbear.mqttsupport;

class ConnectionComboBoxItem {
    public ConnectionParams connectionParams;
    private boolean isNewConnectionItem = false;

    private ConnectionComboBoxItem(){isNewConnectionItem = true;}
    public ConnectionComboBoxItem(ConnectionParams params){connectionParams = params;}

    public static final ConnectionComboBoxItem newConnectionItem = new ConnectionComboBoxItem();

    @Override
    public String toString(){
        if(isNewConnectionItem){
            return "Add New Connection...";
        }
        else if(connectionParams == null){
            return "Default (no authentication)";
        }
        else{
            return connectionParams.getName();
        }
    }

}
