package org.towerhawk.monitor.check.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.scripting.GroovyScriptEvaluator;
import org.towerhawk.serde.resolver.TransformType;

@Getter
@Setter
@Slf4j
@TransformType("groovy")
public class GroovyScriptTransform implements Transform {
	protected GroovyScriptEvaluator evaluator;

	@JsonCreator
	public GroovyScriptTransform(
			@JsonProperty("name") String name,
			@JsonProperty("function") String function,
			@JsonProperty("script") String script,
			@JsonProperty("file") String file
	) {
		if (name == null || name.isEmpty()) {
			name = "jsTransform";
		}
		if (function == null || function.isEmpty()) {
			function = "transform";
		}
		evaluator = new GroovyScriptEvaluator(name, function, script, file);
	}

	@Override
	public Object transform(Object value) throws Exception {
		return evaluator.invoke(value);
	}
}
