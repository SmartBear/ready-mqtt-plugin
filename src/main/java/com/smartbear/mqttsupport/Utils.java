package com.smartbear.mqttsupport;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.support.editor.views.xml.outline.support.JsonObjectTree;
import com.eviware.soapui.support.editor.views.xml.outline.support.XmlObjectTree;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.SpinnerAdapterFactory;
import com.jgoodies.binding.adapter.SpinnerToValueModelConnector;
import com.jgoodies.binding.value.ValueModel;
import com.toedter.calendar.JSpinnerDateEditor;

import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import java.awt.event.FocusEvent;

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

    public static void showMemo(JTextArea memo, boolean visible){
        memo.setVisible(visible);
        if(memo.getParent() instanceof JScrollPane) {
            memo.getParent().setVisible(visible);
        }
        else if(memo.getParent().getParent() instanceof JScrollPane){
            memo.getParent().getParent().setVisible(visible);
        }

    }

    public static class JsonTreeEditor extends JsonObjectTree {
        private String prevValue = null;
        private boolean isCurValueNull = false;

        public JsonTreeEditor(boolean editable, ModelItem modelItem){
            super(editable, modelItem);
        }

        public void setText(String text){
            isCurValueNull = text == null;
            if(isCurValueNull) text = "";
            setContent(text);
            detectChange();
        }

        public String getText(){
            String result = getXml();
            return "".equals(result) && isCurValueNull ? null : result;
        }

        @Override
        protected void processFocusEvent(FocusEvent event){
            super.processFocusEvent(event);
            if(!event.isTemporary()) detectChange();
        }

        private void detectChange(){
            String newValue = getText();
            if(!Utils.areStringsEqual(prevValue, newValue)){
                firePropertyChange("text", prevValue, newValue);
                prevValue = newValue;
            }
        }

    }

    public static class XmlTreeEditor extends XmlObjectTree {
        private String prevValue = null;

        public XmlTreeEditor(boolean editable, ModelItem modelItem){
            super(editable, modelItem);
        }

        public void setText(String text){
            setContent(text);
            detectChange(text);
        }

        public String getText(){return getXml();}

        @Override
        protected void processFocusEvent(FocusEvent event){
            super.processFocusEvent(event);
            if(!event.isTemporary()) detectChange(getText());
        }

        private void detectChange(String newValue){
            if(!Utils.areStringsEqual(prevValue, newValue)){
                firePropertyChange("text", prevValue, newValue);
                prevValue = newValue;
            }
        }

    }

}
