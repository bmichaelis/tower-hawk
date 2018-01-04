package org.towerhawk.serde.resolver;

import java.lang.annotation.Annotation;

public class CheckExecutorTypeResolver extends AbstractTypeResolver {

	@Override
	protected Class<? extends Annotation> getAnnotationType() {
		return CheckExecutorType.class;
	}

	@Override
	protected String getType(Class c) {
		CheckExecutorType type = (CheckExecutorType) c.getAnnotation(CheckExecutorType.class);
		return type.value();
	}
}
