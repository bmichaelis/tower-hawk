package org.towerhawk.monitor.check.execution.constant;

import org.towerhawk.monitor.check.execution.CheckExecutor;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.serde.resolver.CheckExecutorType;

@CheckExecutorType("timeout")
public class TimeOutCheck implements CheckExecutor {

	@Override
	public ExecutionResult execute(CheckRun.Builder builder, RunContext runContext) throws Exception {
		builder.message("Sleeping forever");
		Thread.sleep(Long.MAX_VALUE);
		return ExecutionResult.of("Sleeping forever");
	}
}
