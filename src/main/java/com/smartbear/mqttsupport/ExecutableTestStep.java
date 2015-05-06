package com.smartbear.mqttsupport;

import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansion;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.testsuite.TestRunContext;

public interface ExecutableTestStep extends ModelItem {
    public ExecutableTestStepResult execute(PropertyExpansionContext context, CancellationToken cancellationToken);
    public void addExecutionListener(ExecutionListener listener);
    public void removeExecutionListener(ExecutionListener listener);
}
