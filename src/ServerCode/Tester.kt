package ServerCode

import LockManagerCustom.LockManager
import LockManagerCustom.LockType
import ResourceManagerCode.ReservableType
import Tcp.*
import Transactions.TransactionalMiddleware
import Transactions.TransactionalRequestReceiver
import Transactions.TransactionalRequestSender
import kotlin.concurrent.thread


object Tester {

    val tester = ResourceManagerImpl()

    @JvmStatic
    fun main(args: Array<String>) {

        //testBasics()
        //testMiddleware()
        //testLockManager()
        testTransactions()

    }

    fun testTransactions() {
        val requestReceiverFlight = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.flightRm)
        requestReceiverFlight.runServer()

        val requestReceiverHotel = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.hotelRm)
        requestReceiverHotel.runServer()

        val requestReceiverCar = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.carRm)
        requestReceiverCar.runServer()

        val requestReceiverCustomer = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.customerRm)
        requestReceiverCustomer.runServer()

        Thread.sleep(500)

        val middleware = TransactionalRequestReceiver(TransactionalMiddleware("127.0.0.1"), PortNumbers.middleware)
        middleware.runServer()

        Thread.sleep(500)

        val client = TransactionalRequestSender(PortNumbers.middleware, "127.0.0.1")

        client.createResource(1, ReservableType.FLIGHT, "fly_1", 50, 500)

        client.start(3)

        client.createResource(3, ReservableType.FLIGHT, "fly_1", 50, 500)



        val resource = client.queryResource(3, "fly_1")
        println("\n\n\nresource 1 $resource")

        client.abort(3)


        println("\n\n\n")
        val resource2 = client.queryResource(3, "fly_1")
        println("\n\n\nresource 2 $resource2")

        println("\n\n DONE")


    }

    fun testLockManager() {
        val lm = LockManager()

        val r9 = lm.lock(3, "a", LockType.READ)
        println("3 acquired A_read: $r9")

        thread {
            val r1 = lm.lock(1, "a", LockType.WRITE)
            println("1 acquired A_write: $r1")
        }

        val r10 = lm.unlockAll(3);
        println("3 unlocked all: $r10")

        thread {
            val r2 = lm.lock(2, "a", LockType.WRITE)
            println("2 acquired A_write: $r2")
        }

        val r4 = lm.lock(1, "a", LockType.READ)
        println("1 acquired A_read: $r4")

        val r5 = lm.lock(1, "a", LockType.WRITE)
        println("1 acquired A_write: $r5")

        Thread.sleep(3000)
        val r3 = lm.unlockAll(1)
        println("1 unlocked all: $r3")


    }

    fun testMiddleware() {

        val requestReceiverFlight = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.flightRm)
        requestReceiverFlight.runServer()


        val requestReceiverHotel = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.hotelRm)
        requestReceiverHotel.runServer()


        val requestReceiverCar = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.carRm)
        requestReceiverCar.runServer()

        Thread.sleep(500)


        val middleware = TcpRequestReceiver(Middleware("127.0.0.1", 0), PortNumbers.middleware)
        middleware.runServer()

        val client = TcpRequestSender(PortNumbers.middleware, "127.0.0.1", 0)



        println("\n\n\n TEST 1")
        val response = client.createResource(ReservableType.FLIGHT, "fly_1", 5, 5)
        println("TESTER received response: $response")




        println("\n\n\n TEST 2")
        val response2 = client.queryResource("fly_1")
        val resource1 = response2
        println("TESTER query previous resource: $response2")



        println("\n\n\n TEST 3")
        val response3 = client.queryResource("hotel_5")
        println("TESTER query nonexistant resource: $response3")



        println("\n\n\n TEST 4")
        val response4 = client.createCustomer(5)
        println("TESTER created customer: $response4")



        println("\n\n\n TEST 5")
        var response5: Boolean = false
        if (resource1 != null) {
            response5 = client.customerAddReservation(5, 12, resource1.item)
        }
        println("TESTER response is $response5")



        println("\n\n\n TEST 6")
        val response6 = client.queryCustomer(5)?.reservations
        println("TESTER customer now has $response6.")



        println("\n\n\n TEST 7 itinerary")
        client.createResource(ReservableType.CAR, "car_1", 10, 1)

        val resource2 = client.queryResource("car_1")
        if (resource1 != null && resource2 != null) {
            client.itinerary(5, mutableMapOf(1 to resource1.item, 2 to resource2.item))
        }
        val newReservations = client.queryCustomer(5)?.reservations
        println("TESTER customer now has $newReservations.")

    }

    fun testBasics() {
        println("try creating customer")
        tester.createCustomer(tester.uniqueCustomerId())
        pp()

        println("try creating two customers with same id")
        println(tester.createCustomer(1))
        pp()

        println("try creating second customer")
        tester.createCustomer(tester.uniqueCustomerId())
        pp()

        println("try deleting valid customer 1")
        tester.deleteCustomer(1)
        pp()

        println("try deleting invalid customer 5 ")
        tester.deleteCustomer(5)
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
