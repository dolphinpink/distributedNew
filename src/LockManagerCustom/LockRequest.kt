package LockManagerCustom

import java.util.*

/**
 * a class that holds data about an acquired lock.
 *
 * Since we don't start off with a set of object locks that we can lock and unlock,
 * we instead create an LockRequest whenever a transaction needs to lock an object, and delete
 * it whenever the lock is released
 *
 * xId: the id of the transaction that has the lock
 * objectId: a unique identifier for the object that is being locked. LockRequest conflicts, etc. are detected through conflicting objectToLock
 * type: type of lock, either read or write
 * acquisitionTime: time when lock was created, i.e. started waiting for lock acquisition
 */
data class LockRequest(val xId: Int, val objectId: String, var type: LockType, var creationTime: Date) {


    // returns true if the lock argument conflicts with this lock
    fun isConflicting(potential: LockRequest): Boolean {
        when (type) {
            LockType.READ -> return potential.xId == this.xId && potential.objectId == this.objectId && potential.type == LockType.WRITE
            LockType.WRITE -> return potential.xId == this.xId && potential.objectId == this.objectId
        }
    }

    // returns true if lock argument has all permissions this lock has needs
    // e.g. returns true if this is read request and potential is read OR write request for same object
    // e.g. returns false if this is write request and potential is read request for same object
    fun hasSameRequirementsAs(potential: LockRequest): Boolean {
        when (type) {
            LockType.READ -> return potential.xId == this.xId && potential.objectId == this.objectId
            LockType.WRITE -> return potential.xId == this.xId && potential.objectId == this.objectId && potential.type == LockType.WRITE
        }
    }
}