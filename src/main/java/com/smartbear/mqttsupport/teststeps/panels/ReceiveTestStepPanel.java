package com.smartbear.mqttsupport.teststeps.panels;

import com.eviware.soapui.impl.support.actions.ShowOnlineHelpAction;
import com.eviware.soapui.impl.wsdl.panels.teststeps.AssertionsPanel;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionsListener;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.settings.UISettings;
import com.eviware.soapui.support.DateUtil;
import com.eviware.soapui.support.JsonUtil;
import com.eviware.soapui.support.ListDataChangeListener;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.log.JLogList;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
import com.eviware.soapui.support.xml.SyntaxEditorUtil;
import com.eviware.soapui.support.xml.XmlUtils;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.smartbear.mqttsupport.Utils;
import com.smartbear.mqttsupport.teststeps.ExecutableTestStep;
import com.smartbear.mqttsupport.teststeps.ExecutableTestStepResult;
import com.smartbear.mqttsupport.teststeps.ExecutionListener;
import com.smartbear.mqttsupport.teststeps.PublishedMessageType;
import com.smartbear.mqttsupport.teststeps.ReceiveTestStep;
import com.smartbear.mqttsupport.teststeps.actions.RunTestStepAction;
import com.smartbear.ready.core.ApplicationEnvironment;
import com.smartbear.ready.ui.style.GlobalStyles;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.Date;

public class ReceiveTestStepPanel extends MqttConnectedTestStepPanel<ReceiveTestStep> implements AssertionsListener, ExecutionListener {

    private static final String HELP_LINK = "/soapui/steps/mqtt-receive.html";

    private JComponentInspector<JComponent> assertionInspector;
    private JInspectorPanel inspectorPanel;
    private AssertionsPanel assertionsPanel;
    private JTextArea recMessageMemo;
    private JTabbedPane jsonEditor;
    private JComponent jsonTreeEditor;
    private JTabbedPane xmlEditor;
    private JComponent xmlTreeEditor;

    private JComponentInspector<JComponent> logInspector;
    private JLogList logArea;

    private final static String LOG_TAB_TITLE = "Test Step Log (%d)";
    private CardLayout messageLayouts;
    private JPanel currentMessage;

    public ReceiveTestStepPanel(ReceiveTestStep modelItem) {
        super(modelItem);
        buildUI();
        modelItem.addAssertionsListener(this);
        modelItem.addExecutionListener(this);
    }

    private void buildUI() {

        JComponent mainPanel = buildMainPanel();
        inspectorPanel = JInspectorPanelFactory.build(mainPanel);

        assertionsPanel = buildAssertionsPanel();

        assertionInspector = new JComponentInspector<JComponent>(assertionsPanel, "Assertions ("
                + getModelItem().getAssertionCount() + ")", "Assertions for this Message", true);

        inspectorPanel.addInspector(assertionInspector);

        logInspector = new JComponentInspector<JComponent>(buildLogPanel(), String.format(LOG_TAB_TITLE, 0), "Log of the test step executions", true);
        inspectorPanel.addInspector(logInspector);

        inspectorPanel.setDefaultDividerLocation(0.6F);
        inspectorPanel.setCurrentInspector("Assertions");

        updateStatusIcon();

        add(inspectorPanel.getComponent());

        propertyChange(new PropertyChangeEvent(getModelItem(), "receivedMessage", null, getModelItem().getReceivedMessage()));

    }


