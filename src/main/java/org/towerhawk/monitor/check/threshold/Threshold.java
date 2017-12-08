package org.towerhawk.monitor.check.threshold;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.serde.resolver.ThresholdTypeResolver;

/**
 * Classes that implement the Threshold interface should mark
 * the builder status as successful, warning, or critical.
 * They should optionally add a context or set a message
 * if the matching method returns true to help with trouble shooting
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true, defaultImpl = SimpleNumericThreshold.class)
@JsonTypeIdResolver(ThresholdTypeResolver.class)
public interface Threshold {

	boolean isAddContext();

	boolean isSetMessage();

	void evaluate(CheckRun.Builder builder, Object value);
}
