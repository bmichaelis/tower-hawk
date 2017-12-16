package org.towerhawk.monitor.check.type.script;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.monitor.check.impl.AbstractCheck;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.scripting.NashornScriptEvaluator;
import org.towerhawk.serde.resolver.CheckType;

@Getter
@Setter
@Slf4j
@CheckType("js")
public class JavaScriptCheck extends AbstractCheck {

	private NashornScriptEvaluator evaluator;

	@JsonCreator
	public JavaScriptCheck(
			@JsonProperty("name") String name,
			@JsonProperty("function") String function,
			@JsonProperty("script") String script,
			@JsonProperty("file") String file
	) {
		if (name == null || name.isEmpty()) {
			name = "jsCheck";
		}
		if (function == null || function.isEmpty()) {
			function = "run";
		}
		evaluator = new NashornScriptEvaluator(name, function, script, file);
	}

	@Override
	protected void doRun(CheckRun.Builder builder, RunContext context) throws Exception {
		evaluator.invoke(builder, context);
	}
}
