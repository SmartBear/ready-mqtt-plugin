package com.smartbear.mqttsupport;

import com.eviware.soapui.impl.wsdl.actions.project.SimpleDialog;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JUndoableTextField;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
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

    private JCheckBox hidePasswordCheckBox;

    public class Result{
        public String connectionName;
        public ConnectionParams connectionParams;

        public Result(){
            connectionParams = new ConnectionParams();
        }
    }

    private JTextField nameEdit;
    private final static Insets defaultInsets = new Insets(4, 4, 4, 4);
    private JUndoableTextField serverUriEdit;
    private JUndoableTextField clientIDEdit;
    private JCheckBox authRequiredCheckBox;
    private JUndoableTextField loginEdit;
    private JPasswordField passwordEdit;

    private char passwordChar;

    private ModelItem modelItemOfConnection;
    private String initialName;
    private ConnectionParams initialParams;
    private Result result = null;


    protected EditConnectionDialog(String title, ModelItem modelItemOfConnection){
        super(title, "Please specify the parameters for the connection", null, true);
        this.modelItemOfConnection = modelItemOfConnection;
    }

    public static Result showDialog(String initialConnectionName, ConnectionParams initialConnectionParams, ModelItem modelItemOfConnection){
        EditConnectionDialog dialog = new EditConnectionDialog("Configure MQTT Server Connection", modelItemOfConnection);
        try {
            dialog.setModal(true);
            UISupport.centerDialog(dialog);
            dialog.initialName = initialConnectionName;
            dialog.initialParams = initialConnectionParams;
            dialog.setVisible(true);
        }
        finally {
            dialog.dispose();
        }
        return dialog.result;
    }

    @Override
    protected void beforeShow() {
        super.beforeShow();
        nameEdit.setText(initialName);
        if(initialParams == null) {
            loginEdit.setEnabled(false);
            passwordEdit.setEnabled(false);
            hidePasswordCheckBox.setEnabled(false);
            authRequiredCheckBox.setSelected(false);
        }
        else{
            serverUriEdit.setText(initialParams.getServerUri());
            clientIDEdit.setText(initialParams.fixedId);
            if(StringUtils.isNullOrEmpty(initialParams.login)) {
                loginEdit.setEnabled(false);
                passwordEdit.setEnabled(false);
                hidePasswordCheckBox.setEnabled(false);
                authRequiredCheckBox.setSelected(false);
            }
            else{
                authRequiredCheckBox.setSelected(true);
                loginEdit.setText(initialParams.login);
                passwordEdit.setText(initialParams.password);
            }
        }
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

    private JLabel createLabel(String text, JComponent targetComponent, int hitCharNo){
        JLabel label = new JLabel(text);
        label.setLabelFor(targetComponent);
        if(hitCharNo >= 0) {
            label.setDisplayedMnemonic(text.charAt(hitCharNo));
            label.setDisplayedMnemonicIndex(hitCharNo);
        }
        return label;
    }

    @Override
    protected Component buildContent() {
        JLabel label;
        final int defEditCharCount = 15;

        int row = 0;

        JPanel mainPanel = new JPanel(new GridBagLayout());

        nameEdit = new JUndoableTextField(defEditCharCount);
        nameEdit.setToolTipText("The unique connection name to identify it.");
        mainPanel.add(nameEdit, componentPlace(row));
        mainPanel.add(createLabel("Name:", nameEdit, 0), labelPlace(row));
        ++row;

        serverUriEdit = new JUndoableTextField(defEditCharCount);
        serverUriEdit.setToolTipText("The MQTT server URI");
        mainPanel.add(serverUriEdit, componentPlace(row));
        mainPanel.add(createLabel("Server URI:", serverUriEdit, 0), labelPlace(row));
        ++row;

        clientIDEdit = new JUndoableTextField(defEditCharCount);
        clientIDEdit.setToolTipText("Fill this field if you want to connect with fixed Client ID or leave it empty so a unique ID will be generated");
        mainPanel.add(clientIDEdit, componentPlace(row));
        mainPanel.add(createLabel("Client ID (optional):", clientIDEdit, 0), labelPlace(row));
        ++row;

//        label = new JLabel("Authentication:");
//        mainPanel.add(label, labelPlace(row));
        authRequiredCheckBox = new JCheckBox("The server requires authentication");
        authRequiredCheckBox.setToolTipText("Check if the MQTT server requires authentication to connect");
        authRequiredCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginEdit.setEnabled(authRequiredCheckBox.isSelected());
                passwordEdit.setEnabled(authRequiredCheckBox.isSelected());
                hidePasswordCheckBox.setEnabled(authRequiredCheckBox.isSelected());
                if(authRequiredCheckBox.isSelected()) loginEdit.grabFocus();
            }
        });
        mainPanel.add(authRequiredCheckBox, componentPlace(row));
        mainPanel.add(createLabel("Authentication:", authRequiredCheckBox, 0), labelPlace(row));
        ++row;

        loginEdit = new JUndoableTextField(defEditCharCount);
        loginEdit.setToolTipText("Login for MQTT server");
        mainPanel.add(loginEdit, componentPlace(row));
        mainPanel.add(createLabel("Login:", loginEdit, 0), labelPlace(row));
        ++row;

        passwordEdit = new JPasswordField(defEditCharCount);
        passwordEdit.setToolTipText("Password for MQTT server");
        mainPanel.add(passwordEdit, componentPlace(row));
        mainPanel.add(createLabel("Password:", passwordEdit, 0), labelPlace(row));
        hidePasswordCheckBox = new JCheckBox("Hide", true);
        hidePasswordCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hidePasswordCheckBox.isSelected()) {
                    passwordEdit.setEchoChar(passwordChar);
                } else {
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
        result = new Result();
        result.connectionName = nameEdit.getText();
        result.connectionParams.setServerUri(serverUriEdit.getText());
        result.connectionParams.fixedId = clientIDEdit.getText();
        if(authRequiredCheckBox.isSelected()) {
            result.connectionParams.setCredentials(loginEdit.getText(), new String(passwordEdit.getPassword()));
        }
        return true;
    }
}
