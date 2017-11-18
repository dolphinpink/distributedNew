package ServerCode

import ResourceManagerCode.ReservableItem
import ResourceManagerCode.ReservableType
import ResourceManagerCode.Reservation
import Tcp.*
import Transactions.TransactionalMiddleware
import Transactions.TransactionalRequestReceiver
import Transactions.TransactionalRequestSender
import java.util.*

import kotlin.system.measureTimeMillis



object Tester {

    val customerList: MutableList<Int> = mutableListOf()
    val resourceList: MutableList<String> = mutableListOf()
    val times: MutableMap<Int, MutableList<Long>> = mutableMapOf()

    val finishedLock = Any()
    var finishedCounter = 0

    val random = Random()

    fun rand(from: Int, to: Int) : Int {
        return random.nextInt(to - from) + from
    }

    @JvmStatic
    fun main(args: Array<String>) {

        performanceTest()

        }

    fun performanceTest() {
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
            client.createResource(12, ReservableType.FLIGHT, "1", 50, 5)
            client.createResource(12, ReservableType.HOTEL, "2", 50, 5)
            client.createResource(12, ReservableType.CAR, "3", 50, 5)


            resourceList.add("1")
            resourceList.add("2")
            resourceList.add("3")


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

        var numClients = 2
        while (true){

            if (numClients > 2)
                break

            finishedCounter = 0

            for (i in 1..numClients) {
                Thread {
                    runCustomerSetTime(client, i, numClients)
                }.start()
            }
            while (true) {
                if (finishedCounter == numClients)
                    break
                Thread.sleep(10)
            }
            println("$numClients clients finished")

            numClients *= 2
        }

        println("END\n")

        times.forEach{(numClients, times) -> println("clients $numClients average time ${times.average()}")}
        return

    }

    fun runCustomer(client: TransactionalRequestSender, transactionId: Int, totalClients: Int){
        // The customer transaction types are: add reservation, itinerary
        // We assume all other transaction types are administrative, e.g. not accessible to
        // the average customer.

        for(i in 0..10) {
            client.start(transactionId)

            var timeElapsed = measureTimeMillis {
                client.queryCustomer(transactionId, 1)
            }

            if (times.get(totalClients) == null)
                times.put(totalClients, mutableListOf())

            times.get(totalClients)!!.add(timeElapsed)

            client.commit(transactionId)
            Thread.sleep(100)
        }

        finished()
    }

    fun runCustomerSetTime(client: TransactionalRequestSender, transactionId: Int, totalClients: Int){
        // The customer transaction types are: add reservation, itinerary
        // We assume all other transaction types are administrative, e.g. not accessible to
        // the average customer.

        var customerStartTime = Date().time

        while(true) {
            client.start(transactionId)



            var timeElapsed = measureTimeMillis {


                when (rand(1, 3)) {
                    0 -> {
                        println("$transactionId running query")
                        client.queryResource(transactionId, resourceList.get(rand(0, resourceList.size)))
                    }
                    1,2 -> {
                        println("$transactionId running create")
                        val randId = rand(0, 200).toString()
                        client.createResource(transactionId, ReservableType.FLIGHT, randId , 10, 1000)
                        resourceList.add(randId)
                    }
                    else -> {
                        var size = 0
                        synchronized(resourceList) {
                            size = resourceList.size
                        }
                        if (size > 2) {

                            var resourceId = "-1"
                            synchronized(resourceList) {
                                val index = rand(0, resourceList.size)
                                resourceId = resourceList.get(index)
                                resourceList.removeAt(index)
                            }

                            client.deleteResource(transactionId, resourceId)

                        }
                    }
                }
            }

            /*if (times.get(totalClients) == null)
                times.put(totalClients, mutableListOf())

            times.get(totalClients)!!.add(timeElapsed)*/

            client.commit(transactionId)
            Thread.sleep(100)
            if (Date().time - customerStartTime > 10000)
                break
        }

        finished()
    }

    fun finished() {
        synchronized(finishedLock){
            finishedCounter += 1
        }
    }

}
