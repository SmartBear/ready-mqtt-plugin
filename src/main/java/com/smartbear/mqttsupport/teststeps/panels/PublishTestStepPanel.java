package com.smartbear.mqttsupport.teststeps.panels;


import com.eviware.soapui.impl.support.actions.ShowOnlineHelpAction;
import com.eviware.soapui.support.DateUtil;
import com.eviware.soapui.support.ListDataChangeListener;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.support.log.JLogList;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
import com.eviware.soapui.support.xml.SyntaxEditorUtil;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.smartbear.mqttsupport.Utils;
import com.smartbear.mqttsupport.teststeps.ExecutableTestStep;
import com.smartbear.mqttsupport.teststeps.ExecutableTestStepResult;
import com.smartbear.mqttsupport.teststeps.ExecutionListener;
import com.smartbear.mqttsupport.teststeps.PublishTestStep;
import com.smartbear.mqttsupport.teststeps.PublishedMessageType;
import com.smartbear.mqttsupport.teststeps.actions.RunTestStepAction;
import com.smartbear.ready.ui.style.GlobalStyles;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.Date;

public class PublishTestStepPanel extends MqttConnectedTestStepPanel<PublishTestStep> implements ExecutionListener {

    private static final String HELP_LINK = "/soapui/steps/mqtt-publish.html";

    private JTextField numberEdit;
    private JTextArea textMemo;
    private JTextField fileNameEdit;
    private JButton chooseFileButton;
    private JTabbedPane jsonEditor;
    private JComponent jsonTreeEditor;
    private JTabbedPane xmlEditor;
    private JComponent xmlTreeEditor;

    private JInspectorPanel inspectorPanel;
    private JComponentInspector<JComponent> logInspector;
    private JLogList logArea;
    private final static String LOG_TAB_TITLE = "Test Step Log (%d)";

    private CardLayout messageLayouts;
    private JPanel currentMessage;


    public PublishTestStepPanel(PublishTestStep modelItem) {
        super(modelItem);
        buildUI();
        modelItem.addExecutionListener(this);
    }


