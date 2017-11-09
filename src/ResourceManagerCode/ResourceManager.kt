package ResourceManagerCode

import java.util.*

interface ResourceManager {

    /**
     * Creates resource if it doesn't exist.
     *
     * @return success.
     */
    fun createResource(type: ReservableType, resourceId: String, amount: Int, price: Int): Boolean


    /**
     * Updates total available amount of resource and price, if it exists and it is possible.
     *
     * Will fail if newAmount < amountAlreadyReserved
     *
     * @param resourceId
     * @param newTotalAmount
     * @param newPrice
     * @return
     */
    fun updateResource(resourceId: String, newTotalAmount: Int, newPrice: Int): Boolean


    /**
     * subtracts reservationQuantity from the available amount of the resource if possible.
     *
     * can provide negative value to free up resource
     *
     * @param resourceId
     * @param reservationQuantity
     * @return true if successful, false if not enough available resource
     */
    fun reserveResource(resourceId: String, reservationQuantity: Int): Boolean


    /**
     * Attempts to delete resource
     * Fails if resource doesn't exist, or resource is reserved
     *
     * @param resourceId
     * @return true if successful, false if resource doesn't exist or is reserved
     */
    fun deleteResource(resourceId: String): Boolean


    /**
     * returns amount of resource available
     *
     * @param resourceId
     * @return amount of resource available, -1 if resource doesn't exist
     */
    fun queryResource(resourceId: String): Resource?


    /**
     * returns a unique customer ID. *DOES NOT CREATE CUSTOMER*
     *
     * @return a unique customer ID
     */
    fun uniqueCustomerId(): Int


    /**
     * creates new customer with ID provided
     *
     * fails if customer with ID already exists
     *
     * @param customerId
     * @return true if successful creation, false customer with ID already exists
     */
    fun createCustomer(customerId: Int): Boolean


    /**
     * Deletes customer and all their reservations
     * @param customerId
     * @return true if successful, false otherwise
     */
    fun deleteCustomer(customerId: Int): Boolean


    /**
     * records reservation for customer
     * @param customerId
     * @param reservableItem
     * @param quantity
     * @return true if suceeded, false if failed (customer doesn't exist)
     */
    fun customerAddReservation(customerId: Int, reservationId: Int, reservableItem: ReservableItem, quantity: Int): Boolean


    /**
     * deletes reservation for customer
     * @param customerId
     * @param reservationId
     * @return true if succeeds, false if customer doesn't exist or doesn't hold said resource
     */
    fun customerRemoveReservation(customerId: Int, reservationId: Int): Boolean


    /**
     * returns all reservation information for customer
     *
     * @param customerId
     * @return String containing all customer reservations
     */
    fun queryCustomer(customerId: Int): Customer?


    /**
     * reserves an itinerary for this user
     *
     * @param customerId
     * @param reservationResources a map of reservationIds to the reservableItem you want to reserve
     * @return true if successful, false if resources or customer doesn't exist, or insufficient available resources
     */
    fun itinerary(customerId: Int, reservationResources: MutableMap<Int, ReservableItem>): Boolean

}
