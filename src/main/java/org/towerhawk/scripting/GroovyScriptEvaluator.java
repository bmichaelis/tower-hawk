package org.towerhawk.scripting;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class GroovyScriptEvaluator extends AbstractScriptEvaluator {

	protected Script groovyScript;

	public GroovyScriptEvaluator(String name, String function, String script, String file) {
		super(name, function, script, file);
		GroovyShell shell = new GroovyShell();
		groovyScript = shell.parse(getScript());
	}

	@Override
	public Object invoke(Object... args) throws Exception {
		return groovyScript.invokeMethod(getFunction(), args);
	}
}