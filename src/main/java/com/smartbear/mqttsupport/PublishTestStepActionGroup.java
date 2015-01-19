package com.smartbear.mqttsupport;

import com.eviware.soapui.impl.wsdl.actions.teststep.WsdlTestStepSoapUIActionGroup;
import com.eviware.soapui.plugins.ActionGroup;

//@ActionGroup(defaultTargetType = PublishTestStep.class)
public class PublishTestStepActionGroup extends WsdlTestStepSoapUIActionGroup {
    public PublishTestStepActionGroup() {
        super("PublishTestStepActions", "Publish Using MQTT TestStep Actions");
    }
}
