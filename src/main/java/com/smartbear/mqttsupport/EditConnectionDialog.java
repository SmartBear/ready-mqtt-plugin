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
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.PropertyAdapter;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.AbstractValueModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
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
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class EditConnectionDialog extends SimpleDialog {

    private JCheckBox willCheckBox;

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
    private Connection connection;
    private List<String> presentNames;
    private Result result = null;


    protected EditConnectionDialog(String title, ModelItem modelItemOfConnection, String initialConnectionName, ConnectionParams initialConnectionParams, boolean legacy, List<String> alreadyPresentNames){
        super(title, "Please specify the parameters for the connection", null, true);
        this.modelItemOfConnection = modelItemOfConnection;
        this.legacy = legacy;
        this.presentNames = alreadyPresentNames;
        this.initialName = initialConnectionName;
        if(initialConnectionParams == null) {
            this.connection = new Connection();
            this.connection.setName(initialConnectionName);
        }
        else{
            this.connection = new Connection(initialConnectionName, initialConnectionParams);
        }
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
//        super.beforeShow();
//        if(nameEdit != null) nameEdit.setText(connection.getName());
//        serverUriEdit.setText(initialParams.getServerUri());
//        clientIDEdit.setText(initialParams.fixedId);
        if(StringUtils.isNullOrEmpty(connection.getLogin())) {
            loginEdit.setEnabled(false);
            passwordEdit.setEnabled(false);
            hidePasswordCheckBox.setEnabled(false);
            authRequiredCheckBox.setSelected(false);
        }
        else{
            authRequiredCheckBox.setSelected(true);
//            loginEdit.setText(initialParams.login);
//            passwordEdit.setText(initialParams.password);
        }
        if(!legacy){
            willCheckBox.setSelected(StringUtils.hasContent(connection.getWillTopic()));
        }
    }

    private GridBagConstraints labelPlace(int row){
        return new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, defaultInsets, 0, 0);
    }

    private GridBagConstraints labelPlaceWithIndent(int row){
        return new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, defaultInsetsWithIndent, 0, 0);
    }

    private GridBagConstraints extraComponentPlace(int row){
        return new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, defaultInsets, 0, 0);
    }

    private GridBagConstraints componentPlace(int row){
        return new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, defaultInsets, 0, 0);
    }

    private GridBagConstraints largePlace(int row){
        return new GridBagConstraints(1, row, 2, 1, 0, 0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, defaultInsets, 0, 0);
    }

