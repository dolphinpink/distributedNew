package ResourceManagerCode

data class Customer(val customerId: Int) {

    override fun equals(other : Any?) = customerId == (other as? Customer)?.customerId

    override fun hashCode() = customerId.hashCode()

    override fun toString(): String {
        var reservationString = "reservations:\n"
        reservations.forEach {r -> reservationString += "$r" }
        return ("Customer(customerId=$customerId)\n$reservationString")
    }

    val reservations: MutableSet<Reservation> = mutableSetOf()

    private val lock = Any()

    fun addReservation(reservation: Reservation): Boolean {
        return synchronized(lock) {reservations.add(reservation)}
    }

    fun removeReservation(reservationId: Int): Boolean {
        return synchronized(lock) {reservations.remove(reservations.find {r -> r.reservationId == reservationId })}
    }
}
