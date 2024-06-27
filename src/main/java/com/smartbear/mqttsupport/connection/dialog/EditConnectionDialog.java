package com.smartbear.mqttsupport.connection.dialog;

import com.eviware.soapui.impl.wsdl.actions.project.SimpleDialog;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.propertyexpansion.DefaultPropertyExpansionContext;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JUndoableTextArea;
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
import com.smartbear.mqttsupport.Utils;
import com.smartbear.mqttsupport.connection.Connection;
import com.smartbear.mqttsupport.connection.ConnectionParams;
import com.smartbear.mqttsupport.connection.ConnectionsManager;
import com.smartbear.mqttsupport.teststeps.PublishedMessageType;
import com.smartbear.mqttsupport.teststeps.panels.MqttConnectedTestStepPanel;
import com.smartbear.mqttsupport.teststeps.panels.ReadOnlyValueModel;
import com.smartbear.ready.ui.style.GlobalStyles;
import com.smartbear.soapui.ui.components.ButtonFactory;
import com.smartbear.soapui.ui.components.combobox.ComboBoxFactory;
import com.smartbear.soapui.ui.components.tabbedpane.TabbedPane;
import com.smartbear.soapui.ui.components.textfield.TextFieldFactory;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class EditConnectionDialog extends SimpleDialog {

    private static final String MAIN_PANEL_TAB = "General";
    private static final String SSL_PANEL_TAB = "SSL Properties";
    private static final String AUTH_PANEL_TAB = "Authentication";
    private static final String WILL_PANEL_TAB = "Will Message";

    private JCheckBox willCheckBox;
    private JComponent jsonTreeEditor;
    private JComponent xmlTreeEditor;
    private JCheckBox cleanSessionCheckBox;
    private JTextField willTopicEdit;
    private JTabbedPane tabsHolder;
    private PropertyExpander expander;
    private PropertyExpansionContext context;

    public class Result {
        public String connectionName;
        public ConnectionParams connectionParams;

        public Result() {
            connectionParams = new ConnectionParams();
        }
    }

    private boolean legacy;
    private JTextField nameEdit;
    private final static Insets defaultInsets = new Insets(4, 4, 4, 4);
    private final static Insets defaultInsetsWithIndent = new Insets(defaultInsets.top, defaultInsets.left + 12, defaultInsets.bottom, defaultInsets.right);
    private JTextField serverUriEdit;

    private JTextField caCertificateEdit;
    private JTextField clientCertificateEdit;
    private JTextField privateKeyEdit;
    private JTextField privateKeyPasswordEdit;
    private JTextField sniServerEdit;

    private JTextField clientIDEdit;
    private JCheckBox authRequiredCheckBox;
    private JTextField loginEdit;
    private JUndoableTextField passwordEdit;
    private JCheckBox hidePasswordCheckBox;
    private HashMap<JComponent, JLabel> componentLabelsMap = new HashMap<>();

    private char passwordChar;

    private ModelItem modelItemOfConnection;
    private String initialName;
    private Connection connection;
    private List<String> presentNames;
    private Result result = null;


    protected EditConnectionDialog(String title, ModelItem modelItemOfConnection, String initialConnectionName, ConnectionParams initialConnectionParams, boolean legacy, List<String> alreadyPresentNames) {
        super(title, "Please specify the parameters for the connection", null, true);
        this.modelItemOfConnection = modelItemOfConnection;
        this.legacy = legacy;
        this.presentNames = alreadyPresentNames;
        this.initialName = initialConnectionName;
        this.expander = new PropertyExpander(true);
        this.context = new DefaultPropertyExpansionContext(modelItemOfConnection);
        if (initialConnectionParams == null) {
            this.connection = new Connection();
            this.connection.setName(initialConnectionName);
        } else {
            this.connection = new Connection(initialConnectionName, initialConnectionParams);
        }
    }

    public static Result showDialog(String title, ModelItem modelItemOfConnection, String initialConnectionName, ConnectionParams initialConnectionParams, boolean legacy, List<String> alreadyPresentNames) {
        EditConnectionDialog dialog = new EditConnectionDialog(title, modelItemOfConnection, initialConnectionName, initialConnectionParams, legacy, alreadyPresentNames);
        try {
            dialog.setModal(true);
            UISupport.centerDialog(dialog);
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
        return dialog.result;

    }


    public static Result editConnection(Connection connection, ModelItem modelItemOfConnection) {
        if (connection.isLegacy()) {
            return showDialog("Configure Connection", modelItemOfConnection, null, connection.getParams(), true, null);
        } else {
            Project project = ModelSupport.getModelItemProject(modelItemOfConnection);
            ArrayList<String> existingNames = new ArrayList<>();
            List<Connection> connections = ConnectionsManager.getAvailableConnections(project);
            if (connections != null) {
                for (Connection curConnection : connections) {
                    if (curConnection != connection) {
                        existingNames.add(curConnection.getName());
                    }
                }
            }
            String title = String.format("Configure %s Connection", connection.getName());
            return showDialog(title, project, connection.getName(), connection.getParams(), false, existingNames);
        }

    }

    public static Result createConnection(ModelItem modelItemOfConnection) {
        Project project = ModelSupport.getModelItemProject(modelItemOfConnection);
        ArrayList<String> existingNames = new ArrayList<>();
        List<Connection> connections = ConnectionsManager.getAvailableConnections(project);
        if (connections != null) {
            for (Connection curConnection : connections) {
                existingNames.add(curConnection.getName());
            }
        }
        return showDialog("Create New Connection", project, null, null, false, existingNames);

    }

    public static Result convertLegacyConnection(ConnectionParams srcParams, ModelItem modelItemOfConnection) {
        Project project = ModelSupport.getModelItemProject(modelItemOfConnection);
        ArrayList<String> existingNames = new ArrayList<>();
        List<Connection> connections = ConnectionsManager.getAvailableConnections(project);
        if (connections != null) {
            for (Connection curConnection : connections) {
                existingNames.add(curConnection.getName());
            }
        }
        return showDialog("Convert Legacy Connection", project, null, srcParams, false, existingNames);

    }

    @Override
    protected void beforeShow() {
        if (StringUtils.isNullOrEmpty(connection.getLogin())) {
            loginEdit.setEnabled(false);
            passwordEdit.setEnabled(false);
            hidePasswordCheckBox.setEnabled(false);
            authRequiredCheckBox.setSelected(false);
        } else {
            authRequiredCheckBox.setSelected(true);
        }
        if (!legacy) {
            willCheckBox.setSelected(StringUtils.hasContent(connection.getWillTopic()));
        }
    }

    public static class IsVisibleValueModel extends AbstractValueModel {
        private JComponent component;

        public IsVisibleValueModel(JComponent component) {
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

    public static class IsCheckedValueModel extends AbstractValueModel {
        private JCheckBox component;

        public IsCheckedValueModel(final JCheckBox component) {
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

    private JLabel createLabel(String text, final JComponent targetComponent, int hitCharNo) {
        JLabel label = new JLabel(text);
        label.setLabelFor(targetComponent);
        if (targetComponent != null) {
            Bindings.bind(label, "visible", new IsVisibleValueModel(targetComponent));
            Bindings.bind(label, "enabled", new PropertyAdapter<JComponent>(targetComponent, "enabled", true));
        }
        if (hitCharNo >= 0) {
            label.setDisplayedMnemonic(text.charAt(hitCharNo));
            label.setDisplayedMnemonicIndex(hitCharNo);
        }
        return label;
    }

    private ReadOnlyValueModel<PublishedMessageType> isMsgType(PresentationModel<Connection> presentationModel, final PublishedMessageType... alllowedTypes) {
        return new ReadOnlyValueModel<PublishedMessageType>(presentationModel.getModel(Connection.WILL_MESSAGE_TYPE_BEAN_PROP), new ReadOnlyValueModel.Converter<PublishedMessageType>() {
            @Override
            public Object convert(PublishedMessageType srcValue) {
                for (PublishedMessageType type : alllowedTypes) {
                    if (srcValue == type) {
                        return true;
                    }
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

        JPanel root = new JPanel(new MigLayout("wrap", "[grow,fill]", "8[]8"));
        tabsHolder = new TabbedPane();
        root.add(tabsHolder);

        PresentationModel<Connection> pm = new PresentationModel<Connection>(connection);
        JPanel mainPanel = new JPanel(getInnerLayout());
        tabsHolder.addTab(MAIN_PANEL_TAB, mainPanel);

        if (!legacy) {
            nameEdit = TextFieldFactory.createTextField(defEditCharCount);
            nameEdit.setToolTipText("The unique connection name to identify it.");
            Bindings.bind(nameEdit, pm.getModel("name"));
            mainPanel.add(createLabel("Name:", nameEdit, 0));
            mainPanel.add(nameEdit);
        }

        serverUriEdit = createField(defEditCharCount, pm, mainPanel,
                "serverUri", "Server URL:", "The MQTT server URL");

        clientIDEdit = createField(defEditCharCount, pm, mainPanel,
                Connection.CLIENT_ID_BEAN_PROP, "Client ID (optional):", "Fill this field if you want to connect with fixed Client ID or leave it empty so a unique ID will be generated");

        if (!legacy) {
            creteCheckBox(pm, mainPanel,
                    "Clean session:", "Start clean session on reconnection");
        }

        JPanel sslPanel = new JPanel(getInnerLayout());
        caCertificateEdit = createField(defEditCharCount, pm, sslPanel,
                Connection.CERT_CA_CERT_SETTING_NAME, "CA certificate:", "CA certificate in PEM format");
        clientCertificateEdit = createField(defEditCharCount, pm, sslPanel,
                Connection.CERT_CLIENT_CERT_SETTING_NAME, "Client certificate:", "Client certificate in PEM format");
        privateKeyEdit = createField(defEditCharCount, pm, sslPanel,
                Connection.CERT_KEY_SETTING_NAME, "Key file:", "Client private key in PEM format");
        privateKeyPasswordEdit = createField(defEditCharCount, pm, sslPanel,
                Connection.CERT_KEY_PASSWORD_SETTING_NAME, "Key password:", "Private key password");
        sniServerEdit = createField(defEditCharCount, pm, sslPanel,
                Connection.CERT_SNI_SERVER_SETTING_NAME, "SSL server name:", "Server name to be provided for SSL connection");


        JPanel authPanel = new JPanel(getInnerLayout());
        tabsHolder.addTab(AUTH_PANEL_TAB, authPanel);

        authRequiredCheckBox = ButtonFactory.createCheckBox("The server requires authentication");
        authRequiredCheckBox.setToolTipText("Check if the MQTT server requires authentication to connect");
        authRequiredCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginEdit.setEnabled(authRequiredCheckBox.isSelected());
                passwordEdit.setEnabled(authRequiredCheckBox.isSelected());
                hidePasswordCheckBox.setEnabled(authRequiredCheckBox.isSelected());
                if (authRequiredCheckBox.isSelected()) {
                    loginEdit.grabFocus();
                }
            }
        });
        authPanel.add(new JLabel());
        authPanel.add(authRequiredCheckBox, "span 2");


        loginEdit = TextFieldFactory.createTextField(defEditCharCount);
        loginEdit.setToolTipText("Login for MQTT server");
        Bindings.bind(loginEdit, pm.getModel(Connection.LOGIN_BEAN_PROP));
        authPanel.add(createLabel("Login:", loginEdit, 0));
        authPanel.add(loginEdit);

        passwordEdit = TextFieldFactory.createPasswordTextField().withColumns(defEditCharCount);
        passwordEdit.setToolTipText("Password for MQTT server");
        Bindings.bind(passwordEdit, pm.getModel(Connection.PASSWORD_BEAN_PROP));
        authPanel.add(createLabel("Password:", passwordEdit, 0));
        authPanel.add(passwordEdit);
        hidePasswordCheckBox = ButtonFactory.createCheckBox("Hide");
        hidePasswordCheckBox.setSelected(true);
        hidePasswordCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordEdit.setHidePassword(hidePasswordCheckBox.isSelected());
            }
        });
        passwordEdit.add(hidePasswordCheckBox);

        if (!legacy) {
            JPanel willMessagePanel = new JPanel(getInnerLayout());
            tabsHolder.addTab(WILL_PANEL_TAB, willMessagePanel);

            willCheckBox = ButtonFactory.createCheckBox("Store Will Message on the server");
            willCheckBox.setToolTipText("Set up the message which is published by the server if the connection to the client is terminated unexpectedly.");
            willMessagePanel.add(new JLabel());
            willMessagePanel.add(willCheckBox);

            ValueModel isWillOn = new IsCheckedValueModel(willCheckBox);

            willTopicEdit = TextFieldFactory.createTextField(defEditCharCount);
            Bindings.bind(willTopicEdit, pm.getModel(Connection.WILL_TOPIC_BEAN_PROP));
            Bindings.bind(willTopicEdit, "enabled", isWillOn);
            PropertyExpansionPopupListener.enable(willTopicEdit, modelItemOfConnection);
            willMessagePanel.add(createLabel("Topic:", willTopicEdit, 0));
            willMessagePanel.add(willTopicEdit);

            JComboBox willQos = ComboBoxFactory.createComboBox(new DefaultComboBoxModel());
            Bindings.bind(willQos, new SelectionInList<String>(MqttConnectedTestStepPanel.QOS_NAMES, new ValueHolder(MqttConnectedTestStepPanel.QOS_NAMES[connection.getWillQos()]), pm.getModel(Connection.WILL_QOS_BEAN_PROP)));
            Bindings.bind(willQos, "enabled", isWillOn);
            willMessagePanel.add(createLabel("Quality of Service:", willQos, 0));
            willMessagePanel.add(willQos);

            JCheckBox willRetained = ButtonFactory.createCheckBox("Store Will Message as retained");
            willRetained.setBackground(GlobalStyles.Dialog.DARK_BACKGROUND_COLOR);
            willRetained.setForeground(GlobalStyles.Common.ENABLED_TEXT_COLOR);
            Bindings.bind(willRetained, pm.getModel(Connection.WILL_RETAINED_BEAN_PROP));
            Bindings.bind(willRetained, "enabled", isWillOn);
            willMessagePanel.add(createLabel("Retained:", willRetained, 0));
            willMessagePanel.add(willRetained);

            final CardLayout willMessageOptions = new CardLayout();
            final JPanel currentWillMessage = new JPanel(willMessageOptions);

            final JComboBox willMessageTypeCombo = ComboBoxFactory.createComboBox(new DefaultComboBoxModel(PublishedMessageType.values()));
            Bindings.bind(willMessageTypeCombo, new SelectionInList<PublishedMessageType>(PublishedMessageType.values(), pm.getModel(Connection.WILL_MESSAGE_TYPE_BEAN_PROP)));
            Bindings.bind(willMessageTypeCombo, "enabled", isWillOn);
            willMessagePanel.add(createLabel("Message type:", willMessageTypeCombo, 9));
            willMessagePanel.add(willMessageTypeCombo);
            willMessageTypeCombo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateSelectedWillMessageType(willMessageTypeCombo, willMessageOptions, currentWillMessage);
                }
            });

            willMessagePanel.add(currentWillMessage, "span 2");

            JPanel numberPanel = new JPanel(new MigLayout("wrap 2", "0[100]8[grow,fill]0", "8[]8"));
            final JTextField willNumberEdit = TextFieldFactory.createTextField(defEditCharCount);
            Bindings.bind(willNumberEdit, pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP));
            Bindings.bind(willNumberEdit, "enabled", isWillOn);
            PropertyExpansionPopupListener.enable(willNumberEdit, modelItemOfConnection);
            numberPanel.add(createLabel("Message:", willNumberEdit, 0));
            numberPanel.add(willNumberEdit);
            currentWillMessage.add(numberPanel, PublishedMessageType.IntegerValue.name());

            JPanel textPanel = new JPanel(new MigLayout("wrap", "0[grow,fill]0", "0[grow,fill]8"));
            JTextArea willTextMemo = new JUndoableTextArea();
            willTextMemo.setBackground(GlobalStyles.TextField.ENABLED_COLOR);
            willTextMemo.setForeground(GlobalStyles.Common.ENABLED_TEXT_COLOR);
            JScrollPane willTextScrollPane = new JScrollPane(willTextMemo);
            willTextScrollPane.setPreferredSize(minMemoSize);
            Bindings.bind(willTextMemo, pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP), true);
            Bindings.bind(willTextMemo, "enabled", isWillOn);
            Bindings.bind(willTextScrollPane, "enabled", isWillOn);
            PropertyExpansionPopupListener.enable(willTextMemo, modelItemOfConnection);
            textPanel.add(createLabel("Message:", willTextMemo, 0), "span 2");
            textPanel.add(willTextScrollPane);
            currentWillMessage.add(textPanel, PublishedMessageType.Utf8Text.name());

            JPanel filePanel = new JPanel(new MigLayout("", "0[100]8[grow,fill]8[]0", "0[]8"));
            final JTextField willFileNameEdit = TextFieldFactory.createTextField(defEditCharCount);
            willFileNameEdit.setToolTipText("The file which content will be used as a payload of Will Message");
            PropertyExpansionPopupListener.enable(willFileNameEdit, modelItemOfConnection);
            Bindings.bind(willFileNameEdit, "enabled", isWillOn);
            Bindings.bind(willFileNameEdit, pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP));
            final JButton chooseFileButton = ButtonFactory.createLightButton().withAction(new SelectFileAction(willFileNameEdit));
            Bindings.bind(chooseFileButton, "enabled", isWillOn);
            filePanel.add(createLabel("File with message:", willFileNameEdit, 0));
            filePanel.add(willFileNameEdit);
            filePanel.add(chooseFileButton, "span 2");
            currentWillMessage.add(filePanel, PublishedMessageType.BinaryFile.name());

            JPanel jsonPanel = new JPanel(new MigLayout("wrap", "0[grow,fill]0", "0[grow,fill]8"));
            final JTabbedPane willJson = new TabbedPane();
            RSyntaxTextArea syntaxTextArea = SyntaxEditorUtil.createDefaultJavaScriptSyntaxTextArea();
            Bindings.bind(syntaxTextArea, pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP), true);
            Bindings.bind(syntaxTextArea, "enabled", isWillOn);
            syntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            PropertyExpansionPopupListener.enable(syntaxTextArea, modelItemOfConnection);
            willJson.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

            jsonTreeEditor = Utils.createJsonTreeEditor(true, modelItemOfConnection);
            if (jsonTreeEditor == null) {
                JLabel stubLabel = new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER);
                willJson.addTab("Tree View", stubLabel);
            } else {
                Bindings.bind(jsonTreeEditor, "text", pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP));
                Bindings.bind(jsonTreeEditor, "enabled", isWillOn);
                JScrollPane scrollPane = new JScrollPane(jsonTreeEditor);
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                willJson.addTab("Tree View", scrollPane);
            }
            Bindings.bind(willJson, "enabled", isWillOn);
            willJson.setPreferredSize(minMemoSize);
            jsonPanel.add(createLabel("Message:", willJson, 0));
            jsonPanel.add(willJson);
            currentWillMessage.add(jsonPanel, PublishedMessageType.Json.name());

            JPanel xmlPanel = new JPanel(new MigLayout("wrap", "0[grow,fill]0", "0[grow,fill]8"));
            final JTabbedPane willXml = new TabbedPane();
            syntaxTextArea = SyntaxEditorUtil.createDefaultXmlSyntaxTextArea();
            Bindings.bind(syntaxTextArea, pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP), true);
            Bindings.bind(syntaxTextArea, "enabled", isWillOn);
            PropertyExpansionPopupListener.enable(syntaxTextArea, modelItemOfConnection);
            willXml.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

            xmlTreeEditor = Utils.createXmlTreeEditor(true, modelItemOfConnection);
            if (xmlTreeEditor == null) {
                JLabel stubLabel = new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER);
                willXml.addTab("Tree View", stubLabel);
            } else {
                JScrollPane scrollPane = new JScrollPane(xmlTreeEditor);
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                Bindings.bind(xmlTreeEditor, "text", pm.getModel(Connection.WILL_MESSAGE_BEAN_PROP));
                Bindings.bind(jsonTreeEditor, "enabled", isWillOn);
                willXml.addTab("Tree View", scrollPane);
            }

            willXml.setPreferredSize(minMemoSize);
            Bindings.bind(willXml, "enabled", isWillOn);
            xmlPanel.add(createLabel("Message:", willXml, 0));
            xmlPanel.add(willXml);
            currentWillMessage.add(xmlPanel, PublishedMessageType.Xml.name());

            updateSelectedWillMessageType(willMessageTypeCombo, willMessageOptions, currentWillMessage);
        }
        tabsHolder.addTab(SSL_PANEL_TAB, sslPanel);

        PropertyExpansionPopupListener.enable(serverUriEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(caCertificateEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(clientCertificateEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(privateKeyEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(privateKeyPasswordEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(sniServerEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(loginEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(passwordEdit, modelItemOfConnection);
        PropertyExpansionPopupListener.enable(clientIDEdit, modelItemOfConnection);

        return new JScrollPane(root, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    private void updateSelectedWillMessageType(JComboBox<PublishedMessageType> willMessageTypeCombo, CardLayout willMessageOptions, JPanel currentWillMessage) {
        switch ((PublishedMessageType) willMessageTypeCombo.getSelectedItem()) {
            case IntegerValue:
            case DoubleValue:
            case LongValue:
            case FloatValue:
                willMessageOptions.show(currentWillMessage, PublishedMessageType.IntegerValue.name());
                break;
            case Utf8Text:
            case Utf16Text:
                willMessageOptions.show(currentWillMessage, PublishedMessageType.Utf8Text.name());
                break;
            case BinaryFile:
            case Json:
            case Xml:
                willMessageOptions.show(currentWillMessage, ((PublishedMessageType) willMessageTypeCombo.getSelectedItem()).name());
                break;
        }
    }

    private MigLayout getInnerLayout() {
        return new MigLayout("wrap 2", "8[100]8[grow,fill]8", "8[]8");
    }

    private void creteCheckBox(PresentationModel<Connection> pm, JPanel mainPanel,
                               String caption, String text) {
        cleanSessionCheckBox = ButtonFactory.createCheckBox(text);
        Bindings.bind(cleanSessionCheckBox, pm.getModel(Connection.CLEAN_SESSION_BEAN_PROP));
        mainPanel.add(createLabel(caption, cleanSessionCheckBox, 6));
        mainPanel.add(cleanSessionCheckBox, "span 2");
    }

    private JTextField createField(int defEditCharCount, PresentationModel<Connection> pm, JPanel parent,
                                   String fieldName, String caption, String hint) {
        JTextField newEdit = TextFieldFactory.createTextField(defEditCharCount);
        newEdit.setToolTipText(hint);
        Bindings.bind(newEdit, pm.getModel(fieldName));
        parent.add(createLabel(caption, newEdit, 0));
        parent.add(newEdit);
        return newEdit;
    }

    @Override
    protected boolean handleOk() {
        if (!legacy) {
            if (StringUtils.isNullOrEmpty(nameEdit.getText())) {
                activateTab(MAIN_PANEL_TAB);
                nameEdit.grabFocus();
                UISupport.showErrorMessage("Please specify a name for the connection.");
                return false;
            }
            if (presentNames != null && !Utils.areStringsEqual(initialName, nameEdit.getText(), Connection.ARE_NAMES_CASE_INSENSITIVE)) {
                boolean alreadyExists = false;
                for (String name : presentNames) {
                    if (Utils.areStringsEqual(nameEdit.getText(), name, Connection.ARE_NAMES_CASE_INSENSITIVE)) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (alreadyExists) {
                    activateTab(MAIN_PANEL_TAB);
                    nameEdit.grabFocus();
                    UISupport.showErrorMessage(String.format("The connection with \"%s\" name already exists. Please specify another name.", nameEdit.getText()));
                    return false;
                }
            }
        }
        if (StringUtils.isNullOrEmpty(serverUriEdit.getText())) {
            activateTab(MAIN_PANEL_TAB);
            serverUriEdit.grabFocus();
            UISupport.showErrorMessage("Please specify URI of MQTT server.");
            return false;
        }
        if (authRequiredCheckBox.isSelected() && StringUtils.isNullOrEmpty(loginEdit.getText())) {
            activateTab(AUTH_PANEL_TAB);
            loginEdit.grabFocus();
            UISupport.showErrorMessage("Please specify a login or uncheck \"Authentication required\" check-box if the authentication is not required for this MQTT server.");
            return false;
        }
        if (!legacy && willCheckBox.isSelected()) {
            if (StringUtils.isNullOrEmpty(willTopicEdit.getText())) {
                activateTab(WILL_PANEL_TAB);
                willTopicEdit.grabFocus();
                UISupport.showErrorMessage("Please specify a topic for the Will Message.");
                return false;
            }
        }

        boolean isSSL = !StringUtils.isNullOrEmpty(caCertificateEdit.getText()) || !StringUtils.isNullOrEmpty(clientCertificateEdit.getText())
                || !StringUtils.isNullOrEmpty(privateKeyEdit.getText()) || !StringUtils.isNullOrEmpty(privateKeyPasswordEdit.getText())
                || !StringUtils.isNullOrEmpty(sniServerEdit.getText());

        if (isSSL) {
            if (StringUtils.isNullOrEmpty(caCertificateEdit.getText())) {
                activateTab(SSL_PANEL_TAB);
                caCertificateEdit.grabFocus();
                UISupport.showErrorMessage("Missing CA certificate. Please provide a valid CA certificate and retry");
                return false;
            }

            if (!StringUtils.isNullOrEmpty(clientCertificateEdit.getText())) {
                if (StringUtils.isNullOrEmpty(privateKeyEdit.getText())) {
                    activateTab(SSL_PANEL_TAB);
                    UISupport.showErrorMessage("Client certificate was provided without Key file. Please provide Key file or clear Client certificate");
                    return false;
                }
            } else {
                if (!StringUtils.isNullOrEmpty(privateKeyEdit.getText()) || !StringUtils.isNullOrEmpty(privateKeyPasswordEdit.getText())) {
                    activateTab(SSL_PANEL_TAB);
                    UISupport.showErrorMessage("Key file or Key password was provided without Client certificate. Please provide Client certificate or clear fields");
                    return false;
                }
            }
        }

        if (!checkCertificateField(caCertificateEdit)
                || !checkCertificateField(clientCertificateEdit)
                || !checkCertificateField(privateKeyEdit)) {
            return false;
        }

        result = new Result();
        if (nameEdit != null && !legacy) {
            result.connectionName = nameEdit.getText();
        }

        if (willCheckBox != null && willCheckBox.isSelected()) {
            result.connectionParams = connection.getParams();
        }

        result.connectionParams.setServerUri(serverUriEdit.getText());
        result.connectionParams.setCaCrtFile(caCertificateEdit.getText());
        result.connectionParams.setCrtFile(clientCertificateEdit.getText());
        result.connectionParams.setKeyFile(privateKeyEdit.getText());
        result.connectionParams.setKeysPassword(privateKeyPasswordEdit.getText());
        result.connectionParams.setSniHost(sniServerEdit.getText());
        result.connectionParams.fixedId = clientIDEdit.getText();
        result.connectionParams.cleanSession = cleanSessionCheckBox == null || cleanSessionCheckBox.isSelected();
        if (authRequiredCheckBox.isSelected()) {
            result.connectionParams.setCredentials(loginEdit.getText(), new String(passwordEdit.getPassword()));
        } else {
            result.connectionParams.setCredentials(null, null);
        }
        return true;
    }

    void activateTab(String tabName) {
        for (int i = 0; i < tabsHolder.getTabCount(); i++) {
            String title = tabsHolder.getTitleAt(i);
            if (Utils.areStringsEqual(title, tabName)) {
                tabsHolder.setSelectedIndex(i);
                return;
            }
        }
    }

    boolean checkCertificateField(JTextField edit) {
        String input = edit.getText();

        if (StringUtils.isNullOrEmpty(input)) {
            return true;
        }

        input = expander.expand(context, input);

        if (checkFileExistance(input)) {
            return true;
        }

        activateTab(SSL_PANEL_TAB);
        edit.grabFocus();
        UISupport.showErrorMessage("Please specify a correct file path in the selected field.");
        return false;
    }

    private boolean checkFileExistance(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.isFile();
    }

    @Override
    public void dispose() {
        if (xmlTreeEditor != null) {
            Utils.releaseTreeEditor(xmlTreeEditor);
        }
        if (jsonTreeEditor != null) {
            Utils.releaseTreeEditor(jsonTreeEditor);
        }
        super.dispose();
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
