package com.smartbear.mqttsupport;


import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.model.ModelItem;

import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.support.DateUtil;
import com.eviware.soapui.support.ListDataChangeListener;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.support.editor.views.xml.outline.support.JsonObjectTree;
import com.eviware.soapui.support.editor.views.xml.outline.support.XmlObjectTree;
import com.eviware.soapui.support.log.JLogList;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
import com.eviware.soapui.support.xml.SyntaxEditorUtil;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;

import javax.swing.AbstractAction;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.util.Date;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class PublishTestStepPanel extends MqttConnectedTestStepPanel<PublishTestStep> implements ExecutionListener {

    private JTextField numberEdit;
    private JTextArea textMemo;
    private JTextField fileNameEdit;
    private JButton chooseFileButton;
    private JTabbedPane jsonEditor;
    private Utils.JsonTreeEditor jsonTreeEditor;
    private JTabbedPane xmlEditor;
    private Utils.XmlTreeEditor xmlTreeEditor;

    private JInspectorPanel inspectorPanel;
    private JComponentInspector<JComponent> logInspector;
    private JLogList logArea;
    private final static String LOG_TAB_TITLE = "Test Step Log (%d)";



    public PublishTestStepPanel(PublishTestStep modelItem) {
        super(modelItem);
        buildUI();
        modelItem.addExecutionListener(this);
    }


    private void buildUI(){
        JComponent mainPanel = buildMainPanel();
        inspectorPanel = JInspectorPanelFactory.build(mainPanel);


        logInspector = new JComponentInspector<JComponent>(buildLogPanel(), String.format(LOG_TAB_TITLE, 0), "Log of the test step executions", true);
        inspectorPanel.addInspector(logInspector);

        inspectorPanel.setDefaultDividerLocation(0.6F);
//        inspectorPanel.setCurrentInspector("Assertions");


        add(inspectorPanel.getComponent());
        setPreferredSize(new Dimension(500, 300));

    }

    private JComponent buildMainPanel() {
        PresentationModel<PublishTestStep> pm = new PresentationModel<PublishTestStep>(getModelItem());
        SimpleBindingForm form = new SimpleBindingForm(pm);
        buildConnectionSection(form, pm);

        form.appendSeparator();
        form.appendHeading("Published Message");
        JTextField topicEdit = form.appendTextField("topic", "Topic", "Message Topic");
        PropertyExpansionPopupListener.enable(topicEdit, getModelItem());
        form.appendComboBox("messageKind", "Message kind", PublishTestStep.MessageType.values(), "");
        numberEdit = form.appendTextField("message", "Message", "The number which will be published.");
        textMemo = form.appendTextArea("message", "Message", "The text which will be published.");
        PropertyExpansionPopupListener.enable(textMemo, getModelItem());
        fileNameEdit = form.appendTextField("message", "File name", "The file which content will be used as payload");
        PropertyExpansionPopupListener.enable(fileNameEdit, getModelItem());
        chooseFileButton = form.addRightButton(new SelectFileAction());

        jsonEditor = new JTabbedPane();

        RSyntaxTextArea syntaxTextArea = SyntaxEditorUtil.createDefaultJavaScriptSyntaxTextArea();
        syntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        Bindings.bind(syntaxTextArea, pm.getModel("message"), true);
        PropertyExpansionPopupListener.enable(syntaxTextArea, getModelItem());
        jsonEditor.addTab("Text", new RTextScrollPane(syntaxTextArea));

        jsonTreeEditor = new Utils.JsonTreeEditor(true, getModelItem());
        JScrollPane scrollPane = new JScrollPane(jsonTreeEditor);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        Bindings.bind(jsonTreeEditor, "text", pm.getModel("message"));
        jsonEditor.addTab("Tree View", scrollPane);

        jsonEditor.setPreferredSize(new Dimension(450, 350));
        form.append("Message", jsonEditor);

        xmlEditor = new JTabbedPane();

        syntaxTextArea = SyntaxEditorUtil.createDefaultXmlSyntaxTextArea();
        Bindings.bind(syntaxTextArea, pm.getModel("message"), true);
        PropertyExpansionPopupListener.enable(syntaxTextArea, getModelItem());
        xmlEditor.addTab("Text", new RTextScrollPane(syntaxTextArea));

        xmlTreeEditor = new Utils.XmlTreeEditor(true, getModelItem());
        scrollPane = new JScrollPane(xmlTreeEditor);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        Bindings.bind(xmlTreeEditor, "text", pm.getModel("message"));
        xmlEditor.addTab("Tree View", scrollPane);

        xmlEditor.setPreferredSize(new Dimension(450, 350));
        form.append("Message", xmlEditor);


        form.appendSeparator();
        form.appendHeading("Message Delivering Settings");
        buildQosRadioButtons(form, pm);
        form.appendCheckBox("retained", "Retained", "");
        buildTimeoutSpinEdit(form, pm, "Timeout");

        JPanel result = new JPanel(new BorderLayout(0, 0));
        result.add(new JScrollPane(form.getPanel()), BorderLayout.CENTER);
        result.add(buildToolbar(), BorderLayout.NORTH);

        propertyChange(new PropertyChangeEvent(getModelItem(), "messageKind", null, getModelItem().getMessageKind()));

        return result;
    }

    private JComponent buildToolbar(){
        JXToolBar toolBar = UISupport.createToolbar();
        RunTestStepAction startAction = new RunTestStepAction(getModelItem());
        toolBar.add(UISupport.createActionButton(startAction, true));
        toolBar.add(UISupport.createActionButton(startAction.getCorrespondingStopAction(), false));
        return toolBar;
    }

    protected JComponent buildLogPanel() {
        logArea = new JLogList("Test Step Log");

        logArea.getLogList().getModel().addListDataListener(new ListDataChangeListener() {

            public void dataChanged(ListModel model) {
                logInspector.setTitle(String.format(LOG_TAB_TITLE, model.getSize()));
            }
        });

        return logArea;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);

        if (evt.getPropertyName().equals("messageKind")) {
            PublishTestStep.MessageType newMessageType = (PublishTestStep.MessageType)evt.getNewValue();
            boolean isNumber = newMessageType == PublishTestStep.MessageType.DoubleValue || newMessageType == PublishTestStep.MessageType.FloatValue || newMessageType == PublishTestStep.MessageType.IntegerValue || newMessageType == PublishTestStep.MessageType.LongValue;
            boolean isFile = newMessageType == PublishTestStep.MessageType.BinaryFile;
            boolean isText = newMessageType == PublishTestStep.MessageType.Utf8Text || newMessageType == PublishTestStep.MessageType.Utf16Text;
            numberEdit.setVisible(isNumber);
            textMemo.setVisible(isText);
            if(textMemo.getParent() instanceof JScrollPane) {
                textMemo.getParent().setVisible(isText);
            }
            else if(textMemo.getParent().getParent() instanceof JScrollPane){
                textMemo.getParent().getParent().setVisible(isText);
            }
            fileNameEdit.setVisible(isFile);
            chooseFileButton.setVisible(isFile);
            jsonEditor.setVisible(newMessageType == PublishTestStep.MessageType.Json);
            xmlEditor.setVisible(newMessageType == PublishTestStep.MessageType.Xml);
        }
    }

    @Override
    protected boolean release() {
        getModelItem().removeExecutionListener(this);
        inspectorPanel.release();
        jsonTreeEditor.release();
        xmlTreeEditor.release();
        return super.release();
    }

    @Override
    public void afterExecution(ExecutableTestStep testStep, WsdlTestStepResult executionResult) {
        switch (executionResult.getStatus()){
            case CANCELED:
                logMessage(executionResult.getTimeStamp(), "CANCELED");
                break;
            case FAILED:
                if(executionResult.getError() == null){
                    logMessage(executionResult.getTimeStamp(), "Unable to publish the message (" + StringUtils.join(executionResult.getMessages(), " ") + ")");
                }
                else{
                    logMessage(executionResult.getTimeStamp(), "Error during message publishing: " + Utils.getExceptionMessage(executionResult.getError()));
                }
                break;
            default:
                logMessage(executionResult.getTimeStamp(), String.format("The message has been published within %d ms", executionResult.getTimeTaken()));

        }
    }

    private void logMessage(long time, String message){
        logArea.addLine(DateUtil.formatFull(new Date(time)) + " - " + message);
    }

    public class SelectFileAction extends AbstractAction {
        private JFileChooser fileChooser;

        public SelectFileAction() {
            super("Browse...");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
            }

            int returnVal = fileChooser.showOpenDialog(UISupport.getMainFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
//                String projectDir = new File(getModelItem().getProject().getPath()).getParent();
//                File selectedFile = new File(projectDir, fileChooser.getSelectedFile().getAbsolutePath());
//                fileNameEdit.setText(selectedFile.getPath());
                fileNameEdit.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

}
