package org.towerhawk.plugin.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.pf4j.Extension;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.execution.CheckExecutor;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.spring.config.Configuration;

import java.util.HashMap;

@Extension
public class KafkaRoundTripCheck implements CheckExecutor{

	private Consumer<byte[], byte[]> consumer = new KafkaConsumer<byte[], byte[]>(new HashMap<String, Object>());

	public void init(CheckExecutor checkExecutor, Check check, Configuration configuration) throws Exception {
		consumer.poll(1000);
	}

	public ExecutionResult execute(CheckRun.Builder builder, RunContext context) throws Exception {
		return ExecutionResult.of("plugin-test");
	}

	public void close() throws Exception {
		consumer.close();
	}
}
