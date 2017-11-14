package ServerCode

import LockManagerCustom.LockManager
import LockManagerCustom.LockType
import ResourceManagerCode.ReservableType
import Tcp.*
import Transactions.TransactionalMiddleware
import Transactions.TransactionalRequestReceiver
import Transactions.TransactionalRequestSender
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis


object Tester {

    val tester = ResourceManagerImpl()

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
        if (client.start(12) == true) {
            client.createResource(12, ReservableType.FLIGHT, "fly_1", 50, 5)
            client.createResource(12, ReservableType.HOTEL, "hot_1", 50, 5)
            client.createResource(12, ReservableType.CAR, "car_1", 50, 5)
        }
        if (client.commit(12) == false){
            println("Your transaction failed.\n")
        }

        var clients = 1

        if (clients == 1){
            customer(client, 1)
        } else {

            Thread {
                customer(client, 2)
            }.start()
            Thread {
                customer(client, 3)
            }.start()
            Thread {
                customer(client, 4)
            }.start()
        }

        println("END\n")

    }


    fun customer(middleware: TransactionalRequestSender, custID: Int){
        // The customer transaction types are: add reservation, itinerary
        // We assume all other transaction types are administrative, e.g. not accessible to
        // the average customer.

        val resourceIds = arrayOf("car_1", "fly_1", "hot_1")
        var r1 = 0
        var r2 = 0
        var r3 = 0
        var counter = 1
        var response2 = false
        var response3 = false
        if(middleware.start(999)) {
            middleware.createCustomer(999, custID)
            if(!middleware.commit(999)){
                println("Create customer commit failed. \n")
            }
        } else {
            println("Create customer transaction failed. \n")
        }

        for (i in 0..5) {
            // Random variables for the resource type, sleep time, and randomized method, respectively.
            r1 = (Math.random()*3).toInt()
            r2 = (Math.random()*400).toInt() + 300
            r3 = (Math.random()*2).toInt() + 1

            var timeElapsed = measureTimeMillis {
                if (r3 == 1) {
                    if(middleware.start(counter)) {
                        var response = middleware.queryResource(counter, resourceIds[r1])
                        println("Iteration $i query response: $response")

                        if (response != null) {
                            response2 = middleware.customerAddReservation(counter, custID, counter, response.item)
                            middleware.reserveResource(counter, resourceIds[r1], 1)
                            println("Iteration $i reserve response: $response2")
                        }
                        if(!middleware.commit(counter)){
                            println("Iteration $i commit failed. \n")
                        }
                    } else {
                        println("Iteration $i transaction failed. \n")
                    }
                } else if (r3 == 2) {
                    if(middleware.start(counter)) {
                        var resource1 = middleware.queryResource(counter, resourceIds[r1])
                        r1 = (Math.random() * 3).toInt()
                        var resource2 = middleware.queryResource(counter, resourceIds[r1])
                        if (resource1 != null && resource2 != null) {
                            response3 = middleware.itinerary(counter, custID, mutableMapOf(1 to resource1.item, 2 to resource2.item))
                        }
                        println("Iteration $i itinerary response: $response3")
                        if(!middleware.commit(counter)){
                            println("Iteration $i commit failed. \n")
                        }
                    } else {
                        println("Iteration $i transaction failed. \n")
                    }
                }
            }
            println("Time Elapsed: $timeElapsed \n")
            counter++
            if (custID > 1) {
                Thread.sleep(r2.toLong())
            }
        }

    }

}

