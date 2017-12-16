package org.towerhawk.monitor.check.type.constant;

import org.towerhawk.monitor.check.impl.AbstractCheck;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.serde.resolver.CheckType;

@CheckType("succeeded")
public class SuccessfulCheck extends AbstractCheck {

	@Override
	protected void doRun(CheckRun.Builder builder, RunContext runContext) throws Exception {
		builder.succeeded().message("Always successful");
	}
}
