package com.smartbear.mqttsupport;

import com.eviware.soapui.impl.support.actions.ShowOnlineHelpAction;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.jgoodies.binding.PresentationModel;

import javax.swing.JButton;
import javax.swing.JComponent;
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
        PresentationModel<DropConnectionTestStep> pm = new PresentationModel<DropConnectionTestStep>(getModelItem());
        SimpleBindingForm form = new SimpleBindingForm(pm);
        buildConnectionSection(form, pm);
        form.appendSeparator();
        form.appendHeading("Settings");
        buildRadioButtonsFromEnum(form, pm, "Drop method", DropConnectionTestStep.DROP_METHOD_BEAN_PROP_NAME, DropConnectionTestStep.DropMethod.class);
        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(form.getPanel()), BorderLayout.CENTER);
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
