package com.stylismo.intellij.inspection.example.defaults;

import org.jetbrains.annotations.Nullable;

public class ExampleWithDefaults {
    private static final String PROBLEM = "You got Trumped!";
    private final String ok1 = "How does that feel?";
    private final String ok2;
    private String ok3;
    @Nullable private String ok4;

    public ExampleWithDefaults(String ok2, String ok3, @Nullable String ok4) {
        this.ok2 = ok2;
        this.ok3 = ok3;
        this.ok4 = ok4;
    }

    public String ok(String problem) {
        return problem;
    }

    @Nullable
    public String annotatedNullableOk(@Nullable String ok) {
        return ok;
    }

    public String[] ok(String[] ok) {
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
}
