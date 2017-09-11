package com.stylismo.intellij.inspection;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
import static com.intellij.psi.PsiModifier.FINAL;
import static com.intellij.psi.PsiModifier.PRIVATE;
import static com.intellij.psi.PsiModifier.STATIC;
import static com.stylismo.intellij.inspection.QuickFixFactory.createQuickFixes;


public class NullabilityAnnotationsInspection extends BaseJavaLocalInspectionTool {
    private static final String MISSING_NULLABLE_NONNULL_ANNOTATION = "Missing @Nullable/@Nonnull annotation";
    private boolean reportFields = true;
    private boolean reportInitializedStaticFinalFields = true;
    private boolean reportInitializedFinalFields = true;
    private boolean reportPrivateMethods = true;
    private boolean removeRedundantAnnotations = true;

    @Nullable
    @Override
    public JComponent createOptionsPanel() {
        return new OptionsPanel(this);
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkField(PsiField field, InspectionManager manager, boolean isOnTheFly) {
        if (isMissingNullAnnotation(field, field.getType())) {
            Collection<ProblemDescriptor> problemDescriptors = Lists.newArrayList();
            createProblemDescriptorWithQuickFixes(field, manager, problemDescriptors, field);
            return problemDescriptors.isEmpty()
                    ? null
                    : problemDescriptors.toArray(new ProblemDescriptor[problemDescriptors.size()]);
        }
        return null;
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkMethod(PsiMethod method, InspectionManager manager, boolean isOnTheFly) {
        if (isNonPrivateMethod(method)) {
            List<ProblemDescriptor> problemDescriptors = new ArrayList<>();
            List<MethodSignatureBackedByPsiMethod> superMethodSignatures = superMethods(method);
            PsiParameter[] parameters = method.getParameterList().getParameters();
            checkMethodParams(manager, problemDescriptors, superMethodSignatures, parameters);
            checkMethodReturnType(method, manager, problemDescriptors);
            return problemDescriptors.isEmpty()
                    ? null
                    : problemDescriptors.toArray(new ProblemDescriptor[problemDescriptors.size()]);
        }
        return null;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isReportFields() {
        return reportFields;
    }

    @SuppressWarnings("WeakerAccess")
    public void setReportFields(boolean reportFields) {
        this.reportFields = reportFields;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isReportInitializedFinalFields() {
        return reportInitializedFinalFields;
    }

    @SuppressWarnings("WeakerAccess")
    public void setReportInitializedFinalFields(boolean reportInitializedFinalFields) {
        this.reportInitializedFinalFields = reportInitializedFinalFields;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isReportInitializedStaticFinalFields() {
        return reportInitializedStaticFinalFields;
    }

    @SuppressWarnings("WeakerAccess")
    public void setReportInitializedStaticFinalFields(boolean reportInitializedStaticFinalFields) {
        this.reportInitializedStaticFinalFields = reportInitializedStaticFinalFields;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isReportPrivateMethods() {
        return reportPrivateMethods;
    }

    @SuppressWarnings("WeakerAccess")
    public void setReportPrivateMethods(boolean reportPrivateMethods) {
        this.reportPrivateMethods = reportPrivateMethods;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isRemoveRedundantAnnotations() {
        return removeRedundantAnnotations;
    }

    @SuppressWarnings("WeakerAccess")
    public void setRemoveRedundantAnnotations(boolean removeRedundantAnnotations) {
        this.removeRedundantAnnotations = removeRedundantAnnotations;
    }

    private void checkMethodReturnType(PsiMethod method,
                                       InspectionManager manager,
                                       List<ProblemDescriptor> aProblemDescriptors) {
        if (!method.isConstructor()
                && !(method.getReturnType() instanceof PsiPrimitiveType)
                && !hasAnnotation(method)) {

            PsiTypeElement returnTypeElement = method.getReturnTypeElement();
            if (returnTypeElement == null) {
                return;
            }

            createProblemDescriptorWithQuickFixes(method, manager, aProblemDescriptors, returnTypeElement);
        }
    }

    private void checkMethodParams(InspectionManager manager,
                                   List<ProblemDescriptor> aProblemDescriptors,
                                   List<MethodSignatureBackedByPsiMethod> aSuperMethodSignatures,
                                   PsiParameter[] aParameters) {
        for (int i = 0, parametersLength = aParameters.length; i < parametersLength; i++) {
            PsiParameter parameter = aParameters[i];
            if (parameterNeedsAnnotation(parameter)) {
                if (!hasAnnotation(parameter) && !hasAnnotationInHierarchy(i, aSuperMethodSignatures)) {
                    createProblemDescriptorWithQuickFixes(parameter, manager, aProblemDescriptors, parameter);
                }
            }
        }
    }

    private void createProblemDescriptorWithQuickFixes(PsiModifierListOwner owner,
                                                       InspectionManager manager,
                                                       Collection<ProblemDescriptor> problemDescriptors,
                                                       PsiElement element) {
        if (element.isPhysical()) {
            LocalQuickFix[] localQuickFixes = createQuickFixes(owner, isRemoveRedundantAnnotations());
            ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
                    element,
                    MISSING_NULLABLE_NONNULL_ANNOTATION,
                    localQuickFixes,
                    GENERIC_ERROR_OR_WARNING,
                    true,
                    false);
            problemDescriptors.add(problemDescriptor);
        }
    }

    private boolean isMissingNullAnnotation(PsiField field, PsiType type) {
        return reportFields
                && field.isPhysical()
                && !(field instanceof PsiEnumConstant)
                && !TypeConversionUtil.isPrimitiveAndNotNull(type)
                && (shouldCheckFinalField(field) || shouldCheckStaticFinalField(field))
                && !hasAnnotation(field);
    }

    private boolean shouldCheckStaticFinalField(PsiField field) {
        return reportInitializedStaticFinalFields
                || (
                field.hasModifierProperty(FINAL)
                        && field.hasModifierProperty(STATIC)
                        && !hasExpressionElement(field.getChildren()))
                || !field.hasModifierProperty(FINAL);
    }

    private boolean shouldCheckFinalField(PsiField field) {
        return reportInitializedFinalFields
                || (
                field.hasModifierProperty(FINAL)
                        && !field.hasModifierProperty(STATIC)
                        && !hasExpressionElement(field.getChildren()))
                || !field.hasModifierProperty(FINAL);
    }

    private boolean hasExpressionElement(PsiElement[] psiElements) {
        for (PsiElement psiElement : psiElements) {
            if ((psiElement instanceof PsiExpression)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnnotation(PsiModifierListOwner psiModifierListOwner) {
        return NullableNotNullManager.isNullable(psiModifierListOwner)
                || NullableNotNullManager.isNotNull(psiModifierListOwner);
    }

    private boolean hasAnnotationInHierarchy(int parameter,
                                             List<MethodSignatureBackedByPsiMethod> superMethodSignatures) {
        for (MethodSignatureBackedByPsiMethod methodSignature : superMethodSignatures) {
            PsiMethod superMethod = methodSignature.getMethod();
            PsiParameter[] superParameters = superMethod.getParameterList().getParameters();
            PsiParameter superParameter = superParameters[parameter];
            if (hasAnnotation(superParameter)) {
                return true;
            }
        }
        return false;
    }

    private boolean parameterNeedsAnnotation(PsiParameter parameter) {
        return !(parameter.getType() instanceof PsiPrimitiveType)
                && !parameter.isVarArgs();
    }

    private List<MethodSignatureBackedByPsiMethod> superMethods(PsiMethod method) {
        List<MethodSignatureBackedByPsiMethod> signatures = method.findSuperMethodSignaturesIncludingStatic(true);
        signatures.removeIf(superSignature ->
                superSignature.getMethod().getParameterList().getParametersCount()
                        != method.getParameterList().getParametersCount());
        return signatures;
    }

    private boolean isNonPrivateMethod(PsiMethod method) {
        return reportPrivateMethods || !method.hasModifierProperty(PRIVATE);
    }
}