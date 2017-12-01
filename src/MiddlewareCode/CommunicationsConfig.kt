package MiddlewareCode

class CommunicationsConfig {
    companion object {
        val client = 8085
        val middleware = 8086
        val flightRm = 8087
        val hotelRm = 8088
        val carRm = 8089
        val customerRm = 8090
        val clientMiddlewareCluster = "CLIENT_MIDDLEWARE_CLUSTER"
        val middlewareRmCluster = "MIDDLEWARE_RM_CLUSTER"
    }
}