package crypto.delta.exchange.openexchange.api

import android.content.Context
import androidx.lifecycle.MutableLiveData
import crypto.delta.exchange.openexchange.pojo.DeltaExchangeChartHistoryResponse
import crypto.delta.exchange.openexchange.pojo.DeltaExchangeTickerResponse
import crypto.delta.exchange.openexchange.pojo.OrderBookResponse
import crypto.delta.exchange.openexchange.pojo.products.ProductsResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class DeltaRepository {
    companion object {
        private var deltaRepository: DeltaRepository? = null
        private var context: Context? = null

        fun getInstance(context: Context): DeltaRepository? {
            this.context = context
            if (deltaRepository == null) {
                deltaRepository = DeltaRepository()
            }
            return deltaRepository
        }
    }

    private var deltaExchangeApiEndPoints: DeltaExchangeApiEndPoints? = null

    init {
        deltaExchangeApiEndPoints = DeltaApiClient().getClient(context)!!
            .create(DeltaExchangeApiEndPoints::class.java)
    }

    fun getChartHistory(resolution: String, symbol: String, strFrom: String, strTo: String): MutableLiveData<DeltaExchangeChartHistoryResponse?> {
        val data: MutableLiveData<DeltaExchangeChartHistoryResponse?> = MutableLiveData<DeltaExchangeChartHistoryResponse?>()
        deltaExchangeApiEndPoints!!.getChartHistory(symbol, resolution, strFrom, strTo).enqueue(object :
            Callback<DeltaExchangeChartHistoryResponse?> {
            override fun onResponse(
                call: Call<DeltaExchangeChartHistoryResponse?>?,
                response: Response<DeltaExchangeChartHistoryResponse?>
            ) {
                if (response.isSuccessful) {
                    data.value = response.body()
                } else {
                    data.value = null
                }
            }

            override fun onFailure(call: Call<DeltaExchangeChartHistoryResponse?>?, t: Throwable?) {
                data.value = null
            }
        })
        return data
    }

    fun getOrderBook(productId: String): MutableLiveData<OrderBookResponse?> {
        val data: MutableLiveData<OrderBookResponse?> = MutableLiveData<OrderBookResponse?>()
        deltaExchangeApiEndPoints!!.getOrderBook(productId).enqueue(object :
            Callback<OrderBookResponse?> {
            override fun onResponse(
                call: Call<OrderBookResponse?>?,
                response: Response<OrderBookResponse?>
            ) {
                if (response.isSuccessful) {
                    data.value = response.body()
                } else {
                    data.value = null
                }
            }

            override fun onFailure(call: Call<OrderBookResponse?>?, t: Throwable?) {
                data.value = null
            }
        })
        return data
    }

    fun getProducts(disposables: CompositeDisposable): MutableLiveData<List<ProductsResponse>> {
        val data: MutableLiveData<List<ProductsResponse>> = MutableLiveData<List<ProductsResponse>>()
        deltaExchangeApiEndPoints!!.getProducts()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<List<ProductsResponse>>() {
                override fun onComplete() {
                }

                override fun onNext(response: List<ProductsResponse>) {
                    data.value = response
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    data.value = null
                }

            }).addTo(disposables)
        return data
    }

    fun getProductsData(symbol: String): MutableLiveData<DeltaExchangeTickerResponse>? {
        val data: MutableLiveData<DeltaExchangeTickerResponse> = MutableLiveData<DeltaExchangeTickerResponse>()
        deltaExchangeApiEndPoints!!.getTickers24Hrs(symbol)
            .enqueue(object :
                Callback<DeltaExchangeTickerResponse?> {
                override fun onResponse(
                    call: Call<DeltaExchangeTickerResponse?>?,
                    response: Response<DeltaExchangeTickerResponse?>
                ) {
                    if (response.isSuccessful) {
                        data.value = response.body()
                    } else {
                        data.value = null
                    }
                }

                override fun onFailure(call: Call<DeltaExchangeTickerResponse?>?, t: Throwable?) {
                    data.value = null
                }
            })
        return data
    }
}