package com.smartbear.mqttsupport.teststeps.panels.builders;

import com.eviware.soapui.impl.EmptyPanelBuilder;
import com.eviware.soapui.plugins.auto.PluginPanelBuilder;
import com.eviware.soapui.ui.desktop.DesktopPanel;
import com.smartbear.mqttsupport.teststeps.DropConnectionTestStep;
import com.smartbear.mqttsupport.teststeps.panels.DropConnectionTestStepPanel;

@PluginPanelBuilder(targetModelItem = DropConnectionTestStep.class)
public class DropConnectionTestStepPanelBuilder extends EmptyPanelBuilder<DropConnectionTestStep> {
    @Override
    public DesktopPanel buildDesktopPanel(DropConnectionTestStep testStep) {
        return new DropConnectionTestStepPanel(testStep);
    }

    @Override
    public boolean hasDesktopPanel() {
        return true;
    }

}
