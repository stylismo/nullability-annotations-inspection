package com.stylismo.intellij.inspection;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.codeInsight.intention.impl.AddNotNullAnnotationFix;
import com.intellij.codeInsight.intention.impl.AddNullableAnnotationFix;
import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.codeInsight.AnnotationUtil.ALL_ANNOTATIONS;


public class NullabilityAnnotationsInspection extends BaseJavaLocalInspectionTool {

    @Nullable
    public ProblemDescriptor[] checkField(@NotNull PsiField field,
                                          @NotNull InspectionManager manager,
                                          boolean isOnTheFly) {
        if (hasAnnotation(field)) {
            return null;
        }

        Collection<ProblemDescriptor> myProblemDescriptors = createProblemDescriptors(field, field.getType(), manager);
        return myProblemDescriptors.toArray(new ProblemDescriptor[myProblemDescriptors.size()]);
    }

    @Override
    public ProblemDescriptor[] checkMethod(@NotNull PsiMethod method,
                                           @NotNull InspectionManager manager,
                                           boolean isOnTheFly) {
        List<ProblemDescriptor> problemDescriptors = new ArrayList<>();
        if (isNonPrivateMethod(method)) {
            List<MethodSignatureBackedByPsiMethod> superMethodSignatures = superMethods(method);
            PsiParameter[] parameters = method.getParameterList().getParameters();
            checkMethodParams(manager, problemDescriptors, superMethodSignatures, parameters);

            checkMethodReturnType(method, manager, problemDescriptors);
        }

        return problemDescriptors.isEmpty()
                ? null
                : problemDescriptors.toArray(new ProblemDescriptor[problemDescriptors.size()]);
    }

    private static void checkMethodReturnType(@NotNull PsiMethod method,
                                              @NotNull InspectionManager manager,
                                              @NotNull List<ProblemDescriptor> aProblemDescriptors) {
        if (!method.isConstructor()
                && !(method.getReturnType() instanceof PsiPrimitiveType)
                && !hasAnnotation(method)) {

            LocalQuickFix[] localQuickFixes = createQuickFixes(method);

            PsiTypeElement returnTypeElement = method.getReturnTypeElement();
            if (returnTypeElement == null) {
                throw new RuntimeException("Bad method " + method);
            }

            if (returnTypeElement.isPhysical()) {
                ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
                        returnTypeElement,
                        "Missing @Nullable/@Nonnull annotation",
                        localQuickFixes,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        true,
                        false);
                aProblemDescriptors.add(problemDescriptor);
            }
        }
    }

    private static void checkMethodParams(@NotNull InspectionManager manager,
                                          @NotNull List<ProblemDescriptor> aProblemDescriptors,
                                          @NotNull List<MethodSignatureBackedByPsiMethod> aSuperMethodSignatures,
                                          @NotNull PsiParameter[] aParameters) {
        for (int i = 0, parametersLength = aParameters.length; i < parametersLength; i++) {
            PsiParameter parameter = aParameters[i];
            if (parameterNeedsAnnotation(parameter)) {
                if (!hasAnnotation(parameter) && !hasAnnotationInHierarchy(i, aSuperMethodSignatures)) {

                    LocalQuickFix[] localQuickFixes = createQuickFixes(parameter);

                    if (parameter.isPhysical()) {
                        ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
                                parameter,
                                "Missing @Nullable/@Nonnull annotation",
                                localQuickFixes,
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                true,
                                false);
                        aProblemDescriptors.add(problemDescriptor);
                    }
                }
            }
        }
    }


    private static boolean isMissingNullAnnotation(@NotNull PsiField aField, @NotNull PsiType aType) {
        return (aField.isPhysical())
                && !TypeConversionUtil.isPrimitiveAndNotNull(aType)
                && (!aField.hasModifierProperty(PsiModifier.FINAL) || !hasExpressionElement(aField.getChildren())
                && !(aField instanceof PsiEnumConstant)
                && !AnnotationUtil.isAnnotated(aField, ALL_ANNOTATIONS));
    }

    private static boolean hasExpressionElement(@NotNull PsiElement[] aPsiElements) {
        for (PsiElement myPsiElement : aPsiElements) {
            if ((myPsiElement instanceof PsiExpression)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static Collection<ProblemDescriptor> createProblemDescriptors(@NotNull PsiField aField,
                                                                          @NotNull PsiType aType,
                                                                          @NotNull InspectionManager manager) {
        Collection<ProblemDescriptor> myProblemDescriptors = Lists.newArrayList();
        if (isMissingNullAnnotation(aField, aType)) {
            LocalQuickFix[] localQuickFixes = createQuickFixes(aField);

            ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
                    aField,
                    "Missing @Nullable/@Nonnull annotation",
                    localQuickFixes,
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    true,
                    false);
            myProblemDescriptors.add(problemDescriptor);
        }
        return myProblemDescriptors;
    }
    @NotNull
    private static LocalQuickFix[] createQuickFixes(@NotNull PsiModifierListOwner aOwner) {
        return new LocalQuickFix[]{
                new AddNullableAnnotationFix(aOwner) {
                    @Override
                    protected boolean isAvailable() {
                        return true;
                    }
                },
                new AddNotNullAnnotationFix(aOwner) {
                    @Override
                    protected boolean isAvailable() {
                        return true;
                    }
                }
        };
    }

    private static boolean hasAnnotation(PsiModifierListOwner psiModifierListOwner) {
        return NullableNotNullManager.isNullable(psiModifierListOwner)
                || NullableNotNullManager.isNotNull(psiModifierListOwner);
    }

    private static boolean hasAnnotationInHierarchy(int parameter,
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

    private static boolean parameterNeedsAnnotation(PsiParameter parameter) {
        return !(parameter.getType() instanceof PsiPrimitiveType) && !parameter.isVarArgs();
    }

    private static List<MethodSignatureBackedByPsiMethod> superMethods(PsiMethod method) {
        List<MethodSignatureBackedByPsiMethod> signatures = method.findSuperMethodSignaturesIncludingStatic(true);
        signatures.removeIf(superSignature ->
                superSignature.getMethod().getParameterList().getParametersCount()
                        != method.getParameterList().getParametersCount());
        return signatures;
    }

    private static boolean isNonPrivateMethod(@NotNull PsiMethod method) {
        return !method.hasModifierProperty(PsiModifier.PRIVATE);
    }

}
