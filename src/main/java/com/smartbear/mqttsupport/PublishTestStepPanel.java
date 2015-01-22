package com.smartbear.mqttsupport;

import com.eviware.soapui.config.DataGeneratorPropertyConfig;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;
import com.jgoodies.binding.PresentationModel;

import javax.swing.JScrollPane;
import java.awt.Dimension;

public class PublishTestStepPanel extends ModelItemDesktopPanel<PublishTestStep> {
    public PublishTestStepPanel(PublishTestStep modelItem) {
        super(modelItem);
        buildUI();
    }


    private void buildUI() {
        SimpleBindingForm form = new SimpleBindingForm(new PresentationModel<PublishTestStep>(getModelItem()));
        form.appendTextField("serverUri", "MQTT Server", "The MQTT server URI");
        form.appendComboBox("Connection", new ConnectionComboBoxModel(getModelItem(), getModelItem().getProject()), "Connection parameters");
        //form.appendPasswordField()
        add(new JScrollPane(form.getPanel()));
        setPreferredSize(new Dimension(500, 300));
    }


}
