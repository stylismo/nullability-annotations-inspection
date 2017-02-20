package com.stylismo.intellij.inspection.example;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class DerivedExample extends Example {

    public DerivedExample(String problem2, String problem3, @NotNull String ok1) {
        super(problem2, problem3, ok1);
    }

}
