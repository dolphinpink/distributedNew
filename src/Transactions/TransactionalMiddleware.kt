package Transactions

import LockManagerCustom.DeadlockException
import LockManagerCustom.LockManager
import LockManagerCustom.LockType
import ResourceManagerCode.*
import Tcp.PortNumbers
import Tcp.TcpRequestSender

class TransactionalMiddleware(val server: String): TransactionalResourceManager {
    companion object {
        val CUSTOMER = "customer"
    }

    val resourceType: MutableMap<String, ReservableType> = mutableMapOf()

    private val customers: MutableSet<Customer> = mutableSetOf()
    private val flightRm: ResourceManager = TcpRequestSender(PortNumbers.flightRm, server)
    private val hotelRm: ResourceManager = TcpRequestSender(PortNumbers.hotelRm, server)
    private val carRm: ResourceManager = TcpRequestSender(PortNumbers.carRm, server)

    private val transactionManager = TransactionManager(this)

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
        return transactionManager.commitTransaction(transactionId)
    }

    override fun abort(transactionId: Int): Boolean {
        return transactionManager.abortTransaction(transactionId)
    }

    override fun createResource(transactionId: Int, type: ReservableType, resourceId: String, totalQuantity: Int, price: Int): Boolean {

        if (!transactionManager.exists(transactionId)) return false

        if (lockManager.lock(transactionId, type.toString(), LockType.WRITE)) {
            if (getRm(type).createResource(type, resourceId, totalQuantity, price)) {
                resourceType.put(resourceId, type)
                transactionManager.addRequest(transactionId, DeleteResourceRequest(-1, transactionId, resourceId))
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
                transactionManager.addRequest(transactionId, UpdateResourceRequest(-1, transactionId, resourceId, snapshot.item.totalQuantity, snapshot.item.price))
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
                transactionManager.addRequest(transactionId, ReserveResourceRequest(-1, transactionId, resourceId, -reservationQuantity))
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
                transactionManager.addRequest(transactionId, CreateResourceRequest(-1, transactionId, snapshot.item.type, resourceId, snapshot.item.totalQuantity, snapshot.item.price))
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
            if (customers.add(Customer(customerId))) {
                transactionManager.addRequest(transactionId, DeleteCustomerRequest(-1, transactionId, customerId))
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
            val customer = customers.find { c -> c.customerId == customerId } ?: return false
            if (customer.reservations.isEmpty()) {
                if (customers.remove(customer)) {
                    transactionManager.addRequest(transactionId, CreateCustomerRequest(-1, transactionId, customerId))
                    return true
                }
            }
        } else {
            cleanupDeadlock(transactionId)
        }

        return false
    }

    override fun queryCustomer(transactionId: Int, customerId: Int): Customer? {

        if (!transactionManager.exists(transactionId)) return null

        if (lockManager.lock(transactionId, CUSTOMER, LockType.READ)) {
            return customers.find { c -> c.customerId == customerId}
        } else {
            cleanupDeadlock(transactionId)
        }

        return null
    }

    override fun customerAddReservation(transactionId: Int, customerId: Int, reservationId: Int, reservableItem: ReservableItem): Boolean {

        if (lockManager.lock(transactionId, CUSTOMER, LockType.WRITE)) { // customer write lock acquired

            val customer = queryCustomer(transactionId, customerId) ?: return false
            if (reserveResource(transactionId, reservableItem.id, 1)) { //  resource write lock acquired
                customer.addReservation(Reservation(reservationId, reservableItem, 1, reservableItem.price))
                // reserveResource will automatically add undo action to the transaction manager, resetting resources if this aborts
                // however, doesn't reset customer.
                // customer remove reservation will call a reserve resource as well, adding it to the stack, but will also
                // pop it off the stack, in total reserveResource will be called 4 times, resetting back to normal
                transactionManager.addRequest(transactionId, CustomerRemoveReservationRequest(-1, transactionId, customerId, reservationId))
                return true
            }
        }
        return false
    }

    override fun customerRemoveReservation(transactionId: Int, customerId: Int, reservationId: Int): Boolean {

        if (lockManager.lock(transactionId, CUSTOMER, LockType.WRITE)) { // customer write lock acquired

            val customer = queryCustomer(transactionId, customerId) ?: return false
            val reservation = customer.reservations.find { r -> r.reservationId == reservationId } ?: return false
            customer.removeReservation(reservationId)
            reserveResource(transactionId, reservation.item.id, -1)

            transactionManager.addRequest(transactionId, CustomerAddReservationRequest(-1, transactionId, customerId, ))
        }
        return false
    }

    override fun itinerary(transactionId: Int, customerId: Int, reservationResources: MutableMap<Int, ReservableItem>): Boolean {

        data class Reserved(val rResourceId: String, val rReservationId: Int)

        val reserved: MutableMap<Int, Reserved> = mutableMapOf()

        for ((reservationId, reservableItem) in reservationResources){
            if ( customerAddReservation(transactionId, customerId, reservationId, reservableItem) == true) {
                reserved.put(customerId, Reserved(reservableItem.id, reservationId))
            } else {
                // if any of the fail, undo the reservations so far
                reserved.forEach({(cId, reserved) ->
                    customerRemoveReservation(transactionId, cId, reserved.rReservationId)
                    reserveResource(transactionId, reserved.rResourceId, -1 )
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