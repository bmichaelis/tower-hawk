package org.towerhawk.monitor.check.execution.script;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.towerhawk.monitor.check.DefaultCheck;
import org.towerhawk.monitor.check.execution.CheckExecutor;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.scripting.GroovyScriptEvaluator;
import org.towerhawk.serde.resolver.CheckExecutorType;

import java.util.Map;

@Getter
@CheckExecutorType("groovy")
public class GroovyScriptCheck implements CheckExecutor {

	GroovyScriptEvaluator evaluator;

	@JsonCreator
	public GroovyScriptCheck(
			@JsonProperty("name") String name,
			@JsonProperty("function") String function,
			@JsonProperty("script") String script,
			@JsonProperty("file") String file
	) {
		if (name == null || name.isEmpty()) {
			name = "groovyCheck";
		}
		if (function == null || function.isEmpty()) {
			function = "run";
		}
		evaluator = new GroovyScriptEvaluator(name, function, script, file);
	}

	@Override
	public ExecutionResult execute(CheckRun.Builder builder, RunContext context) throws Exception {
		ExecutionResult result = ExecutionResult.startTimer();
		Object invokeResult = evaluator.invoke(builder, context);
		result.complete();
		if (invokeResult instanceof Map) {
			((Map<?, ?>) invokeResult).forEach((k, v) -> result.addResult(k.toString(), v));
		} else {
			result.setResult(invokeResult);
		}
		return result;
	}
}
