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
import com.intellij.psi.PsiModifier;
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
import static com.stylismo.intellij.inspection.QuickFixFactory.createQuickFixes;


public class NullabilityAnnotationsInspection extends BaseJavaLocalInspectionTool {
    private static final String MISSING_NULLABLE_NONNULL_ANNOTATION = "Missing @Nullable/@Nonnull annotation";
    private boolean theReportFields = true;
    private boolean theReportInitializedFinalFields = true;
    private boolean theReportPrivateMethods = true;

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
        return theReportFields;
    }

    @SuppressWarnings("WeakerAccess")
    public void setReportFields(boolean aReportFields) {
        theReportFields = aReportFields;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isReportInitializedFinalFields() {
        return theReportInitializedFinalFields;
    }

    @SuppressWarnings("WeakerAccess")
    public void setReportInitializedFinalFields(boolean aReportInitializedFinalFields) {
        theReportInitializedFinalFields = aReportInitializedFinalFields;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isReportPrivateMethods() {
        return theReportPrivateMethods;
    }

    @SuppressWarnings("WeakerAccess")
    public void setReportPrivateMethods(boolean aReportPrivateMethods) {
        theReportPrivateMethods = aReportPrivateMethods;
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

    private void createProblemDescriptorWithQuickFixes(PsiModifierListOwner aField,
                                                       InspectionManager manager,
                                                       Collection<ProblemDescriptor> aProblemDescriptors,
                                                       PsiElement aElement) {
        if (aElement.isPhysical()) {
            LocalQuickFix[] localQuickFixes = createQuickFixes(aField);
            ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
                    aElement,
                    MISSING_NULLABLE_NONNULL_ANNOTATION,
                    localQuickFixes,
                    GENERIC_ERROR_OR_WARNING,
                    true,
                    false);
            aProblemDescriptors.add(problemDescriptor);
        }
    }

    private boolean isMissingNullAnnotation(PsiField aField, PsiType aType) {
        return theReportFields
                && aField.isPhysical()
                && !(aField instanceof PsiEnumConstant)
                && !TypeConversionUtil.isPrimitiveAndNotNull(aType)
                && shouldCheckFinalField(aField)
                && !hasAnnotation(aField);
    }

    private boolean shouldCheckFinalField(PsiField aField) {
        return theReportInitializedFinalFields
                || (aField.hasModifierProperty(PsiModifier.FINAL) && !hasExpressionElement(aField.getChildren()))
                || !aField.hasModifierProperty(PsiModifier.FINAL);
    }

    private boolean hasExpressionElement(PsiElement[] aPsiElements) {
        for (PsiElement psiElement : aPsiElements) {
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
        return theReportPrivateMethods || !method.hasModifierProperty(PsiModifier.PRIVATE);
    }
}