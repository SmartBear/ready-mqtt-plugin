package com.smartbear.mqttsupport;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.SpinnerAdapterFactory;
import com.jgoodies.binding.adapter.SpinnerToValueModelConnector;
import com.jgoodies.binding.value.ValueModel;
import com.toedter.calendar.JSpinnerDateEditor;

import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

class Utils {

    public static boolean areStringsEqual(String s1, String s2, boolean caseSensitive, boolean dontDistinctNullAndEmpty){
        if(dontDistinctNullAndEmpty) {
            if (s1 == null || s1.length() == 0) return s2 == null || s2.length() == 0;
        }
        return areStringsEqual(s1, s2, caseSensitive);
    }

    public static boolean areStringsEqual(String s1, String s2, boolean caseSensitive){
        if(s1 == null) return s2 == null;
        if(caseSensitive) return s1.equalsIgnoreCase(s2); else return s1.equals(s2);
    }
    public static boolean areStringsEqual(String s1, String s2){
        return areStringsEqual(s1, s2, false);
    }

    public static <B> JSpinner createBoundSpinEdit(PresentationModel<B> pm, String propertyName, int minPropValue, int maxPropValue, int step){
        ValueModel valueModel = pm.getModel(propertyName);
        Number defValue = (Number)valueModel.getValue();
        SpinnerModel spinnerModel = new SpinnerNumberModel(defValue, minPropValue, maxPropValue, step);
        SpinnerAdapterFactory.connect(spinnerModel, valueModel, defValue);
        return new JSpinner(spinnerModel);

    }
}
