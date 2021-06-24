package com.smartbear.mqttsupport.connection.dialog;

import com.eviware.soapui.impl.wsdl.actions.project.SimpleDialog;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JUndoableTextField;
import com.eviware.soapui.support.components.JXToolBar;
import com.smartbear.mqttsupport.Utils;
import com.smartbear.mqttsupport.connection.Connection;
import com.smartbear.mqttsupport.connection.ConnectionParams;
import com.smartbear.mqttsupport.connection.ConnectionsManager;
import com.smartbear.mqttsupport.teststeps.MqttConnectedTestStep;
import com.smartbear.soapui.ui.components.textfield.TextFieldFactory;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConfigureProjectConnectionsDialog extends SimpleDialog {

    private JTable grid;
    private Project connectionsTargetItem;
    private ConnectionsTableModel tableModel;
    private Action editAction;
    private Action removeAction;

    protected ConfigureProjectConnectionsDialog(ModelItem modelItem){
        super("Configure Connections to MQTT Servers", "Add, remove or edit connections to MQTT servers needed for the project", null, true);
        if(modelItem instanceof Project) {
            this.connectionsTargetItem = (Project) modelItem;
        }
        else{
            this.connectionsTargetItem = ModelSupport.getModelItemProject(modelItem);
        }
    }

    @Override
    protected Component buildContent() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(buildToolbar(), BorderLayout.NORTH);
        mainPanel.add(buildGrid(), BorderLayout.CENTER);
        return mainPanel;
    }

    private JComponent buildToolbar() {
        JXToolBar toolBar = UISupport.createToolbar();
        Action addAction = new AddConnectionAction();
        toolBar.add(UISupport.createActionButton(addAction, addAction.isEnabled()));
        editAction = new EditAction();
        JButton editButton = UISupport.createActionButton(editAction, editAction.isEnabled());
        toolBar.add(editButton);
        removeAction = new RemoveConnectionAction();
        JButton removeButton = UISupport.createActionButton(removeAction, removeAction.isEnabled());
        toolBar.add(removeButton);
        return toolBar;
    }

    private JComponent buildGrid(){
        tableModel = new ConnectionsTableModel();
        grid = new JTable(tableModel);
        for(int i = 0; i < ConnectionsTableModel.Column.values().length; ++i){
            grid.getColumnModel().getColumn(i).setIdentifier(ConnectionsTableModel.Column.values()[i]);
        }
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        grid.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                editAction.setEnabled(grid.getSelectionModel().getLeadSelectionIndex() >= 0);
                removeAction.setEnabled(grid.getSelectedRowCount() > 0);
            }
        });
        tableModel.setData(ConnectionsManager.getAvailableConnections(connectionsTargetItem));
        tableModel.setUsageData(formUsageData());
        grid.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JUndoableTextField pswEdit = TextFieldFactory.createPasswordTextField();
        grid.getColumn(ConnectionsTableModel.Column.Password).setCellRenderer(new PasswordRenderer(pswEdit.getEchoChar()));
        grid.getColumn(ConnectionsTableModel.Column.Password).setCellEditor(new DefaultCellEditor(pswEdit));
        return new JScrollPane(grid);
    }

    private HashMap<Connection, List<TestStep>> formUsageData(){
        HashMap<Connection, List<TestStep>> usageData = new HashMap<>();
        List<? extends TestSuite> testSuites = connectionsTargetItem.getTestSuiteList();
        if(testSuites != null) {
            for (TestSuite testSuite : testSuites) {
                List<? extends TestCase> testCases = testSuite.getTestCaseList();
                if (testCases == null) continue;
                for (TestCase testCase : testCases) {
                    List<TestStep> testSteps = testCase.getTestStepList();
                    if (testSteps == null) continue;
                    for (TestStep testStep : testSteps) {
                        if (testStep instanceof MqttConnectedTestStep) {
                            Connection testStepConnection = ((MqttConnectedTestStep) testStep).getConnection();
                            if (testStepConnection != null && !testStepConnection.isLegacy()) {
                                List<TestStep> usingItems = usageData.get(testStepConnection);
                                if (usingItems == null) {
                                    usingItems = new ArrayList<>();
                                    usageData.put(testStepConnection, usingItems);
                                }
                                usingItems.add(testStep);
                            }
                        }
                    }
                }
            }
        }
        return usageData;
    }

    public static boolean showDialog(ModelItem modelItem){
        ConfigureProjectConnectionsDialog dialog = new ConfigureProjectConnectionsDialog(modelItem);
        try {
            dialog.setModal(true);
            UISupport.centerDialog(dialog);
            UISupport.centerDialog(dialog);
            dialog.setVisible(true);
            return true;
        }
        finally {
            dialog.dispose();
        }
    }


    public Dimension getPreferredSize() {
        return new Dimension(650, 450);
    }

    @Override
    protected boolean handleOk() {
        for(int i = 0; i < tableModel.getRowCount(); ++i){
            ConnectionRecord checkedRecord = tableModel.getItem(i);
            if(StringUtils.isNullOrEmpty(checkedRecord.name)){
                grid.getSelectionModel().clearSelection();
                grid.editCellAt(i, ConnectionsTableModel.Column.Name.ordinal());
                UISupport.showErrorMessage("Please specify a name for the connection to have a possibility to identify it later.");
                return false;
            }
            for(int j = 0; j < i; ++j){
                if(Utils.areStringsEqual(tableModel.getItem(j).name, checkedRecord.name, Connection.ARE_NAMES_CASE_INSENSITIVE)){
                    grid.getSelectionModel().clearSelection();
                    grid.editCellAt(i, ConnectionsTableModel.Column.Name.ordinal());
                    UISupport.showErrorMessage("There are other connections with the same name. Please make it unique.");
                    return false;

                }
            }
            if(StringUtils.isNullOrEmpty(checkedRecord.params.getServerUri())){
                grid.clearSelection();
                grid.editCellAt(i, ConnectionsTableModel.Column.ServerUri.ordinal());
                UISupport.showErrorMessage("Please specify URI of MQTT server");
                return false;
            }
        }
        if(tableModel.getRemovedConnections() != null){
            for(Connection connection: tableModel.getRemovedConnections()){
                List<TestStep> usingTestSteps = tableModel.getUsageData().get(connection);
                if(usingTestSteps != null && usingTestSteps.size() != 0){
                    for(TestStep testStep: usingTestSteps){
                        ((MqttConnectedTestStep)testStep).setConnection(null);
                    }
                }
                ConnectionsManager.removeConnection(this.connectionsTargetItem, connection);
            }
        }
        for(int i = 0; i < tableModel.getRowCount(); ++i){
            ConnectionRecord record = tableModel.getItem(i);
            if(record.originalConnection == null){
                Connection connection = new Connection(record.name,  record.params);
                ConnectionsManager.addConnection(this.connectionsTargetItem, connection);
            }
            else{
                record.originalConnection.setName(record.name);
                record.originalConnection.setParams(record.params);
            }
        }
        return true;
    }

    private static class ConnectionRecord{
        public String name;
        public ConnectionParams params;
        public Connection originalConnection;
    }

    private static class ConnectionsTableModel extends AbstractTableModel{
        public enum Column {
            Name("Name"), ServerUri("MQTT Server URI"), ClientId("Client ID"), Login("Login"), Password("Password"), Used("Used by Test Steps");
            private final String caption;

            Column(String caption){this.caption = caption;}
            public String getCaption(){return caption;}
        }

        private ArrayList<ConnectionRecord> data;
        private ArrayList<Connection> removedConnections;
        private HashMap<Connection, List<TestStep>> usageData;

        public void setData(List<Connection> data){
            this.data = new ArrayList<>(data == null ? 5 : data.size() + 5);
            this.removedConnections = new ArrayList<>();
            if(data != null) {
                for (Connection connection : data) {
                    ConnectionRecord record = new ConnectionRecord();
                    record.name = connection.getName();
                    record.originalConnection = connection;
                    record.params = connection.getParams();
                    this.data.add(record);
                }
            }
            fireTableDataChanged();
        }

        public HashMap<Connection, List<TestStep>> getUsageData(){return usageData;}

        public void setUsageData(HashMap<Connection, List<TestStep>> usageData){
            this.usageData = usageData;
        }

        @Override
        public int getRowCount() {
            return data == null ? 0 : data.size();
        }

        @Override
        public int getColumnCount() {
            return Column.values().length;
        }

        @Override
        public String getColumnName(int column) {
            return Column.values()[column].getCaption();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (Column.values()[columnIndex]){
                case Name :
                    return data.get(rowIndex).name;
                case ServerUri:
                    return data.get(rowIndex).params.getServerUri();
                case ClientId:
                    return data.get(rowIndex).params.fixedId;
                case Login:
                    return data.get(rowIndex).params.login;
                case Password:
                    return data.get(rowIndex).params.password;
                case Used:
                    Connection connection = data.get(rowIndex).originalConnection;
                    if(connection == null) return false;
                    List<TestStep> involvingTestSteps = usageData.get(connection);
                    return involvingTestSteps != null && involvingTestSteps.size() > 0;
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            switch (Column.values()[columnIndex]){
                case Name :
                    data.get(rowIndex).name = (String)aValue;
                    break;
                case ServerUri:
                    data.get(rowIndex).params.setServerUri((String) aValue);
                    break;
                case ClientId:
                    data.get(rowIndex).params.fixedId = (String)aValue;
                    break;
                case Login:
                    data.get(rowIndex).params.login = (String)aValue;
                    break;
                case Password :
                    data.get(rowIndex).params.password = (String)aValue;
                    break;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            Column column =  Column.values()[columnIndex];
            return column == Column.Name || column == Column.ServerUri || column == Column.ClientId || column == Column.Login || column == Column.Password;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if(Column.values()[columnIndex] == Column.Used) return Boolean.class; else return super.getColumnClass(columnIndex);
        }

        public int addItem(String name, ConnectionParams params){
            ConnectionRecord record = new ConnectionRecord();
            record.name = name;
            record.params = params;
            data.add(record);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
            return data.size() - 1;
        }

        public ConnectionRecord getItem(int row){
            return data.get(row);
        }
        public void removeItem(int row) {
            if(data.get(row).originalConnection != null) removedConnections.add(data.get(row).originalConnection);
            data.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public void updateItem(int row, String name, ConnectionParams params){
            data.get(row).name = name;
            data.get(row).params = params;
            fireTableRowsUpdated(row, row);
        }

        public List<Connection> getRemovedConnections(){return removedConnections;}
    }

    private static class PasswordRenderer extends DefaultTableCellRenderer {
        private char passwordChar;
        public PasswordRenderer(char passwordChar) { super(); this.passwordChar = passwordChar;}

        public void setValue(Object value) {
            if(value == null || value.toString() == null) {
                setText("");
            }
            else {
                char[] arr = new char[value.toString().length()];
                Arrays.fill(arr, passwordChar);
                setText(new String(arr));
            }
        }
    }

    private class AddConnectionAction extends AbstractAction {
        public AddConnectionAction() {
            putValue(Action.SHORT_DESCRIPTION, "Add Connection");
            putValue(Action.SMALL_ICON, UISupport.createImageIcon("com/eviware/soapui/resources/images/add.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            EditConnectionDialog.Result result = EditConnectionDialog.showDialog("Create Connection", connectionsTargetItem, null, null, false, null);
            if(result != null){
                tableModel.addItem(result.connectionName, result.connectionParams);
            }

        }
    }


    private class RemoveConnectionAction extends AbstractAction {
        public RemoveConnectionAction() {
            putValue(Action.SHORT_DESCRIPTION, "Remove Selected Connections");
            putValue(Action.SMALL_ICON, UISupport.createImageIcon("com/eviware/soapui/resources/images/delete.png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final int maxCount = 4;
            int[] rows = grid.getSelectedRows();
            if (rows == null || rows.length == 0) return;
            int connectionNo = 0;
            String msg;
            if (rows.length == 1) {
                msg = tableModel.getItem(rows[0]).name;
                if (StringUtils.isNullOrEmpty(msg))
                    msg = "Do you really want to delete this connection?";
                else {
                    msg = "Do you really want to delete \"" + msg + "\" connection?";
                }
            } else {
                msg = "Do you really want to delete these connections?";
                for (int row : rows) {
                    ConnectionRecord record = tableModel.getItem(row);
                    if (connectionNo != maxCount) {
                        msg += "\n";
                        if (StringUtils.hasContent(record.name)) {
                            msg += record.name;
                        } else {
                            msg += "<untitled>";
                        }
                        connectionNo++;
                    } else {
                        msg += "\n...";
                    }
                }
            }
            List<String> affectedModelItems = getAffectedModelItems(rows, maxCount);
            if (affectedModelItems != null && affectedModelItems.size() != 0) {
                msg += "\nNote, that the following test step(s) will be deprived of a connection and have to be customized later:";
                for (int i = 0; i < affectedModelItems.size(); ++i) {
                    msg += "\n";
                    msg += affectedModelItems.get(i);
                }

            }
            if(UISupport.getDialogs().confirm(msg, "Confirm deletion")){
                Arrays.sort(rows);
                for(int i = rows.length - 1; i >= 0; --i){
                    tableModel.removeItem(rows[i]);
                }
            }
        }

        private List<String> getAffectedModelItems(int[] rows, int maxRowCount){
            ArrayList<String> result = new ArrayList<>();
            List<? extends TestSuite> testSuites = connectionsTargetItem.getTestSuiteList();
            if(testSuites != null) {
                for (TestSuite testSuite: testSuites) {
                    List<? extends TestCase> testCases = testSuite.getTestCaseList();
                    if(testCases == null) continue;
                    for (TestCase testCase : testCases){
                        List<TestStep> testSteps = testCase.getTestStepList();
                        if(testSteps == null) continue;
                        for(TestStep testStep: testSteps){
                            if(testStep instanceof MqttConnectedTestStep){
                                Connection testStepConnection = ((MqttConnectedTestStep)testStep).getConnection();
                                if(testStepConnection != null){
                                    for(int row: rows){
                                        ConnectionRecord record = tableModel.getItem(row);
                                        if(record.originalConnection == testStepConnection){
                                            if(result.size() == maxRowCount){
                                                result.add("...");
                                                return result;
                                            }
                                            else {
                                                result.add(String.format("\"%s\" of \"%s\" test case", testStep.getName(), testCase.getName()));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }
    }

    private class EditAction extends AbstractAction{
        public EditAction(){
            putValue(Action.SHORT_DESCRIPTION, "Configure Selected Connection");
            putValue(Action.SMALL_ICON, UISupport.createImageIcon("com/eviware/soapui/resources/images/options.png"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            int rowNo = grid.getSelectionModel().getLeadSelectionIndex();
            if(rowNo < 0) return;
            ConnectionRecord focusedRecord = tableModel.getItem(rowNo);
            EditConnectionDialog.Result result = EditConnectionDialog.showDialog(String.format("Edit %s Connection", focusedRecord.name), connectionsTargetItem, focusedRecord.name, focusedRecord.params, false, null);
            if(result != null){
                tableModel.updateItem(rowNo, result.connectionName, result.connectionParams);
            }


        }
    }

}
