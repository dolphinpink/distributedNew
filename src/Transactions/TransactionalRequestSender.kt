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

    private val requestHistory: MutableMap<Int, String> = mutableMapOf()

    private val outToServer: PrintWriter
    private val inFromServer: BufferedReader
    private val mapper: ObjectMapper

    private val requestIdLock = Any()
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
                    //println("SENDER RESPONSE LOOP extracted $reply")
                    synchronized(replies) {
                        replies.add(reply)
                    }


                } catch (e: Exception) {
                    println(e)
                }
            }
        }.start()


    }

    fun sendRequest(request: TransactionalRequestCommand): Reply {

        val json = mapper.writeValueAsString(request)
        requestHistory.put(request.requestId, json)
        //println("${request.requestId} submitted $json")
        outToServer.println(json)
        //println("SENDER sent $json")

        val reply = getReply(request.requestId)

        //println("SENDER reply received")

        return reply
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

    fun generateRequestId(): Int {
        synchronized(requestIdLock) {
            requestIdCounter += 1
            return requestIdCounter
        }
    }
    
    fun getBoolean(reply: Reply): Boolean {
        if (reply !is BooleanReply) {
            throw Exception("SENDER Getting BooleanReply failed. got $reply. request was ${requestHistory.get(reply.requestId)}")
        }
        return reply.value
    }

    fun getReply(requestId: Int): Reply {

        var reply: Reply? = null

        var wait = true
        while (wait) {
            synchronized(replies) {
                wait = { reply = replies.find { r -> r.requestId == requestId }; reply }() == null
            }
            Thread.sleep(TuningParameters.CHECK_DELAY)
        }
        synchronized(replies) {
            replies.remove(reply)
        }

        return reply!!
    }
}