package Tcp

abstract class Reply(val requestId: Int)

class BooleanReply(val reply: Boolean, requestId: Int): Reply(requestId)

class IntReply(val reply: Int, requestId: Int): Reply(requestId)

class StringReply(val reply: String, requestId: Int): Reply(requestId)