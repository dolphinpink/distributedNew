package ServerCode

import Tcp.TcpRequestReceiver
import Tcp.TcpRequestSender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException
import java.net.InetAddress


object Tester {

    val tester = ResourceManagerImpl()

    @JvmStatic
    fun main(args: Array<String>) {

        /*println("try creating customer")
        tester.createCustomer(tester.uniqueCustomerId)
        pp()

        println("try creating two customers with same id")
        println(tester.createCustomer("1"))
        pp()

        println("try creating second customer")
        tester.createCustomer(tester.uniqueCustomerId)
        pp()

        println("try deleting valid customer 1")
        tester.deleteCustomer("1")
        pp()

        println("try deleting invalid customer 5 ")
        tester.deleteCustomer("5")
        pp()

        println("create flight 5")
        tester.createResource(ReservableType.FLIGHT, "5", 5, 500)
        pp()

        println("create car 10")
        tester.createResource(ReservableType.CAR, "10", 10, 50)
        pp()

        println("create hotel 10")
        tester.createResource(ReservableType.HOTEL, "10", 100, 300)
        pp()

        println("update flight 20, 10000")
        tester.updateResource("5", 20, 10000)
        pp()

        println("delete resource")
        tester.deleteResource("5")
        pp()

        val mapper = jacksonObjectMapper()

        val json = mapper.writeValueAsString(ReservableItem(ReservableType.CAR, "10", 20, 500))

        try {
            val state = mapper.readValue<ReservableItem>(json)
            print("extracted $state")
        } catch (e: Exception) {
            println(e)
        }
        */
        val requestReceiver = TcpRequestReceiver(ResourceManagerImpl(), 8080)

        Thread {
            try {
                requestReceiver.runServer()
            } catch (e: IOException) {
                println("receiver failed to start")
            }

        }.start()

        val sender = TcpRequestSender(8080, "127.0.0.1")
        val response = sender.createResource(ReservableType.FLIGHT, "4", 5, 5)
        println("TESTER received response: $response")

        val response2 = sender.queryResource("4")
        println("TESTER query previous resource: $response2")

        val response3 = sender.queryResource("10190")
        println("TESTER query nonexistant resource: $response3")


    }

    fun pp() {
        for (customer in tester.customers) {
            println("$customer")
        }
        for (resource in tester.resources) {
            println("$resource")
        }
        println()
    }
}
