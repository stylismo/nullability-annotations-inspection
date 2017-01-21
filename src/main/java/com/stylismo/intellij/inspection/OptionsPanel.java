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
    private final JCheckBox shouldCheckInitializedFinalFields = new JCheckBox("Check initialized final fields");
    private final JCheckBox shouldCheckPrivateMethods = new JCheckBox("Check private methods");
    private final NullabilityAnnotationsInspection owner;

    OptionsPanel(NullabilityAnnotationsInspection owner) {
        super(new BorderLayout());

        this.owner = owner;

        init();
    }

    private void init() {
        ActionListener actionListener = it -> apply();

        shouldCheckFields.addActionListener(actionListener);
        shouldCheckInitializedFinalFields.addActionListener(actionListener);
        shouldCheckPrivateMethods.addActionListener(actionListener);

        JButton myOpenConfigureAnnotationsDialogButton = new JButton("Configure annotations");
        myOpenConfigureAnnotationsDialogButton.addActionListener(it -> {
            DataContext myDataContext = DataManager.getInstance().getDataContext(OptionsPanel.this);
            Project project = CommonDataKeys.PROJECT.getData(myDataContext);
            if (project == null) {
                project = ProjectManager.getInstance().getDefaultProject();
            }
            NullableNotNullDialog dialog = new NullableNotNullDialog(project);
            dialog.show();
        });

        JPanel myPanel = new JPanel(new GridLayoutManager(9, 1));
        myPanel.add(new Spacer(),
                new GridConstraints(8, 0, 1, 1, 0, 2, 1, 6, null, null, null, 0));

        myPanel.add(shouldCheckFields,
                new GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null, 0));
        myPanel.add(shouldCheckInitializedFinalFields,
                new GridConstraints(1, 0, 1, 1, 8, 0, 3, 0, null, null, null, 2));
        myPanel.add(shouldCheckPrivateMethods,
                new GridConstraints(2, 0, 1, 1, 8, 0, 3, 0, null, null, null, 0));

        myPanel.add(myOpenConfigureAnnotationsDialogButton,
                new GridConstraints(6, 0, 1, 1, 8, 0, 0, 0, null, null, null, 0));

        add(myPanel, BorderLayout.CENTER);

        reset();
    }

    private void reset() {
        shouldCheckFields.setSelected(owner.isReportFields());
        shouldCheckInitializedFinalFields.setSelected(owner.isReportInitializedFinalFields());
        shouldCheckInitializedFinalFields.setEnabled(owner.isReportFields());
        shouldCheckPrivateMethods.setSelected(owner.isReportPrivateMethods());
    }

    private void apply() {
        owner.setReportFields(shouldCheckFields.isSelected());
        shouldCheckInitializedFinalFields.setEnabled(owner.isReportFields());
        owner.setReportInitializedFinalFields(shouldCheckInitializedFinalFields.isSelected());
        owner.setReportPrivateMethods(shouldCheckPrivateMethods.isSelected());
    }
}
