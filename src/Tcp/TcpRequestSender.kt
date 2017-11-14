package Tcp

import ResourceManagerCode.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket


class TcpRequestSender(val portNum: Int, val serverName: String, val requestIdStart: Int): ResourceManager {
    companion object {
        val MAX_REQUESTS = 100000
    }

    private val outToServer: PrintWriter
    private val inFromServer: BufferedReader
    private val mapper: ObjectMapper

    private var requestIdCounter: Int = requestIdStart

    private val replies: MutableSet<Reply> = mutableSetOf()

    init {
        val socket = Socket(serverName, portNum) // establish a socket with a server using the given port#
        outToServer = PrintWriter(socket.getOutputStream(), true) // open an output stream to the server...
        inFromServer = BufferedReader(InputStreamReader(socket.getInputStream())) // open an input stream from the server...

        mapper = jacksonObjectMapper()
        mapper.enableDefaultTyping()

        Thread {
            var inputJson: String

            var json: String? = null
            while ({ json = inFromServer.readLine(); json }() != null) {

                try {
                    val reply = mapper.readValue<Reply>(json!!)
                    println("SENDER RESPONSE LOOP extracted $reply")
                    replies.add(reply)

                } catch (e: Exception) {
                    println(e)
                }
            }
        }.start()


    }

    fun sendRequest(request: RequestCommand): Reply {

        val json = mapper.writeValueAsString(request)

        outToServer.println(json)
        println("SENDER sent $json AHAH")

        val reply = getReply(request.requestId)

        println("SENDER reply received")

        return reply

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

    fun generateRequestId(): Int {
        synchronized(requestIdCounter) {
            if (requestIdCounter >= requestIdStart + MAX_REQUESTS - 1) {
                requestIdCounter = requestIdStart
            }
            return requestIdCounter++
        }
    }

    fun getReply(requestId: Int): Reply {

        var reply: Reply? = null

        while({reply = replies.find {r -> r.requestId == requestId}; reply}() == null) {
            Thread.sleep(5)
        }

        replies.remove(reply)

        return reply!!
    }
}