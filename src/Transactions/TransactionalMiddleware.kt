package Transactions

import LockManagerCustom.DeadlockException
import LockManagerCustom.LockManager
import LockManagerCustom.LockType
import ResourceManagerCode.*
import Tcp.*

class TransactionalMiddleware(val server: String): TransactionalResourceManager {
    companion object {
        val CUSTOMER = "customer"
    }

    val resourceType: MutableMap<String, ReservableType> = mutableMapOf()

    private val customerRm: ResourceManager = TcpRequestSender(PortNumbers.customerRm, server, 0)
    private val flightRm: ResourceManager = TcpRequestSender(PortNumbers.flightRm, server, 0)
    private val hotelRm: ResourceManager = TcpRequestSender(PortNumbers.hotelRm, server, 0)
    private val carRm: ResourceManager = TcpRequestSender(PortNumbers.carRm, server, 0)

    private val transactionManager = TransactionManager(resourceType, customerRm, flightRm, hotelRm, carRm)

    private val lockManager = LockManager()

    private var customerIdCounter: Int = 0

    private fun getRm(type: ReservableType): ResourceManager {
        return when(type) {
            ReservableType.FLIGHT -> flightRm
            ReservableType.HOTEL -> hotelRm
            ReservableType.CAR -> carRm
        }
    }

    private fun cleanupDeadlock(transactionId: Int) {
        abort(transactionId)
        throw DeadlockException(transactionId, "")
    }

    override fun start(transactionId: Int): Boolean {
        return transactionManager.createTransaction(transactionId)
    }

    override fun commit(transactionId: Int): Boolean {
        return transactionManager.commitTransaction(transactionId) && lockManager.unlockAll(transactionId)
    }

    override fun abort(transactionId: Int): Boolean {
        return transactionManager.abortTransaction(transactionId) && lockManager.unlockAll(transactionId)
    }

    override fun createResource(transactionId: Int, type: ReservableType, resourceId: String, totalQuantity: Int, price: Int): Boolean {

        if (!transactionManager.exists(transactionId)) return false

        if (lockManager.lock(transactionId, type.toString(), LockType.WRITE)) {
            if (getRm(type).createResource(type, resourceId, totalQuantity, price)) {
                resourceType.put(resourceId, type)
                transactionManager.addRequest(transactionId, DeleteResourceRequest(-1, resourceId))
                return true
            }
        } else {
            cleanupDeadlock(transactionId)
        }

        return false
    }

    override fun updateResource(transactionId: Int, resourceId: String, newTotalQuantity: Int, newPrice: Int): Boolean {

        if (!transactionManager.exists(transactionId)) return false

        val type = resourceType[resourceId] ?: return false

        if (lockManager.lock(transactionId, type.toString(), LockType.WRITE)) {
            val snapshot = getRm(type).queryResource(resourceId) ?: return false
            if (getRm(type).updateResource(resourceId, newTotalQuantity, newPrice)) {
                transactionManager.addRequest(transactionId, UpdateResourceRequest(-1, resourceId, snapshot.item.totalQuantity, snapshot.item.price))
            }
        } else {
            cleanupDeadlock(transactionId)
        }

        return false
    }

    override fun reserveResource(transactionId: Int, resourceId: String, reservationQuantity: Int): Boolean {

        if (!transactionManager.exists(transactionId)) return false
        val type = resourceType[resourceId] ?: return false

        if (lockManager.lock(transactionId, type.toString(), LockType.WRITE)) {
            if (getRm(type).reserveResource(resourceId, reservationQuantity)) {
                transactionManager.addRequest(transactionId, ReserveResourceRequest(-1, resourceId, -reservationQuantity))
            }
        } else {
            cleanupDeadlock(transactionId)
        }

        return false
    }

    override fun deleteResource(transactionId: Int, resourceId: String): Boolean {

        if (!transactionManager.exists(transactionId)) return false
        val type = resourceType[resourceId] ?: return false

        if (lockManager.lock(transactionId, type.toString(), LockType.WRITE)) {
            val snapshot = getRm(type).queryResource(resourceId) ?: return false
            if (getRm(type).deleteResource(resourceId)) {
                transactionManager.addRequest(transactionId, CreateResourceRequest(-1, snapshot.item.type, resourceId, snapshot.item.totalQuantity, snapshot.item.price))
                resourceType.remove(resourceId)
                return true
            }
        } else {
            cleanupDeadlock(transactionId)
        }

        return false
    }

