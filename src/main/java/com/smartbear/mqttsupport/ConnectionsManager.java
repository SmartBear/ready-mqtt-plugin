package com.smartbear.mqttsupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.model.workspace.Workspace;
import com.eviware.soapui.support.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class ConnectionsManager  {
    private final static String CONNECTIONS_SETTING_NAME = "MQTTConnections";
    private final static String CONNECTION_SECTION_NAME = "Connection";

    private HashMap<Project, ArrayList<Connection>> connections = new HashMap<>();
    private HashMap<Project, ArrayList<ConnectionsListener>> listeners = new HashMap<>();
    private static ConnectionsManager instance = null;

    private ConnectionsManager(){
        Workspace workspace = SoapUI.getWorkspace();
        for(Project project: workspace.getProjectList()){
            if(project.isOpen()){
                ArrayList<Connection> projectConnections = grabConnections(project);
                this.connections.put(project, projectConnections);
            }
        }
    }

    private static ConnectionsManager getInstance(){
        if(instance == null) instance = new ConnectionsManager();
        return instance;
    }


    public static Connection getConnection(ModelItem modelItem, String connectionName){
        Project project = ModelSupport.getModelItemProject(modelItem);
        if(project == null || StringUtils.isNullOrEmpty(connectionName)) throw new IllegalArgumentException();
        ArrayList<Connection> projectConnections = getInstance().connections.get(project);
        for(Connection connection: projectConnections){
            if(Utils.areStringsEqual(connectionName, connection.getName())) return connection;
        }
        return null;
    }

    public static List<Connection> getAvailableConnections(ModelItem modelItem){
        Project project = ModelSupport.getModelItemProject(modelItem);
        if(project == null) throw new IllegalArgumentException();
        ArrayList<Connection> projectConnections = getInstance().connections.get(project);
        if(projectConnections == null) return null;
        return new ArrayList<Connection>(projectConnections);
    }

    public static void addConnection(ModelItem modelItem, Connection connection){
        Project project = ModelSupport.getModelItemProject(modelItem);
        if(project == null) throw new IllegalArgumentException();
        ArrayList<Connection> projectConnections = getInstance().connections.get(project);
        if(projectConnections == null) {
            projectConnections = new ArrayList<Connection>();
            getInstance().connections.put(project, projectConnections);
        }
        boolean alreadyAdded = false;
        for(Connection curConnection: projectConnections){
            if(curConnection == connection) {
                alreadyAdded = true;
                break;
            }
        }
        if(!alreadyAdded){
            projectConnections.add(connection);
            fireConnectionsListChangedEvent(project);
        }
    };

    public static void removeConnection(ModelItem modelItem, Connection connection){
        Project project = ModelSupport.getModelItemProject(modelItem);
        if(project == null) throw new IllegalArgumentException();
        ArrayList<Connection> projectConnections = getInstance().connections.get(project);
        if(projectConnections == null) return;
        boolean removed = false;
        for(int i = 0; i < projectConnections.size(); ++i){
            if(projectConnections.get(i) == connection) {
                projectConnections.remove(i);
                fireConnectionsListChangedEvent(project);
                return;
            }
        }
    };


    public static void addConnectionsListener(ModelItem modelItem, ConnectionsListener listener){
        Project project = ModelSupport.getModelItemProject(modelItem);
        if(project == null) throw new IllegalArgumentException();

        ArrayList<ConnectionsListener> projectListeners = getInstance().listeners.get(project);
        if(projectListeners == null) {
            projectListeners = new ArrayList<ConnectionsListener>();
            getInstance().listeners.put(project, projectListeners);
        }
        projectListeners.add(listener);
    }

    public static void removeConnectionsListener(ModelItem modelItem, ConnectionsListener listener){
        Project project = ModelSupport.getModelItemProject(modelItem);
        if(project == null) throw new IllegalArgumentException();

        ArrayList<ConnectionsListener> projectListeners = getInstance().listeners.get(project);
        if(projectListeners == null) {
            SoapUI.log("MQTT plugin: Internal error: this object is not subscribed to a connection list change.");
            return;
        }
        projectListeners.remove(listener);

    }


    static void onProjectLoaded(Project project){
        if(instance == null) return;
        ArrayList<Connection> projectConnections = grabConnections(project);
        getInstance().connections.put(project, projectConnections);
        fireConnectionsListChangedEvent(project);
    }

    static void beforeProjectSaved(Project project){
        //if(instance == null) return;
        saveConnections(project, getInstance().connections.get(project));
    }

    private static ArrayList<Connection> grabConnections(Project project){
        ArrayList<Connection> result = null;
        String settingValue = project.getSettings().getString(CONNECTIONS_SETTING_NAME, "");
        if(StringUtils.hasContent(settingValue)) {
            XmlObject root = null;
            try {
                root = XmlObject.Factory.parse(settingValue);
            }
            catch (XmlException e) {
                SoapUI.logError(e);
                return result;
            }
            result = new ArrayList<Connection>();
            XmlObject[] connectionSections = root.selectPath("$this/" + CONNECTION_SECTION_NAME);
            for(XmlObject section : connectionSections){
                Connection connection = new Connection();
                connection.load(section);
                result.add(connection);
            }
        }
        return result;
    }

    private static void saveConnections(Project project, List<Connection> connections){
        if(connections == null || connections.size() == 0){
            project.getSettings().clearSetting(CONNECTIONS_SETTING_NAME);
        }
        else {
            XmlObjectBuilder builder = new XmlObjectBuilder();
            for (Connection connection : connections) {
                XmlObject connectionXml = connection.save();
                builder.addSection(CONNECTION_SECTION_NAME, connectionXml);
            }
            project.getSettings().setString(CONNECTIONS_SETTING_NAME, builder.finish().toString());
        }
    }

    private static void fireConnectionsListChangedEvent(Project project){
        ArrayList<ConnectionsListener> listenersForProject = getInstance().listeners.get(project);
        if(listenersForProject != null){
            for(ConnectionsListener listener: listenersForProject){
                try {
                    listener.connectionListChanged();
                }
                catch (Throwable e){
                    SoapUI.logError(e);
                }
            }
        }
    }
}
