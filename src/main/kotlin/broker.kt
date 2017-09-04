package broker

import Worker

data class Message(val senderId: String, val data: Any)

interface MessageBrokerListener {
	fun consumeMessage(message: Message)
	fun getListenerId() : String
}  

class MessageBroker() {

    private val workers = mutableMapOf<MessageBrokerListener, MutableSet<Int>>()

    fun register(messageListener: MessageBrokerListener, addPartitions: Boolean = true, partitionIds: Set<Int> = setOf(0) ) {
		if (addPartitions) {
			workers.getOrPut(messageListener, { mutableSetOf<Int>() }).addAll(partitionIds)			
		} else {
			workers[messageListener] = partitionIds.toMutableSet()
		}
	}
	
    fun unregister(messageListener: MessageBrokerListener) = workers.remove(messageListener)
	
	fun unregisterAll() = workers.clear()
	
	fun getPartitionIds(listener: MessageBrokerListener) = getPartitionIds(listener.getListenerId())
	
    fun getPartitionIds(listenerId: String) = workers.filterKeys { it.getListenerId() == listenerId }.map { it.value }.singleOrNull()?: throw IllegalArgumentException("Unknown listener with id: $listenerId")

    fun dispatchMessage(message: Message) {
		val partitionIds = getPartitionIds(message.senderId)
		workers.filter { it.value.intersect(partitionIds).isNotEmpty() }.forEach {it.key.consumeMessage(message)}
    }

    override fun toString(): String {
		return workers
				.map { it.key.getListenerId() to it.value.sortedBy { it } }
				.sortedBy { it.first }
				.joinToString(prefix = "MessageBroker(",postfix = ")")
    }
}

