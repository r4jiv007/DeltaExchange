package crypto.delta.exchange.openexchange.ui.order

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.ExponentialWithJitterBackoffStrategy
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import crypto.delta.exchange.openexchange.BaseViewModel
import crypto.delta.exchange.openexchange.api.DeltaExchangeSocketServiceRepository
import crypto.delta.exchange.openexchange.api.DeltaRepository
import crypto.delta.exchange.openexchange.pojo.*
import crypto.delta.exchange.openexchange.utils.AppPreferenceManager
import crypto.delta.exchange.openexchange.utils.Native
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class OrderBookViewModel(application: Application) : BaseViewModel(application) {
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var deltaExchangeSocketServiceRepository: DeltaExchangeSocketServiceRepository? = null
    private var deltaRepository: DeltaRepository? = null
    private lateinit var appPreferenceManager: AppPreferenceManager
    private val backoffStrategy = ExponentialWithJitterBackoffStrategy(5000, 5000)
    private val foreground: Lifecycle = AndroidLifecycle.ofApplicationForeground(application)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE))
        .build()

    fun init() {
        appPreferenceManager = AppPreferenceManager(this.getApplication())
        deltaRepository = DeltaRepository.getInstance(this.getApplication())
        deltaExchangeSocketServiceRepository = Scarlet.Builder()
            .webSocketFactory(okHttpClient.newWebSocketFactory(Native.deltaExchangeBaseWebSocketUrl))
            .addMessageAdapterFactory(MoshiMessageAdapter.Factory())
            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
            .backoffStrategy(backoffStrategy)
            .lifecycle(foreground)
            .build()
            .create()
    }

    fun getRecentTrade(productId: String): LiveData<OrderBookResponse?> {
        return deltaRepository!!.getOrderBook(productId)
    }

    fun observeWebSocketEvent() {
        deltaExchangeSocketServiceRepository!!.observeWebSocketEvent()
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it is WebSocket.Event.OnConnectionOpened<*>) {
                    val channel2 = Channel()
                    channel2.name = "l2_orderbook"
                    channel2.symbols = arrayListOf(appPreferenceManager.currentProductSymbol!!)
                    val payload = Payload()
                    payload.channels = arrayListOf(channel2)
                    val subscribe = Subscribe(
                        "subscribe",
                        payload
                    )
                    // Subscribe
                    sendSubscribe(subscribe)
                }
            }, { error ->
                Log.e(
                    OrderBookViewModel::class.java.simpleName,
                    "Error while observing socket ${error.cause}"
                )
                error.printStackTrace()
            }).addTo(disposables)
    }

    fun observeOrderBook(): MutableLiveData<DeltaExchangeL2OrderBookResponse> {
        val test: MutableLiveData<DeltaExchangeL2OrderBookResponse> =
            MutableLiveData<DeltaExchangeL2OrderBookResponse>()
        deltaExchangeSocketServiceRepository!!.observeOrderBook()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ any ->
                if (!any.toString().contains("subscription".toRegex())) {
                    if (null != any) {
                        val data = any.toString().replace("size", "d_size")
                        val gson = Gson()
                        val response =
                            gson.fromJson(
                                data,
                                DeltaExchangeL2OrderBookResponse::class.java
                            )
                        if (response.buy!!.size > 12) {
                            response.buy =
                                response.buy!!.slice(IntRange(1, 12)).sortedBy {
                                    it.d_size
                                }
                        }
                        if (response.sell!!.size > 12) {
                            response.sell =
                                response.sell!!.slice(IntRange(1, 12)).sortedBy {
                                    it.d_size
                                }
                        }
                        test.value = response
                    }
                }
            }, { error ->
                Log.e(
                    OrderBookViewModel::class.java.simpleName,
                    "Error while observing ticker ${error.cause}"
                )
                error.printStackTrace()
            }).addTo(disposables)
        return test
    }

    private fun sendSubscribe(action: Subscribe) {
        deltaExchangeSocketServiceRepository!!.sendSubscribe(action)
    }


    fun sendUnSubscribe(action: Subscribe) {
        deltaExchangeSocketServiceRepository!!.sendUnSubscribe(action)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}