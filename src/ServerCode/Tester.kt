package ServerCode

import ResourceManagerCode.ReservableType
import Tcp.*
import Transactions.TransactionalMiddleware
import Transactions.TransactionalRequestReceiver
import Transactions.TransactionalRequestSender

import kotlin.system.measureTimeMillis



object Tester {

    val tester = ResourceManagerImpl()
    val customerList: MutableList<Int> = mutableListOf()
    val resourceList: MutableList<String> = mutableListOf()

    @JvmStatic
    fun main(args: Array<String>) {

        println(" Performance Analysis Testing: \n")
        println("START: \n")

        val requestReceiverFlight = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.flightRm)
        requestReceiverFlight.runServer()

        val requestReceiverHotel = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.hotelRm)
        requestReceiverHotel.runServer()

        val requestReceiverCar = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.carRm)
        requestReceiverCar.runServer()

        val requestReceiverCustomer = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.customerRm)
        requestReceiverCustomer.runServer()

        Thread.sleep(500)

        val midware = TransactionalRequestReceiver(TransactionalMiddleware("127.0.0.1"), PortNumbers.middleware)
        midware.runServer()

        Thread.sleep(500)

        val client = TransactionalRequestSender(PortNumbers.middleware, "127.0.0.1")

        // Initiate resources to test on
        if (client.start(12)) {
            client.createResource(12, ReservableType.FLIGHT, "fly_1", 50, 5)
            client.createResource(12, ReservableType.HOTEL, "hot_1", 50, 5)
            client.createResource(12, ReservableType.CAR, "car_1", 50, 5)
            client.createResource(12, ReservableType.FLIGHT, "fly_2", 50, 5)
            client.createResource(12, ReservableType.HOTEL, "hot_2", 50, 5)
            client.createResource(12, ReservableType.CAR, "car_2", 50, 5)

            resourceList.add("fly_1")
            resourceList.add("fly_2")
            resourceList.add("car_1")
            resourceList.add("car_2")
            resourceList.add("hot_1")
            resourceList.add("hot_2")

            client.createCustomer(12, 1)
            client.createCustomer(12, 2)
            client.createCustomer(12, 3)

            customerList.add(1)
            customerList.add(2)
            customerList.add(3)

        }

        if (!client.commit(12)){
            println("Your transaction failed.\n")
        }

        var numClients = 1000

        for (i in 0..numClients) {
            Thread {
                runCustomer(client, i)
            }.start()
        }

        println("END\n")

    }


    fun runCustomer(client: TransactionalRequestSender, transactionId: Int){
        // The customer transaction types are: add reservation, itinerary
        // We assume all other transaction types are administrative, e.g. not accessible to
        // the average customer.

        var method = (Math.random()*4).toInt()
        var r1 = (Math.random()*3).toInt()
        var r2 = (Math.random()*10).toInt()
        var r3 = (Math.random()*100).toInt()
        var r4 = (Math.random()*100).toInt()
        var r5 = (Math.random()*10000).toInt()



        var keepgoing = true
        for(i in 0..3) {
            client.start(transactionId)

            var timeElapsed = measureTimeMillis {
                client.queryCustomer(transactionId, 1)
                println("Create customer")
            }
            println("Time Elapsed: $timeElapsed \n")

            client.commit(transactionId)
            Thread.sleep(100)

            r1 = (Math.random()*3).toInt()
            r2 = (Math.random()*10).toInt()
            r3 = (Math.random()*100).toInt()
            r4 = (Math.random()*100).toInt()
            r5 = (Math.random()*10000).toInt()
        }

    }

}
