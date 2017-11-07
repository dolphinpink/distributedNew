package ServerCode

data class Reservation(val reservationId: String,
                       val item: ReservableItem,
                       val quantity: Int,
                       val pricePaid: Int) {

    // two reservations are the same if they have the same id
    // keeps id unique
    override fun equals(other : Any?) = reservationId == (other as? Reservation)?.reservationId

    override fun hashCode() = reservationId.hashCode()
}