//    public static class IsEnabledValueModel extends PropertyAdapter<JComponent>{
//        public IsEnabledValueModel(JComponent component){ super(component, "enabled", true);}
//    }


    public static class IsVisibleValueModel extends AbstractValueModel{
        private JComponent component;
        public IsVisibleValueModel(JComponent component){
            this.component = component;
            this.component.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    fireValueChange(false, true);
                }

                @Override
                public void componentHidden(ComponentEvent e) {
                    fireValueChange(true, false);
                }
            });
        }
        @Override
        public Object getValue() {
            return component.isVisible();
        }
        @Override
        public void setValue(Object newValue) {
        }
    }
    public static class IsCheckedValueModel extends AbstractValueModel{
        private JCheckBox component;
        public IsCheckedValueModel(final JCheckBox component){
            this.component = component;
            this.component.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    fireValueChange(!component.isSelected(), component.isSelected());
                }
            });
        }
        @Override
        public Object getValue() {
            return component.isSelected();
        }
        @Override
        public void setValue(Object newValue) {
        }

    }

    private JLabel createLabel(String text, final JComponent targetComponent, int hitCharNo){
        JLabel label = new JLabel(text);
        label.setLabelFor(targetComponent);
        if(targetComponent != null){
            Bindings.bind(label, "visible", new IsVisibleValueModel(targetComponent));
            Bindings.bind(label, "enabled", new PropertyAdapter<JComponent>(targetComponent, "enabled", true));
        }
        if(hitCharNo >= 0) {
            label.setDisplayedMnemonic(text.charAt(hitCharNo));
            label.setDisplayedMnemonicIndex(hitCharNo);
        }
        return label;
    }

    private ReadOnlyValueModel<PublishedMessageType> isMsgType(PresentationModel<Connection> presentationModel, final PublishedMessageType ... alllowedTypes){
        return new ReadOnlyValueModel<PublishedMessageType>(presentationModel.getModel(Connection.WILL_MESSAGE_TYPE_BEAN_PROP), new ReadOnlyValueModel.Converter<PublishedMessageType>() {
            @Override
            public Object convert(PublishedMessageType srcValue) {
                for(PublishedMessageType type: alllowedTypes){
                    if(srcValue == type) return true;
                }
                return false;
            }
        });
    }

    @Override
    protected Component buildContent() {
        JLabel label;
        final int defEditCharCount = 30;
        final Dimension minMemoSize = new Dimension(50, 180);
        final int indentSize = 20;

        int row = 0;

        PresentationModel<Connection> pm = new PresentationModel<Connection>(connection);
        JPanel mainPanel = new JPanel(new GridBagLayout());

        if(!legacy) {
            nameEdit = new JUndoableTextField(defEditCharCount);
            nameEdit.setToolTipText("The unique connection name to identify it.");
            Bindings.bind(nameEdit, pm.getModel("name"));
            mainPanel.add(nameEdit, componentPlace(row));
            mainPanel.add(createLabel("Name:", nameEdit, 0), labelPlace(row));
            ++row;
        }

        serverUriEdit = new JUndoableTextField(defEditCharCount);
        serverUriEdit.setToolTipText("The MQTT server URI");
        Bindings.bind(serverUriEdit, pm.getModel("serverUri"));
        mainPanel.add(serverUriEdit, componentPlace(row));
        mainPanel.add(createLabel("Server URI:", serverUriEdit, 0), labelPlace(row));
        ++row;

        clientIDEdit = new JUndoableTextField(defEditCharCount);
        clientIDEdit.setToolTipText("Fill this field if you want to connect with fixed Client ID or leave it empty so a unique ID will be generated");
        Bindings.bind(clientIDEdit, pm.getModel(Connection.CLIENT_ID_BEAN_PROP));
        mainPanel.add(clientIDEdit, componentPlace(row));
        mainPanel.add(createLabel("Client ID (optional):", clientIDEdit, 0), labelPlace(row));
        ++row;

        JPanel indent = new JPanel();
        indent.setPreferredSize(new Dimension(1, indentSize));
        mainPanel.add(indent, componentPlace(row));
        ++row;

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
        mainPanel.add(authRequiredCheckBox, largePlace(row));
        mainPanel.add(createLabel("Authentication:", authRequiredCheckBox, 0), labelPlace(row));
        ++row;

        loginEdit = new JUndoableTextField(defEditCharCount);
        loginEdit.setToolTipText("Login for MQTT server");
        Bindings.bind(loginEdit, pm.getModel(Connection.LOGIN_BEAN_PROP));
        mainPanel.add(loginEdit, componentPlace(row));
        mainPanel.add(createLabel("Login:", loginEdit, 0), labelPlaceWithIndent(row));
        ++row;

        passwordEdit = new JPasswordField(defEditCharCount);
        passwordEdit.setToolTipText("Password for MQTT server");
        Bindings.bind(passwordEdit, pm.getModel(Connection.PASSWORD_BEAN_PROP));
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
        mainPanel.add(hidePasswordCheckBox, extraComponentPlace(row));
        ++row;

        if(!legacy) {

            indent = new JPanel();
            indent.setPreferredSize(new Dimension(1, indentSize));
            mainPanel.add(indent, componentPlace(row));
            ++row;

            willCheckBox = new JCheckBox("Store Will Message on the server");
            willCheckBox.setToolTipText("Set up the message which is published by the server if the connection to the client is terminated unexpectedly.");
            mainPanel.add(willCheckBox, largePlace(row));
            mainPanel.add(createLabel("Will Message:", willCheckBox, 0), labelPlace(row));
            ++row;
            ValueModel isWillOn = new IsCheckedValueModel(willCheckBox);

            JTextField willTopicEdit = new JTextField(defEditCharCount);
            Bindings.bind(willTopicEdit, pm.getModel(Connection.WILL_TOPIC_BEAN_PROP));
            Bindings.bind(willTopicEdit, "enabled", isWillOn);
            PropertyExpansionPopupListener.enable(willTopicEdit, modelItemOfConnection);
            mainPanel.add(willTopicEdit, componentPlace(row));
            mainPanel.add(createLabel("Topic:", willTopicEdit, 0), labelPlaceWithIndent(row));
            ++row;

            final JComboBox<PublishedMessageType> willMessageTypeCombo = new JComboBox<PublishedMessageType>(PublishedMessageType.values());
            Bindings.bind(willMessageTypeCombo, new SelectionInList<PublishedMessageType>(PublishedMessageType.values(), pm.getModel(Connection.WILL_MESSAGE_TYPE_BEAN_PROP)));
            Bindings.bind(willMessageTypeCombo, "enabled", isWillOn);
            mainPanel.add(willMessageTypeCombo, componentPlace(row));
            mainPanel.add(createLabel("Message type:", willMessageTypeCombo, 9), labelPlaceWithIndent(row));
            ++row;
            willMessageTypeCombo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            pack();
                        }
                    });
                }
            });

            final JTextField willNumberEdit = new JTextField(defEditCharCount);
            Bindings.bind(willNumberEdit, pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP));
            Bindings.bind(willNumberEdit, "visible", isMsgType(pm, PublishedMessageType.IntegerValue, PublishedMessageType.DoubleValue, PublishedMessageType.LongValue, PublishedMessageType.FloatValue));
            Bindings.bind(willNumberEdit, "enabled", isWillOn);
            PropertyExpansionPopupListener.enable(willNumberEdit, modelItemOfConnection);
            mainPanel.add(willNumberEdit, componentPlace(row));
            mainPanel.add(createLabel("Message:", willNumberEdit, 0), labelPlaceWithIndent(row));
            ++row;

            JTextArea willTextMemo = new JTextArea();
            JScrollPane willTextScrollPane = new JScrollPane(willTextMemo);
            willTextScrollPane.setPreferredSize(minMemoSize);
            Bindings.bind(willTextMemo, "visible", isMsgType(pm, PublishedMessageType.Utf8Text, PublishedMessageType.Utf16Text));
            Bindings.bind(willTextScrollPane, "visible", isMsgType(pm, PublishedMessageType.Utf8Text, PublishedMessageType.Utf16Text));
            Bindings.bind(willTextMemo, pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP), true);
            Bindings.bind(willTextMemo, "enabled", isWillOn);
            Bindings.bind(willTextScrollPane, "enabled", isWillOn);
            PropertyExpansionPopupListener.enable(willTextMemo, modelItemOfConnection);
            mainPanel.add(willTextScrollPane, largePlace(row));
            mainPanel.add(createLabel("Message:", willTextMemo, 0), labelPlaceWithIndent(row));
            ++row;

            ValueModel isFileMsgType = isMsgType(pm, PublishedMessageType.BinaryFile);
            final JTextField willFileNameEdit = new JTextField(defEditCharCount);
            willFileNameEdit.setToolTipText("The file which content will be used as a payload of Will Message");
            PropertyExpansionPopupListener.enable(willFileNameEdit, modelItemOfConnection);
            Bindings.bind(willFileNameEdit, "visible", isFileMsgType);
            Bindings.bind(willFileNameEdit, "enabled", isWillOn);
            Bindings.bind(willFileNameEdit, pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP));
            final JButton chooseFileButton = new JButton(new SelectFileAction(willFileNameEdit));
            Bindings.bind(chooseFileButton, "visible", isFileMsgType);
            Bindings.bind(chooseFileButton, "enabled", isWillOn);
            mainPanel.add(willFileNameEdit, componentPlace(row));
            mainPanel.add(chooseFileButton, extraComponentPlace(row));
            mainPanel.add(createLabel("File with message:", willFileNameEdit, 0), labelPlaceWithIndent(row));
            ++row;

            final JTabbedPane willJson = new JTabbedPane();
            RSyntaxTextArea syntaxTextArea = SyntaxEditorUtil.createDefaultJavaScriptSyntaxTextArea();
            Bindings.bind(syntaxTextArea, pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP), true);
            Bindings.bind(syntaxTextArea, "enabled", isWillOn);
            syntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            PropertyExpansionPopupListener.enable(syntaxTextArea, modelItemOfConnection);
            willJson.addTab("Text", new RTextScrollPane(syntaxTextArea));

            Utils.JsonTreeEditor jsonTreeEditor = new Utils.JsonTreeEditor(true, modelItemOfConnection);
            Bindings.bind(jsonTreeEditor, "text", pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP));
            Bindings.bind(jsonTreeEditor, "enabled", isWillOn);
            JScrollPane scrollPane = new JScrollPane(jsonTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            willJson.addTab("Tree View", scrollPane);

            Bindings.bind(willJson, "visible", isMsgType(pm, PublishedMessageType.Json));
            Bindings.bind(willJson, "enabled", isWillOn);
            willJson.setPreferredSize(minMemoSize);
            mainPanel.add(willJson, largePlace(row));
            mainPanel.add(createLabel("Message:", willJson, 0), labelPlaceWithIndent(row));
            ++row;

            final JTabbedPane willXml = new JTabbedPane();

            syntaxTextArea = SyntaxEditorUtil.createDefaultXmlSyntaxTextArea();
            Bindings.bind(syntaxTextArea, pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP), true);
            Bindings.bind(syntaxTextArea, "enabled", isWillOn);
            PropertyExpansionPopupListener.enable(syntaxTextArea, modelItemOfConnection);
            willXml.addTab("Text", new RTextScrollPane(syntaxTextArea));

            Utils.XmlTreeEditor xmlTreeEditor = new Utils.XmlTreeEditor(true, modelItemOfConnection);
            scrollPane = new JScrollPane(xmlTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Bindings.bind(xmlTreeEditor, "text", pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP));
            Bindings.bind(jsonTreeEditor, "enabled", isWillOn);
            willXml.addTab("Tree View", scrollPane);

            willXml.setPreferredSize(minMemoSize);
            Bindings.bind(willXml, "visible", isMsgType(pm, PublishedMessageType.Xml));
            Bindings.bind(willXml, "enabled", isWillOn);
            mainPanel.add(willXml, largePlace(row));
            mainPanel.add(createLabel("Message:", willXml, 0), labelPlace(row));
            ++row;

            JComboBox<String> willQos = new JComboBox<String>();
            Bindings.bind(willQos, new SelectionInList<String>(MqttConnectedTestStepPanel.QOS_NAMES, new ValueHolder(MqttConnectedTestStepPanel.QOS_NAMES[connection.getWillQos()]), pm.getModel(Connection.WILL_QOS_BEAN_PROP)));
            Bindings.bind(willQos, "enabled", isWillOn);
            mainPanel.add(willQos, componentPlace(row));
            mainPanel.add(createLabel("Quality of Service:", willQos, 0), labelPlaceWithIndent(row));
            ++row;

            JCheckBox willRetained = new JCheckBox("Store Will Message as retained");
            Bindings.bind(willRetained, pm.getModel(Connection.WILL_RETAINED_BEAN_PROP));
            Bindings.bind(willRetained, "enabled", isWillOn);
            mainPanel.add(willRetained, componentPlace(row));
            mainPanel.add(createLabel("Retained:", willRetained, 0), labelPlaceWithIndent(row));
            ++row;
        };
        PropertyExpansionPopupListener.enable(serverUriEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(loginEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(passwordEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(clientIDEdit, modelItemOfConnection);

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

        if(willCheckBox.isSelected()) {
            result.connectionParams = connection.getParams();
        }
        result.connectionParams.setServerUri(serverUriEdit.getText());
        result.connectionParams.fixedId = clientIDEdit.getText();
        if(authRequiredCheckBox.isSelected()) {
            result.connectionParams.setCredentials(loginEdit.getText(), new String(passwordEdit.getPassword()));
        }
        else{
            result.connectionParams.setCredentials(null, null);
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
