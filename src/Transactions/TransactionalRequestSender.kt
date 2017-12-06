package Transactions

import ResourceManagerCode.*
import MiddlewareCode.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jgroups.JChannel
import org.jgroups.Message
import org.jgroups.ReceiverAdapter
import org.jgroups.View


class TransactionalRequestSender(val senderId: Int): TransactionalResourceManager, ReceiverAdapter() {

    private val requestHistory: MutableMap<Int, String> = mutableMapOf() // for testing

    val requestChannel: JChannel = JChannel()
    val replyChannel: JChannel = JChannel()

    private val replies: MutableSet<Reply> = mutableSetOf()
    private val alreadyReceived: MutableSet<Int> = mutableSetOf()
    private val waitingCalls: MutableMap<Int, Any> = mutableMapOf()

    private val mapper: ObjectMapper = jacksonObjectMapper()

    private val requestIdLock = Any()
    private val requestIdStart = senderId * CommunicationsConfig.REQUEST_ID_OFFSET
    private var requestIdCounter: Int = requestIdStart

    init {
        mapper.enableDefaultTyping()
        requestChannel.connect(CommunicationsConfig.CM_REQUEST_CLUSTER)

        replyChannel.receiver = this
        replyChannel.connect(CommunicationsConfig.CM_REPLY_CLUSTER)

       //println("SENDER ready")
    }

    override fun start(transactionId: Int): Boolean {
        val reply = sendRequest(TransactionalStartRequest(generateRequestId(), transactionId))
        return getBoolean(reply)
    }

    override fun commit(transactionId: Int): Boolean {
        val reply = sendRequest(TransactionalCommitRequest(generateRequestId(), transactionId))
        return getBoolean(reply)
    }

    override fun abort(transactionId: Int): Boolean {
        val reply = sendRequest(TransactionalAbortRequest(generateRequestId(), transactionId))
        return getBoolean(reply)
    }

    override fun createResource(transactionId: Int, type: ReservableType, id: String, totalQuantity: Int, price: Int): Boolean {
        val reply = sendRequest(TransactionalCreateResourceRequest(generateRequestId(), transactionId, type, id, totalQuantity, price))
        return getBoolean(reply)
    }

    override fun updateResource(transactionId: Int, id: String, newTotalQuantity: Int, newPrice: Int): Boolean {
        val reply = sendRequest(TransactionalUpdateResourceRequest(generateRequestId(), transactionId, id, newTotalQuantity, newPrice))
        return getBoolean(reply)
    }

    override fun reserveResource(transactionId: Int, resourceId: String, reservationQuantity: Int): Boolean {
        val reply = sendRequest(TransactionalReserveResourceRequest(generateRequestId(), transactionId, resourceId, reservationQuantity))
        return getBoolean(reply)
    }

    override fun deleteResource(transactionId: Int, id: String): Boolean {
        val reply = sendRequest(TransactionalDeleteResourceRequest(generateRequestId(), transactionId, id))
        return getBoolean(reply)
    }

    override fun queryResource(transactionId: Int, resourceId: String): Resource? {

        val reply = sendRequest(TransactionalQueryResourceRequest(generateRequestId(), transactionId, resourceId))

        if (reply !is ResourceReply)
            throw Exception("SENDER Getting ResourceReply failed. got $reply. request was ${requestHistory.get(reply.requestId)}")

        return reply.value
    }

    override fun uniqueCustomerId(transactionId: Int): Int {
        val reply = sendRequest(TransactionalUniqueCustomerIdRequest(generateRequestId(), transactionId))

        if (reply !is IntReply)
            throw Exception("SENDER Getting IntReply failed. got $reply. request was ${requestHistory.get(reply.requestId)}")

        return reply.value
    }

    override fun createCustomer(transactionId: Int, customerId: Int): Boolean {
        val reply = sendRequest(TransactionalCreateCustomerRequest(generateRequestId(), transactionId, customerId))
        return getBoolean(reply)
    }

