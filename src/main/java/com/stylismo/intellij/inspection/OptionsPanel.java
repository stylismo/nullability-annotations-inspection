package com.stylismo.intellij.inspection;


import com.intellij.codeInsight.NullableNotNullDialog;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

class OptionsPanel extends JPanel {
    private final JCheckBox shouldCheckFields = new JCheckBox("Check fields");
    private final JCheckBox shouldCheckInitializedStaticFinalFields =
            new JCheckBox("Check initialized static final fields");
    private final JCheckBox shouldCheckInitializedFinalFields = new JCheckBox("Check initialized final fields");
    private final JCheckBox shouldCheckPrivateMethods = new JCheckBox("Check private methods");
    private final JCheckBox shouldCheckTypeVariableTyped = new JCheckBox("Check type variable typed declarations");
    private final JCheckBox shouldRemoveRedundantAnnotations =
            new JCheckBox("Remove redundant annotations after applying 'default' to package");
    private final NullabilityAnnotationsInspection owner;

    OptionsPanel(NullabilityAnnotationsInspection owner) {
        super(new BorderLayout());

        this.owner = owner;

        init();
    }

    private void init() {
        ActionListener actionListener = it -> apply();

        shouldCheckFields.addActionListener(actionListener);
        shouldCheckInitializedStaticFinalFields.addActionListener(actionListener);
        shouldCheckInitializedFinalFields.addActionListener(actionListener);
        shouldCheckPrivateMethods.addActionListener(actionListener);
        shouldCheckTypeVariableTyped.addActionListener(actionListener);
        shouldRemoveRedundantAnnotations.addActionListener(actionListener);

        JButton openConfigureAnnotationsDialogButton = new JButton("Configure annotations");
        openConfigureAnnotationsDialogButton.addActionListener(it -> {
            DataContext dataContext = DataManager.getInstance().getDataContext(OptionsPanel.this);
            Project project = CommonDataKeys.PROJECT.getData(dataContext);
            if (project == null) {
                project = ProjectManager.getInstance().getDefaultProject();
            }
            NullableNotNullDialog dialog = new NullableNotNullDialog(project);
            dialog.show();
        });

        JPanel panel = new JPanel(new GridLayoutManager(9, 1));
        panel.add(new Spacer(),
                new GridConstraints(8, 0, 1, 1, 0, 2, 1, 6, null, null, null, 0));

        panel.add(shouldCheckFields,
                new GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null, 0));
        panel.add(shouldCheckInitializedStaticFinalFields,
                new GridConstraints(1, 0, 1, 1, 8, 0, 3, 0, null, null, null, 2));
        panel.add(shouldCheckInitializedFinalFields,
                new GridConstraints(2, 0, 1, 1, 8, 0, 3, 0, null, null, null, 2));
        panel.add(shouldCheckPrivateMethods,
                new GridConstraints(3, 0, 1, 1, 8, 0, 3, 0, null, null, null, 0));
        panel.add(shouldCheckTypeVariableTyped,
                new GridConstraints(4, 0, 1, 1, 8, 0, 3, 0, null, null, null, 0));

        panel.add(shouldRemoveRedundantAnnotations,
                new GridConstraints(6, 0, 1, 1, 8, 0, 3, 0, null, null, null, 0));

        panel.add(openConfigureAnnotationsDialogButton,
                new GridConstraints(8, 0, 1, 1, 8, 0, 0, 0, null, null, null, 0));

        add(panel, BorderLayout.CENTER);

        reset();
    }

    private void reset() {
        shouldCheckFields.setSelected(owner.isReportFields());
        shouldCheckInitializedStaticFinalFields.setSelected(owner.isReportInitializedStaticFinalFields());
        shouldCheckInitializedStaticFinalFields.setEnabled(owner.isReportFields());
        shouldCheckInitializedFinalFields.setSelected(owner.isReportInitializedFinalFields());
        shouldCheckInitializedFinalFields.setEnabled(owner.isReportFields());
        shouldCheckPrivateMethods.setSelected(owner.isReportPrivateMethods());
        shouldCheckTypeVariableTyped.setSelected(owner.isReportTypeVariableTyped());
        shouldRemoveRedundantAnnotations.setSelected(owner.isRemoveRedundantAnnotations());
    }

    private void apply() {
        owner.setReportFields(shouldCheckFields.isSelected());
        shouldCheckInitializedStaticFinalFields.setEnabled(owner.isReportFields());
        owner.setReportInitializedStaticFinalFields(shouldCheckInitializedStaticFinalFields.isSelected());
        shouldCheckInitializedFinalFields.setEnabled(owner.isReportFields());
        owner.setReportInitializedFinalFields(shouldCheckInitializedFinalFields.isSelected());
        owner.setReportPrivateMethods(shouldCheckPrivateMethods.isSelected());
        owner.setReportTypeVariableTyped(shouldCheckTypeVariableTyped.isSelected());
        owner.setRemoveRedundantAnnotations(shouldRemoveRedundantAnnotations.isSelected());
    }
}