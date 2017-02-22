package com.stylismo.intellij.inspection;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.intellij.codeInsight.AnnotationTargetUtil.extractRequiredAnnotationTargets;
import static com.intellij.codeInsight.AnnotationTargetUtil.getTargetsForLocation;

class NullabilityAnnotationsWithTypeQualifierDefault {
    private static final String TYPE_QUALIFIER_DEFAULT = "javax.annotation.meta.TypeQualifierDefault";
    private static final String JAVAX_ANNOTATION_NULLABLE = "javax.annotation.Nullable";
    private static final String JAVAX_ANNOTATION_NONNULL = "javax.annotation.Nonnull";

    static List<String> findAnnotations(@NotNull PsiModifierListOwner element, boolean nullable) {
        if (overridesSuper(element)) {
            return Collections.emptyList();
        }

        List<String> annotations = new ArrayList<>();
        Project project = element.getProject();
        PsiModifierList modifierList = element.getModifierList();
        List<String> nullabilityAnnotations = nullable
                ? NullableNotNullManager.getInstance(project).getNullables()
                : NullableNotNullManager.getInstance(project).getNotNulls();
        for (String notNullAnnotationFqn : nullabilityAnnotations) {
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
            PsiAnnotation annotation = factory.createAnnotationFromText("@" + notNullAnnotationFqn, null);
            PsiAnnotation.TargetType[] targetTypes = getTargetsForLocation(modifierList);
            if (isNullabilityAnnotationForTypeQualifierDefault(annotation, nullable, targetTypes)) {
                annotations.add(annotation.getQualifiedName());
            }
        }
        return annotations;
    }

    private static boolean overridesSuper(PsiModifierListOwner element) {
        if (element instanceof PsiParameter) {
            PsiMethod method = (PsiMethod) element.getParent().getParent();
            return !superMethods(method).isEmpty();
        } else if (element instanceof PsiMethod) {
            return !superMethods((PsiMethod) element).isEmpty();
        }
        return false;
    }

    private static List<MethodSignatureBackedByPsiMethod> superMethods(PsiMethod method) {
        List<MethodSignatureBackedByPsiMethod> signatures = method.findSuperMethodSignaturesIncludingStatic(true);
        signatures.removeIf(superSignature ->
                superSignature.getMethod().getParameterList().getParametersCount()
                        != method.getParameterList().getParametersCount());
        return signatures;
    }

    private static boolean isNullabilityAnnotationForTypeQualifierDefault(@NotNull PsiAnnotation annotation,
                                                                          boolean nullable,
                                                                          PsiAnnotation.TargetType[] targetTypes) {
        PsiJavaCodeReferenceElement element = annotation.getNameReferenceElement();
        PsiElement declaration = element == null ? null : element.resolve();
        if (!(declaration instanceof PsiClass)) {
            return false;
        }

        String fqn = nullable ? JAVAX_ANNOTATION_NULLABLE : JAVAX_ANNOTATION_NONNULL;
        PsiClass classDeclaration = (PsiClass) declaration;
        if (!AnnotationUtil.isAnnotated(classDeclaration, fqn, false, true)) {
            return false;
        }

        PsiAnnotation tqDefault = AnnotationUtil.findAnnotation(classDeclaration, true, TYPE_QUALIFIER_DEFAULT);
        if (tqDefault == null) {
            return false;
        }

        Set<PsiAnnotation.TargetType> required = extractRequiredAnnotationTargets(tqDefault.findAttributeValue(null));
        return required != null
                && (required.isEmpty() || ContainerUtil.intersects(required, Arrays.asList(targetTypes)));
    }
}