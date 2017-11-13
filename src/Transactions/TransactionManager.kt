package Transactions

import ResourceManagerCode.ResourceManager
import Tcp.Middleware
import Tcp.RequestCommand
import java.util.*

class TransactionManager() {

    val rm = Middleware("127.0.0.1", 1000000)

    val requestStacks: MutableMap<Int, Stack<RequestCommand>> = mutableMapOf()

    fun createTransaction(transactionId: Int): Boolean {
        synchronized(requestStacks) {
            if (requestStacks.contains(transactionId)) return false

            requestStacks.put(transactionId, Stack())
            return true
        }
    }

    fun addRequest(transactionId: Int, request: RequestCommand): Boolean {
        synchronized(requestStacks) {
            requestStacks[transactionId]?.push(request) ?: return false
            return true
        }
    }

    fun abortTransaction(transactionId: Int): Boolean {
        synchronized(requestStacks) {
            val requestStack = requestStacks[transactionId] ?: return false
            while(requestStack.isNotEmpty()) {
                requestStack.pop().execute(rm)
            }
            return true
        }
    }

    fun commitTransaction(transactionId: Int): Boolean {
        synchronized(requestStacks) {
            if (requestStacks.remove(transactionId) != null)
                return true

            return false
        }
    }

    fun exists(transactionId: Int): Boolean {
        if (requestStacks[transactionId] != null)
            return true

        return false
    }



}