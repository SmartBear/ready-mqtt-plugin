package com.smartbear.mqttsupport;

import com.eviware.soapui.impl.EmptyPanelBuilder;
import com.eviware.soapui.impl.wsdl.panels.teststeps.FileWaitDesktopPanel;
import com.eviware.soapui.impl.wsdl.teststeps.FileWaitTestStep;
import com.eviware.soapui.plugins.auto.PluginPanelBuilder;
import com.eviware.soapui.ui.desktop.DesktopPanel;

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
