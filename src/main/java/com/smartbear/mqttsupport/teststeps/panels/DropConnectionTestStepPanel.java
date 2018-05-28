package com.smartbear.mqttsupport.teststeps.panels;

import com.eviware.soapui.impl.support.actions.ShowOnlineHelpAction;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JXToolBar;
import com.jgoodies.binding.PresentationModel;
import com.smartbear.mqttsupport.teststeps.DropConnectionTestStep;
import com.smartbear.ready.ui.style.GlobalStyles;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class DropConnectionTestStepPanel extends MqttConnectedTestStepPanel<DropConnectionTestStep> {

    private static final String HELP_LINK = "/soapui/steps/mqtt-drop.html";

    public DropConnectionTestStepPanel(DropConnectionTestStep modelItem) {
        super(modelItem);
        buildUI();
    }

    protected void buildUI() {
        JPanel root = new JPanel(new MigLayout("wrap", "0[grow,fill]0", "0[]0[grow,fill]0"));

        PresentationModel<DropConnectionTestStep> pm = new PresentationModel<DropConnectionTestStep>(getModelItem());
        root.add(buildConnectionSection(pm));

        JPanel propsPanel = new JPanel(new MigLayout("wrap 2", "8[100]8[grow,fill]0", "0[]0"));
        FormBuilder formBuilder = new FormBuilder(pm, propsPanel);

        buildRadioButtonsFromEnum(formBuilder, pm, "Drop method", DropConnectionTestStep.DROP_METHOD_BEAN_PROP_NAME,
                DropConnectionTestStep.DropMethod.class);

        root.add(propsPanel);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GlobalStyles.getDefaultBorderColor()));
        add(buildToolbar(), BorderLayout.NORTH);
        add(scrollPane);
        setPreferredSize(new Dimension(500, 300));
    }

    private JComponent buildToolbar() {
        JXToolBar toolBar = UISupport.createToolbar();
        ShowOnlineHelpAction showOnlineHelpAction = new ShowOnlineHelpAction(HELP_LINK);
        JButton help = UISupport.createActionButton(showOnlineHelpAction, showOnlineHelpAction.isEnabled());
        toolBar.addGlue();
        toolBar.add(help);
        return toolBar;
    }
}
