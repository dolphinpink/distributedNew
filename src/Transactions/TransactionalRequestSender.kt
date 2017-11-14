package Transactions

import ResourceManagerCode.*
import Tcp.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket


class TransactionalRequestSender(val portNum: Int, val serverName: String): TransactionalResourceManager {



    private val outToServer: PrintWriter
    private val inFromServer: BufferedReader
    private val mapper: ObjectMapper

    private var requestIdCounter: Int = 0

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

    override fun start(transactionId: Int): Boolean {

        val reply = sendRequest(TransactionalStartRequest(generateRequestId(), transactionId))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun commit(transactionId: Int): Boolean {
        val reply = sendRequest(TransactionalCommitRequest(generateRequestId(), transactionId))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun abort(transactionId: Int): Boolean {
        val reply = sendRequest(TransactionalAbortRequest(generateRequestId(), transactionId))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    fun sendRequest(request: TransactionalRequestCommand): Reply {

        val json = mapper.writeValueAsString(request)

        outToServer.println(json)
        println("SENDER sent $json")

        val reply = getReply(request.requestId)

        println("SENDER reply received")

        return reply
    }

    override fun createResource(transactionId: Int, type: ReservableType, id: String, totalQuantity: Int, price: Int): Boolean {

        val reply = sendRequest(TransactionalCreateResourceRequest(generateRequestId(), transactionId, type, id, totalQuantity, price))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun updateResource(transactionId: Int, id: String, newTotalQuantity: Int, newPrice: Int): Boolean {

        val reply = sendRequest(TransactionalUpdateResourceRequest(generateRequestId(), transactionId, id, newTotalQuantity, newPrice))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun reserveResource(transactionId: Int, resourceId: String, reservationQuantity: Int): Boolean {
        val reply = sendRequest(TransactionalReserveResourceRequest(generateRequestId(), transactionId, resourceId, reservationQuantity))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun deleteResource(transactionId: Int, id: String): Boolean {

        val reply = sendRequest(TransactionalDeleteResourceRequest(generateRequestId(), transactionId, id))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value

    }

    override fun queryResource(transactionId: Int, resourceId: String): Resource? {

        val reply = sendRequest(TransactionalQueryResourceRequest(generateRequestId(), transactionId, resourceId))

        if (reply !is ResourceReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun uniqueCustomerId(transactionId: Int): Int {
        val reply = sendRequest(TransactionalUniqueCustomerIdRequest(generateRequestId(), transactionId))

        if (reply !is IntReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun createCustomer(transactionId: Int, customerId: Int): Boolean {
        val reply = sendRequest(TransactionalCreateCustomerRequest(generateRequestId(), transactionId, customerId))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun deleteCustomer(transactionId: Int, customerId: Int): Boolean {
        val reply = sendRequest(TransactionalDeleteCustomerRequest(generateRequestId(), transactionId, customerId))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun customerAddReservation(transactionId: Int, customerId: Int, reservationId: Int, reservableItem: ReservableItem): Boolean {
        val reply = sendRequest(TransactionalCustomerAddReservationRequest(generateRequestId(), transactionId, customerId, reservationId, reservableItem))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun customerRemoveReservation(transactionId: Int, customerId: Int, reservationId: Int): Boolean {
        val reply = sendRequest(TransactionalCustomerRemoveReservationRequest(generateRequestId(), transactionId, customerId, reservationId))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun queryCustomer(transactionId: Int, customerId: Int): Customer? {
        val reply = sendRequest(TransactionalQueryCustomerRequest(generateRequestId(), transactionId, customerId))

        if (reply !is CustomerReply)
            throw Exception("SENDER Remote failed")

        return reply.value
    }

    override fun itinerary(transactionId: Int, customerId: Int, reservationResources: MutableMap<Int, ReservableItem>): Boolean {

        val reply = sendRequest(TransactionalItineraryRequest(generateRequestId(), transactionId, customerId, reservationResources))

        if (reply !is BooleanReply)
            throw Exception("SENDER Remote failed")

        return reply.value

    }

    fun generateRequestId(): Int {
        synchronized(requestIdCounter) {
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