package MiddlewareCode

import ResourceManagerCode.ResourceManager
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jgroups.JChannel
import org.jgroups.Message
import org.jgroups.ReceiverAdapter
import org.jgroups.View
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket


class RequestReceiver(private val rm: ResourceManager, val requestChannelName: String, val replyChannelName: String): ReceiverAdapter() {

    val requestChannel: JChannel = JChannel()
    val replyChannel: JChannel = JChannel()
    val mapper = jacksonObjectMapper()

    val receivedRequestIds: MutableSet<Int> = mutableSetOf()

    init {
        mapper.enableDefaultTyping()
        replyChannel.connect(replyChannelName)

        requestChannel.receiver = this
        requestChannel.connect(requestChannelName)

        println("RECEIVER ready")
    }


    override fun viewAccepted(new_view: View) {}

    override fun receive(msg: Message) {

        println("${msg.src}: ${msg.getObject<String>()}")

        try {
            val request = mapper.readValue<RequestCommand>(msg.getObject<String>())
            println("RM $requestChannelName extracted $request")
            if (!receivedRequestIds.contains(request.requestId)) {
                receivedRequestIds.add(request.requestId)
                val result = request.execute(rm)
                val resultJson = mapper.writeValueAsString(result)
                println("RM $requestChannelName sending $resultJson")
                replyChannel.send(null, resultJson)
            }


        } catch (e: Exception) {
            println(e)
        }


    }
}