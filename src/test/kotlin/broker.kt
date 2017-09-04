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
        val listeners = (0..2).map { DummyMessageBrokerListener(it) }.toList()
		val broker = MessageBroker()

		beforeEachTest {
			listeners.forEach(DummyMessageBrokerListener::clear)
			broker.unregisterAll()
		}
		
        on("connected") {
			listeners.forEach {broker.register(it, false)}
			
			it("sends to everyone") {
				for (i in 0..2) {
					broker.dispatchMessage(Message(i.toString(), "sender $i"))
					assertTrue(listeners.all { it.messages.size == i + 1 })
				}				
			}
				
		}

		on("disconnected") {
        	listeners.forEach {broker.register(it, false, setOf(it.id.toInt()))}
        	
        	it("sends only to itself") {
        		for (i in 0..2) {
        			broker.dispatchMessage(Message(i.toString(), "sender $i"))
        			assertTrue(listeners.filter { it.id <=i }.all { it.messages.size == 1 })
					assertTrue(listeners.filter { it.id > i }.all { it.messages.size == 0 })
        		}
        		
        	}        	
        }

		on("shared partition") {
        	listeners.forEach {broker.register(it, false, setOf(it.id.toInt()))}
        	broker.register(listeners[2], true, setOf(0,1))
			
        	it("0 sends to itself and shared") {
    			broker.dispatchMessage(Message("0", "data"))
    			assertEquals(1, listeners[0].messages.size)
    			assertEquals(0, listeners[1].messages.size)
    			assertEquals(1, listeners[2].messages.size)				
        	}        	

        	it("1 sends to itself and shared") {
    			broker.dispatchMessage(Message("1", "data"))
    			assertEquals(0, listeners[0].messages.size)
    			assertEquals(1, listeners[1].messages.size)
    			assertEquals(1, listeners[2].messages.size)				
        	}        	
			
        	it("2 sends to all") {
    			broker.dispatchMessage(Message("2", "data"))
    			assertEquals(1, listeners[0].messages.size)
    			assertEquals(1, listeners[1].messages.size)
    			assertEquals(1, listeners[2].messages.size)								
        	}        	
        }
				
    }
})