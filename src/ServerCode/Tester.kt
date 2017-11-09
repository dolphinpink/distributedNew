package ServerCode

import ClientCode.Middleware
import ResourceManagerCode.ReservableType
import ResourceManagerCode.Resource
import Tcp.PortNumbers
import Tcp.TcpRequestReceiver
import Tcp.TcpRequestSender
import java.io.IOException


object Tester {

    val tester = ResourceManagerImpl()

    @JvmStatic
    fun main(args: Array<String>) {

        //testBasics()

        testMiddleware()

    }

    fun testMiddleware() {

        val requestReceiverFlight = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.flightRm)
        requestReceiverFlight.runServer()


        val requestReceiverHotel = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.hotelRm)
        requestReceiverHotel.runServer()


        val requestReceiverCar = TcpRequestReceiver(ResourceManagerImpl(), PortNumbers.carRm)
        requestReceiverCar.runServer()

        Thread.sleep(500)


        val middleware = Middleware("127.0.0.1")



        println("\n\n\n TEST 1")
        val response = middleware.createResource(ReservableType.FLIGHT, "fly_1", 5, 5)
        println("TESTER received response: $response")




        println("\n\n\n TEST 2")
        val response2 = middleware.queryResource("fly_1")
        val resource1 = response2
        println("TESTER query previous resource: $response2")



        println("\n\n\n TEST 3")
        val response3 = middleware.queryResource("hotel_5")
        println("TESTER query nonexistant resource: $response3")



        println("\n\n\n TEST 4")
        val response4 = middleware.createCustomer(5)
        println("TESTER created customer: $response4")



        println("\n\n\n TEST 5")
        var response5: Boolean = false
        if (resource1 != null) {
            response5 = middleware.customerAddReservation(5, 12, resource1.item, 1)
        }
        println("TESTER response is $response5")



        println("\n\n\n TEST 6")
        val response6 = middleware.queryCustomer(5)?.reservations
        println("TESTER customer now has $response6.")



        println("\n\n\n TEST 7 itinerary")
        middleware.createResource(ReservableType.CAR, "car_1", 10, 1)

        val resource2 = middleware.queryResource("car_1")
        if (resource1 != null && resource2 != null) {
            middleware.itinerary(5, mutableMapOf(1 to resource1.item, 2 to resource2.item))
        }
        val newReservations = middleware.queryCustomer(5)?.reservations
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
