package com.stylismo.intellij.inspection;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.CreatePackageInfoAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiPackageStatement;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class AddPackageInfoWithNullabilityDefaultsFix extends LocalQuickFixOnPsiElement {
    private final String annotationForTypeQualifierFqn;

    AddPackageInfoWithNullabilityDefaultsFix(PsiModifierListOwner element,
                                             String annotationForTypeQualifierFqn) {
        super(element);

        this.annotationForTypeQualifierFqn = annotationForTypeQualifierFqn;
    }

    @Override
    public boolean isAvailable(Project project,
                               PsiFile file,
                               PsiElement startElement,
                               PsiElement endElement) {
        return true;
    }

    @Override
    public String getText() {
        String shortName = annotationForTypeQualifierFqn.substring(annotationForTypeQualifierFqn.lastIndexOf('.') + 1);
        return "Annotate package as @" + shortName;
    }

    @Override
    public String getFamilyName() {
        return CodeInsightBundle.message("intention.add.annotation.family");
    }

    @Override
    public void invoke(@NotNull Project project,
                       @NotNull PsiFile file,
                       @NotNull PsiElement startElement,
                       @NotNull PsiElement endElement) {
        if (!(file instanceof PsiJavaFile)) {
            return;
        }

        PsiPackageStatement packageStatement = ((PsiJavaFile) file).getPackageStatement();
        if (packageStatement == null) {
            return;
        }

        PsiJavaCodeReferenceElement packageReference = packageStatement.getPackageReference();
        PsiElement target = packageReference.resolve();
        if (!(target instanceof PsiPackage)) {
            return;
        }

        PsiJavaFile packageInfoFile = packageInfoFile((PsiPackage) target, file.getContainingDirectory());
        if (packageInfoFile == null) {
            DataManager.getInstance().getDataContextFromFocus().doWhenDone((Consumer<DataContext>) context -> {
                AnActionEvent event =
                        new AnActionEvent(null, context, "", new Presentation(), ActionManager.getInstance(), 0);
                new CreatePackageInfoAction().actionPerformed(event);
            });
            packageInfoFile = packageInfoFile((PsiPackage) target, file.getContainingDirectory());
        }

        if (packageInfoFile != null) {
            addAnnotationToPackageInfo(project, packageInfoFile);
        }
    }

    private void addAnnotationToPackageInfo(@NotNull Project project, PsiJavaFile packageInfoFile) {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(packageInfoFile)) {
            return;
        }

        PsiPackageStatement packageStatement = packageInfoFile.getPackageStatement();
        if (packageStatement == null) {
            return;
        }

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiAnnotation annotation = factory.createAnnotationFromText("@" + annotationForTypeQualifierFqn,
                packageInfoFile.getContext());
        PsiElement addedAnnotation = packageInfoFile.addBefore(annotation, packageStatement);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(addedAnnotation);
    }

    @Nullable
    private static PsiJavaFile packageInfoFile(@Nullable PsiPackage aPackage, PsiDirectory containingDirectory) {
        if (aPackage == null) {
            return null;
        }

        PsiJavaFile packageInfoFile = (PsiJavaFile) containingDirectory.findFile(PsiPackage.PACKAGE_INFO_FILE);
        if (packageInfoFile != null) {
            return packageInfoFile;
        }

        return null;
    }
}