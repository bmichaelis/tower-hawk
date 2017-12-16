package org.towerhawk.serde.resolver;

import java.lang.annotation.Annotation;

public class TransformTypeResolver extends AbstractTypeResolver {

	@Override
	protected Class<? extends Annotation> getAnnotationType() {
		return TransformType.class;
	}

	@Override
	protected String getType(Class c) {
		TransformType type = (TransformType) c.getAnnotation(TransformType.class);
		return type.value();
	}
}
