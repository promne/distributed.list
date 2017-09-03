import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

data class WorkerItem(val timestamp: VectorTimestamp<String>, val data: String, val time: Long = System.nanoTime())

class Worker(val id: String, val broker: MessageBroker) : Runnable {

    val myQueue = mutableListOf<WorkerItem>()
    val receivedQueue = ConcurrentLinkedQueue<Message>()
    private val stopRunner = AtomicBoolean(false)
    var myClock = VectorClock<String>(id)

    fun queueReceivedMessage(message: Message) {
        if (message.senderId != id) {
            receivedQueue.add(message)
        }
    }

    fun stopWorker() = stopRunner.set(true)

    fun sendMessage(data: String) {
        val item = WorkerItem(myClock.increment(), data)
        myQueue.add(item)
        broker.dispatchMessage(Message(id, item))
    }

    override fun run() {
        while (!stopRunner.get()) {
            if (receivedQueue.isNotEmpty() && false) {
                receivedQueue.peek().let { message ->
                    val messageData = message.data
                    when (messageData::class) {
                        WorkerItem::class -> (messageData as WorkerItem).let {
                            myClock.merge(it.timestamp)
                        }
                    // different message types
                    }
                    receivedQueue.remove() // do the remove after successful processing. Consider some retry-counter and a dead queue.
                }

            }
            Thread.sleep(50)
        }
    }

}