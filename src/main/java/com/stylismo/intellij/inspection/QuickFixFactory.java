package com.stylismo.intellij.inspection;

import com.intellij.codeInsight.intention.impl.AddNotNullAnnotationFix;
import com.intellij.codeInsight.intention.impl.AddNullableAnnotationFix;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiModifierListOwner;

import java.util.ArrayList;
import java.util.List;

class QuickFixFactory {

    static LocalQuickFix[] createQuickFixes(PsiModifierListOwner owner) {
        List<LocalQuickFix> quickFixes = new ArrayList<>();

        NullabilityAnnotationsWithTypeQualifierDefault.findAnnotations(owner, false)
                .forEach(s -> quickFixes.add(new AddPackageInfoWithNullabilityDefaultsFix(owner, s)));

        NullabilityAnnotationsWithTypeQualifierDefault.findAnnotations(owner, true)
                .forEach(s -> quickFixes.add(new AddPackageInfoWithNullabilityDefaultsFix(owner, s)));

        quickFixes.add(new AddNotNullAnnotationFix(owner) {
            @Override
            protected boolean isAvailable() {
                return true;
            }
        });

        quickFixes.add(new AddNullableAnnotationFix(owner) {
            @Override
            protected boolean isAvailable() {
                return true;
            }
        });

        return quickFixes.toArray(new LocalQuickFix[0]);
    }
}