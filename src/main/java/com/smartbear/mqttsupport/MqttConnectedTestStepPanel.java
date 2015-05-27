package com.smartbear.mqttsupport;

import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.AbstractValueModel;
import com.jgoodies.binding.value.ValueModel;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class MqttConnectedTestStepPanel<MqttTestStep extends MqttConnectedTestStep> extends ModelItemDesktopPanel<MqttTestStep>{

    private final static String LEGACY_CONNECTION_NAME = "Individual (legacy) connection";
    private final static String NEW_CONNECTION_ITEM = "<New Connection...>";
    private final static String CONVERT_BUTTON_CAPTION = "Convert Connection";
    private final static String LEGACY_WARNING = "This test step was created by the old version of the plugin. The current version uses another management model for MQTT connections which allows manage them centrally and share between test steps of a project. Click " + CONVERT_BUTTON_CAPTION + " button to create a new style connection and assign it to the test step.";
    private JButton configureConnectionButton;
    private char passwordChar;
    private ConnectionsComboBoxModel connectionsModel;

    public interface UIOption{
        String getTitle();
    }

    class ConfigureConnectionsAction extends AbstractAction{
        public ConfigureConnectionsAction() {
            putValue(Action.SHORT_DESCRIPTION, "Configure MQTT Connections of the Project");
            putValue(Action.SMALL_ICON, UISupport.createImageIcon("com/smartbear/mqttsupport/edit_connections.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ConfigureProjectConnectionsDialog.showDialog(getModelItem());
        }
    }

    static class ConnectionComboItem{
        private Connection obj;
        public ConnectionComboItem(Connection connection){
            obj = connection;
        }

        @Override
        public String toString(){
            if(obj.isLegacy()) return LEGACY_CONNECTION_NAME; else return obj.getName();
        }

        public Connection getObject(){return obj;}

        @Override
        public boolean equals(Object op) {
            if(op instanceof ConnectionComboItem){
                return ((ConnectionComboItem) op).obj == obj;
            }
            else{
                return false;
            }
        }
    }

    static class NewConnectionComboItem extends ConnectionComboItem{
        private final static NewConnectionComboItem instance = new NewConnectionComboItem();
        public static NewConnectionComboItem getInstance(){return instance;}
        private NewConnectionComboItem(){
            super(null);
        }

        @Override
        public String toString() {
            return NEW_CONNECTION_ITEM;
        }
    }

    class ConnectionsComboBoxModel extends AbstractListModel<ConnectionComboItem> implements ComboBoxModel<ConnectionComboItem>, ConnectionsListener{

        private ArrayList<ConnectionComboItem> items = new ArrayList<>();
        private boolean connectionCreationInProgress = false;
        public ConnectionsComboBoxModel(){
            updateItems();
        }

        private void updateItems(){
            items.clear();
            List<Connection> list = ConnectionsManager.getAvailableConnections(getModelItem());
            if(list != null){
                for(Connection curParams: list){
                    items.add(new ConnectionComboItem(curParams));
                }
            }
            if(getModelItem().getLegacyConnection() != null){
                items.add(new ConnectionComboItem(getModelItem().getLegacyConnection()));
            }
            items.add(NewConnectionComboItem.getInstance());
            fireContentsChanged(this, -1, -1);
        }


        @Override
        public void setSelectedItem(Object anItem) {
            if(anItem == null){
                getModelItem().setConnection(null);
            }
            else {
                if(anItem instanceof NewConnectionComboItem){
                    connectionCreationInProgress = true;
                    UISupport.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            EditConnectionDialog.Result dialogResult = EditConnectionDialog.createConnection(getModelItem());
                            connectionCreationInProgress = false;
                            if(dialogResult == null) {
                                fireContentsChanged(ConnectionsComboBoxModel.this, -1, -1);
                            }
                            else {
                                Connection newConnection = new Connection(dialogResult.connectionName, dialogResult.connectionParams);
                                ConnectionsManager.addConnection(getModelItem(), newConnection);
                                getModelItem().setConnection(newConnection);
                            }

                        }
                    });
                }
                else {
                    Connection newParams = ((ConnectionComboItem) anItem).getObject();
                    getModelItem().setConnection(newParams);
                    fireContentsChanged(this, -1, -1);
                }
            }
        }

        @Override
        public Object getSelectedItem() {
            if(connectionCreationInProgress) return NewConnectionComboItem.getInstance();
            if(getModelItem().getConnection() == null) return null;
            return new ConnectionComboItem(getModelItem().getConnection());
        }

        @Override
        public void connectionListChanged() {
            updateItems();
        }

        @Override
        public void connectionChanged(Connection connection, String propertyName, Object oldPropertyValue, Object newPropertyValue) {
            if(Utils.areStringsEqual(propertyName, "name")){
                for(int i = 0; i <items.size(); ++i){
                    if(items.get(i).getObject() == connection){
                        fireContentsChanged(connection, i, i);
                        break;
                    }
                }
            }
        }

        @Override
        public int getSize() {
            return items.size();
        }

        @Override
        public ConnectionComboItem getElementAt(int index) {
            return items.get(index);
        }

    }

    public interface Converter<SrcType>{
        Object convert(SrcType srcValue);
    }

    public static class ReadOnlyValueModel<SrcType> extends AbstractValueModel {
        private ValueModel source;
        private Converter<SrcType> converter;
        public ReadOnlyValueModel(ValueModel source, Converter<SrcType> converter){
            this.source = source;
            this.converter = converter;
            source.addValueChangeListener(new SubjectValueChangeHandler());
        }

        @Override
        public Object getValue(){
            return converter.convert((SrcType) source.getValue());
        }

        @Override
        public void setValue(Object newValue){}

        private final class SubjectValueChangeHandler implements PropertyChangeListener {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fireValueChange(converter.convert((SrcType) evt.getOldValue()), converter.convert((SrcType) evt.getNewValue()) , true);
            }
        }
    }

    public MqttConnectedTestStepPanel(MqttTestStep modelItem) {
        super(modelItem);
    }

    protected void buildConnectionSection(SimpleBindingForm form,  PresentationModel<MqttTestStep> pm) {
        connectionsModel = new ConnectionsComboBoxModel();
        ConnectionsManager.addConnectionsListener(getModelItem(), connectionsModel);

        form.appendHeading("Connection to MQTT Server");
        form.appendComboBox("Connection", connectionsModel, "Choose one of pre-configured connections");
        configureConnectionButton = form.appendButtonWithoutLabel("Configure...", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Connection connection = getModelItem().getConnection();
                EditConnectionDialog.Result dialogResult = EditConnectionDialog.editConnection(connection, getModelItem());
                if (dialogResult != null) {
                    if(!connection.isLegacy()) connection.setName(dialogResult.connectionName);
                    connection.setParams(dialogResult.connectionParams);
                }
            }
        });
        configureConnectionButton.setIcon(UISupport.createImageIcon("com/eviware/soapui/resources/images/options.png"));
        configureConnectionButton.setEnabled(getModelItem().getConnection() != null);
