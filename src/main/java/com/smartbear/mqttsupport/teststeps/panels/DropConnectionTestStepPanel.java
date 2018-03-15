package com.smartbear.mqttsupport.teststeps.panels;

import com.jgoodies.binding.PresentationModel;
import com.smartbear.mqttsupport.teststeps.DropConnectionTestStep;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Dimension;

public class DropConnectionTestStepPanel extends MqttConnectedTestStepPanel<DropConnectionTestStep> {

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
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane);
        setPreferredSize(new Dimension(500, 300));
    }
}
