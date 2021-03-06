package crypto.delta.exchange.openexchange.ui.order

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import crypto.delta.exchange.openexchange.BaseViewModel
import crypto.delta.exchange.openexchange.api.DeltaRepository
import crypto.delta.exchange.openexchange.pojo.order.CreateOrderResponse

class OpenOrdersViewModel(application: Application) : BaseViewModel(application) {
    private var mutableLiveData: MutableLiveData<List<CreateOrderResponse>?>? = null
    private var deltaRepository: DeltaRepository? = null

    fun init() {
        if (deltaRepository == null) {
            deltaRepository = DeltaRepository.getInstance(this.getApplication())
        }
    }

    fun getOrders(
        apiKey: String,
        timestamp: String,
        signature: String,
        productId: String,
        state: String
    ): LiveData<List<CreateOrderResponse>?>? {
        mutableLiveData =
            deltaRepository!!.getOrders(apiKey, timestamp, signature, productId, state)
        return mutableLiveData
    }
}