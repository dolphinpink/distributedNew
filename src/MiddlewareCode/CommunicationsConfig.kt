package MiddlewareCode

class CommunicationsConfig {
    companion object {
        val CM_REQUEST_CLUSTER = "CLIENT_MIDDLEWARE_REQUEST_CLUSTER"
        val CM_REPLY_CLUSTER = "CLIENT_MIDDLEWARE_REPLY_CLUSTER"
        val REQUEST_ID_OFFSET = 10000

        val CUSTOMER_REQUEST = "CUSTOMER_REQUEST"
        val FLIGHT_REQUEST = "FLIGHT_REQUEST"
        val HOTEL_REQUEST = "HOTEL_REQUEST"
        val CAR_REQUEST = "CAR_REQUEST"

        val CUSTOMER_REPLY = "CUSTOMER_REPLY"
        val FLIGHT_REPLY = "FLIGHT_REPLY"
        val HOTEL_REPLY = "HOTEL_REPLY"
        val CAR_REPLY = "CAR_REPLY"
    }
}