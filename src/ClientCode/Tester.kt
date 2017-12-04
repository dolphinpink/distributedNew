package ClientCode

import MiddlewareCode.CommunicationsConfig
import MiddlewareCode.CommunicationsConfig.Companion.CAR_REPLY
import MiddlewareCode.CommunicationsConfig.Companion.CAR_REQUEST
import MiddlewareCode.CommunicationsConfig.Companion.CUSTOMER_REPLY
import MiddlewareCode.CommunicationsConfig.Companion.CUSTOMER_REQUEST
import MiddlewareCode.CommunicationsConfig.Companion.FLIGHT_REPLY
import MiddlewareCode.CommunicationsConfig.Companion.FLIGHT_REQUEST
import MiddlewareCode.CommunicationsConfig.Companion.HOTEL_REPLY
import MiddlewareCode.CommunicationsConfig.Companion.HOTEL_REQUEST
import ResourceManagerCode.ReservableType
import ResourceManagerCode.ResourceManagerImpl
import Transactions.TransactionalMiddleware
import Transactions.TransactionalRequestSender
import java.util.*

import kotlin.system.measureTimeMillis
import java.util.logging.Level
import java.util.logging.LogManager

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

        val rootLogger = LogManager.getLogManager().getLogger("")
        rootLogger.level = Level.SEVERE
        for (h in rootLogger.handlers) {
            h.level = Level.SEVERE
        }

        ResourceManagerImpl(CUSTOMER_REQUEST, CUSTOMER_REPLY)
        ResourceManagerImpl(FLIGHT_REQUEST, FLIGHT_REPLY)
        ResourceManagerImpl(HOTEL_REQUEST, HOTEL_REPLY)
        ResourceManagerImpl(CAR_REQUEST, CAR_REPLY)

        ResourceManagerImpl(CUSTOMER_REQUEST, CUSTOMER_REPLY)
        ResourceManagerImpl(FLIGHT_REQUEST, FLIGHT_REPLY)
        ResourceManagerImpl(HOTEL_REQUEST, HOTEL_REPLY)
        ResourceManagerImpl(CAR_REQUEST, CAR_REPLY)

        TransactionalMiddleware(0)
        TransactionalMiddleware(0)

        //val requestReceiverFlight2 = TransactionalMiddleware(1)

        val requestSender = TransactionalRequestSender(7)
        println("\n\n\n\n")
        Thread.sleep(100)

        requestSender.start(1)
        println("\n\n\n\n")
        Thread.sleep(100)

        requestSender.createCustomer(1, 1)
        println("\n\n\n\n")
        Thread.sleep(100)

        requestSender.createResource(1, ReservableType.FLIGHT, "flight", 100, 1000)


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
