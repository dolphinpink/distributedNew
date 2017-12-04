package ResourceManagerCode

import MiddlewareCode.RequestReceiver
import org.jgroups.JChannel
import java.util.*

class ResourceManagerImpl(requestChannelName: String, replyChannelName: String) : ResourceManager {

    val receiver = RequestReceiver(this, requestChannelName, replyChannelName)
    val resources: MutableSet<Resource> = mutableSetOf()
    val customers: MutableSet<Customer> = mutableSetOf()

    private val resourceLock = Any()
    private val customerLock = Any()

    private val customerIdLock = Any()
    private var customerIdCounter: Int = 0

    override fun createResource(type: ReservableType, resourceId: String, totalQuantity: Int, price: Int): Boolean {
        synchronized(resourceLock) {
            return resources.add(Resource(ReservableItem(type, resourceId, totalQuantity, price), totalQuantity))
        }
    }

    override fun updateResource(resourceId: String, newTotalQuantity: Int, newPrice: Int): Boolean {

        val random = Random()

        if (random.nextInt(4) == 0) {
           println("\nRM randomly crashing! -------------------------\n")
            receiver.requestChannel = JChannel().connect("not a real channel")
            receiver.replyChannel = JChannel().connect("not a real channel")
        }

        synchronized(resourceLock) {
            val r = resources.find{r -> r.item.id == resourceId} ?: return false

            // new total quantity >= amount already reserved by customers
            if (newTotalQuantity >= r.item.totalQuantity - r.remainingQuantity) {
                r.remainingQuantity += newTotalQuantity - r.item.totalQuantity
                r.item.totalQuantity = newTotalQuantity
                r.item.price = newPrice
                return true
            } else {
                return false
            }
        }
    }

    override fun reserveResource(resourceId: String, reservationQuantity: Int): Boolean {
        synchronized(resourceLock) {
            val r = resources.find{r -> r.item.id == resourceId} ?: return false

            // new total quantity >= amount already reserved by customers
            if (r.remainingQuantity >= reservationQuantity) {
                r.remainingQuantity -= reservationQuantity
                return true
            } else {
                return false
            }
        }
    }

    override fun deleteResource(id: String): Boolean {
        synchronized(resourceLock) {
            return resources.remove(resources.find { r -> r.item.id == id })
        }
    }

    override fun queryResource(resourceId: String): Resource? {
        return resources.find {r -> r.item.id == resourceId}
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
            val customer = customers.find { c -> c.customerId == customerId} ?: return false
            if (customer.reservations.isEmpty()) {
                customers.remove(customer)
                return true
            }
        }
        return false
    }

    override fun queryCustomer(customerId: Int): Customer? {
        return customers.find { c -> c.customerId == customerId}
    }

    override fun customerAddReservation(customerId: Int, reservationId: Int, reservableItem: ReservableItem): Boolean {
        val customer: Customer = customers.find {c -> c.customerId == customerId} ?: return false
        synchronized(customerLock) {
            customer.addReservation(Reservation(reservationId, reservableItem, 1, reservableItem.price))
            return true
        }
    }

    override fun customerRemoveReservation(customerId: Int, reservationId: Int): Boolean {
        val customer: Customer = customers.find {c -> c.customerId == customerId} ?: return false
        synchronized(customerLock) {
            customer.removeReservation(reservationId)
            return true
        }
    }

    override fun itinerary(customerId: Int, reservationResources: MutableMap<Int, ReservableItem>): Boolean {
        TODO("not implemented")
    }

    private fun generateCustomerId(): Int {
        synchronized(customerIdLock) {
            customerIdCounter +=1
            return customerIdCounter
        }
    }

}
