package com.stylismo.intellij.inspection;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.CreatePackageInfoAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiPackageStatement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.intellij.codeInsight.AnnotationTargetUtil.extractRequiredAnnotationTargets;
import static com.intellij.codeInsight.AnnotationTargetUtil.getTargetsForLocation;
import static com.intellij.codeInsight.intention.AddAnnotationPsiFix.removePhysicalAnnotations;
import static com.stylismo.intellij.inspection.NullabilityAnnotationsWithTypeQualifierDefault.TYPE_QUALIFIER_DEFAULT;

public class AddPackageInfoWithNullabilityDefaultsFix extends LocalQuickFixOnPsiElement {
    private final String annotationForTypeQualifierFqn;
    private final boolean nullable;
    private final boolean removeRedundantAnnotations;

    AddPackageInfoWithNullabilityDefaultsFix(PsiModifierListOwner element,
                                             String annotationForTypeQualifierFqn,
                                             boolean nullable,
                                             boolean removeRedundantAnnotations) {
        super(element);

        this.annotationForTypeQualifierFqn = annotationForTypeQualifierFqn;
        this.nullable = nullable;
        this.removeRedundantAnnotations = removeRedundantAnnotations;
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
    public void invoke(Project project,
                       PsiFile file,
                       PsiElement startElement,
                       PsiElement endElement) {
        PsiJavaFile packageInfoFile = getOrCreatePackageInfoFile(file);
        if (packageInfoFile != null) {
            addAnnotationToPackageInfo(project, packageInfoFile);
        }
    }

    @Nullable
    private PsiJavaFile getOrCreatePackageInfoFile(PsiFile file) {
        if (!(file instanceof PsiJavaFile)) {
            return null;
        }

        PsiPackageStatement packageStatement = ((PsiJavaFile) file).getPackageStatement();
        if (packageStatement == null) {
            return null;
        }

        PsiJavaCodeReferenceElement packageReference = packageStatement.getPackageReference();
        PsiElement target = packageReference.resolve();
        if (!(target instanceof PsiPackage)) {
            return null;
        }

        PsiJavaFile packageInfoFile = packageInfoFile((PsiPackage) target, file.getContainingDirectory());
        if (packageInfoFile == null) {
            packageInfoFile = createPackageInfoFile(file, (PsiPackage) target);
        }

        return packageInfoFile;
    }

    @Nullable
    private PsiJavaFile createPackageInfoFile(PsiFile file, PsiPackage target) {
        DataManager.getInstance().getDataContextFromFocus().doWhenDone((Consumer<DataContext>) context -> {
            AnActionEvent event =
                    new AnActionEvent(null, context, "", new Presentation(), ActionManager.getInstance(), 0);
            new CreatePackageInfoAction().actionPerformed(event);
        });
        return packageInfoFile(target, file.getContainingDirectory());
    }

    private void addAnnotationToPackageInfo(Project project, PsiJavaFile packageInfoFile) {
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

        removeRedundantAnnotationsInPackage(project, packageInfoFile.getContainingDirectory().getFiles(), annotation);
    }

    private void removeRedundantAnnotationsInPackage(Project project, PsiFile[] files, PsiAnnotation annotation) {
        if (!removeRedundantAnnotations)
        {
            return;
        }

        List<String> redundantAnnotations = nullable
                ? NullableNotNullManager.getInstance(project).getNullables()
                : NullableNotNullManager.getInstance(project).getNotNulls();

        Set<PsiAnnotation.TargetType> targetsForDefaultAnnotation = targetTypesForDefault(annotation);
        if (targetsForDefaultAnnotation == null) {
            return;
        }

        for (PsiFile file : files) {
            if (file instanceof PsiJavaFile) {
                JavaElementVisitor visitor = new JavaElementVisitor() {
                    @Override
                    public void visitMethod(@Nonnull PsiMethod method) {
                        removeRedundantAnnotations(method, redundantAnnotations, targetsForDefaultAnnotation);
                    }

                    @Override
                    public void visitParameter(@Nonnull PsiParameter parameter) {
                        removeRedundantAnnotations(parameter, redundantAnnotations, targetsForDefaultAnnotation);
                    }

                    @Override
                    public void visitField(@Nonnull PsiField field) {
                        removeRedundantAnnotations(field, redundantAnnotations, targetsForDefaultAnnotation);
                    }

                    @Override
                    public void visitLocalVariable(@Nonnull PsiLocalVariable variable) {
                        super.visitLocalVariable(variable);
                    }
                };

                PsiRecursiveElementWalkingVisitor recursiveVisitor = new PsiRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitElement(@Nonnull PsiElement element) {
                        element.accept(visitor);

                        super.visitElement(element);
                    }
                };

                file.accept(recursiveVisitor);

                JavaCodeStyleManager.getInstance(project).optimizeImports(file);
            }
        }
    }

    private void removeRedundantAnnotations(PsiModifierListOwner element,
                                            List<String> redundantAnnotations,
                                            Set<PsiAnnotation.TargetType> targetsForDefaultAnnotation) {
        PsiAnnotation.TargetType[] targetTypes = getTargetsForLocation(element.getModifierList());
        boolean isTargeted = targetsForDefaultAnnotation.isEmpty()
                || ContainerUtil.intersects(targetsForDefaultAnnotation, Arrays.asList(targetTypes));
        if (isTargeted) {
            removePhysicalAnnotations(element, ArrayUtil.toStringArray(redundantAnnotations));
        }
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

    @Nullable
    private static Set<PsiAnnotation.TargetType> targetTypesForDefault(PsiAnnotation annotation) {
        PsiJavaCodeReferenceElement element = annotation.getNameReferenceElement();
        PsiElement declaration = element == null ? null : element.resolve();

        if (!(declaration instanceof PsiClass)) {
            return Collections.emptySet();
        }

        PsiClass classDeclaration = (PsiClass) declaration;
        PsiAnnotation tqDefault = AnnotationUtil.findAnnotation(classDeclaration, true, TYPE_QUALIFIER_DEFAULT);

        if (tqDefault == null) {
            return Collections.emptySet();
        }

        return extractRequiredAnnotationTargets(tqDefault.findAttributeValue(null));
    }
}