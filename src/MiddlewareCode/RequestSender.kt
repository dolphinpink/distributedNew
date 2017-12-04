package MiddlewareCode

import ResourceManagerCode.*
import Transactions.TuningParameters
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jgroups.JChannel
import org.jgroups.Message
import org.jgroups.ReceiverAdapter
import org.jgroups.View
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket


class RequestSender(val senderId: Int, val requestChannelName: String, val replyChannelName: String): ResourceManager, ReceiverAdapter() {

    val requestChannel: JChannel = JChannel()
    val replyChannel: JChannel = JChannel()

    private val replies: MutableSet<Reply> = mutableSetOf()
    private val alreadyReceived: MutableSet<Int> = mutableSetOf()
    private val waitingCalls: MutableMap<Int, Any> = mutableMapOf()

    private val mapper: ObjectMapper = jacksonObjectMapper()

    private val requestIdLock = Any()
    private var requestIdCounter: Int = senderId * CommunicationsConfig.REQUEST_ID_OFFSET
    private val requestIdStart = requestIdCounter

    init {
        mapper.enableDefaultTyping()
        requestChannel.connect(requestChannelName)

        replyChannel.receiver = this
        replyChannel.connect(replyChannelName)

       //println("SENDER ready")
    }

    override fun createResource(type: ReservableType, id: String, totalQuantity: Int, price: Int): Boolean {

        val reply = sendRequest(CreateResourceRequest(generateRequestId(), type, id, totalQuantity, price))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun updateResource(id: String, newTotalQuantity: Int, newPrice: Int): Boolean {

        val reply = sendRequest(UpdateResourceRequest(generateRequestId(), id, newTotalQuantity, newPrice))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun reserveResource(resourceId: String, reservationQuantity: Int): Boolean {
        val reply = sendRequest(ReserveResourceRequest(generateRequestId(), resourceId, reservationQuantity))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun deleteResource(id: String): Boolean {

        val reply = sendRequest(DeleteResourceRequest(generateRequestId(), id))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value

    }

    override fun queryResource(resourceId: String): Resource? {

        val reply = sendRequest(QueryResourceRequest(generateRequestId(), resourceId))

        if (reply !is ResourceReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun uniqueCustomerId(): Int {
        val reply = sendRequest(UniqueCustomerIdRequest(generateRequestId()))

        if (reply !is IntReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun createCustomer(customerId: Int): Boolean {
        val reply = sendRequest(CreateCustomerRequest(generateRequestId(), customerId))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun deleteCustomer(customerId: Int): Boolean {
        val reply = sendRequest(DeleteCustomerRequest(generateRequestId(), customerId))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun customerAddReservation(customerId: Int, reservationId: Int, reservableItem: ReservableItem): Boolean {
        val reply = sendRequest(CustomerAddReservationRequest(generateRequestId(), customerId, reservationId, reservableItem))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun customerRemoveReservation(customerId: Int, reservationId: Int): Boolean {
        val reply = sendRequest(CustomerRemoveReservationRequest(generateRequestId(), customerId, reservationId))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun queryCustomer(customerId: Int): Customer? {
        val reply = sendRequest(QueryCustomerRequest(generateRequestId(), customerId))

        if (reply !is CustomerReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun itinerary(customerId: Int, reservationResources: MutableMap<Int, ReservableItem>): Boolean {

        val reply = sendRequest(ItineraryRequest(generateRequestId(), customerId, reservationResources))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value

    }

    override fun viewAccepted(new_view: View) {

        //println("** view: " + new_view)

    }

    override fun receive(msg: Message) {

       //println("${msg.src}: ${msg.getObject<String>()}")

        try {
            val reply = mapper.readValue<Reply>(msg.getObject<String>())
           //println("MIDDLEWARE received reply $reply")
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

    fun sendRequest(request: RequestCommand): Reply {

        val json = mapper.writeValueAsString(request)

       //println("MIDDLEWARE sending request $json")

        requestChannel.send(null, json)

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