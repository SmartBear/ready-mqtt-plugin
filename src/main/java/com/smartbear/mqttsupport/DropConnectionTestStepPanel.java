package com.smartbear.mqttsupport;

import com.eviware.soapui.support.components.SimpleBindingForm;
import com.jgoodies.binding.PresentationModel;

import javax.swing.JScrollPane;
import java.awt.Dimension;

public class DropConnectionTestStepPanel extends MqttConnectedTestStepPanel<DropConnectionTestStep>{

    public DropConnectionTestStepPanel(DropConnectionTestStep modelItem) {
        super(modelItem);
        buildUI();
    }

    protected void buildUI(){
        PresentationModel<DropConnectionTestStep> pm = new PresentationModel<DropConnectionTestStep>(getModelItem());
        SimpleBindingForm form = new SimpleBindingForm(pm);
        buildConnectionSection(form, pm);
        form.appendSeparator();
        form.appendHeading("Settings");
        buildRadioButtonsFromEnum(form,  pm, "Drop method", DropConnectionTestStep.DROP_METHOD_BEAN_PROP_NAME, DropConnectionTestStep.DropMethod.class);

        add(new JScrollPane(form.getPanel()));
        setPreferredSize(new Dimension(500, 300));
    }

}
