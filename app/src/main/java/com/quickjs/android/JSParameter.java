package com.quickjs.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface JSParameter {
    String value() default "";

    String defaultValue() default "";

    int defaultInt() default -1;
}
