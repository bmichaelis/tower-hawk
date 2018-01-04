package org.towerhawk.monitor.check.execution;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.DefaultCheck;
import org.towerhawk.monitor.check.execution.constant.SuccessfulCheck;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.serde.resolver.CheckExecutorTypeResolver;
import org.towerhawk.spring.config.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true, defaultImpl = SuccessfulCheck.class)
@JsonTypeIdResolver(CheckExecutorTypeResolver.class)
public interface CheckExecutor extends AutoCloseable {

	/**
	 * Similar to a @PostConstruct. This method should be called by the deserialization
	 * framework to allow checks to initialize any state they need to, like caching a
	 * computation, setting default objects, or starting a background thread.
	 *
	 * @param checkExecutor The previous {@link CheckExecutor} that was defined with the same app and Id
	 * @param check         The {@link Check} that is running this CheckExecutor
	 * @param configuration The Configuration provided by Spring so that checks can
	 *                      get defaults and dynamically configure themselves.
	 * @throws Exception The calling framework should handle any exceptions this throws,
	 *                   so there is little need to handle exceptions inside the method
	 */
	default void init(CheckExecutor checkExecutor, Check check, Configuration configuration) throws Exception {
		// do nothing
	}

	// maybe implement this if Jackson can't hand the type to the Check?
	// String getType();

	/**
	 * This is the method each concrete class should implement
	 * The builder is retained by the holding ${@link DefaultCheck}
	 * class and used to set values like getDuration,
	 * getStartTime, getEndTime, and other things that should
	 * be handled in a standard way.
	 *
	 * @param builder
	 */
	ExecutionResult execute(CheckRun.Builder builder, RunContext context) throws Exception;

	default void close() throws Exception {
		// do nothing
	}

}
