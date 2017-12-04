package MiddlewareCode

import ResourceManagerCode.*

class Middleware(val senderId: Int,
                 val resourceType: MutableMap<String, ReservableType> = mutableMapOf(),
                 private val customerRm: ResourceManager = RequestSender(senderId, CommunicationsConfig.CUSTOMER_REQUEST, CommunicationsConfig.CUSTOMER_REPLY),
                 private val flightRm: ResourceManager = RequestSender(senderId, CommunicationsConfig.FLIGHT_REQUEST, CommunicationsConfig.FLIGHT_REPLY),
                 private val hotelRm: ResourceManager = RequestSender(senderId, CommunicationsConfig.HOTEL_REQUEST, CommunicationsConfig.HOTEL_REPLY),
                 private val carRm: ResourceManager = RequestSender(senderId, CommunicationsConfig.CAR_REQUEST, CommunicationsConfig.CAR_REPLY)) : ResourceManager {



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
        return getRm(resourceType[resourceId] ?: return false).reserveResource(resourceId, reservationQuantity)
    }

    override fun deleteResource(resourceId: String): Boolean {

        if (getRm(resourceType[resourceId] ?: return false).deleteResource(resourceId)) {
            resourceType.remove(resourceId)
            return true
        }
        println("could not delete")
        return false
    }

    override fun queryResource(resourceId: String): Resource? {
        return getRm(resourceType[resourceId] ?: return null).queryResource(resourceId)
    }

    override fun uniqueCustomerId(): Int {
        return customerRm.uniqueCustomerId()
    }

    override fun createCustomer(customerId: Int): Boolean {
        return customerRm.createCustomer(customerId)
    }

    override fun deleteCustomer(customerId: Int): Boolean {
        return customerRm.deleteCustomer(customerId)
    }

    override fun queryCustomer(customerId: Int): Customer? {
        return customerRm.queryCustomer(customerId)
    }

    override fun customerAddReservation(customerId: Int, reservationId: Int, reservableItem: ReservableItem): Boolean {
        return customerRm.customerAddReservation(customerId, reservationId, reservableItem)
    }

    override fun customerRemoveReservation(customerId: Int, reservationId: Int): Boolean {
        return customerRm.customerRemoveReservation(customerId, reservationId)
    }

    override fun itinerary(customerId: Int, reservationResources: MutableMap<Int, ReservableItem>): Boolean {
        return false;
    }

}