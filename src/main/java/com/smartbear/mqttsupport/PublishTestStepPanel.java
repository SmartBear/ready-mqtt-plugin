package com.smartbear.mqttsupport;

import com.eviware.soapui.config.DataGeneratorPropertyConfig;
import com.eviware.soapui.support.components.PropertyComponent;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;
import com.jgoodies.binding.PresentationModel;

import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PublishTestStepPanel extends ModelItemDesktopPanel<PublishTestStep> {
    public PublishTestStepPanel(PublishTestStep modelItem) {
        super(modelItem);
        buildUI();
    }


    private void buildUI() {
        SimpleBindingForm form = new SimpleBindingForm(new PresentationModel<PublishTestStep>(getModelItem()));
        form.appendHeading("Connection to MQTT Server");
        form.appendTextField("serverUri", "MQTT Server URI", "The MQTT server URI");
        form.appendTextField("clientId", "Client ID (optional)", "Fill this field if you want to connect with fixed Client ID or leave it empty so a unique ID will be generated");
        form.appendTextField("login", "Login (optional)", "Login to MQTT server. Fill this if the server requires authentication.");
        form.appendTextField("password", "Password (optional)", "Password to MQTT server. Fill this if the server requires authentication.");
        form.addButtonWithoutLabelToTheRight("Use Connection of Another Test Step...", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        form.appendSeparator();
        form.appendHeading("Published Message");
        form.appendTextField("topic", "Topic", "Message Topic");
        form.appendComboBox("messageKind", "Message kind", PublishTestStep.MessageType.values(), "");

        add(new JScrollPane(form.getPanel()));
        setPreferredSize(new Dimension(500, 300));
    }


}
