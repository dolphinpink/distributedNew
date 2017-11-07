package Tcp


import ServerCode.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket


class TcpRequestSender(val portNum: Int, val serverName: String)/*: ResourceManager*/ {

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
                    print("extracted $reply")
                    replies.add(reply)

                } catch (e: Exception) {
                    println(e)
                }
            }
        }.start()


    }

    private var customerIdGenerator: Long = 0L

    fun createResource(type: ReservableType, id: String, totalQuantity: Int, price: Int): Boolean {

        val requestId = generateRequestId()
        val json = mapper.writeValueAsString(CreateResourceRequest(requestId, type, id, totalQuantity, price))
        outToServer.println(json)
        println("SENDER sent $json")

        val reply: Reply = getReply(requestId)

        println("reply received")

        if (reply !is BooleanReply)
            throw Exception("Remote failed")

        return reply.reply
    }

    /*override fun updateResource(id: String, newTotalQuantity: Int, newPrice: Int): Boolean {

    }

    override fun deleteResource(id: String): Boolean {
        synchronized(resourceLock) {
            return resources.remove(resources.find { r -> r.item.id == id })
        }
    }

    override fun queryResource(resourceId: String): Int {
        return resources.find {r -> r.item.id == resourceId} ?.remainingQuantity ?: -1
    }

    override fun getUniqueCustomerId(): String {
        synchronized(customerIdGenerator) {
            customerIdGenerator +=1
            return customerIdGenerator.toString()
        }
    }

    override fun createCustomer(customerId: String): Boolean {
        synchronized(customerLock) {
            return customers.add(Customer(customerId))
        }
    }

    override fun deleteCustomer(customerId: String): Boolean {
        synchronized(customerLock) {
            return customers.remove(customers.find { c -> c.customerId == customerId})
        }
    }

    override fun queryCustomerInfo(customerId: String): String {
        return customers.find { c -> c.customerId == customerId}.toString()
    }

    override fun reserveResource(customerId: String?, type: ReservableType?, resourceId: String?): Boolean {
        TODO("not implemented")
    }

    override fun itinerary(customerId: String?, resourceIds: MutableSet<String>?): Boolean {
        TODO("not implemented")
    }*/

    fun generateRequestId(): Int {
        synchronized(requestIdCounter) {
            return requestIdCounter++
        }
    }

    fun getReply(requestId: Int): Reply {

        var reply: Reply? = null

        while({reply = replies.find {r -> r.requestId == requestId}; reply}() == null) {
            Thread.sleep(50)
        }

        replies.remove(reply)

        return reply!!
    }
}