    private JComponent buildMainPanel() {
        JPanel root = new JPanel(new MigLayout("wrap", "0[grow,fill]0", "0[]0[grow,fill]0"));

        PresentationModel<ReceiveTestStep> pm = new PresentationModel<ReceiveTestStep>(getModelItem());
        root.add(buildConnectionSection(pm));

        JPanel receivePanel = new JPanel(new MigLayout("", "8[]8[grow,fill]8", "0[grow,fill]0"));

        JPanel subscribePanel = new JPanel(new MigLayout("wrap", "0[grow,fill]0", "0[200]0[]0"));
        subscribePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, GlobalStyles.getDefaultBorderColor()));

        JPanel topicsPanel = new JPanel(new MigLayout("wrap", "0[grow,fill]8", "8[]8[200,fill]0"));
        subscribePanel.add(topicsPanel);
        FormBuilder formBuilder = new FormBuilder(pm, topicsPanel);
        JTextArea topicsMemo = formBuilder.appendTextArea("listenedTopics", "Subscribed topics", "The list of topic filters (one filter per line)");

        JPanel linePropsPanel = new JPanel(new MigLayout("wrap 2", "0[100]8[grow,fill]8", "8[]0"));
        subscribePanel.add(linePropsPanel);
        formBuilder = new FormBuilder(pm, linePropsPanel);
        PropertyExpansionPopupListener.enable(topicsMemo, getModelItem());
        buildRadioButtonsFromEnum(formBuilder, pm, "On unexpected topic", "onUnexpectedTopic", ReceiveTestStep.UnexpectedTopicBehavior.class);
        buildQosRadioButtons(formBuilder, pm);
        formBuilder.appendComboBox("expectedMessageType", "Expected message type", ReceiveTestStep.MessageType.values(), "Expected type of a received message");
        buildTimeoutSpinEdit(formBuilder, pm, "Timeout");
        receivePanel.add(subscribePanel);

        JPanel recvMessagePanel = new JPanel(new MigLayout("wrap", "0[grow,fill]0", "8[]0[grow,fill]8"));
        JPanel topicPanel = new JPanel(new MigLayout("wrap 2", "0[100]8[grow,fill]0", "0[]0"));
        formBuilder = new FormBuilder(pm, topicPanel);
        JTextField recTopicEdit = formBuilder.appendTextField("receivedMessageTopic", "Topic", "The topic of the received message");
        recTopicEdit.setEditable(false);
        recvMessagePanel.add(topicPanel);

        messageLayouts = new CardLayout();
        currentMessage = new JPanel(messageLayouts);
        recvMessagePanel.add(currentMessage);
        formBuilder = new FormBuilder(pm, currentMessage);
        recMessageMemo = formBuilder.addCard(PublishedMessageType.Utf8Text.name(), new MigLayout("wrap", "0[grow,fill]0", "8[]8[grow,fill]0")).appendTextArea("receivedMessage", "Message", "The payload of the received message");
        recMessageMemo.setEditable(false);
        recMessageMemo.getCaret().setVisible(true);
        recMessageMemo.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                recMessageMemo.getCaret().setVisible(true);

            }

            @Override
            public void focusLost(FocusEvent e) {

            }
        });

        jsonEditor = new JTabbedPane();

        RSyntaxTextArea syntaxTextArea = SyntaxEditorUtil.createDefaultJavaScriptSyntaxTextArea();
        syntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        Bindings.bind(syntaxTextArea, pm.getModel("receivedMessage"), true);
        syntaxTextArea.setEditable(false);
        jsonEditor.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

        jsonTreeEditor = Utils.createJsonTreeEditor(false, getModelItem());
        if (jsonTreeEditor == null) {
            jsonEditor.addTab("Tree View", new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER));
        } else {
            JScrollPane scrollPane = new JScrollPane(jsonTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Bindings.bind(jsonTreeEditor, "text", pm.getModel("receivedMessage"));
            jsonEditor.addTab("Tree View", scrollPane);
        }

        jsonEditor.setPreferredSize(new Dimension(450, 350));
        formBuilder.addCard(PublishedMessageType.Json.name(), new MigLayout("", "0[grow,fill]0", "8[grow,fill]8")).add(jsonEditor);

        xmlEditor = new JTabbedPane();

        syntaxTextArea = SyntaxEditorUtil.createDefaultXmlSyntaxTextArea();
        syntaxTextArea.setEditable(false);
        Bindings.bind(syntaxTextArea, pm.getModel("receivedMessage"), true);
        xmlEditor.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

        xmlTreeEditor = Utils.createXmlTreeEditor(false, getModelItem());
        if (xmlTreeEditor == null) {
            xmlEditor.addTab("Tree View", new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER));
        } else {
            JScrollPane scrollPane = new JScrollPane(xmlTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Bindings.bind(xmlTreeEditor, "text", pm.getModel("receivedMessage"));
            xmlEditor.addTab("Tree View", scrollPane);
        }

        xmlEditor.setPreferredSize(new Dimension(450, 350));
        formBuilder.addCard(PublishedMessageType.Xml.name(), new MigLayout("", "0[grow,fill]0", "8[grow,fill]8")).add(xmlEditor);

        receivePanel.add(recvMessagePanel);
        root.add(receivePanel);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.add(buildToolbar(), BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(root, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GlobalStyles.getDefaultBorderColor()));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;
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

    private AssertionsPanel buildAssertionsPanel() {
        return new AssertionsPanel(getModelItem());
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

    private void updateStatusIcon() {
        Assertable.AssertionStatus status = getModelItem().getAssertionStatus();
        switch (status) {
            case FAILED: {
                assertionInspector.setIcon(UISupport.createCurrentModeIcon("com/smartbear/mqttsupport/failed_assertion.png"));
                inspectorPanel.activate(assertionInspector);
                break;
            }
            case UNKNOWN: {
                assertionInspector.setIcon(UISupport.createImageIcon("com/smartbear/mqttsupport/unknown_assertion.png"));
                break;
            }
            case VALID: {
                assertionInspector.setIcon(UISupport.createCurrentModeIcon("com/smartbear/mqttsupport/valid_assertion.png"));
                inspectorPanel.deactivate();
                break;
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getPropertyName().equals("assertionStatus")) {
            updateStatusIcon();
        } else if (event.getPropertyName().equals("receivedMessage")) {
            String msg = (String) event.getNewValue();
            if (StringUtils.isNullOrEmpty(msg)) {
                messageLayouts.show(currentMessage, PublishedMessageType.Utf8Text.name());
            } else if (JsonUtil.seemsToBeJson(msg)) {
                messageLayouts.show(currentMessage, PublishedMessageType.Json.name());
            } else if (XmlUtils.seemsToBeXml(msg)) {
                messageLayouts.show(currentMessage, PublishedMessageType.Xml.name());
            } else {
                messageLayouts.show(currentMessage, PublishedMessageType.Utf8Text.name());
            }
        }

    }

    @Override
    public boolean onClose(boolean canCancel) {
        if (super.onClose(canCancel)) {
            ReceiveTestStep testStep = getModelItem();
            if (testStep != null) {
                testStep.removeExecutionListener(this);
                testStep.removeAssertionsListener(this);
            }
            if (assertionsPanel != null) {
                assertionsPanel.release();
            }
            if (inspectorPanel != null) {
                inspectorPanel.release();
            }
            if (jsonTreeEditor != null) {
                Utils.releaseTreeEditor(jsonTreeEditor);
            }
            if (xmlTreeEditor != null) {
                Utils.releaseTreeEditor(xmlTreeEditor);
            }
            return true;
        }

        return false;
    }

    private void assertionListChanged() {
        assertionInspector.setTitle(String.format("Assertions (%d)", getModelItem().getAssertionCount()));
    }

    @Override
    public void assertionAdded(TestAssertion assertion) {
        assertionListChanged();
    }

    @Override
    public void assertionRemoved(TestAssertion assertion) {
        assertionListChanged();
    }

    @Override
    public void assertionMoved(TestAssertion testAssertion, int i) {
        assertionListChanged();
    }

    @Override
    public void afterExecution(ExecutableTestStep testStep, ExecutableTestStepResult executionResult) {
        logArea.addLine(DateUtil.formatFull(new Date(executionResult.getTimeStamp())) + " - " + executionResult.getOutcome());
    }

    private void logMessage(long time, String message) {
        logArea.addLine(DateUtil.formatFull(new Date(time)) + " - " + message);
    }
}
