package com.smartbear.mqttsupport;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.swing.DefaultActionList;
import com.eviware.soapui.support.components.JButtonBar;
import com.eviware.soapui.support.components.JXToolBar;
import org.apache.xpath.operations.Mod;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.List;

public class ConfigureConnectionsDialog extends JDialog {

    private JTable grid;
    private ModelItem connectionsTargetItem;

    protected ConfigureConnectionsDialog(Frame owner, ModelItem modelItem){
        super(owner);
        this.connectionsTargetItem = modelItem;
        buildUI();
    }

    protected ConfigureConnectionsDialog(ModelItem modelItem){
        super();
        this.connectionsTargetItem = modelItem;
        buildUI();
    }

    private void buildUI(){
        getRootPane().add(UISupport.buildDescription("Configure MQTT Server Connections", "Specify MQTT servers required for the test project and customize connections to them", null), BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout());
        JComponent toolBar = buildToolbar();
        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(buildGrid(), BorderLayout.CENTER);
        getRootPane().add(mainPanel, BorderLayout.CENTER);

        DefaultActionList actions = new DefaultActionList();
        actions.addAction(new OkAction(), true);
        JButtonBar buttons = UISupport.initDialogActions(actions, this);
        buttons
                .setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Color.WHITE)), BorderFactory.createEmptyBorder(3, 5,
                        3, 5)));

        getContentPane().add(buttons, BorderLayout.SOUTH);

    }

    private JComponent buildToolbar() {
        JXToolBar toolBar = UISupport.createToolbar();
        Action addAction = new AddConnectionAction();
        toolBar.add(UISupport.createActionButton(addAction, addAction.isEnabled()));
        Action removeAction = new RemoveConnectionAction();
        JButton removeButton = UISupport.createActionButton(addAction, addAction.isEnabled());
        toolBar.add(removeButton);
        return toolBar;
    }

    private JComponent buildGrid(){
        ConnectionsTableModel tableModel = new ConnectionsTableModel();
        tableModel.setData(ConnectionsManager.getAvailableConnections(connectionsTargetItem));
        grid = new JTable(tableModel);

        return grid;
    }

    public boolean showDialog(ModelItem modelItem){
        ConfigureConnectionsDialog dialog = new ConfigureConnectionsDialog(modelItem);
        dialog.setModal(true);
        dialog.setVisible(true);
        return true;
    }

    public boolean showDialog(Frame frame, ModelItem modelItem){
        ConfigureConnectionsDialog dialog = new ConfigureConnectionsDialog(frame, modelItem);
        dialog.setModal(true);
        dialog.setVisible(true);
        return true;
    }

    private static class ConnectionsTableModel extends AbstractTableModel{
        public enum Column {
            Name("Name"), ServerUri("MQTT Server URI"), ClientId("Client ID"), Login("Login");
            private final String caption;

            Column(String caption){this.caption = caption;}
            public String getCaption(){return caption;}
        }

        private List<ConnectionParams> data;

        public void setData(List<ConnectionParams> data){
            this.data = data;
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
                    return data.get(rowIndex).getName();
                case ServerUri:
                    return data.get(rowIndex).getServerUri();
                case ClientId:
                    return data.get(rowIndex).getFixedId();
                case Login:
                    return data.get(rowIndex).getLogin();
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            switch (Column.values()[columnIndex]){
                case Name :
                    data.get(rowIndex).setName((String)aValue);
                case ServerUri:
                    data.get(rowIndex).setServerUri((String)aValue);
                case ClientId:
                    data.get(rowIndex).setFixedId((String)aValue);
                case Login:
                    data.get(rowIndex).setLogin((String)aValue);
            }
        }
    }

    private class OkAction extends AbstractAction{
        public OkAction() {
            super("Close");
        }

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }
    private class AddConnectionAction extends AbstractAction {
        public AddConnectionAction() {
            super("Add Connection");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

        }
    }

    private class RemoveConnectionAction extends AbstractAction {
        public RemoveConnectionAction() {
            super("Remove Connection");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

        }
    }

}
