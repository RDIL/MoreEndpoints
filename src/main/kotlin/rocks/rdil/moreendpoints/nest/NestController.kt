package rocks.rdil.moreendpoints.nest

fun ensureLeadAndTrailingSlash(route: String): String {
    return if (route.startsWith("/")) {
        if (route.endsWith("/")) {
            route
        } else {
            "$route/"
        }
    } else {
        if (route.endsWith("/")) {
            "/$route"
        } else {
            "/$route/"
        }
    }
}

data class NestController(val routeBase: String?, val parent: NestController?) {
    fun getBaseUrl(): String {
        return ensureLeadAndTrailingSlash(
            if (routeBase != null) {
                if (parent != null) {
                    parent.getBaseUrl() + routeBase
                } else {
                    routeBase
                }
            } else {
                parent?.getBaseUrl() ?: "/"
            }
        )
    }
}
