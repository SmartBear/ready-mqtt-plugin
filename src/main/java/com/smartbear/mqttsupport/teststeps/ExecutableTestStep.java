package com.smartbear.mqttsupport.teststeps;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.smartbear.mqttsupport.CancellationToken;

public interface ExecutableTestStep extends ModelItem {
    public ExecutableTestStepResult execute(PropertyExpansionContext context, CancellationToken cancellationToken);
    public void addExecutionListener(ExecutionListener listener);
    public void removeExecutionListener(ExecutionListener listener);
}
