package Tcp

import ResourceManagerCode.*

class Middleware(val server: String): ResourceManager {

    val resourceType: MutableMap<String, ReservableType> = mutableMapOf()

    private val customers: MutableSet<Customer> = mutableSetOf()
    private val flightRm: ResourceManager = TcpRequestSender(PortNumbers.flightRm, server)
    private val hotelRm: ResourceManager = TcpRequestSender(PortNumbers.hotelRm, server)
    private val carRm: ResourceManager = TcpRequestSender(PortNumbers.carRm, server)

    private var customerIdCounter: Int = 0

    private val customerLock = Any()

    fun getRm(type: ReservableType): ResourceManager {
        return when(type) {
            ReservableType.FLIGHT -> flightRm
            ReservableType.HOTEL -> hotelRm
            ReservableType.CAR -> carRm
        }
    }

    override fun createResource(type: ReservableType, resourceId: String, totalQuantity: Int, price: Int): Boolean {
        if (getRm(type).createResource(type, resourceId, totalQuantity, price)) {
            resourceType.put(resourceId, type)
            return true
        }
        return false
    }

    override fun updateResource(resourceId: String, newTotalQuantity: Int, newPrice: Int): Boolean {
        return getRm(resourceType[resourceId] ?: return false).updateResource(resourceId, newTotalQuantity, newPrice)
    }

    override fun reserveResource(resourceId: String, reservationQuantity: Int): Boolean {
        println("MIDDLEWARE" + resourceType[resourceId])
        return getRm(resourceType[resourceId] ?: return false).reserveResource(resourceId, reservationQuantity)
    }

    override fun deleteResource(resourceId: String): Boolean {
        if (getRm(resourceType[resourceId] ?: return false).deleteResource(resourceId)) {
            resourceType.remove(resourceId)
            return true
        }
        return false
    }

    override fun queryResource(resourceId: String): Resource? {
        return getRm(resourceType[resourceId] ?: return null).queryResource(resourceId)
    }

    override fun uniqueCustomerId(): Int {
        return generateCustomerId()
    }

    override fun createCustomer(customerId: Int): Boolean {
        synchronized(customerLock) {
            return customers.add(Customer(customerId))
        }
    }

    override fun deleteCustomer(customerId: Int): Boolean {
        synchronized(customerLock) {
            return customers.remove(customers.find { c -> c.customerId == customerId})
        }
    }

    override fun queryCustomer(customerId: Int): Customer? {
        return customers.find { c -> c.customerId == customerId}
    }

    override fun customerAddReservation(customerId: Int, reservationId: Int, reservableItem: ReservableItem, quantity: Int): Boolean {

        val customer: Customer? = queryCustomer(customerId)
        if (customer != null) {
            if (reserveResource(reservableItem.id, quantity)) {
                val resource = queryResource(reservableItem.id)?.item ?: return false
                if (customer.addReservation(Reservation(reservationId, reservableItem, quantity, quantity * reservableItem.price))) {
                    return true
                } else {
                    // customer may have been deleted in the meantime
                    reserveResource(reservableItem.id, quantity)
                }
            }
        }
        return false
    }

    override fun customerRemoveReservation(customerId: Int, reservationId: Int): Boolean {

        val customer: Customer = queryCustomer(customerId) ?: return false
        val reservation = customer.reservations.find { r -> r.reservationId == reservationId } ?: return false
        customer.removeReservation(reservationId)
        reserveResource(reservation.item.id, -1)
        return false
    }

    override fun itinerary(customerId: Int, reservationResources: MutableMap<Int, ReservableItem>): Boolean {

        data class Reserved(val rResourceId: String, val rReservationId: Int)

        val reserved: MutableMap<Int, Reserved> = mutableMapOf()

        for ((reservationId, reservableItem) in reservationResources){
            if ( customerAddReservation(customerId, reservationId, reservableItem, 1) == true) {
                reserved.put(customerId, Reserved(reservableItem.id, reservationId))
            } else {
                // if any of the fail, undo the reservations so far
                reserved.forEach({(cId, reserved) ->
                    customerRemoveReservation(cId, reserved.rReservationId)
                    reserveResource(reserved.rResourceId, -1 )
                    return false
                })
            }

        }
        return true
    }

    private fun generateCustomerId(): Int {
        synchronized(customerIdCounter) {
            customerIdCounter +=1
            return customerIdCounter
        }
    }

}