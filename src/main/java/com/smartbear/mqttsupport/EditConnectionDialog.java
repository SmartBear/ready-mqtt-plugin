package com.smartbear.mqttsupport;

import com.eviware.soapui.impl.wsdl.actions.project.SimpleDialog;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansion;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JUndoableTextField;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
import com.eviware.soapui.support.xml.SyntaxEditorUtil;
import com.jgoodies.binding.adapter.Bindings;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class EditConnectionDialog extends SimpleDialog {

    public class Result{
        public String connectionName;
        public ConnectionParams connectionParams;

        public Result(){
            connectionParams = new ConnectionParams();
        }
    }

    private boolean legacy;
    private JTextField nameEdit;
    private final static Insets defaultInsets = new Insets(4, 4, 4, 4);
    private final static Insets defaultInsetsWithIndent = new Insets(defaultInsets.top, defaultInsets.left + 12, defaultInsets.bottom, defaultInsets.right);
    private JUndoableTextField serverUriEdit;
    private JUndoableTextField clientIDEdit;
    private JCheckBox authRequiredCheckBox;
    private JUndoableTextField loginEdit;
    private JPasswordField passwordEdit;
    private JCheckBox hidePasswordCheckBox;
    private HashMap<JComponent, JLabel> componentLabelsMap = new HashMap<>();

    private char passwordChar;

    private ModelItem modelItemOfConnection;
    private String initialName;
    private ConnectionParams initialParams;
    private List<String> presentNames;
    private Result result = null;


    protected EditConnectionDialog(String title, ModelItem modelItemOfConnection, String initialConnectionName, ConnectionParams initialConnectionParams, boolean legacy, List<String> alreadyPresentNames){
        super(title, "Please specify the parameters for the connection", null, true);
        this.modelItemOfConnection = modelItemOfConnection;
        this.legacy = legacy;
        this.presentNames = alreadyPresentNames;
        this.initialName = initialConnectionName;
        this.initialParams = initialConnectionParams;
    }

    public static Result showDialog(String title, ModelItem modelItemOfConnection, String initialConnectionName, ConnectionParams initialConnectionParams, boolean legacy, List<String> alreadyPresentNames){
        EditConnectionDialog dialog = new EditConnectionDialog(title,  modelItemOfConnection, initialConnectionName, initialConnectionParams, legacy, alreadyPresentNames);
        try {
            dialog.setModal(true);
            UISupport.centerDialog(dialog);
            dialog.setVisible(true);
        }
        finally {
            dialog.dispose();
        }
        return dialog.result;

    }


    public static Result editConnection(Connection connection, ModelItem modelItemOfConnection){
        if(connection.isLegacy()){
            return showDialog("Configure Connection", modelItemOfConnection, null, connection.getParams(), true, null);
        }
        else{
            Project project = ModelSupport.getModelItemProject(modelItemOfConnection);
            ArrayList<String> existingNames = new ArrayList<>();
            List<Connection> connections = ConnectionsManager.getAvailableConnections(project);
            if(connections != null) {
                for (Connection curConnection : connections) {
                    if (curConnection != connection) existingNames.add(curConnection.getName());
                }
            }
            String title = String.format("Configure %s Connection", connection.getName());
            return showDialog(title, project, connection.getName(), connection.getParams(), false, existingNames);
        }

    }

    public static Result createConnection(ModelItem modelItemOfConnection){
        Project project = ModelSupport.getModelItemProject(modelItemOfConnection);
        ArrayList<String> existingNames = new ArrayList<>();
        List<Connection> connections = ConnectionsManager.getAvailableConnections(project);
        if(connections != null) {
            for (Connection curConnection : connections) {
                existingNames.add(curConnection.getName());
            }
        }
        return showDialog("Create New Connection", project, null, null, false, existingNames);

    }

    public static Result convertLegacyConnection(ConnectionParams srcParams, ModelItem modelItemOfConnection){
        Project project = ModelSupport.getModelItemProject(modelItemOfConnection);
        ArrayList<String> existingNames = new ArrayList<>();
        List<Connection> connections = ConnectionsManager.getAvailableConnections(project);
        if(connections != null) {
            for (Connection curConnection : connections) {
                existingNames.add(curConnection.getName());
            }
        }
        return showDialog("Convert Legacy Connection", project, null, srcParams, false, existingNames);

    }

    @Override
    protected void beforeShow() {
        super.beforeShow();
        if(nameEdit != null) nameEdit.setText(initialName);
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

    private GridBagConstraints labelPlaceWithIndent(int row){
        return new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, defaultInsetsWithIndent, 0, 0);
    }

    private GridBagConstraints extraComponentPlace(int row, boolean fill){
        return new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, fill ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE, defaultInsets, 0, 0);
    }

    private GridBagConstraints componentPlace(int row){
        return new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, defaultInsets, 0, 0);
    }

    private JLabel createLabel(String text, JComponent targetComponent, int hitCharNo){
        JLabel label = new JLabel(text);
        label.setLabelFor(targetComponent);
        if(targetComponent != null) componentLabelsMap.put(targetComponent, label);
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
        final int defMemoRows = 5;

        int row = 0;

        JPanel mainPanel = new JPanel(new GridBagLayout());

        if(!legacy) {
            nameEdit = new JUndoableTextField(defEditCharCount);
            nameEdit.setToolTipText("The unique connection name to identify it.");
            mainPanel.add(nameEdit, componentPlace(row));
            mainPanel.add(createLabel("Name:", nameEdit, 0), labelPlace(row));
            ++row;
        }

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
        mainPanel.add(createLabel("Login:", loginEdit, 0), labelPlaceWithIndent(row));
        ++row;

        passwordEdit = new JPasswordField(defEditCharCount);
        passwordEdit.setToolTipText("Password for MQTT server");
        mainPanel.add(passwordEdit, componentPlace(row));
        mainPanel.add(createLabel("Password:", passwordEdit, 0), labelPlaceWithIndent(row));
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
        mainPanel.add(hidePasswordCheckBox, extraComponentPlace(row, false));
        ++row;

        JCheckBox willCheckBox = new JCheckBox("Store Will Message on the server");
        willCheckBox.setToolTipText("Set up the message which is published by the server if the connection to the client is terminated unexpectedly.");
        mainPanel.add(willCheckBox, componentPlace(row));
        mainPanel.add(createLabel("Will Message:", willCheckBox, 0), labelPlace(row));
        ++row;

        JTextField willTopicEdit = new JTextField(defEditCharCount);
        mainPanel.add(willTopicEdit, componentPlace(row));
        mainPanel.add(createLabel("Topic:", willTopicEdit, 0), labelPlace(row));
        ++row;

        final JComboBox<PublishedMessageType> willMessageTypeCombo = new JComboBox<PublishedMessageType>(PublishedMessageType.values());
        mainPanel.add(willMessageTypeCombo, componentPlace(row));
        mainPanel.add(createLabel("Message type:", willMessageTypeCombo, 9), labelPlaceWithIndent(row));
        ++row;

        final JTextField willNumberEdit = new JTextField(defEditCharCount);
        mainPanel.add(willNumberEdit, componentPlace(row));
        PropertyExpansionPopupListener.enable(willNumberEdit, modelItemOfConnection);
        mainPanel.add(createLabel("Message:", willNumberEdit, 0), labelPlace(row));
        ++row;

        final JTextArea willTextMemo = new JTextArea(defEditCharCount, defMemoRows);
        PropertyExpansionPopupListener.enable(willTextMemo, modelItemOfConnection);
        mainPanel.add(new JScrollPane(willTextMemo), componentPlace(row));
        mainPanel.add(createLabel("Message:", willTextMemo, 0), labelPlace(row));
        ++row;

        final JTextField willFileNameEdit = new JTextField(defEditCharCount);
        willFileNameEdit.setToolTipText("The file which content will be used as a payload of Will Message");
        PropertyExpansionPopupListener.enable(willFileNameEdit, modelItemOfConnection);
        final JButton chooseFileButton = new JButton(new SelectFileAction(willFileNameEdit));
        mainPanel.add(willFileNameEdit, componentPlace(row));
        mainPanel.add(chooseFileButton, extraComponentPlace(row, false));
        mainPanel.add(createLabel("File with message:", willFileNameEdit, 0), labelPlace(row));
        ++row;

        final JTabbedPane willJson = new JTabbedPane();
        RSyntaxTextArea syntaxTextArea = SyntaxEditorUtil.createDefaultJavaScriptSyntaxTextArea();
        syntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        PropertyExpansionPopupListener.enable(syntaxTextArea, modelItemOfConnection);
        willJson.addTab("Text", new RTextScrollPane(syntaxTextArea));

        Utils.JsonTreeEditor jsonTreeEditor = new Utils.JsonTreeEditor(true, modelItemOfConnection);
        JScrollPane scrollPane = new JScrollPane(jsonTreeEditor);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        willJson.addTab("Tree View", scrollPane);

        willJson.setPreferredSize(new Dimension(450, 350));
        mainPanel.add(willJson, new GridBagConstraints(1, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, defaultInsets, 0, 0));
        mainPanel.add(createLabel("Message:", willJson, 0), labelPlace(row));
        ++row;

        final JTabbedPane willXml = new JTabbedPane();

        syntaxTextArea = SyntaxEditorUtil.createDefaultXmlSyntaxTextArea();
        PropertyExpansionPopupListener.enable(syntaxTextArea, modelItemOfConnection);
        willXml.addTab("Text", new RTextScrollPane(syntaxTextArea));

        Utils.XmlTreeEditor xmlTreeEditor = new Utils.XmlTreeEditor(true, modelItemOfConnection);
        scrollPane = new JScrollPane(xmlTreeEditor);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        willXml.addTab("Tree View", scrollPane);

        willXml.setPreferredSize(new Dimension(450, 350));
        mainPanel.add(willXml, new GridBagConstraints(1, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, defaultInsets, 0, 0));
        mainPanel.add(createLabel("Message:", willXml, 0), labelPlace(row));
        ++row;

        willMessageTypeCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                PublishedMessageType messageType = (PublishedMessageType)willMessageTypeCombo.getSelectedItem();
                boolean isNumber = messageType == PublishedMessageType.DoubleValue || messageType == PublishedMessageType.FloatValue || messageType == PublishedMessageType.IntegerValue || messageType == PublishedMessageType.LongValue;
                boolean isFile = messageType == PublishedMessageType.BinaryFile;
                boolean isText = messageType == PublishedMessageType.Utf8Text || messageType == PublishedMessageType.Utf16Text;
                willNumberEdit.setVisible(isNumber);
                componentLabelsMap.get(willNumberEdit).setVisible(isNumber);
                willTextMemo.setVisible(isText);
                componentLabelsMap.get(willTextMemo).setVisible(isText);
                if(willTextMemo.getParent() instanceof JScrollPane) {
                    willTextMemo.getParent().setVisible(isText);
                }
                else if(willTextMemo.getParent().getParent() instanceof JScrollPane){
                    willTextMemo.getParent().getParent().setVisible(isText);
                }
                willFileNameEdit.setVisible(isFile);
                chooseFileButton.setVisible(isFile);
                componentLabelsMap.get(willFileNameEdit).setVisible(isFile);
                willJson.setVisible(messageType == PublishedMessageType.Json);
                componentLabelsMap.get(willJson).setVisible(messageType == PublishedMessageType.Json);
                willXml.setVisible(messageType == PublishedMessageType.Xml);
                componentLabelsMap.get(willTextMemo).setVisible(messageType == PublishedMessageType.Xml);

            }
        });


        JComboBox<String> willQos = new JComboBox<String>(MqttConnectedTestStepPanel.QOS_NAMES);
        mainPanel.add(willQos, componentPlace(row));
        mainPanel.add(createLabel("Quality of Service:", willQos, 0), labelPlace(row));
        ++row;

        JCheckBox willRetained = new JCheckBox("Store Will Message as retained");
        mainPanel.add(willRetained, componentPlace(row));
        mainPanel.add(createLabel("Retained:", willRetained, 0));
        ++row;

        PropertyExpansionPopupListener.enable(serverUriEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(loginEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(passwordEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(clientIDEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(willTopicEdit, modelItemOfConnection);

        return new JScrollPane(mainPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    @Override
    protected boolean handleOk() {
        if(!legacy) {
            if (StringUtils.isNullOrEmpty(nameEdit.getText())) {
                nameEdit.grabFocus();
                UISupport.showErrorMessage("Please specify a name for the connection.");
                return false;
            }
            if(presentNames != null && !Utils.areStringsEqual(initialName, nameEdit.getText(), Connection.ARE_NAMES_CASE_INSENSITIVE)){
                boolean alreadyExists = false;
                for(String name: presentNames){
                    if(Utils.areStringsEqual(nameEdit.getText(), name, Connection.ARE_NAMES_CASE_INSENSITIVE)){
                        alreadyExists = true;
                        break;
                    }
                }
                if(alreadyExists) {
                    nameEdit.grabFocus();
                    UISupport.showErrorMessage(String.format("The connection with \"%s\" name already exists. Please specify another name.", nameEdit.getText()));
                    return false;
                }
            }
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
        if(nameEdit != null && !legacy) result.connectionName = nameEdit.getText();
        result.connectionParams.setServerUri(serverUriEdit.getText());
        result.connectionParams.fixedId = clientIDEdit.getText();
        if(authRequiredCheckBox.isSelected()) {
            result.connectionParams.setCredentials(loginEdit.getText(), new String(passwordEdit.getPassword()));
        }
        return true;
    }

    public class SelectFileAction extends AbstractAction {
        private JFileChooser fileChooser;
        private JTextField fileNameEdit;

        public SelectFileAction(JTextField fileNameEdit) {
            super("Browse...");
            this.fileNameEdit = fileNameEdit;
        }

        public void actionPerformed(ActionEvent arg0) {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
            }

            int returnVal = fileChooser.showOpenDialog(UISupport.getMainFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                fileNameEdit.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

}
