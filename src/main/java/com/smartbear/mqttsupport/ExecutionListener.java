package com.smartbear.mqttsupport;

import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;

public interface ExecutionListener {
    void afterExecution(ExecutableTestStep testStep, WsdlTestStepResult executionResult);
}
