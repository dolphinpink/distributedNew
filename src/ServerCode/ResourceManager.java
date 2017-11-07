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
     * @param newAmount
     * @param newPrice
     * @return
     */
    public boolean updateResource(String resourceId, int newAmount, int newPrice);

    /**
     * Attempts to delete resource
     * Fails if resource doesn't exist, resource is reserved
     *
     * @param resourceId
     * @return
     */
    public boolean deleteResource(String resourceId);

    /**
     * returns amount of resource available
     *
     * -1 if resource doesn't exist
     *
     * @param resourceId
     * @return
     */
    public int queryResource(String resourceId);


    /**
     * returns a unique customer ID. DOES NOT CREATE CUSTOMER
     *
     * @return
     */
    public String getUniqueCustomerId();

    /**
     * creates new customer with ID provresourceIded
     *
     * @param customerId
     * @return
     */
    public boolean createCustomer(String customerId);


    /**
     * Deletes customer and all their reservations
     * @param customerId
     * @return
     */
    public boolean deleteCustomer(String customerId);


    /**
     * returns all reservation information for customer
     *
     * @param customerId
     * @return
     */
    public String queryCustomerInfo(String customerId);


    /**
     * reserves one unit of this resource
     *
     * @param customerId
     * @param resourceId
     * @return
     */
    public boolean reserveResource(String customerId, ReservableType type, String resourceId);


    /**
     * reserves an itinerary for this user
     *
     * @param customerId
     * @param resourceIds
     * @return
     */
    public boolean itinerary(String customerId, Set<String> resourceIds);

}
