package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountStatus
import com.example.data.local.AdBannerEntity
import com.example.data.local.AppConfigEntity
import com.example.data.local.AppDatabase
import com.example.data.local.PaymentMethodEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.repository.AuthResult
import com.example.data.repository.TransferResult
import com.example.data.repository.WalletRepository
import com.example.security.SecurityUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val sessionPrefs = SessionPreferences(application)
    val repository = WalletRepository(application, db, sessionPrefs)

    private val _loggedInPhone = MutableStateFlow<String?>(null)
    val loggedInPhone: StateFlow<String?> = _loggedInPhone.asStateFlow()

    private val _authStatusState = MutableStateFlow<AuthResult?>(null)
    val authStatusState: StateFlow<AuthResult?> = _authStatusState.asStateFlow()

    private val _isAuthBusy = MutableStateFlow(false)
    val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessages = _toastMessages.asSharedFlow()

    private val _lastReceiptTx = MutableStateFlow<TransactionEntity?>(null)
    val lastReceiptTx: StateFlow<TransactionEntity?> = _lastReceiptTx.asStateFlow()

    val appConfig: StateFlow<AppConfigEntity> = repository.appConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfigEntity())

    val rememberedPhone: StateFlow<String?> = repository.rememberedPhoneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val rememberedUser: StateFlow<UserEntity?> = rememberedPhone
        .flatMapLatest { phone ->
            if (phone.isNullOrBlank()) flowOf(null) else repository.observeUser(phone)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUser: StateFlow<UserEntity?> = _loggedInPhone
        .flatMapLatest { phone ->
            if (phone.isNullOrBlank()) flowOf(null) else repository.observeUser(phone)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userTransactions: StateFlow<List<TransactionEntity>> = _loggedInPhone
        .flatMapLatest { phone ->
            if (phone.isNullOrBlank()) flowOf(emptyList()) else repository.observeUserTransactions(phone)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val releasedMethods: StateFlow<List<PaymentMethodEntity>> = repository.releasedPaymentMethodsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMethods: StateFlow<List<PaymentMethodEntity>> = repository.allPaymentMethodsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingUsers: StateFlow<List<UserEntity>> = repository.pendingUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAdBanners: StateFlow<List<AdBannerEntity>> = repository.allAdBannersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(10_000L)
        }
    }

    val activeAdBanners: StateFlow<List<AdBannerEntity>> = tickerFlow
        .flatMapLatest { now -> repository.observeActiveAdBanners(now) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun clearAuthStatus() {
        _authStatusState.value = null
    }

    fun login(phone: String, mpin: String) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            val result = repository.loginWithMpin(phone, mpin)
            _authStatusState.value = result
            if (result is AuthResult.Success) {
                _loggedInPhone.value = result.user.phoneNumber
            }
            _isAuthBusy.value = false
        }
    }

    fun register(
        fullName: String,
        phone: String,
        cnic: String,
        mpin: String,
        confirmMpin: String,
        dpUri: Uri?
    ) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            val result = repository.registerUser(fullName, phone, cnic, mpin, confirmMpin, dpUri)
            _authStatusState.value = result
            _isAuthBusy.value = false
        }
    }

    fun switchRememberedAccount() {
        viewModelScope.launch {
            repository.setRememberedPhone(null)
            _authStatusState.value = null
        }
    }

    fun logout() {
        _loggedInPhone.value = null
        _authStatusState.value = null
        _lastReceiptTx.value = null
    }

    suspend fun lookupRegisteredRecipient(rawPhone: String): UserEntity? {
        val normalized = SecurityUtils.normalizePhone(rawPhone)
        if (!SecurityUtils.isValidPakistaniPhone(normalized)) return null
        return repository.getUser(normalized)
    }

    fun executeTransfer(
        receiverInput: String,
        amountPkr: Double,
        mpin: String,
        methodKey: String,
        note: String = "",
        onResult: (TransferResult) -> Unit
    ) {
        val senderPhone = _loggedInPhone.value ?: run {
            onResult(TransferResult.Error("Session expired. Please login again."))
            return
        }
        viewModelScope.launch {
            val result = repository.executeTransfer(
                senderPhone = senderPhone,
                receiverInput = receiverInput,
                amountPkr = amountPkr,
                mpin = mpin,
                methodKey = methodKey,
                note = note
            )
            if (result is TransferResult.Success) {
                _lastReceiptTx.value = result.transaction
            }
            onResult(result)
        }
    }

    fun showReceipt(tx: TransactionEntity) {
        _lastReceiptTx.value = tx
    }

    fun dismissReceipt() {
        _lastReceiptTx.value = null
    }

    fun updateProfileDp(uri: Uri) {
        val phone = _loggedInPhone.value ?: return
        viewModelScope.launch {
            val saved = repository.updateProfileImage(phone, uri)
            _toastMessages.tryEmit(if (saved != null) "Profile photo updated" else "Could not update photo")
        }
    }

    fun updateCardBackground(uri: Uri?) {
        val phone = _loggedInPhone.value ?: return
        viewModelScope.launch {
            repository.updateCardBackground(phone, uri)
            _toastMessages.tryEmit(if (uri != null) "Card photo updated" else "Card photo removed")
        }
    }

    fun adminSetUserStatus(targetPhone: String, status: AccountStatus) {
        val adminPhone = _loggedInPhone.value ?: return
        viewModelScope.launch {
            if (repository.adminSetUserStatus(adminPhone, targetPhone, status)) {
                _toastMessages.tryEmit("Account $targetPhone set to ${status.name}")
            }
        }
    }

    fun adminSendBonus(targetPhone: String, amount: Double, note: String) {
        val adminPhone = _loggedInPhone.value ?: return
        viewModelScope.launch {
            when (val res = repository.adminSendBonus(adminPhone, targetPhone, amount, note)) {
                is TransferResult.Success -> _toastMessages.tryEmit("Bonus Rs. ${"%,.0f".format(amount)} sent!")
                is TransferResult.Error -> _toastMessages.tryEmit(res.message)
            }
        }
    }

    fun adminUpdateConfig(updated: AppConfigEntity) {
        val adminPhone = _loggedInPhone.value ?: return
        viewModelScope.launch {
            if (repository.adminUpdateConfig(adminPhone, updated)) {
                _toastMessages.tryEmit("App settings updated")
            }
        }
    }

    fun adminToggleMethod(methodKey: String, released: Boolean) {
        val adminPhone = _loggedInPhone.value ?: return
        viewModelScope.launch {
            if (repository.adminToggleMethod(adminPhone, methodKey, released)) {
                _toastMessages.tryEmit(if (released) "Method released to App" else "Method hidden from Users")
            }
        }
    }

    fun adminAddCustomMethod(title: String, subtitle: String, category: String, iconKey: String, releaseNow: Boolean) {
        val adminPhone = _loggedInPhone.value ?: return
        viewModelScope.launch {
            if (repository.adminAddCustomMethod(adminPhone, title, subtitle, category, iconKey, releaseNow)) {
                _toastMessages.tryEmit("Added $title")
            } else {
                _toastMessages.tryEmit("Enter a valid method name")
            }
        }
    }

    fun adminDeleteCustomMethod(methodKey: String) {
        val adminPhone = _loggedInPhone.value ?: return
        viewModelScope.launch {
            repository.adminDeleteCustomMethod(adminPhone, methodKey)
            _toastMessages.tryEmit("Method removed")
        }
    }

    fun adminPublishAdBanner(
        title: String,
        mediaUri: Uri,
        isVideo: Boolean,
        targetUrl: String,
        durationLabel: String,
        durationMillis: Long
    ) {
        val adminPhone = _loggedInPhone.value ?: return
        viewModelScope.launch {
            val res = repository.adminPublishAdBanner(
                adminPhone = adminPhone,
                title = title,
                mediaPickerUri = mediaUri,
                isVideo = isVideo,
                targetUrl = targetUrl,
                durationLabel = durationLabel,
                durationMillis = durationMillis
            )
            res.onSuccess {
                _toastMessages.tryEmit("Ad banner published for $durationLabel")
            }.onFailure {
                _toastMessages.tryEmit(it.message ?: "Failed to publish ad banner")
            }
        }
    }

    fun adminDeleteAdBanner(bannerId: Long) {
        val adminPhone = _loggedInPhone.value ?: return
        viewModelScope.launch {
            repository.adminDeleteAdBanner(adminPhone, bannerId)
            _toastMessages.tryEmit("Ad banner removed")
        }
    }

    fun notifyMessage(msg: String) {
        _toastMessages.tryEmit(msg)
    }
}
