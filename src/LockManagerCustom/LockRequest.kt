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
data class LockRequest(val xId: Int, val objectId: String, var type: LockType, var creationTime: Date)