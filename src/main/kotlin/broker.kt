data class Message(val senderId: String, val data: Any)

class MessageBroker() {

    private val workers = mutableMapOf<String, Pair<Worker, Int>>()

    fun register(addWorkers: Collection<Worker>, partitionId: Int = 0) {
        addWorkers.forEach {
            workers.put(it.id, it to partitionId)
        }
    }

    fun gerPartitionId(workerId: String) = workers.getOrElse(workerId) { throw IllegalArgumentException("Unknown worker: $workerId") }.second

    fun partition(partitionId: Int, workerIds: Collection<String>) {
        register(workerIds.map { workers.getOrElse(it) { throw IllegalArgumentException("Unknown worker: $it") }.first }, partitionId)
    }

    fun dispatchMessage(message: Message) {
        val partitionId = workers.getOrElse(message.senderId) { throw IllegalArgumentException("Unknown worker: ${message.senderId}") }.second
        workers.filter { it.value.second == partitionId }.forEach { it.value.first.queueReceivedMessage(message) }
    }
}