    override fun queryResource(transactionId: Int, resourceId: String): Resource? {

        if (!transactionManager.exists(transactionId)) return null
        val type = resourceType[resourceId] ?: return null

        if (lockManager.lock(transactionId, type.toString(), LockType.READ)) {
            return getRm(resourceType[resourceId] ?: return null).queryResource(resourceId)
        } else {
            cleanupDeadlock(transactionId)
        }

        return null
    }

    override fun uniqueCustomerId(transactionId: Int): Int {
        return generateCustomerId()
    }

    override fun createCustomer(transactionId: Int, customerId: Int): Boolean {

        if (!transactionManager.exists(transactionId)) return false

        if (lockManager.lock(transactionId, CUSTOMER, LockType.WRITE)) {
            if (customerRm.createCustomer(customerId)) {
                transactionManager.addRequest(transactionId, DeleteCustomerRequest(-1, customerId))
                return true
            }
        } else {
            cleanupDeadlock(transactionId)
        }

        return false
    }

    override fun deleteCustomer(transactionId: Int, customerId: Int): Boolean {

        if (!transactionManager.exists(transactionId)) return false

        if (lockManager.lock(transactionId, CUSTOMER, LockType.WRITE)) {
            if(customerRm.deleteCustomer(customerId)) {
                transactionManager.addRequest(transactionId, CreateCustomerRequest(-1, customerId))
            }
        } else {
            cleanupDeadlock(transactionId)
        }

        return false
    }

    override fun queryCustomer(transactionId: Int, customerId: Int): Customer? {

        if (!transactionManager.exists(transactionId)) return null

        if (lockManager.lock(transactionId, CUSTOMER, LockType.READ)) {
            return customerRm.queryCustomer(customerId)
        } else {
            cleanupDeadlock(transactionId)
        }

        return null
    }

    override fun customerAddReservation(transactionId: Int, customerId: Int, reservationId: Int, reservableItem: ReservableItem): Boolean {

        if (lockManager.lock(transactionId, CUSTOMER, LockType.WRITE)) { // customer write lock acquired
            customerRm.customerAddReservation(customerId, reservationId, reservableItem)
            transactionManager.addRequest(transactionId, CustomerRemoveReservationRequest(-1, customerId, reservationId))
        } else {
            cleanupDeadlock(transactionId)
        }
        return false
    }

    override fun customerRemoveReservation(transactionId: Int, customerId: Int, reservationId: Int): Boolean {

        if (lockManager.lock(transactionId, CUSTOMER, LockType.WRITE)) { // customer write lock acquired
            val snapshot = customerRm.queryCustomer(customerId)?.reservations?.find { r -> r.reservationId == reservationId } ?: return false
            customerRm.customerRemoveReservation(customerId, reservationId)
            transactionManager.addRequest(transactionId, CustomerAddReservationRequest(-1, customerId, reservationId, snapshot.item))
        } else {
            cleanupDeadlock(transactionId)
        }
        return false
    }

    override fun itinerary(transactionId: Int, customerId: Int, reservationResources: MutableMap<Int, ReservableItem>): Boolean {

        if (lockManager.lock(transactionId, CUSTOMER, LockType.WRITE)) { // customer write lock acquired

            reservationResources.forEach {(reservationId, reservableItem) ->
                if (!lockManager.lock(transactionId, reservableItem.type.toString(), LockType.WRITE)) return false
                queryResource(transactionId, reservableItem.id) ?: return false
            }

            val customer = queryCustomer(transactionId, customerId) ?: return false

            reservationResources.forEach {(reservationId, reservableItem) ->
                val resource = queryResource(transactionId, reservableItem.id)!!
                customerAddReservation(transactionId, customerId, reservationId, resource.item)
            }

        } else {
            cleanupDeadlock(transactionId)
        }
        return false
    }

    private fun generateCustomerId(): Int {
        synchronized(customerIdCounter) {
            customerIdCounter +=1
            return customerIdCounter
        }
    }

}