//        form.addButtonWithoutLabelToTheRight("Configure Connections...", new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                ConfigureConnectionsDialog.showDialog(getModelItem());
//            }
//        });

        JTextArea legacyInfoLabel = new JTextArea(LEGACY_WARNING, 0, form.getDefaultTextAreaColumns());
        legacyInfoLabel.setEnabled(false);
        legacyInfoLabel.setLineWrap(true);
        legacyInfoLabel.setWrapStyleWord(true);
        form.addLeftComponent(legacyInfoLabel);

        JButton convertConnectionButton = form.appendButtonWithoutLabel(CONVERT_BUTTON_CAPTION + "...", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            EditConnectionDialog.Result dialogResult = EditConnectionDialog.convertLegacyConnection(getModelItem().getConnection().getParams(), getModelItem());
            if(dialogResult != null){
                Connection newConnection = new Connection(dialogResult.connectionName, dialogResult.connectionParams);
                ConnectionsManager.addConnection(getModelItem(), newConnection);
                getModelItem().setConnection(newConnection);
            }

            }
        });

//        JTextField serverEdit = form.appendTextField("serverUri", "MQTT Server URI", "The MQTT server URI");
//        JTextField clientIdEdit = form.appendTextField("clientId", "Client ID (optional)", "Fill this field if you want to connect with fixed Client ID or leave it empty so a unique ID will be generated");
//        JTextField loginEdit = form.appendTextField("login", "Login (optional)", "Login to MQTT server. Fill this if the server requires authentication.");
//
//        final String PASSWORD_TOOLTIP = "Password to MQTT server. Fill this if the server requires authentication.";
//
//
//        final JPanel passwordPanel = new JPanel();
//        passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.X_AXIS));
//        final JPasswordField passwordEdit = new JPasswordField(loginEdit.getColumns());
//        passwordEdit.setToolTipText(PASSWORD_TOOLTIP);
//        passwordEdit.getAccessibleContext().setAccessibleDescription(PASSWORD_TOOLTIP);
//        Bindings.bind(passwordEdit, pm.getModel("password"));
//        passwordPanel.add(passwordEdit);
//        final JCheckBox hidePasswordCheckBox = new JCheckBox("Hide");
//        hidePasswordCheckBox.setSelected(true);
//        passwordPanel.add(hidePasswordCheckBox);
//        hidePasswordCheckBox.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if(hidePasswordCheckBox.isSelected()){
//                    passwordEdit.setEchoChar(passwordChar);
//                }
//                else{
//                    passwordChar = passwordEdit.getEchoChar();
//                    passwordEdit.setEchoChar('\0');
//                }
//            }
//        });
//        form.append("Password (optional)", passwordPanel);

        ReadOnlyValueModel<Connection> legacyModeAdapter = new ReadOnlyValueModel<>(pm.getModel("connection"), new Converter<Connection>() {
            @Override
            public Object convert(Connection srcValue) {
                return srcValue != null && srcValue.isLegacy();
            }
        });
