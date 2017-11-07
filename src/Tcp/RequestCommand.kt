package Tcp

import ServerCode.ReservableType
import ServerCode.ResourceManager
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
        JsonSubTypes.Type(value = UpdateResourceRequest::class, name = "UpdateResourceRequest"))
interface RequestCommand {
    abstract fun execute(rm: ResourceManager)
}

class CreateResourceRequest(val requestId: Int, val type: ReservableType, val resourceId: String, val amount: Int, val price: Int): RequestCommand {
    override fun execute(rm: ResourceManager) {
        rm.createResource(type, resourceId, amount, price)
        println("requestCommand created Resource")
    }
}


class UpdateResourceRequest(val requestId: Int, val resourceId: String, val newAmount: Int, val newPrice: Int): RequestCommand {
    override fun execute(rm: ResourceManager) {
        rm.updateResource(resourceId, newAmount, newPrice)
    }
}

class ReserveResourceRequest(val requestId: Int, val resourceId: String, val reservationQuantity: Int): RequestCommand {
    override fun execute(rm: ResourceManager) {
        rm.reserveResource(resourceId, reservationQuantity)
    }
}

class DeleteResourceRequest(val requestId: Int, val resourceId: String): RequestCommand {
    override fun execute(rm: ResourceManager) {
        rm.deleteResource(resourceId)
    }
}


class QueryResourceRequest(val requestId: Int, val resourceId: String): RequestCommand {
    override fun execute(rm: ResourceManager) {
        rm.queryResource(resourceId)
    }
}


class GetUniqueCustomerIdRequest(val requestId: Int): RequestCommand {
    override fun execute(rm: ResourceManager) {
        rm.getUniqueCustomerId()
    }
}


class CreateCustomerRequest(val requestId: Int, val customerId: String): RequestCommand {
    override fun execute(rm: ResourceManager) {
        rm.createCustomer(customerId)
    }
}


class DeleteCustomerRequest(val requestId: Int, val customerId: String): RequestCommand {
    override fun execute(rm: ResourceManager) {
        rm.deleteCustomer(customerId)
    }
}


class QueryCustomerInfoRequest(val requestId: Int, val customerId: String): RequestCommand {
    override fun execute(rm: ResourceManager) {
        rm.queryCustomerInfo(customerId)
    }
}

class CreateReservationRequest(val requestId: Int, val customerId: String, val type: ReservableType, val resourceId: String): RequestCommand {
    override fun execute(rm: ResourceManager) {
        rm.createReservation(customerId, type, resourceId)
    }
}


class ItineraryRequest(val requestId: Int, val customerId: String, val resourceIds: Set<String>): RequestCommand {
    override fun execute(rm: ResourceManager) {
        rm.itinerary(customerId, resourceIds)
    }
}