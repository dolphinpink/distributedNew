package Transactions

import MiddlewareCode.CommunicationsConfig
import MiddlewareCode.RequestCommand
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

class TransactionalRequestReceiver(private val rm: TransactionalResourceManager): ReceiverAdapter() {

    val requestChannel: JChannel = JChannel()
    val replyChannel: JChannel = JChannel()
    val mapper = jacksonObjectMapper()

    init {
        mapper.enableDefaultTyping()
        replyChannel.connect(CommunicationsConfig.CM_REPLY_CLUSTER)

        requestChannel.receiver = this
        requestChannel.connect(CommunicationsConfig.CM_REQUEST_CLUSTER)

        println("RECEIVER ready")
    }

    override fun viewAccepted(new_view: View) {
        //println("** view: " + new_view)
    }


    override fun receive(msg: Message) {

        println("${msg.src}: ${msg.getObject<String>()}")

        try {
            val request = mapper.readValue<TransactionalRequestCommand>(msg.getObject<String>())
            println("MIDDLEWARE RECEIVER extracted $request")
            val result = request.execute(rm)
            val resultJson = mapper.writeValueAsString(result)
            println("MIDDLEWARE RECEIVER sending $resultJson")
            replyChannel.send(null, resultJson)

        } catch (e: Exception) {
            println(e)
        }


    }
}