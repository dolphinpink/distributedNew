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
        JsonSubTypes.Type(value = TransactionalStartRequest::class, name = "TransactionalStartRequest"),
        JsonSubTypes.Type(value = TransactionalCommitRequest::class, name = "TransactionalCommitRequest"),
        JsonSubTypes.Type(value = TransactionalAbortRequest::class, name = "TransactionalAbortRequest"),
        JsonSubTypes.Type(value = TransactionalCreateResourceRequest::class, name = "TransactionalCreateResourceRequest"),
        JsonSubTypes.Type(value = TransactionalUpdateResourceRequest::class, name = "TransactionalUpdateResourceRequest"),
        JsonSubTypes.Type(value = TransactionalReserveResourceRequest::class, name = "TransactionalReserveResourceRequest"),
        JsonSubTypes.Type(value = TransactionalDeleteResourceRequest::class, name = "TransactionalDeleteResourceRequest"),
        JsonSubTypes.Type(value = TransactionalQueryResourceRequest::class, name = "TransactionalQueryResourceRequest"),
        JsonSubTypes.Type(value = TransactionalUniqueCustomerIdRequest::class, name = "TransactionalUniqueCustomerIdRequest"),
        JsonSubTypes.Type(value = TransactionalCreateCustomerRequest::class, name = "TransactionalCreateCustomerRequest"),
        JsonSubTypes.Type(value = TransactionalDeleteCustomerRequest::class, name = "TransactionalDeleteCustomerRequest"),
        JsonSubTypes.Type(value = TransactionalCustomerAddReservationRequest::class, name = "TransactionalCustomerAddReservationRequest"),
        JsonSubTypes.Type(value = TransactionalCustomerRemoveReservationRequest::class, name = "TransactionalCustomerRemoveReservationRequest"),
        JsonSubTypes.Type(value = TransactionalQueryCustomerRequest::class, name = "TransactionalQueryCustomerRequest"),
        JsonSubTypes.Type(value = TransactionalItineraryRequest::class, name = "TransactionalItineraryRequest"))
abstract class TransactionalRequestCommand(val requestId: Int, val transactionId: Int) {
    abstract fun execute(rm: TransactionalResourceManager): Reply
}

class TransactionalStartRequest(requestId: Int,transactionId: Int): TransactionalRequestCommand(requestId, transactionId) {
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.start(transactionId))
    }
}

class TransactionalCommitRequest(requestId: Int,transactionId: Int): TransactionalRequestCommand(requestId, transactionId) {
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.commit(transactionId))
    }
}

class TransactionalAbortRequest(requestId: Int,transactionId: Int): TransactionalRequestCommand(requestId, transactionId) {
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.abort(transactionId))
    }
}

class TransactionalCreateResourceRequest(requestId: Int,transactionId: Int, val type: ReservableType, val resourceId: String, val amount: Int, val price: Int): TransactionalRequestCommand(requestId, transactionId) {
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.createResource(transactionId, type, resourceId, amount, price))
    }
}

class TransactionalUpdateResourceRequest(requestId: Int, transactionId: Int, val resourceId: String, val newAmount: Int, val newPrice: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.updateResource(transactionId, resourceId, newAmount, newPrice))
    }
}

class TransactionalReserveResourceRequest(requestId: Int, transactionId: Int, val resourceId: String, val reservationQuantity: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.reserveResource(transactionId, resourceId, reservationQuantity))
    }
}

class TransactionalDeleteResourceRequest(requestId: Int, transactionId: Int, val resourceId: String): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.deleteResource(transactionId, resourceId))
    }
}

class TransactionalQueryResourceRequest(requestId: Int, transactionId: Int, val resourceId: String): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return ResourceReply(requestId, rm.queryResource(transactionId, resourceId))
    }
}

class TransactionalUniqueCustomerIdRequest(requestId: Int, transactionId: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return IntReply(requestId, rm.uniqueCustomerId(transactionId))
    }
}

class TransactionalCreateCustomerRequest(requestId: Int, transactionId: Int, val customerId: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.createCustomer(transactionId, customerId))
    }
}

class TransactionalDeleteCustomerRequest(requestId: Int, transactionId: Int, val customerId: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.deleteCustomer(transactionId, customerId))
    }
}

class TransactionalCustomerAddReservationRequest(requestId: Int, transactionId: Int, val customerId: Int, val reservationId: Int, val resourceId: String): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.customerAddReservation(transactionId, customerId, reservationId, resourceId))
    }
}

class TransactionalCustomerRemoveReservationRequest(requestId: Int, transactionId: Int, val customerId: Int, val reservationId: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.customerRemoveReservation(transactionId, customerId, reservationId))
    }
}

class TransactionalQueryCustomerRequest(requestId: Int, transactionId: Int, val customerId: Int): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return CustomerReply(requestId, rm.queryCustomer(transactionId, customerId))
    }
}

class TransactionalItineraryRequest(requestId: Int, transactionId: Int, val customerId: Int, val reservationResources: MutableMap<Int, String>): TransactionalRequestCommand(requestId, transactionId){
    override fun execute(rm: TransactionalResourceManager): Reply {
        return BooleanReply(requestId, rm.itinerary(transactionId, customerId, reservationResources))
    }
}