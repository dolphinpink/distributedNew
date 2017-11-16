package Transactions

import ResourceManagerCode.Customer
import ResourceManagerCode.ReservableItem
import ResourceManagerCode.ReservableType
import ResourceManagerCode.Resource

interface TransactionalResourceManager {

    /**
     * starts a transaction with this id
     *
     * @ return true if succeeded, false if this transactionId already exists
     */
    fun start(transactionId: Int): Boolean

    /**
     * commits the transaction with this Id
     */
    fun commit(transactionId: Int): Boolean

    fun abort(transactionId: Int): Boolean


    /**
     * Creates resource if it doesn't exist.
     *
     * @return success.
     */
    fun createResource(transactionId: Int, type: ReservableType, resourceId: String, amount: Int, price: Int): Boolean


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
    fun updateResource(transactionId: Int, resourceId: String, newTotalAmount: Int, newPrice: Int): Boolean


    /**
     * subtracts reservationQuantity from the available amount of the resource if possible.
     *
     * can provide negative value to free up resource
     *
     * @param resourceId
     * @param reservationQuantity
     * @return true if successful, false if not enough available resource
     */
    fun reserveResource(transactionId: Int, resourceId: String, reservationQuantity: Int): Boolean


    /**
     * Attempts to delete resource
     * Fails if resource doesn't exist, or resource is reserved
     *
     * @param resourceId
     * @return true if successful, false if resource doesn't exist or is reserved
     */
    fun deleteResource(transactionId: Int, resourceId: String): Boolean


    /**
     * returns amount of resource available
     *
     * @param resourceId
     * @return amount of resource available, -1 if resource doesn't exist
     */
    fun queryResource(transactionId: Int, resourceId: String): Resource?


    /**
     * returns a unique customer ID. *DOES NOT CREATE CUSTOMER*
     *
     * @return a unique customer ID
     */
    fun uniqueCustomerId(transactionId: Int): Int


    /**
     * creates new customer with ID provided
     *
     * fails if customer with ID already exists
     *
     * @param customerId
     * @return true if successful creation, false customer with ID already exists
     */
    fun createCustomer(transactionId: Int, customerId: Int): Boolean


    /**
     * Deletes customer and all their reservations
     * @param customerId
     * @return true if successful, false otherwise
     */
    fun deleteCustomer(transactionId: Int, customerId: Int): Boolean


    /**
     * records reservation for customer
     * @param customerId
     * @param reservationId
     * @param resourceId
     * @return true if suceeded, false if failed (transactionId: Int, customer doesn't exist)
     */
    fun customerAddReservation(transactionId: Int, customerId: Int, reservationId: Int, resourceId: String): Boolean


    /**
     * deletes reservation for customer
     * @param customerId
     * @param reservationId
     * @return true if succeeds, false if customer doesn't exist or doesn't hold said resource
     */
    fun customerRemoveReservation(transactionId: Int, customerId: Int, reservationId: Int): Boolean


    /**
     * returns all reservation information for customer
     *
     * @param customerId
     * @return String containing all customer reservations
     */
    fun queryCustomer(transactionId: Int, customerId: Int): Customer?


    /**
     * reserves an itinerary for this user
     *
     * @param customerId
     * @param reservationResources a map of reservationIds to the resourceIds you want to reserve
     * @return true if successful, false if resources or customer doesn't exist, or insufficient available resources
     */
    fun itinerary(transactionId: Int, customerId: Int, reservationResources: MutableMap<Int, String>): Boolean

}