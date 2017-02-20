package com.stylismo.intellij.inspection.example;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class Example {
    private static final String PROBLEM = "You got Trumped!";
    private final String problem1 = "How does that feel?";
    private final String problem2;
    private String problem3;
    @NotNull private final String ok1;
    @NotNull private String ok2;

    public Example(String problem2, String problem3, @NotNull String ok1) {
        this.problem2 = problem2;
        this.problem3 = problem3;
        this.ok1 = ok1;
        this.ok2 = PROBLEM;
    }

    public String problem(String problem) {
        return problem;
    }

    public String[] problem(String[] problem) {
        return problem;
    }

    @NotNull
    public String annotatedNotNullOk(@NotNull String ok) {
        return ok;
    }

    @Nonnull
    public String annotatedNonNullOk(@Nonnull String ok) {
        return ok;
    }

    @Nullable
    public String annotatedNullableOk(@Nullable String ok) {
        return ok;
    }

    public int okPrimitives(int ok) {
        return ok;
    }

    public void okVarArg(String... ok) {
    }

    private String privateMethodProblem(String problem) {
        return problem;
    }

    public static String getPROBLEM() {
        return PROBLEM;
    }

    public String getProblem1() {
        return problem1;
    }

    public String getProblem2() {
        return problem2;
    }

    public String getProblem3() {
        return problem3;
    }

    public void setProblem3(String problem3) {
        this.problem3 = problem3;
    }

    @NotNull
    public String getOk1() {
        return ok1;
    }

    @NotNull
    public String getOk2() {
        return ok2;
    }

    public void setOk2(@NotNull String ok2) {
        this.ok2 = ok2;
    }
}
