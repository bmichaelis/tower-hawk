package org.towerhawk.monitor.check.transform;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import lombok.NonNull;
import org.towerhawk.serde.resolver.TransformTypeResolver;

/**
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true, defaultImpl = IdentityTransform.class)
@JsonTypeIdResolver(TransformTypeResolver.class)
public interface Transform<T> {
	T transform(@NonNull Object value) throws Exception;
}
