package org.towerhawk.plugin.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.pf4j.Extension;
import org.towerhawk.config.Config;
import org.towerhawk.monitor.check.Check;
import org.towerhawk.monitor.check.execution.CheckExecutor;
import org.towerhawk.monitor.check.execution.ExecutionResult;
import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.run.context.RunContext;
import org.towerhawk.serde.resolver.TowerhawkType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Extension
@TowerhawkType("kafkaRoundTrip")
public class KafkaRoundTripCheck implements CheckExecutor {

	private Consumer<byte[], byte[]> consumer;

	public KafkaRoundTripCheck() {
		String kafkaServers;
		{
			kafkaServers = System.getenv("KAFKA_BROKERS");
			if (kafkaServers == null || kafkaServers.isEmpty()) {
				kafkaServers = System.getProperty("KAFKA_BROKERS");
			}
			if (kafkaServers == null || kafkaServers.isEmpty()) {
				kafkaServers = "localhost:9092"; //"mesos-master-1.sw.ax.vivintsky.com:9092,mesos-master-2.sw.ax.vivintsky.com:9092,mesos-master-3.sw.ax.vivintsky.com:9092"
			}
		}
		log.info("kafkaServers = {}", kafkaServers);
		Map<String, Object> consumerConfigs = new HashMap<>();
		consumerConfigs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerConfigs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, TelegrafMetricDeserializer.class);
		consumerConfigs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumerConfigs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
		consumerConfigs.put(ConsumerConfig.CLIENT_ID_CONFIG, "telegraf-to-druid-transformer");
		consumerConfigs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		consumerConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, "telegraf-to-druid-transfomer");
		consumerConfigs.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50000);
		consumerConfigs.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60000);
		consumerConfigs.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 70000);
		consumerConfigs.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 60000);
		consumerConfigs.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 5000);
		consumerConfigs.put("serDeObjectMapper", mapper);
		consumer = new KafkaConsumer<>(consumerConfigs);
		consumer.subscribe(Pattern.compile("telegraf.*"), new ConsumerRebalanceListener() {
			@Override
			public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

			}

			@Override
			public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
				rollBack = true;
			}
		});

		Map<String, Object> producerConfigs = new HashMap<>();
		producerConfigs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerConfigs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, DruidMetricSerializer.class);
		producerConfigs.put(ProducerConfig.ACKS_CONFIG, "all");
		producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
		producerConfigs.put(ProducerConfig.CLIENT_ID_CONFIG, "telegraf-to-druid-transformer");
		producerConfigs.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
		producerConfigs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
		producerConfigs.put(ProducerConfig.LINGER_MS_CONFIG, 1000);
		producerConfigs.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
		producerConfigs.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 100);
		producerConfigs.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
		producerConfigs.put(ProducerConfig.RETRIES_CONFIG, 10);
		producerConfigs.put("serDeObjectMapper", mapper);
		producer = new KafkaProducer<>(producerConfigs);

		Executors.newSingleThreadExecutor().submit(this);
	}

	public void init(CheckExecutor checkExecutor, Check check, Config config) throws Exception {
		consumer.poll(1000);
	}

	public ExecutionResult execute(CheckRun.Builder builder, RunContext context) throws Exception {
		return ExecutionResult.of("plugin-test");
	}

	public void close() throws Exception {
		consumer.close();
	}
}
