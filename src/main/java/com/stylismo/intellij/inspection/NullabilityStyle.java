package com.stylismo.intellij.inspection;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;

@Documented
@Nonnull
@TypeQualifierDefault({
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.TYPE
})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface NullabilityStyle {
}
