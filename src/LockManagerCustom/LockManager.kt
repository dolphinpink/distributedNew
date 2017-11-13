package LockManagerCustom

import java.util.*

class LockManager {

    companion object {
        private val DEADLOCK_TIMEOUT = 10000
    }

    private var acquiredLocks: MutableSet<LockRequest> = mutableSetOf()
    private var waitingLocks: MutableList<LockRequest> = mutableListOf()

    /**
     * if the resource is already locked for the passed lockRequest type, return that lockRequest
     * e.g. an acquired write lock for object A would make a read lock redundant
     * @param lockRequest a lockRequest that you're checking for redundancy
     * @return the lockRequest that makes the passed lockRequest redundant, or null
     */
    private fun sameLockAs(lockRequest: LockRequest): LockRequest? {
        synchronized(acquiredLocks) {
            synchronized(waitingLocks) {

                val acquiredLocksForObject = acquiredLocks.filter { acquiredLock -> acquiredLock.objectId == lockRequest.objectId }
                val waitingLocksForObject = waitingLocks.filter { waitingLock -> waitingLock.objectId == lockRequest.objectId }

                return when (lockRequest.type) {
                    LockType.READ -> acquiredLocksForObject.find { aLock -> aLock.xId == lockRequest.xId } ?:
                            waitingLocksForObject.find { wLock -> wLock.xId == lockRequest.xId }
                    LockType.WRITE -> acquiredLocksForObject.find { aLock -> aLock.xId == lockRequest.xId && aLock.type == lockRequest.type } ?:
                            waitingLocksForObject.find { wLock -> wLock.xId == lockRequest.xId && wLock.type == lockRequest.type }
                }
            }
        }
    }

    private fun isAcquired(lockRequest: LockRequest): Boolean {

        synchronized(acquiredLocks) {
            val acquiredLocksForObject = acquiredLocks.filter { acquiredLock -> acquiredLock.objectId == lockRequest.objectId }

            return when (lockRequest.type) {
                LockType.READ -> acquiredLocksForObject.find { aLock -> aLock.xId == lockRequest.xId } != null
                LockType.WRITE -> acquiredLocksForObject.find { aLock -> aLock.xId == lockRequest.xId && aLock.type == lockRequest.type } != null
            }
        }
    }

    private fun addToWaitQueue(lockRequest: LockRequest) {
        synchronized(acquiredLocks) {
            synchronized(waitingLocks) {
                waitingLocks.add(lockRequest)
                waitlistAcquire()
            }
        }
    }

    // remove all locks for this transaction in the lock table.
    fun unlockAll(xId: Int): Boolean {

        // if any parameter is invalid, then return false
        if (xId < 0) return false

        synchronized(acquiredLocks) {
            synchronized(waitingLocks) {
                acquiredLocks = acquiredLocks.filter { acquiredLock -> acquiredLock.xId != xId }.toMutableSet()
                waitingLocks = waitingLocks.filter { acquiredLock -> acquiredLock.xId != xId }.toMutableList()
                waitlistAcquire()
            }
        }

        return true
    }

    // cleanupDeadlock cleans up stampTable and waitTable, and throws DeadlockException
    private fun cleanupDeadlock(lockRequest: LockRequest) {
        synchronized(waitingLocks) {
            waitingLocks.remove(lockRequest)
            waitlistAcquire()
        }
    }

    /**
     * lets waitlisted locks acquire locks if they are first in queue
     * must be called everytime the waitlist or acquired locks list is modified
     */
    private fun waitlistAcquire() {
        synchronized(acquiredLocks) {
            synchronized(waitingLocks) {
                waitingLocks.forEach { wLock ->
                    when (wLock.type) {
                        LockType.READ -> {
                            if (acquiredLocks.find { aLock -> aLock.objectId == wLock.objectId && aLock.type == LockType.WRITE } == null)
                                acquireLock(wLock)
                        }
                        LockType.WRITE -> {
                            if (acquiredLocks.find { aLock -> aLock.objectId == wLock.objectId && aLock.xId != wLock.xId} == null) {
                                acquiredLocks.removeIf { aLock -> aLock.objectId == wLock.objectId && aLock.xId == wLock.xId }
                                acquireLock(wLock)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun acquireLock(lockRequest: LockRequest) {
        synchronized(acquiredLocks) {
            acquiredLocks.add(lockRequest)
        }
    }

    /**
     * returns true if lock is acquired, or was already acquired
     * doesn't throw redundant lock exceptions
     *
     * throws false if lock is not acquired (deadlockde)
     */
    fun lock(xId: Int, objectId: String, type: LockType): Boolean {

        // if any parameter is invalid, then return false
        if (xId < 0) return false

        var lock = LockRequest(xId, objectId, type, Date())

        synchronized(acquiredLocks) {
            synchronized(waitingLocks) {
                val sameLock = sameLockAs(lock)
                if (sameLock == null)
                    addToWaitQueue(lock)
                else
                    lock = sameLock
            }
        }

        while (Date().time - lock.creationTime.time < DEADLOCK_TIMEOUT) {
            if (isAcquired(lock)) {
                return true
            }
        }

        cleanupDeadlock(lock)
        println("Timeout reached for lock request: transactionId $xId, objectId $objectId, type $type. Deadlock assumed.")
        return false
    }

}
