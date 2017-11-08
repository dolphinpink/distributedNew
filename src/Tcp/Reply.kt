package Tcp

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = BooleanReply::class, name = "BooleanReply"),
        JsonSubTypes.Type(value = IntReply::class, name = "IntReply"),
        JsonSubTypes.Type(value = StringReply::class, name = "StringReply"))
abstract class Reply(val requestId: Int)

class BooleanReply(requestId: Int, val value: Boolean): Reply(requestId)

class IntReply(requestId: Int, val value: Int): Reply(requestId)

class StringReply(requestId: Int, val value: String): Reply(requestId)