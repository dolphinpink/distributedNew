package MiddlewareCode

import ResourceManagerCode.ReservableItem
import ResourceManagerCode.ReservableType
import ResourceManagerCode.ResourceManager
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
        JsonSubTypes.Type(value = ItineraryRequest::class, name = "ItineraryRequest"),
        JsonSubTypes.Type(value = ShutdownRequest::class, name = "ShutdownRequest"))
abstract class RequestCommand(val requestId: Int) {
    abstract fun execute(rm: ResourceManager): Reply
}

class CreateResourceRequest(requestId: Int, val type: ReservableType, val resourceId: String, val amount: Int, val price: Int): RequestCommand(requestId) {
    override fun execute(rm: ResourceManager): Reply {
        return BooleanReply(requestId, rm.createResource(type, resourceId, amount, price))
    }
}

class UpdateResourceRequest(requestId: Int, val resourceId: String, val newAmount: Int, val newPrice: Int): RequestCommand(requestId){
    override fun execute(rm: ResourceManager): Reply {
        return BooleanReply(requestId, rm.updateResource(resourceId, newAmount, newPrice))
    }
}

class ReserveResourceRequest(requestId: Int, val resourceId: String, val reservationQuantity: Int): RequestCommand(requestId){
    override fun execute(rm: ResourceManager): Reply {
        return BooleanReply(requestId, rm.reserveResource(resourceId, reservationQuantity))
    }
}

class DeleteResourceRequest(requestId: Int, val resourceId: String): RequestCommand(requestId){
    override fun execute(rm: ResourceManager): Reply {
        return BooleanReply(requestId, rm.deleteResource(resourceId))
    }
}

class QueryResourceRequest(requestId: Int, val resourceId: String): RequestCommand(requestId){
    override fun execute(rm: ResourceManager): Reply {
        return ResourceReply(requestId, rm.queryResource(resourceId))
    }
}

class UniqueCustomerIdRequest(requestId: Int): RequestCommand(requestId){
    override fun execute(rm: ResourceManager): Reply {
        return IntReply(requestId, rm.uniqueCustomerId())
    }
}

class CreateCustomerRequest(requestId: Int, val customerId: Int): RequestCommand(requestId){
    override fun execute(rm: ResourceManager): Reply {
        return BooleanReply(requestId, rm.createCustomer(customerId))
    }
}

class DeleteCustomerRequest(requestId: Int, val customerId: Int): RequestCommand(requestId){
    override fun execute(rm: ResourceManager): Reply {
        return BooleanReply(requestId, rm.deleteCustomer(customerId))
    }
}

class CustomerAddReservationRequest(requestId: Int, val customerId: Int, val reservationId: Int, val reservableItem: ReservableItem): RequestCommand(requestId){
    override fun execute(rm: ResourceManager): Reply {
        return BooleanReply(requestId, rm.customerAddReservation(customerId, reservationId, reservableItem))
    }
}

class CustomerRemoveReservationRequest(requestId: Int, val customerId: Int, val reservationId: Int): RequestCommand(requestId){
    override fun execute(rm: ResourceManager): Reply {
        return BooleanReply(requestId, rm.customerRemoveReservation(customerId, reservationId))
    }
}

class QueryCustomerRequest(requestId: Int, val customerId: Int): RequestCommand(requestId){
    override fun execute(rm: ResourceManager): Reply {
        return CustomerReply(requestId, rm.queryCustomer(customerId))
    }
}

class ItineraryRequest(requestId: Int, val customerId: Int, val reservationResources: MutableMap<Int, ReservableItem>): RequestCommand(requestId){
    override fun execute(rm: ResourceManager): Reply {
        return BooleanReply(requestId, rm.itinerary(customerId, reservationResources))
    }
}

class ShutdownRequest(requestId: Int, val name: String): RequestCommand(requestId) {
    override fun execute(rm: ResourceManager): Reply {
        rm.shutdown(name)
        return BooleanReply(requestId, true)
    }
}