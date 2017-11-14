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

        var numClients = 5

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

        var method = (Math.random()*15).toInt()
        var r1 = (Math.random()*3).toInt()
        var r2 = (Math.random()*10).toInt()
        var r3 = (Math.random()*100).toInt()
        var r4 = (Math.random()*100).toInt()
        var r5 = (Math.random()*10000).toInt()

        client.start(transactionId)

        var keepgoing = true
        while (keepgoing) {

            when (method) {
                0-> {
                    var timeElapsed = measureTimeMillis {
                        client.createCustomer(transactionId, r3)
                        customerList.add(r3)
                        println("Create customer")
                    }
                    println("Time Elapsed: $timeElapsed \n")
                }
                1-> {
                    var timeElapsed = measureTimeMillis {
                        client.deleteCustomer(transactionId, customerList[r1])
                        customerList.removeAt(r1)
                        println("Delete customer")
                    }
                    println("Time Elapsed: $timeElapsed \n")
                }
                2 -> {
                    var timeElapsed = measureTimeMillis {
                        val type = when(r1) {
                            0 -> ReservableType.FLIGHT
                            1 -> ReservableType.CAR
                            2 -> ReservableType.HOTEL
                            else -> ReservableType.FLIGHT
                        }
                        client.createResource(transactionId, type, r5.toString(), r2, r3)
                        resourceList.add(r5.toString())
                        println("Create resource")
                    }
                    println("Time Elapsed: $timeElapsed \n")
                }
                3 -> {
                    var timeElapsed = measureTimeMillis {
                        client.deleteResource(transactionId, resourceList[r1])
                        resourceList.removeAt(r1)
                        println("Delete resource")
                    }
                    println("Time Elapsed: $timeElapsed \n")
                }
                4 -> {
                    var timeElapsed = measureTimeMillis {
                        val resource = client.queryResource(transactionId, resourceList[r1])
                        if (resource != null) {
                            client.customerAddReservation(transactionId, customerList[r1], r1, resource.item)
                            client.reserveResource(transactionId, resource.item.id, 1)
                            println("Add reservation response: $resource")
                        }
                    }
                    println("Time Elapsed: $timeElapsed \n")

                }
                5,6 -> {
                    var timeElapsed = measureTimeMillis {
                        client.commit(transactionId)
                        keepgoing = false
                        println("Commit response: $keepgoing")
                    }
                    println("Time Elapsed: $timeElapsed \n")
                }

                else -> Thread.sleep(r4.toLong())
            }

            r1 = (Math.random()*3).toInt()
            r2 = (Math.random()*10).toInt()
            r3 = (Math.random()*100).toInt()
            r4 = (Math.random()*100).toInt()
            r5 = (Math.random()*10000).toInt()
        }


        /*
        if(client.start(transactionId)) {
            client.createCustomer(transactionId, )
            if(!client.commit(transactionId)){
                println("Create customer commit failed. \n")
            }
        } else {
            println("Create customer transaction failed. \n")
        }
        for (i in 0..100) {
            when(r1) {
                0 -> client.
            }
        }
        for (i in 0..5) {
            // Random variables for the resource type, sleep time, and randomized method, respectively.
            r1 = (Math.random()*3).toInt()
            r2 = (Math.random()*400).toInt() + 300
            r3 = (Math.random()*2).toInt() + 1
            var timeElapsed = measureTimeMillis {
                if (r3 == 1) {
                    if(client.start(counter)) {
                        var response = client.queryResource(counter, resourceIds[r1])
                        println("Iteration $i query response: $response")
                        if (response != null) {
                            response2 = client.customerAddReservation(counter, custID, counter, response.item)
                            client.reserveResource(counter, resourceIds[r1], 1)
                            println("Iteration $i reserve response: $response2")
                        }
                        if(!client.commit(counter)){
                            println("Iteration $i commit failed. \n")
                        }
                    } else {
                        println("Iteration $i transaction failed. \n")
                    }
                } else if (r3 == 2) {
                    if(client.start(counter)) {
                        var resource1 = client.queryResource(counter, resourceIds[r1])
                        r1 = (Math.random() * 3).toInt()
                        var resource2 = client.queryResource(counter, resourceIds[r1])
                        if (resource1 != null && resource2 != null) {
                            response3 = client.itinerary(counter, custID, mutableMapOf(1 to resource1.item, 2 to resource2.item))
                        }
                        println("Iteration $i itinerary response: $response3")
                        if(!client.commit(counter)){
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
        }*/

    }

}
