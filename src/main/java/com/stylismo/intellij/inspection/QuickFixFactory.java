package com.stylismo.intellij.inspection;

import com.intellij.codeInsight.intention.AddAnnotationPsiFix;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiModifierListOwner;

import java.util.ArrayList;
import java.util.List;

class QuickFixFactory {

    static LocalQuickFix[] createQuickFixes(PsiModifierListOwner owner, boolean removeRedundantAnnotations) {
        List<LocalQuickFix> quickFixes = new ArrayList<>();

        NullabilityAnnotationsWithTypeQualifierDefault.findAnnotations(owner, false)
                .forEach(defaultAnnotation -> quickFixes.add(new AddPackageInfoWithNullabilityDefaultsFix(owner,
                        defaultAnnotation,
                        false,
                        removeRedundantAnnotations)));

        NullabilityAnnotationsWithTypeQualifierDefault.findAnnotations(owner, true)
                .forEach(defaultAnnotation -> quickFixes.add(new AddPackageInfoWithNullabilityDefaultsFix(owner,
                        defaultAnnotation,
                        true,
                        removeRedundantAnnotations)));

        quickFixes.add(AddAnnotationPsiFix.createAddNotNullFix(owner));

        quickFixes.add(AddAnnotationPsiFix.createAddNullableFix(owner));

        return quickFixes.toArray(new LocalQuickFix[0]);
    }
}
