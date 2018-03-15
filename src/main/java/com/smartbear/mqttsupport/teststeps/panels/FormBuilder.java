package com.smartbear.mqttsupport.teststeps.panels;

import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JUndoableTextArea;
import com.eviware.soapui.support.swing.JTextComponentPopupMenu;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.list.SelectionInList;
import net.miginfocom.swing.MigLayout;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Dimension;

public class FormBuilder {
    private final PresentationModel<?> pm;
    private JPanel parent;

    public FormBuilder(PresentationModel<?> pm, JPanel parent) {
        this.pm = pm;
        this.parent = parent;
    }

    public FormBuilder addCard(String name, MigLayout layout) {
        if (layout == null) {
            layout = new MigLayout("", "0[100]8[grow,fill]0", "8[]0");
        }
        JPanel newCard = new JPanel(layout);
        parent.add(newCard, name);
        return new FormBuilder(pm, newCard);
    }

    public void add(JComponent component) {
        add(component, null);
    }

    public void add(JComponent component, Object constrains) {
        parent.add(component, constrains);
    }

    public JTextField appendTextField(String propertyName, String label, String tooltip) {
        JTextField textField = addTextField(parent, label, propertyName, tooltip);
        Bindings.bind(textField, pm.getModel(propertyName));
        return textField;
    }

    public static JTextField addTextField(JComponent parent, String label, String name, String tooltip) {
        JTextField textField = new JTextField();
        textField.setName(name);
        setToolTip(textField, tooltip);
        textField.getAccessibleContext().setAccessibleDescription(tooltip);
        JTextComponentPopupMenu.add(textField);
        append(parent, label, textField);
        return textField;
    }

    public JTextArea appendTextArea(String propertyName, String label, String tooltip) {
        JTextArea textArea = addTextArea(parent, label, tooltip);
        Bindings.bind(textArea, pm.getModel(propertyName));
        return textArea;
    }

    private static JTextArea addTextArea(JComponent parent, String label, String tooltip) {
        JTextArea textArea = new JUndoableTextArea();
        Dimension textAreaDimension = textArea.getPreferredSize();
        int bordersSize = 2;
        textAreaDimension.setSize(textAreaDimension.getWidth() + bordersSize,
                textAreaDimension.getHeight() + bordersSize);
        textArea.setAutoscrolls(true);
        setToolTip(textArea, tooltip);
        textArea.getAccessibleContext().setAccessibleDescription(tooltip);
        JTextComponentPopupMenu.add(textArea);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(textAreaDimension);
        append(parent, label, scrollPane);
        return textArea;
    }

    public JComboBox appendComboBox(String propertyName, String label, Object[] values, String tooltip) {
        JComboBox comboBox = addComboBox(parent, label, values, tooltip);
        Bindings.bind(comboBox, new SelectionInList<Object>(values, pm.getModel(propertyName)));

        return comboBox;
    }

    public JComboBox addComboBox(JComponent parent, String label, Object[] values, String tooltip) {
        JComboBox comboBox = new JComboBox(values);
        setToolTip(comboBox, tooltip);
        comboBox.getAccessibleContext().setAccessibleDescription(tooltip);
        append(parent, label, comboBox);
        return comboBox;
    }

    public JCheckBox appendCheckBox(String propertyName, String label, String tooltip) {
        JCheckBox checkBox = addCheckBox(parent, label, tooltip, false);
        Bindings.bind(checkBox, pm.getModel(propertyName));
        return checkBox;
    }

    private JCheckBox addCheckBox(JComponent parent, String caption, String label, boolean selected) {
        JCheckBox checkBox = new JCheckBox(label, selected);
        checkBox.getAccessibleContext().setAccessibleDescription(caption);
        append(parent, caption, checkBox);
        return checkBox;
    }

    public void append(String label, JComponent component) {
        append(parent, label, component);
    }

    private static void append(JComponent parent, String label, JComponent component) {
        if (!StringUtils.isNullOrEmpty(label) && !label.endsWith(":")) {
            label += ":";
        }
        parent.add(new JLabel(label));
        parent.add(component);
    }

    private static void setToolTip(JComponent component, String tooltip) {
        component.setToolTipText((StringUtils.isNullOrEmpty(tooltip) ? null : tooltip));
    }

    private static JLabel createLabel(String labelText) {
        return new JLabel(labelText);
    }

    public JButton addRightButton(Action action) {
        JButton button = new JButton(action);
        parent.add(new JLabel());
        parent.add(button);
        return button;
    }
}
