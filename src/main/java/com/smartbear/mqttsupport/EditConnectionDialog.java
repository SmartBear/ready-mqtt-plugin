package com.smartbear.mqttsupport;

import com.eviware.soapui.impl.wsdl.actions.project.SimpleDialog;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class EditConnectionDialog extends SimpleDialog {

    private JTextField nameEdit;
    private final static Insets defaultInsets = new Insets(4, 4, 4, 4);
    private JTextField serverUriEdit;
    private JTextField clientIDEdit;
    private JCheckBox authRequiredCheckBox;
    private JTextField loginEdit;
    private JPasswordField passwordEdit;

    private char passwordChar;

    private ModelItem modelItemOfConnection;
    private Params result = null;

    public static class Params{
        public String name, serverUri, clientId, login, password;
        public Params(){};
        public Params(String name, String serverUri, String clientId, String login, String password){
            this.name = name;
            this.serverUri = serverUri;
            this.clientId = clientId;
            this.login = login;
            this.password = password;
        }
    }

    protected EditConnectionDialog(Frame frame, String title, ModelItem modelItemOfConnection){
        super(frame, title, "Please specify the parameters for the connection", null, true);
        this.modelItemOfConnection = modelItemOfConnection;
    }

    public static Params showDialog(Frame frame, String title, Params initialConnectionParams, ModelItem modelItemOfConnection){
        EditConnectionDialog dialog = new EditConnectionDialog(frame, title, modelItemOfConnection);
        try {
            dialog.setModal(true);
            dialog.nameEdit.setText(initialConnectionParams.name);
            dialog.serverUriEdit.setText(initialConnectionParams.serverUri);
            dialog.clientIDEdit.setText(initialConnectionParams.clientId);
            dialog.loginEdit.setText(initialConnectionParams.login);
            dialog.passwordEdit.setText(initialConnectionParams.password);
            dialog.setVisible(true);
        }
        finally {
            dialog.dispose();
        }
        return dialog.result;
    }


    private GridBagConstraints labelPlace(int row){
        return new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, defaultInsets, 0, 0);
    }

    private GridBagConstraints componentPlace(int row, int col){
        return new GridBagConstraints(col, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, defaultInsets, 0, 0);
    }

    private GridBagConstraints componentPlace(int row){
        return componentPlace(row, 1);
    }

    @Override
    protected Component buildContent() {
        final int defEditCharCount = 15;

        int row = 0;

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.add(new JLabel("Name:"), labelPlace(row));
        nameEdit = new JTextField(defEditCharCount);
        nameEdit.setToolTipText("The unique connection name to identify it.");
        mainPanel.add(nameEdit, componentPlace(row));
        ++row;

        mainPanel.add(new JLabel("Server URI:"), labelPlace(row));
        serverUriEdit = new JTextField(defEditCharCount);
        serverUriEdit.setToolTipText("The MQTT server URI");
        mainPanel.add(serverUriEdit, componentPlace(row));
        ++row;

        mainPanel.add(new JLabel("Client ID (optional):"), labelPlace(row));
        clientIDEdit = new JTextField(defEditCharCount);
        clientIDEdit.setToolTipText("Fill this field if you want to connect with fixed Client ID or leave it empty so a unique ID will be generated");
        mainPanel.add(clientIDEdit, componentPlace(row));
        ++row;

        mainPanel.add(new JLabel("Authentication:"), labelPlace(row));
        authRequiredCheckBox = new JCheckBox("The server requires authentication");
        authRequiredCheckBox.setToolTipText("Check if the MQTT server requires authentication to connect");
        authRequiredCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginEdit.setEnabled(authRequiredCheckBox.isSelected());
                passwordEdit.setEnabled(authRequiredCheckBox.isSelected());
            }
        });
        mainPanel.add(authRequiredCheckBox, componentPlace(row));
        ++row;

        mainPanel.add(new JLabel("Login:"), labelPlace(row));
        loginEdit = new JTextField(defEditCharCount);
        loginEdit.setToolTipText("Login for MQTT server");
        mainPanel.add(loginEdit, componentPlace(row));
        ++row;

        mainPanel.add(new JLabel("Password:"), labelPlace(row));
        passwordEdit = new JPasswordField(defEditCharCount);
        passwordEdit.setToolTipText("Password for MQTT server");
        mainPanel.add(passwordEdit, componentPlace(row));
        final JCheckBox hidePasswordCheckBox = new JCheckBox("Hide", true);
        hidePasswordCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(hidePasswordCheckBox.isSelected()){
                    passwordEdit.setEchoChar(passwordChar);
                }
                else{
                    passwordChar = passwordEdit.getEchoChar();
                    passwordEdit.setEchoChar('\0');
                }
            }
        });
        mainPanel.add(hidePasswordCheckBox, componentPlace(row, 2));
        ++row;

        PropertyExpansionPopupListener.enable(serverUriEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(loginEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(passwordEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(clientIDEdit, modelItemOfConnection);

        return mainPanel;
    }

    @Override
    protected boolean handleOk() {
        if(StringUtils.isNullOrEmpty(nameEdit.getText())){
            nameEdit.grabFocus();
            UISupport.showErrorMessage("Please specify a name for the connection.");
            return false;
        }
        if(StringUtils.isNullOrEmpty(serverUriEdit.getText())){
            serverUriEdit.grabFocus();
            UISupport.showErrorMessage("Please specify URI of MQTT server.");
            return false;
        }
        if(authRequiredCheckBox.isSelected() && StringUtils.isNullOrEmpty(loginEdit.getText())){
            loginEdit.grabFocus();
            UISupport.showErrorMessage("Please specify a login or uncheck \"Authentication required\" check-box if the authentication is not required for this MQTT server.");
            return false;
        }
        result = new Params();
        result.name = nameEdit.getText();
        result.serverUri = serverUriEdit.getText();
        result.clientId = clientIDEdit.getText();
        if(authRequiredCheckBox.isSelected()) {
            result.login = loginEdit.getText();
            result.password = new String(passwordEdit.getPassword());
        }
        return true;
    }
}