    private void buildUI() {
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
        JPanel root = new JPanel(new MigLayout("wrap", "0[grow,fill]0", "0[]0[grow,fill]0"));
        root.setMaximumSize(new Dimension(500, 500));

        PresentationModel<PublishTestStep> pm = new PresentationModel<PublishTestStep>(getModelItem());
        root.add(buildConnectionSection(pm));

        JPanel publishPanel = new JPanel(new MigLayout("", "0[grow,fill]8[]8", "0[grow,fill]0"));

        JPanel mesagePanelRoot = new JPanel(new MigLayout("wrap", "8[grow,fill]8", "0[]0[grow,fill]0"));
        mesagePanelRoot.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, GlobalStyles.getDefaultBorderColor()));

        JPanel mesagePanel = new JPanel(new MigLayout("wrap 2", "0[100]8[grow,fill]0", "8[]0"));
        mesagePanelRoot.add(mesagePanel);
        FormBuilder formBuilder = new FormBuilder(pm, mesagePanel);
        JTextField topicEdit = formBuilder.appendTextField("topic", "Topic", "Message Topic");
        PropertyExpansionPopupListener.enable(topicEdit, getModelItem());
        formBuilder.appendComboBox("messageKind", "Message type", PublishedMessageType.values(), "");

        messageLayouts = new CardLayout();
        currentMessage = new JPanel(messageLayouts);
        mesagePanelRoot.add(currentMessage);
        formBuilder = new FormBuilder(pm, currentMessage);
        numberEdit = formBuilder.addCard(PublishedMessageType.IntegerValue.name(), null).appendTextField("message", "Message", "The number which will be published.");
        textMemo = formBuilder.addCard(PublishedMessageType.Utf8Text.name(), new MigLayout("wrap", "0[grow,fill]0", "8[]8[grow,fill]0")).appendTextArea("message", "Message", "The text which will be published.");
        PropertyExpansionPopupListener.enable(textMemo, getModelItem());
        FormBuilder cardBuilder = formBuilder.addCard(PublishedMessageType.BinaryFile.name(), new MigLayout("", "0[100]8[grow,fill]8[]0", "8[]0"));
        fileNameEdit = cardBuilder.appendTextField("message", "File name", "The file which content will be used as payload");
        PropertyExpansionPopupListener.enable(fileNameEdit, getModelItem());
        chooseFileButton = cardBuilder.addRightButton(new SelectFileAction());

        JScrollPane scrollPane;

        jsonEditor = new JTabbedPane();
        RSyntaxTextArea syntaxTextArea = SyntaxEditorUtil.createDefaultJavaScriptSyntaxTextArea();
        syntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        Bindings.bind(syntaxTextArea, pm.getModel("message"), true);
        PropertyExpansionPopupListener.enable(syntaxTextArea, getModelItem());
        jsonEditor.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

        jsonTreeEditor = Utils.createJsonTreeEditor(true, getModelItem());
        if (jsonTreeEditor != null) {
            scrollPane = new JScrollPane(jsonTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Bindings.bind(jsonTreeEditor, "text", pm.getModel("message"));
            jsonEditor.addTab("Tree View", scrollPane);
        } else {
            jsonEditor.addTab("Tree View", new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER));
        }

        jsonEditor.setPreferredSize(new Dimension(450, 350));
        formBuilder.addCard(PublishedMessageType.Json.name(), new MigLayout("", "0[grow,fill]0", "8[grow,fill]8")).add(jsonEditor);

        xmlEditor = new JTabbedPane();

        syntaxTextArea = SyntaxEditorUtil.createDefaultXmlSyntaxTextArea();
        Bindings.bind(syntaxTextArea, pm.getModel("message"), true);
        PropertyExpansionPopupListener.enable(syntaxTextArea, getModelItem());
        xmlEditor.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

        xmlTreeEditor = Utils.createXmlTreeEditor(true, getModelItem());
        if (xmlTreeEditor != null) {
            scrollPane = new JScrollPane(xmlTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Bindings.bind(xmlTreeEditor, "text", pm.getModel("message"));
            xmlEditor.addTab("Tree View", scrollPane);
        } else {
            xmlEditor.addTab("Tree View", new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER));
        }

        xmlEditor.setPreferredSize(new Dimension(450, 350));
        formBuilder.addCard(PublishedMessageType.Xml.name(), new MigLayout("", "0[grow,fill]0", "8[grow,fill]8")).add(xmlEditor);
        publishPanel.add(mesagePanelRoot);

        JPanel propsPanel = new JPanel(new MigLayout("wrap 2", "0[100]8[grow,fill]8", "8[]8"));
        formBuilder = new FormBuilder(pm, propsPanel);
        buildQosRadioButtons(formBuilder, pm);
        formBuilder.appendCheckBox("retained", "Retained", "");
        buildTimeoutSpinEdit(formBuilder, pm, "Timeout");
        publishPanel.add(propsPanel);

        root.add(publishPanel);

        JPanel result = new JPanel(new BorderLayout(0, 0));
        scrollPane = new JScrollPane(root, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GlobalStyles.getDefaultBorderColor()));
        result.add(scrollPane, BorderLayout.CENTER);
        result.add(buildToolbar(), BorderLayout.NORTH);

        propertyChange(new PropertyChangeEvent(getModelItem(), "messageKind", null, getModelItem().getMessageKind()));

        return result;
    }

    private JComponent buildToolbar() {
        JXToolBar toolBar = UISupport.createToolbar();
        RunTestStepAction startAction = new RunTestStepAction(getModelItem());
        JButton submitButton = UISupport.createActionButton(startAction, startAction.isEnabled());
        toolBar.add(submitButton);
        submitButton.setMnemonic(KeyEvent.VK_ENTER);
        toolBar.add(UISupport.createActionButton(startAction.getCorrespondingStopAction(), startAction.getCorrespondingStopAction().isEnabled()));
        addConnectionActionsToToolbar(toolBar);

        ShowOnlineHelpAction showOnlineHelpAction = new ShowOnlineHelpAction(HELP_LINK);
        JButton help = UISupport.createActionButton(showOnlineHelpAction, showOnlineHelpAction.isEnabled());
        toolBar.addGlue();
        toolBar.add(help);

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
            PublishedMessageType messageType = (PublishedMessageType) evt.getNewValue();
            updateSelectedMessageType(messageType);
        }
    }

    private void updateSelectedMessageType(PublishedMessageType messageType) {
        switch (messageType) {
            case DoubleValue:
            case FloatValue:
            case IntegerValue:
            case LongValue:
                messageLayouts.show(currentMessage, PublishedMessageType.IntegerValue.name());
                break;
            case Utf8Text:
            case Utf16Text:
                messageLayouts.show(currentMessage, PublishedMessageType.Utf8Text.name());
                break;
            default:
                messageLayouts.show(currentMessage, messageType.name());
                break;
        }
    }

    @Override
    protected boolean release() {
        getModelItem().removeExecutionListener(this);
        inspectorPanel.release();
        if (jsonTreeEditor != null) {
            Utils.releaseTreeEditor(jsonTreeEditor);
        }
        if (xmlTreeEditor != null) {
            Utils.releaseTreeEditor(xmlTreeEditor);
        }
        return super.release();
    }

    @Override
    public void afterExecution(ExecutableTestStep testStep, ExecutableTestStepResult executionResult) {
        logArea.addLine(DateUtil.formatFull(new Date(executionResult.getTimeStamp())) + " - " + executionResult.getOutcome());
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
