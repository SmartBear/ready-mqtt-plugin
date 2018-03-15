package com.smartbear.mqttsupport.teststeps;

public interface ExecutionListener {
    void afterExecution(ExecutableTestStep testStep, ExecutableTestStepResult executionResult);
}