//        Bindings.bind(serverEdit, "enabled", legacyModeAdapter);
//        Bindings.bind(clientIdEdit, "enabled", legacyModeAdapter);
//        Bindings.bind(loginEdit, "enabled", legacyModeAdapter);
//        Bindings.bind(passwordPanel, "visible", legacyModeAdapter);
        Bindings.bind(convertConnectionButton, "visible", legacyModeAdapter);
        Bindings.bind(legacyInfoLabel, "visible", legacyModeAdapter);

//        PropertyExpansionPopupListener.enable(serverEdit, getModelItem());
//        PropertyExpansionPopupListener.enable(loginEdit, getModelItem());
//        PropertyExpansionPopupListener.enable(passwordEdit, getModelItem());
//        PropertyExpansionPopupListener.enable(clientIdEdit, getModelItem());

    }

    protected void buildQosRadioButtons(SimpleBindingForm form,  PresentationModel<MqttTestStep> pm){
        JPanel qosPanel = new JPanel();
        qosPanel.setLayout(new BoxLayout(qosPanel, BoxLayout.X_AXIS));
        JRadioButton qos0Radio = new JRadioButton("At most once (0)"), qos1Radio = new JRadioButton("At least once (1)"), qos2Radio = new JRadioButton("Exactly once (2)");
        Bindings.bind(qos0Radio, pm.getModel("qos"), 0);
        Bindings.bind(qos1Radio, pm.getModel("qos"), 1);
        Bindings.bind(qos2Radio, pm.getModel("qos"), 2);
        qosPanel.add(qos0Radio);
        qosPanel.add(qos1Radio);
        qosPanel.add(qos2Radio);
        form.append("Quality of service", qosPanel);

    }


    protected void buildRadioButtonsFromEnum(SimpleBindingForm form, PresentationModel<MqttTestStep> pm, String label, String propertyName, Class propertyType) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        for(Object option : propertyType.getEnumConstants()){
            UIOption uiOption = (UIOption) option;
            JRadioButton radioButton = new JRadioButton(uiOption.getTitle());
            Bindings.bind(radioButton, pm.getModel(propertyName), option);
            panel.add(radioButton);
        }
        form.append(label, panel);
    }

    protected void buildTimeoutSpinEdit(SimpleBindingForm form, PresentationModel<MqttTestStep> pm, String label){
        JPanel timeoutPanel = new JPanel();
        timeoutPanel.setLayout(new BoxLayout(timeoutPanel, BoxLayout.X_AXIS));
        JSpinner spinEdit = Utils.createBoundSpinEdit(pm, "shownTimeout", 0, Integer.MAX_VALUE, 1);
        spinEdit.setPreferredSize(new Dimension(80, spinEdit.getHeight()));
        timeoutPanel.add(spinEdit);
        JComboBox measureCombo = new JComboBox(MqttConnectedTestStep.TimeMeasure.values());
        Bindings.bind(measureCombo, new SelectionInList<Object>(MqttConnectedTestStep.TimeMeasure.values(), pm.getModel("timeoutMeasure")));
        timeoutPanel.add(measureCombo);
        timeoutPanel.add(new JLabel(" (0 - forever)"));
        form.append(label, timeoutPanel);

    }

    protected void addConnectionActionsToToolbar(JXToolBar toolBar){
        Action configureConnectionsAction = new ConfigureConnectionsAction();
        JButton button = UISupport.createActionButton(configureConnectionsAction, configureConnectionsAction.isEnabled());
        toolBar.add(button);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        if(Utils.areStringsEqual(evt.getPropertyName(), "connection")){
            if(connectionsModel != null) connectionsModel.setSelectedItem(new ConnectionComboItem((Connection)evt.getNewValue()));
            configureConnectionButton.setEnabled(evt.getNewValue() != null);
        }
    }

    @Override
    protected boolean release() {
        if(connectionsModel != null) ConnectionsManager.removeConnectionsListener(getModelItem(), connectionsModel);
        return super.release();
    }

}
