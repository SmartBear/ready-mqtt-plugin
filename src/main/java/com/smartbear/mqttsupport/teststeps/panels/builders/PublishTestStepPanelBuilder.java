package com.smartbear.mqttsupport.teststeps.panels.builders;

import com.eviware.soapui.impl.EmptyPanelBuilder;
import com.eviware.soapui.plugins.auto.PluginPanelBuilder;
import com.eviware.soapui.ui.desktop.DesktopPanel;
import com.smartbear.mqttsupport.teststeps.PublishTestStep;
import com.smartbear.mqttsupport.teststeps.panels.PublishTestStepPanel;

@PluginPanelBuilder(targetModelItem = PublishTestStep.class)
public class PublishTestStepPanelBuilder extends EmptyPanelBuilder<PublishTestStep> {

        @Override
    public DesktopPanel buildDesktopPanel(PublishTestStep testStep) {
        return new PublishTestStepPanel(testStep);
    }

    @Override
    public boolean hasDesktopPanel() {
        return true;
    }

}
