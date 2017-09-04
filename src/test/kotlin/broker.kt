import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import broker.Message
import broker.MessageBrokerListener
import broker.MessageBroker

class BrokerSpek : Spek({
	
	class DummyMessageBrokerListener(val id: Int, val messages : MutableList<Message> = mutableListOf()) : MessageBrokerListener {
		override fun consumeMessage(message: Message) {	messages.add(message) }
		override fun getListenerId(): String = id.toString()
		fun clear() = messages.clear()
	}

		
    describe("a cluster of three") {
		val listenerRange = (0..2)
        val listeners = listenerRange.map { DummyMessageBrokerListener(it) }.toList()

		beforeEachTest {
			listeners.forEach(DummyMessageBrokerListener::clear)
		}

        describe("in the same partition") {
            val broker = MessageBroker()
            listeners.forEach {broker.register(it, false)}

			on("broadcast") {
                for (i in listenerRange) {
                    broker.dispatchMessage(Message(i.toString(), "sender $i"))
                }

				it("delivers all messages to everyone") {
                    listeners.forEach {
						assertEquals(3, it.messages.size)
						for (i in listenerRange) {
                            assertTrue(it.messages.any { it.senderId== i.toString() })
                        }
                    }
				}
			}

		}

		describe("each in separate partition") {
            val broker = MessageBroker()
			listeners.forEach { broker.register(it, false, setOf(it.id.toInt())) }

			on("broadcast") {
                for (i in listenerRange) {
                    broker.dispatchMessage(Message(i.toString(), "sender $i"))
                }

                it("delivers messages only to itself") {
                    listeners.forEach { listener ->
                        assertEquals(1, listener.messages.size)
                        assertTrue(listener.messages.any { it.senderId==listener.getListenerId() })
                    }
                }

			}

        }

		describe("partitions bridged by listener 2") {
            val broker = MessageBroker()
        	listeners.forEach {broker.register(it, false, setOf(it.id.toInt()))}
        	broker.register(listeners[2], true, setOf(0,1))

        	on("broadcasts from 0") {
    			broker.dispatchMessage(Message("0", "data"))

				it("delivers to itself and shared") {
					assertEquals(1, listeners[0].messages.size)
					assertEquals(0, listeners[1].messages.size)
					assertEquals(1, listeners[2].messages.size)
				}
        	}

        	on("broadcasts from 1") {
    			broker.dispatchMessage(Message("1", "data"))

				it("delivers to itself and shared") {
					assertEquals(0, listeners[0].messages.size)
					assertEquals(1, listeners[1].messages.size)
					assertEquals(1, listeners[2].messages.size)
				}
        	}

        	on("broadcasts from 2") {
    			broker.dispatchMessage(Message("2", "data"))

				it ("delivers to all") {
					assertEquals(1, listeners[0].messages.size)
					assertEquals(1, listeners[1].messages.size)
					assertEquals(1, listeners[2].messages.size)
				}
        	}
        }
				
    }
})