    override fun deleteCustomer(transactionId: Int, customerId: Int): Boolean {
        val reply = sendRequest(TransactionalDeleteCustomerRequest(generateRequestId(), transactionId, customerId))
        return getBoolean(reply)
    }

    override fun customerAddReservation(transactionId: Int, customerId: Int, reservationId: Int, resourceId: String): Boolean {
        val reply = sendRequest(TransactionalCustomerAddReservationRequest(generateRequestId(), transactionId, customerId, reservationId, resourceId))
        return getBoolean(reply)
    }

    override fun customerRemoveReservation(transactionId: Int, customerId: Int, reservationId: Int): Boolean {
        val reply = sendRequest(TransactionalCustomerRemoveReservationRequest(generateRequestId(), transactionId, customerId, reservationId))
        return getBoolean(reply)
    }

    override fun queryCustomer(transactionId: Int, customerId: Int): Customer? {
        val reply = sendRequest(TransactionalQueryCustomerRequest(generateRequestId(), transactionId, customerId))

        if (reply !is CustomerReply)
            throw Exception("SENDER Getting CustomerReply failed. got $reply. request was ${requestHistory.get(reply.requestId)}")

        return reply.value
    }

    override fun itinerary(transactionId: Int, customerId: Int, reservationResources: MutableMap<Int, String>): Boolean {
        val reply = sendRequest(TransactionalItineraryRequest(generateRequestId(), transactionId, customerId, reservationResources))
        return getBoolean(reply)
    }

    override fun shutdown(name: String) {
        sendRequest(TransactionalShutdownRequest(generateRequestId(), name))
    }
    
    fun getBoolean(reply: Reply): Boolean {
        if (reply !is BooleanReply) {
            throw Exception("SENDER Getting BooleanReply failed. got $reply. request was ${requestHistory.get(reply.requestId)}")
        }
        return reply.value
    }

    override fun viewAccepted(new_view: View) {

        //println("** view: " + new_view)

    }

    override fun receive(msg: Message) {

       //println("${msg.src}: ${msg.getObject<String>()}")

        try {
            val reply = mapper.readValue<Reply>(msg.getObject<String>())
           //println("SENDER received reply $reply")
            synchronized(alreadyReceived) {
                synchronized(replies) {
                    if (requestIdInRange(reply.requestId) && !alreadyReceived.contains(reply.requestId) ) {
                        alreadyReceived.add(reply.requestId)
                        replies.add(reply)
                        val notifier = waitingCalls[reply.requestId] ?: throw Exception ("trying to notify something that isn't being waited on $reply")
                        synchronized(notifier) {
                            notifier.notifyAll()
                        }

                       //println("notified ${reply.requestId}")
                    }
                }
            }

        } catch (e: Exception) {
           //println(e)
        }

    }

    fun sendRequest(request: TransactionalRequestCommand): Reply {

        val json = mapper.writeValueAsString(request)

        println("SENDER sending request $json")

        requestChannel.send(null, json)

        println("SENDER messsage sent")

        return getReply(request.requestId)
    }

    fun getReply(requestId: Int): Reply {

        val notifier = Any()

        waitingCalls.put(requestId, notifier)

        synchronized(notifier) {
            notifier.wait()
        }

        var reply: Reply? = replies.find {r -> r.requestId == requestId} ?: throw Exception("Can't find reply")

        return reply!!
    }

    fun generateRequestId(): Int {
        synchronized(requestIdLock) {
            if (requestIdCounter >= requestIdStart + CommunicationsConfig.REQUEST_ID_OFFSET - 1) {
                requestIdCounter = requestIdStart
            }
            return requestIdCounter++
        }
    }

    fun requestIdInRange(requestId: Int): Boolean {
        return requestId >= requestIdStart && requestId <= requestIdStart + CommunicationsConfig.REQUEST_ID_OFFSET
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun Any.wait() = (this as java.lang.Object).wait()

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun Any.notifyAll() = (this as java.lang.Object).notifyAll()


}