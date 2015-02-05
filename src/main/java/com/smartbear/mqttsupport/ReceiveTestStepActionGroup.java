package com.smartbear.mqttsupport;

import com.eviware.soapui.impl.wsdl.actions.teststep.WsdlTestStepSoapUIActionGroup;

public class ReceiveTestStepActionGroup extends WsdlTestStepSoapUIActionGroup {
    public ReceiveTestStepActionGroup(){
        super("ReceiveTestStepActions", "Receive MQTT Message TestStep Actions");
    }
}
