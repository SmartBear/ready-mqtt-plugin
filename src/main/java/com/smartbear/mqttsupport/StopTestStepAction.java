package com.smartbear.mqttsupport;

import com.eviware.soapui.support.UISupport;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

public class StopTestStepAction extends AbstractAction {
    private RunTestStepAction runAction;

    public StopTestStepAction(RunTestStepAction correspondingRunAction) {
        super();
        this.runAction = correspondingRunAction;
        putValue(Action.SMALL_ICON, UISupport.createImageIcon("/stop.png"));
        putValue(Action.SHORT_DESCRIPTION, "Aborts ongoing test step execution");
        putValue(Action.ACCELERATOR_KEY, UISupport.getKeyStroke("alt X"));
    }

    public void actionPerformed(ActionEvent e) {
        runAction.cancel();
    }
}
