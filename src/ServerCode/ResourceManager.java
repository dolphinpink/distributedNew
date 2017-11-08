package ServerCode;

import java.util.*;

public interface ResourceManager {

    /**
     * Creates resource if it doesn't exist.
     *
     * @return success.
     */
    public boolean createResource(ReservableType type, String resourceId, int amount, int price);


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
    public boolean updateResource(String resourceId, int newTotalAmount, int newPrice);


    /**
     * subtracts reservationQuantity from the available amount of the resource if possible.
     *
     * can provide negative value to free up resource
     *
     * @param resourceId
     * @param reservationQuantity
     * @return true if successful, false if not enough available resource
     */
    public boolean reserveResource(String resourceId, int reservationQuantity);


    /**
     * Attempts to delete resource
     * Fails if resource doesn't exist, or resource is reserved
     *
     * @param resourceId
     * @return true if successful, false if resource doesn't exist or is reserved
     */
    public boolean deleteResource(String resourceId);


    /**
     * returns amount of resource available
     *
     * @param resourceId
     * @return amount of resource available, -1 if resource doesn't exist
     */
    public int queryResource(String resourceId);


    /**
     * returns a unique customer ID. *DOES NOT CREATE CUSTOMER*
     *
     * @return a unique customer ID
     */
    public int uniqueCustomerId();


    /**
     * creates new customer with ID provided
     *
     * fails if customer with ID already exists
     *
     * @param customerId
     * @return true if successful creation, false customer with ID already exists
     */
    public boolean createCustomer(int customerId);


    /**
     * Deletes customer and all their reservations
     * @param customerId
     * @return true if successful, false otherwise
     */
    public boolean deleteCustomer(int customerId);


    /**
     * returns all reservation information for customer
     *
     * @param customerId
     * @return String containing all customer reservations
     */
    public String queryCustomerInfo(int customerId);


    /**
     * reserves one unit of this resource for this customer
     *
     * @param customerId
     * @param resourceId
     * @return true if successful, false if customer or resource doesn't exist, or not enough available resource
     */
    public boolean createReservation(int customerId, ReservableType type, String resourceId);


    /**
     * reserves an itinerary for this user
     *
     * @param customerId
     * @param resourceIds
     * @return true if successful, false if resources or customer doesn't exist, or insufficient available resources
     */
    public boolean itinerary(int customerId, Set<String> resourceIds);

}
