package org.towerhawk.plugin.kafka.consumer.lag

import kafka.admin.ConsumerGroupCommand._
import org.apache.kafka.common.{KafkaException, Node}
import org.towerhawk.monitor.check.execution.ExecutionResult

class ConsumerLagAdapter(brokers: String, groupId: String) extends AutoCloseable {

  val opts = new ConsumerGroupCommandOptions(Array("--bootstrap-server", brokers, "--group", groupId))

  val consumerGroupService = new KafkaConsumerGroupService(opts)

  private val MISSING_VALUE = "-"

  def getLags(): ExecutionResult = {
    val result = ExecutionResult.startTimer()

    val (state, assignments) = consumerGroupService.describeGroup()
    result.complete()

    def convertAssignments(): Unit = {
      //End up creating a map of group -> topic -> partition -> lag info

      val resultList = new java.util.ArrayList[KafkaConsumerLagDTO]()
      assignments.get.foreach(a => {
        val lag = new KafkaConsumerLagDTO
        lag.setGroup(a.group)
        lag.setTopic(a.topic.getOrElse(MISSING_VALUE))
        lag.setPartition(String.valueOf(a.partition.getOrElse(-1)))
        lag.setLag(a.lag.getOrElse(-1))
        lag.setLogEndOffset(a.logEndOffset.getOrElse(-1))
        lag.setOffset(a.offset.getOrElse(-1))
        lag.setClientId(a.clientId.getOrElse(MISSING_VALUE))
        lag.setConsumerId(a.consumerId.getOrElse(MISSING_VALUE))
        lag.setHost(a.host.getOrElse(MISSING_VALUE))
        val node = a.coordinator.getOrElse(Node.noNode)
        lag.setCoordinatorId(node.id())
        lag.setCoordinatorHost(node.host())
        resultList.add(lag)
      })
      result.setResult(resultList)
    }

    assignments match {
      case None =>
        // applies to both old and new consumer
        throw new IllegalStateException(s"Consumer group '$groupId' does not exist.")
      _ =>
        state match {
          case Some("Dead") =>
            throw new IllegalStateException(s"Consumer group '$groupId' does not exist")
          case Some("Empty") =>
            result.addResult("state", "no live consumers")
            convertAssignments()
          case Some("PreparingRebalance") | Some("AwaitingSync") =>
            result.addResult("state", s"$groupId is rebalancing")
            convertAssignments()
          case Some("Stable") =>
            convertAssignments()
          case other =>
            // the control should never reach here
            throw new KafkaException(s"Expected a valid consumer group state, but found '${other.getOrElse("NONE")}'.")
        }
    }
    result
  }

  override def close(): Unit = {
    consumerGroupService.close()
  }
}

