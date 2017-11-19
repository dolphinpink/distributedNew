package LockManagerCustom

import java.util.*

class LockManager {

    companion object {
        private val DEADLOCK_TIMEOUT = 5000
    }

    init {
        Thread {
            checkLocks()
            Thread.sleep(5)
        }.start()
    }

    private val acquiredLocks: MutableSet<LockRequest> = mutableSetOf()
    private val waitingLocks: MutableList<LockRequest> = mutableListOf()

    /**
     * returns true if lock is acquired, or was already acquired
     * doesn't throw redundant lock exceptions
     *
     * throws false if lock is not acquired (deadlocked)
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

                if (isAcquired(lock))
                    return true

                synchronized(lock) {
                    // will be notified either by acquireLock or cleanupDeadlock
                    lock.wait()
                }
            }
        }
        return (isAcquired(lock))
    }

    private fun checkLocks() {

        synchronized(waitingLocks) {
            val now = Date().time
            val deadLocked = mutableListOf<LockRequest>()
            waitingLocks.forEach { wLock ->
                if (now - wLock.creationTime.time > DEADLOCK_TIMEOUT)
                    deadLocked.add(wLock)
            }

            deadLocked.forEach { deadlock -> cleanupDeadlock(deadlock) }
        }

    }

    /**
     * if the resource is already locked for the passed lockRequest type, return that lockRequest
     * e.g. an acquired write lock for object A would make a read lock redundant
     * @param lockRequest a lockRequest that you're checking for redundancy
     * @return the lockRequest that makes the passed lockRequest redundant, or null
     */
    private fun sameLockAs(lockRequest: LockRequest): LockRequest? {
        synchronized(acquiredLocks) {
            synchronized(waitingLocks) {

                return acquiredLocks.find { acquiredLock -> lockRequest.hasSameRequirementsAs(acquiredLock) } ?:
                        waitingLocks.find { waitingLock -> lockRequest.hasSameRequirementsAs(waitingLock) }
            }
        }
    }


    private fun isAcquired(lockRequest: LockRequest): Boolean {
        synchronized(acquiredLocks) {
            return acquiredLocks.find { acquiredLocks -> lockRequest.hasSameRequirementsAs(acquiredLocks) } != null
        }
    }

    private fun addToWaitQueue(lockRequest: LockRequest) {
        synchronized(acquiredLocks) {
            synchronized(waitingLocks) {
                waitingLocks.add(lockRequest)
                println("$lockRequest added to wait queue")
                println(this.toString())
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
                println("\nunlocking for $xId")
                print(this.toString())
                acquiredLocks.removeIf { acquiredLock -> acquiredLock.xId == xId }
                //waitingLocks.removeIf { acquiredLock -> acquiredLock.xId == xId }
                println("unlocked for $xId. reacquiring")
                waitlistAcquire()
                println("reacquired, list is now ")
                println(this.toString())
                println("\n")
            }
        }

        return true
    }

    // cleanupDeadlock cleans up stampTable and waitTable, and throws DeadlockException
    private fun cleanupDeadlock(lockRequest: LockRequest) {
        lockRequest.notifyAll()
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

        // can't modify list while iterating
        val locksToDelete: MutableList<LockRequest> = mutableListOf()

        synchronized(acquiredLocks) {
            synchronized(waitingLocks) {
                waitingLocks.forEach { waitingLock ->
                    if (acquiredLocks.find { acquiredLock -> waitingLock.isConflicting(acquiredLock) } == null) {
                        acquiredLocks.removeIf { acquiredLock -> acquiredLock.hasSameRequirementsAs(waitingLock) } // remove read lock if adding same write lock
                        acquireLock(waitingLock)
                        locksToDelete.add(waitingLock)
                    }
                }
                locksToDelete.forEach { del -> waitingLocks.removeIf { ll -> ll == del } }
            }
        }
        println("notifed")
        println(locksToDelete)
        println(this.toString())
    }

    private fun acquireLock(lockRequest: LockRequest) {
        synchronized(acquiredLocks) {
            synchronized(waitingLocks) {
                acquiredLocks.add(lockRequest)
                synchronized(lockRequest) {
                    lockRequest.notifyAll()
                }
            }
        }
    }

    override fun toString(): String {

        synchronized(acquiredLocks) {
            synchronized(waitingLocks) {
                var str = ""
                acquiredLocks.forEach { ll -> str += "acqu $ll   ${System.identityHashCode(ll)}\n" }
                waitingLocks.forEach { ll -> str += "wait $ll   ${System.identityHashCode(ll)}\n" }
                return str
            }
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun Any.wait() = (this as java.lang.Object).wait()

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun Any.notifyAll() = (this as java.lang.Object).notifyAll()

}
