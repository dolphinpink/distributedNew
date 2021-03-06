package Tcp

import ResourceManagerCode.Customer
import ResourceManagerCode.Resource
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = BooleanReply::class, name = "BooleanReply"),
        JsonSubTypes.Type(value = IntReply::class, name = "IntReply"),
        JsonSubTypes.Type(value = CustomerReply::class, name = "CustomerReply"),
        JsonSubTypes.Type(value = ResourceReply::class, name = "ResourceReply"))
abstract class Reply(val requestId: Int)

class BooleanReply(requestId: Int, val value: Boolean): Reply(requestId) {
    override fun toString(): String = "requestId: $requestId, return value: $value"
}

class IntReply(requestId: Int, val value: Int): Reply(requestId) {
    override fun toString(): String = "requestId: $requestId, return value: $value"
}

class CustomerReply(requestId: Int, val value: Customer?): Reply(requestId) {
    override fun toString(): String = "requestId: $requestId, return value: $value"
}

class ResourceReply(requestId: Int, val value: Resource?): Reply(requestId) {
    override fun toString(): String = "requestId: $requestId, return value: $value"
}