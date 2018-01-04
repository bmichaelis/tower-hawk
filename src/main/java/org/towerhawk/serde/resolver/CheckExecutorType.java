package org.towerhawk.serde.resolver;

import org.towerhawk.monitor.check.execution.ExecutionResult;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckExecutorType {

	String value();

	String[] resultKeys() default {ExecutionResult.RESULT};
}
