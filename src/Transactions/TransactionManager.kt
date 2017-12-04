package Transactions

import ResourceManagerCode.ReservableType
import ResourceManagerCode.ResourceManager
import MiddlewareCode.*
import java.util.*

class TransactionManager(val resourceType: MutableMap<String, ReservableType> = mutableMapOf(),
                         private val customerRm: ResourceManager,
                         private val flightRm: ResourceManager,
                         private val hotelRm: ResourceManager,
                         private val carRm: ResourceManager) {

    private val rm = Middleware(CommunicationsConfig.REQUEST_ID_OFFSET, resourceType, customerRm, flightRm, hotelRm, carRm)

    private val requestStacks: MutableMap<Int, Stack<RequestCommand>> = mutableMapOf()

    fun createTransaction(transactionId: Int): Boolean {
        synchronized(requestStacks) {
           //println("$requestStacks")
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
                val succeeded = requestStack.pop().execute(rm) as BooleanReply
               //println("TRANSACTION MANAGER popping ${succeeded.value}")

            }
            requestStacks.remove(transactionId)
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