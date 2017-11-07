package ServerCode

// val is immutable, var is mutable
data class ReservableItem(val type: ReservableType,
                          val id: String, // location for {hotels, cars}, flightnum for {flights}
                          var totalQuantity: Int,
                          var price: Int) {

    // two items are the same if they have the same id
    // prevents two of the same type of item from being created
    override fun equals(other : Any?) = id == (other as? ReservableItem)?.id

    override fun hashCode() = id.hashCode()

}