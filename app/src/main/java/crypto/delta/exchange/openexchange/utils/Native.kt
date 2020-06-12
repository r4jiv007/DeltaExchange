package crypto.delta.exchange.openexchange.utils

object Native {
    val deltaExchangeBaseUrl: String
        @JvmStatic
        external get
    val deltaExchangeBaseWebSocketUrl: String
        @JvmStatic
        external get
    val deltaExchangeTestNetBaseUrl: String
        @JvmStatic
        external get
    val deltaExchangeTestNetBaseWebSocketUrl: String
        @JvmStatic
        external get
    init {
        System.loadLibrary("delta-exchange-lib")
    }
}