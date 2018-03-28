package org.towerhawk.plugin.kafka.consumer.lag;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.config.Config;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.execution.CheckExecutor;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.plugin.PluginContext;
import org.towerhawk.serde.resolver.TowerhawkType;

@Slf4j
@Getter
@Setter
@TowerhawkType("kafkaConsumerLag")
public class KafkaConsumerLagCheck implements CheckExecutor {

	protected ConsumerLagAdapter adapter;
	protected String brokers = "localhost:9092";
	protected String group;

	@Override
	public void init(CheckExecutor checkExecutor, Check check, Config config) throws Exception {
		ClassLoader cached = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(PluginContext.getClassLoader(this.getClass()));
		adapter = new ConsumerLagAdapter(brokers, group);
		adapter.getLags();
		Thread.currentThread().setContextClassLoader(cached);
	}

	@Override
	public ExecutionResult execute(CheckRun.Builder builder, RunContext context) throws Exception {
		return adapter.getLags();
	}

	@Override
	public void close() throws Exception {
		adapter.close();
	}
}
