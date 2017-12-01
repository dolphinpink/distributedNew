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


class RequestReceiver(private var rm: ResourceManager): ReceiverAdapter() {

    var channel: JChannel = JChannel() // use the default config, udp.xml
    val mapper = jacksonObjectMapper()

    init {
        mapper.enableDefaultTyping()
    }


    @Throws(Exception::class)
    fun start() {

        channel.receiver = this
        channel.connect(CommunicationsConfig.middlewareRmCluster)
        println("RECEIVER ready")

    }

    override fun viewAccepted(new_view: View) {

        println("** view: " + new_view)

    }


    override fun receive(msg: Message) {

        println("${msg.src}: ${msg.getObject<String>()}")

        try {
            val request = mapper.readValue<RequestCommand>(msg.getObject<String>())
            //println("RECEIVER extracted $request")
            val result = request.execute(rm)
            val resultJson = mapper.writeValueAsString(result)
            //println("RECEIVER sending $json")
            channel.send(null, resultJson)

        } catch (e: Exception) {
            println(e)
        }

    }
}