package Transactions

import ResourceManagerCode.ReservableItem
import ResourceManagerCode.ReservableType
import Transactions.TransactionalResourceManager
import Tcp.*
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Command pattern to execute remote calls
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = CreateResourceRequest::class, name = "CreateResourceRequest"),
        JsonSubTypes.Type(value = UpdateResourceRequest::class, name = "UpdateResourceRequest"),
        JsonSubTypes.Type(value = ReserveResourceRequest::class, name = "ReserveResourceRequest"),
        JsonSubTypes.Type(value = DeleteResourceRequest::class, name = "DeleteResourceRequest"),
        JsonSubTypes.Type(value = QueryResourceRequest::class, name = "QueryResourceRequest"),
        JsonSubTypes.Type(value = UniqueCustomerIdRequest::class, name = "UniqueCustomerIdRequest"),
        JsonSubTypes.Type(value = CreateCustomerRequest::class, name = "CreateCustomerRequest"),
        JsonSubTypes.Type(value = DeleteCustomerRequest::class, name = "DeleteCustomerRequest"),
        JsonSubTypes.Type(value = CustomerAddReservationRequest::class, name = "CustomerAddReservationRequest"),
        JsonSubTypes.Type(value = CustomerRemoveReservationRequest::class, name = "CustomerRemoveReservationRequest"),
        JsonSubTypes.Type(value = QueryCustomerRequest::class, name = "QueryCustomerRequest"),
        JsonSubTypes.Type(value = ItineraryRequest::class, name = "ItineraryRequest"))
abstract class TransactionalRequestCommand(val requestId: Int, val transactionId: Int) {
    abstract fun execute(rm: TransactionalResourceManager): Reply
}

class CreateResourceRequest(requestId: Int,transactionId: Int, val type: ReservableType, val resourceId: String, val amount: Int, val price: Int): TransactionalRequestCommand(requestId, transactionId) {
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.createResource(transactionId, type, resourceId, amount, price))
    }
}

class UpdateResourceRequest(requestId: Int, transactionId: Int, val resourceId: String, val newAmount: Int, val newPrice: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.updateResource(transactionId, resourceId, newAmount, newPrice))
    }
}

class ReserveResourceRequest(requestId: Int, transactionId: Int, val resourceId: String, val reservationQuantity: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.reserveResource(transactionId, resourceId, reservationQuantity))
    }
}

class DeleteResourceRequest(requestId: Int, transactionId: Int, val resourceId: String): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.deleteResource(transactionId, resourceId))
    }
}

class QueryResourceRequest(requestId: Int, transactionId: Int, val resourceId: String): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return ResourceReply(requestId, rm.queryResource(transactionId, resourceId))
    }
}

class UniqueCustomerIdRequest(requestId: Int, transactionId: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return IntReply(requestId, rm.uniqueCustomerId(transactionId))
    }
}

class CreateCustomerRequest(requestId: Int, transactionId: Int, val customerId: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.createCustomer(transactionId, customerId))
    }
}

class DeleteCustomerRequest(requestId: Int, transactionId: Int, val customerId: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.deleteCustomer(transactionId, customerId))
    }
}

class CustomerAddReservationRequest(requestId: Int, transactionId: Int, val customerId: Int, val reservationId: Int, val reservableItem: ReservableItem): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.customerAddReservation(transactionId, customerId, reservationId, reservableItem))
    }
}

class CustomerRemoveReservationRequest(requestId: Int, transactionId: Int, val customerId: Int, val reservationId: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.customerRemoveReservation(transactionId, customerId, reservationId))
    }
}

class QueryCustomerRequest(requestId: Int, transactionId: Int, val customerId: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return CustomerReply(requestId, rm.queryCustomer(transactionId, customerId))
    }
}

class ItineraryRequest(requestId: Int, transactionId: Int, val customerId: Int, val reservationResources: MutableMap<Int, ReservableItem>): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.itinerary(transactionId, customerId, reservationResources))
    }
}