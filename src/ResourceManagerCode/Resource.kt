package ResourceManagerCode

data class Resource(val item: ReservableItem, var remainingQuantity: Int) {
    override fun equals(other: Any?) = item == (other as? Resource)?.item
    override fun hashCode() = item.hashCode()
}
