package org.towerhawk.monitor.check.type.script;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.towerhawk.monitor.check.impl.AbstractCheck;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.scripting.GroovyScriptEvaluator;
import org.towerhawk.serde.resolver.CheckType;

@Getter
@CheckType("groovy")
public class GroovyScriptCheck extends AbstractCheck {

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
	protected void doRun(CheckRun.Builder builder, RunContext context) throws Exception {
		evaluator.invoke(builder, context);
	}
}
