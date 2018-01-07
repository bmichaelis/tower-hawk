package org.towerhawk.monitor.check.execution.constant;

import org.towerhawk.monitor.check.execution.CheckExecutor;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.serde.resolver.CheckExecutorType;

@CheckExecutorType("succeeded")
public class SuccessfulCheck implements CheckExecutor {

	@Override
	public ExecutionResult execute(CheckRun.Builder builder, RunContext runContext) throws Exception {
		builder.succeeded().message("Always successful");
		return ExecutionResult.of("Always successful");
	}
}