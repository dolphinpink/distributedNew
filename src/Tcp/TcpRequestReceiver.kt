package Tcp

import ServerCode.ResourceManager
import ServerCode.ResourceManagerImpl
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket


class TcpRequestReceiver(private var rm: ResourceManager, private var socket: Int) {

    @Throws(IOException::class)
    fun runServer() {

        val serverSocket = ServerSocket(socket)
        val clientSocket = serverSocket.accept()
        val outToServer = PrintWriter(clientSocket.getOutputStream(), true)
        val inFromSender = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

        val mapper = jacksonObjectMapper()
        mapper.enableDefaultTyping()

        println("RECEIVER ready")

        var json: String? = null
        while ({ json = inFromSender.readLine(); json }() != null) {

            println("RECEIVER received $json")

            try {
                val request = mapper.readValue<RequestCommand>(json!!)
                print("RECEIVER extracted $request")
                val result = request.execute(rm)
                val storedResource = rm.queryResource("4")
                println("stored resource is $storedResource")

            } catch (e: Exception) {
                println(e)
            }
        }
        println("RECEIVER finished")
    }
}
