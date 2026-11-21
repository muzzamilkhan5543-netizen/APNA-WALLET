package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.local.AccountStatus
import com.example.data.local.AdBannerEntity
import com.example.data.local.AppConfigEntity
import com.example.data.local.AppDatabase
import com.example.data.local.PaymentMethodEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.security.SecurityUtils
import com.example.util.QrAndMediaUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.random.Random

sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class PendingApproval(val user: UserEntity) : AuthResult()
    data class Rejected(val user: UserEntity) : AuthResult()
    data class Suspended(val user: UserEntity) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class TransferResult {
    data class Success(val transaction: TransactionEntity) : TransferResult()
    data class Error(val message: String) : TransferResult()
}

class WalletRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val sessionPrefs: SessionPreferences
) {
    private val userDao = db.userDao()
    private val txDao = db.transactionDao()
    private val methodDao = db.paymentMethodDao()
    private val adDao = db.adBannerDao()
    private val configDao = db.appConfigDao()

    val rememberedPhoneFlow: Flow<String?> = sessionPrefs.rememberedPhoneFlow
    val appConfigFlow: Flow<AppConfigEntity> = configDao.observeConfig().map { it ?: AppConfigEntity() }
    val allPaymentMethodsFlow: Flow<List<PaymentMethodEntity>> = methodDao.observeAllMethods()
    val releasedPaymentMethodsFlow: Flow<List<PaymentMethodEntity>> = methodDao.observeReleasedMethods()
    val allUsersFlow: Flow<List<UserEntity>> = userDao.observeAllUsers()
    val pendingUsersFlow: Flow<List<UserEntity>> = userDao.observePendingUsers()
    val allTransactionsFlow: Flow<List<TransactionEntity>> = txDao.observeAllTransactions()
    val allAdBannersFlow: Flow<List<AdBannerEntity>> = adDao.observeAllBanners()

    fun observeActiveAdBanners(nowMillis: Long): Flow<List<AdBannerEntity>> =
        adDao.observeActiveBanners(nowMillis)

    fun observeUser(phone: String): Flow<UserEntity?> = userDao.observeUserByPhone(phone)

    fun observeUserTransactions(phone: String): Flow<List<TransactionEntity>> =
        txDao.observeUserTransactions(phone)

    suspend fun getUser(phone: String): UserEntity? =
        userDao.getUserByPhone(SecurityUtils.normalizePhone(phone))

    suspend fun setRememberedPhone(phone: String?) = sessionPrefs.setRememberedPhone(phone)

    suspend fun ensureInitialized() {
        configDao.insertIfAbsent(AppConfigEntity())

        val adminPhone = "03705590963"
        val existingAdmin = userDao.getUserByPhone(adminPhone)
        if (existingAdmin == null) {
            userDao.insertUserIfAbsent(
                UserEntity(
                    phoneNumber = adminPhone,
                    fullName = "ABID EJAZ",
                    cnic = "35202-0000000-1",
                    mpinHash = SecurityUtils.hashMpin("7174"),
                    role = UserRole.ADMIN,
                    status = AccountStatus.APPROVED,
                    balancePkr = 999_999_999.0,
                    isUnlimitedBalance = true,
                    debitCardNumber = SecurityUtils.generateCardNumber("4214"),
                    debitCardExpiry = "12/30",
                    debitCardCvc = SecurityUtils.generateCvc(),
                    creditCardNumber = SecurityUtils.generateCardNumber("5412"),
                    creditCardExpiry = "12/31",
                    creditCardCvc = SecurityUtils.generateCvc()
                )
            )
        }

        val defaultMethods = listOf(
            PaymentMethodEntity(
                methodKey = "transfer_money",
                title = "Transfer Money",
                subtitle = "Send to Registered User",
                category = "TRANSFER",
                iconKey = "send",
                isReleased = true,
                isBuiltIn = true,
                sortOrder = 1
            ),
            PaymentMethodEntity(
                methodKey = "jazzcash",
                title = "JazzCash",
                subtitle = "Send to JazzCash Wallet",
                category = "WALLET",
                iconKey = "wallet",
                isReleased = false,
                isBuiltIn = true,
                sortOrder = 2
            ),
            PaymentMethodEntity(
                methodKey = "easypaisa",
                title = "EasyPaisa",
                subtitle = "Send to EasyPaisa Account",
                category = "WALLET",
                iconKey = "wallet",
                isReleased = false,
                isBuiltIn = true,
                sortOrder = 3
            ),
            PaymentMethodEntity(
                methodKey = "sadapay",
                title = "SadaPay",
                subtitle = "Instant SadaPay Transfer",
                category = "WALLET",
                iconKey = "flash",
                isReleased = false,
                isBuiltIn = true,
                sortOrder = 4
            ),
            PaymentMethodEntity(
                methodKey = "mobile_load",
                title = "Mobile Load",
                subtitle = "Jazz, Zong, Telenor, Ufone",
                category = "TOPUP",
                iconKey = "phone",
                isReleased = false,
                isBuiltIn = true,
                sortOrder = 5
            ),
            PaymentMethodEntity(
                methodKey = "bill_payment",
                title = "Bill Payment",
                subtitle = "Electricity, Gas, Water, Net",
                category = "UTILITY",
                iconKey = "receipt",
                isReleased = false,
                isBuiltIn = true,
                sortOrder = 6
            ),
            PaymentMethodEntity(
                methodKey = "bank_transfer",
                title = "Bank Transfer",
                subtitle = "All Pakistani Banks (IBFT)",
                category = "BANK",
                iconKey = "bank",
                isReleased = false,
                isBuiltIn = true,
                sortOrder = 7
            ),
            PaymentMethodEntity(
                methodKey = "add_balance",
                title = "Add Balance",
                subtitle = "Cash Deposit / Top-Up",
                category = "TOPUP",
                iconKey = "add_money",
                isReleased = false,
                isBuiltIn = true,
                sortOrder = 8
            )
        )
        methodDao.insertIfAbsent(defaultMethods)
    }

    suspend fun registerUser(
        fullName: String,
        rawPhone: String,
        rawCnic: String,
        mpin: String,
        confirmMpin: String,
        dpPickerUri: Uri?
    ): AuthResult {
        val cleanName = fullName.trim()
        val phone = SecurityUtils.normalizePhone(rawPhone)
        val formattedCnic = SecurityUtils.formatCnic(rawCnic)

        if (cleanName.length < 3) {
            return AuthResult.Error("Enter your full legal name as printed on CNIC.")
        }
        if (!SecurityUtils.isValidPakistaniPhone(phone)) {
            return AuthResult.Error("Enter a valid 11-digit mobile number (03XXXXXXXXX).")
        }
        if (!SecurityUtils.isValidCnic(formattedCnic)) {
            return AuthResult.Error("Enter a valid 13-digit CNIC number.")
        }
        if (mpin.length != 4 || !mpin.all { it.isDigit() }) {
            return AuthResult.Error("MPIN must be exactly 4 digits.")
        }
        if (mpin != confirmMpin) {
            return AuthResult.Error("MPIN and Confirm MPIN do not match.")
        }

        val existing = userDao.getUserByPhone(phone)
        if (existing != null) {
            return AuthResult.Error("This mobile number is already registered.")
        }

        val savedDpPath = dpPickerUri?.let {
            QrAndMediaUtils.copyUriToInternalStorage(context, it, "dp_$phone")
        }

        val newUser = UserEntity(
            phoneNumber = phone,
            fullName = cleanName,
            cnic = formattedCnic,
            mpinHash = SecurityUtils.hashMpin(mpin),
            role = UserRole.USER,
            status = AccountStatus.PENDING,
            balancePkr = 0.0,
            isUnlimitedBalance = false,
            profileImageUri = savedDpPath,
            debitCardNumber = SecurityUtils.generateCardNumber("4214"),
            debitCardExpiry = SecurityUtils.generateExpiry(),
            debitCardCvc = SecurityUtils.generateCvc(),
            creditCardNumber = SecurityUtils.generateCardNumber("5412"),
            creditCardExpiry = SecurityUtils.generateExpiry(),
            creditCardCvc = SecurityUtils.generateCvc()
        )
        userDao.insertUser(newUser)
        sessionPrefs.setRememberedPhone(phone)
        return AuthResult.PendingApproval(newUser)
    }

    suspend fun loginWithMpin(rawPhone: String, mpin: String): AuthResult {
        val phone = SecurityUtils.normalizePhone(rawPhone)
        if (!SecurityUtils.isValidPakistaniPhone(phone)) {
            return AuthResult.Error("Enter a valid 11-digit mobile number.")
        }
        if (mpin.length != 4) {
            return AuthResult.Error("Enter your 4-digit MPIN.")
        }
        val user = userDao.getUserByPhone(phone)
            ?: return AuthResult.Error("No account found for $phone.")

        if (!SecurityUtils.verifyMpin(mpin, user.mpinHash)) {
            return AuthResult.Error("Incorrect MPIN. Please try again.")
        }

        sessionPrefs.setRememberedPhone(user.phoneNumber)

        return when (user.status) {
            AccountStatus.PENDING -> AuthResult.PendingApproval(user)
            AccountStatus.REJECTED -> AuthResult.Rejected(user)
            AccountStatus.SUSPENDED -> AuthResult.Suspended(user)
            AccountStatus.APPROVED, AccountStatus.FROZEN -> AuthResult.Success(user)
        }
    }

    suspend fun executeTransfer(
        senderPhone: String,
        receiverInput: String,
        amountPkr: Double,
        mpin: String,
        methodKey: String,
        note: String = ""
    ): TransferResult {
        if (amountPkr <= 0.0) {
            return TransferResult.Error("Enter a valid amount greater than Rs. 0.")
        }
        if (mpin.length != 4) {
            return TransferResult.Error("Enter your 4-digit security MPIN.")
        }

        return db.withTransaction {
            val config = configDao.getConfig() ?: AppConfigEntity()
            val sender = userDao.getUserByPhone(SecurityUtils.normalizePhone(senderPhone))
                ?: return@withTransaction TransferResult.Error("Sender account not found.")

            if (!config.globalPaymentsEnabled && sender.role != UserRole.ADMIN) {
                return@withTransaction TransferResult.Error("Payments are temporarily paused by Admin.")
            }

            if (sender.status == AccountStatus.FROZEN) {
                return@withTransaction TransferResult.Error("Your account is frozen by Admin. Transfers are disabled.")
            }
            if (sender.status != AccountStatus.APPROVED) {
                return@withTransaction TransferResult.Error("Your account is not active for transactions.")
            }
            if (!SecurityUtils.verifyMpin(mpin, sender.mpinHash)) {
                return@withTransaction TransferResult.Error("Invalid MPIN. Transaction declined.")
            }

            val method = methodDao.getByKey(methodKey)
            val methodTitle = method?.title ?: "Transfer Money"
            if (method != null && !method.isReleased && sender.role != UserRole.ADMIN) {
                return@withTransaction TransferResult.Error("$methodTitle is not released by Admin yet.")
            }

            if (methodKey == "add_balance") {
                val updatedBalance = sender.balancePkr + amountPkr
                userDao.updateBalance(sender.phoneNumber, updatedBalance)
                val tx = TransactionEntity(
                    transactionRef = generateRef(),
                    senderPhone = "SYSTEM-TOPUP",
                    senderName = "Apna Wallet Top-Up",
                    receiverPhone = sender.phoneNumber,
                    receiverName = sender.fullName,
                    amountPkr = amountPkr,
                    feePkr = 0.0,
                    methodKey = methodKey,
                    methodTitle = methodTitle,
                    note = note.ifBlank { "Wallet Balance Added" },
                    status = "SUCCESS"
                )
                val id = txDao.insertTransaction(tx)
                return@withTransaction TransferResult.Success(tx.copy(id = id))
            }

            if (!sender.isUnlimitedBalance && sender.balancePkr < amountPkr) {
                return@withTransaction TransferResult.Error("Insufficient balance. Available: Rs. ${"%,.2f".format(sender.balancePkr)}")
            }

            val cleanReceiver = SecurityUtils.normalizePhone(receiverInput)

            if (methodKey == "transfer_money") {
                if (!SecurityUtils.isValidPakistaniPhone(cleanReceiver)) {
                    return@withTransaction TransferResult.Error("Enter a valid 11-digit registered number.")
                }
                if (cleanReceiver == sender.phoneNumber) {
                    return@withTransaction TransferResult.Error("You cannot transfer money to your own number.")
                }
                val receiver = userDao.getUserByPhone(cleanReceiver)
                    ?: return@withTransaction TransferResult.Error("Receiver $cleanReceiver is not registered on ${config.walletName}.")
                if (receiver.status != AccountStatus.APPROVED) {
                    return@withTransaction TransferResult.Error("Receiver account is ${receiver.status.name.lowercase()} and cannot receive funds.")
                }

                if (!sender.isUnlimitedBalance) {
                    userDao.updateBalance(sender.phoneNumber, sender.balancePkr - amountPkr)
                }
                if (!receiver.isUnlimitedBalance) {
                    userDao.updateBalance(receiver.phoneNumber, receiver.balancePkr + amountPkr)
                }

                val tx = TransactionEntity(
                    transactionRef = generateRef(),
                    senderPhone = sender.phoneNumber,
                    senderName = sender.fullName,
                    receiverPhone = receiver.phoneNumber,
                    receiverName = receiver.fullName,
                    amountPkr = amountPkr,
                    feePkr = 0.0,
                    methodKey = methodKey,
                    methodTitle = methodTitle,
                    note = note.ifBlank { "Wallet Transfer" },
                    status = "SUCCESS"
                )
                val id = txDao.insertTransaction(tx)
                return@withTransaction TransferResult.Success(tx.copy(id = id))
            } else {
                // Other Admin-released payment methods (JazzCash, EasyPaisa, SadaPay, Mobile Load, Bill Payment, Bank Transfer, Custom)
                val targetLabel = receiverInput.trim()
                if (targetLabel.length < 4) {
                    return@withTransaction TransferResult.Error("Enter a valid account, mobile, or consumer number.")
                }

                val maybeRegistered = if (SecurityUtils.isValidPakistaniPhone(cleanReceiver)) {
                    userDao.getUserByPhone(cleanReceiver)
                } else null

                if (maybeRegistered != null && maybeRegistered.phoneNumber == sender.phoneNumber) {
                    return@withTransaction TransferResult.Error("Cannot send to your own number.")
                }

                if (!sender.isUnlimitedBalance) {
                    userDao.updateBalance(sender.phoneNumber, sender.balancePkr - amountPkr)
                }
                if (maybeRegistered != null && maybeRegistered.status == AccountStatus.APPROVED && !maybeRegistered.isUnlimitedBalance) {
                    userDao.updateBalance(maybeRegistered.phoneNumber, maybeRegistered.balancePkr + amountPkr)
                }

                val receiverName = maybeRegistered?.fullName ?: "$methodTitle ($targetLabel)"
                val receiverPhone = maybeRegistered?.phoneNumber ?: targetLabel

                val tx = TransactionEntity(
                    transactionRef = generateRef(),
                    senderPhone = sender.phoneNumber,
                    senderName = sender.fullName,
                    receiverPhone = receiverPhone,
                    receiverName = receiverName,
                    amountPkr = amountPkr,
                    feePkr = 0.0,
                    methodKey = methodKey,
                    methodTitle = methodTitle,
                    note = note.ifBlank { methodTitle },
                    status = "SUCCESS"
                )
                val id = txDao.insertTransaction(tx)
                return@withTransaction TransferResult.Success(tx.copy(id = id))
            }
        }
    }

    suspend fun adminSendBonus(adminPhone: String, targetPhone: String, amountPkr: Double, note: String): TransferResult {
        if (amountPkr <= 0.0) return TransferResult.Error("Amount must be greater than 0.")
        return db.withTransaction {
            val admin = userDao.getUserByPhone(adminPhone)
            if (admin == null || admin.role != UserRole.ADMIN) {
                return@withTransaction TransferResult.Error("Unauthorized: Admin access required.")
            }
            val target = userDao.getUserByPhone(SecurityUtils.normalizePhone(targetPhone))
                ?: return@withTransaction TransferResult.Error("User not found.")
            if (!target.isUnlimitedBalance) {
                userDao.updateBalance(target.phoneNumber, target.balancePkr + amountPkr)
            }
            val tx = TransactionEntity(
                transactionRef = generateRef(),
                senderPhone = admin.phoneNumber,
                senderName = "${admin.fullName} (Admin Bonus)",
                receiverPhone = target.phoneNumber,
                receiverName = target.fullName,
                amountPkr = amountPkr,
                feePkr = 0.0,
                methodKey = "admin_bonus",
                methodTitle = "Admin Bonus",
                note = note.ifBlank { "Bonus credited by Admin" },
                status = "SUCCESS"
            )
            val id = txDao.insertTransaction(tx)
            TransferResult.Success(tx.copy(id = id))
        }
    }

    suspend fun adminSetUserStatus(adminPhone: String, targetPhone: String, status: AccountStatus): Boolean {
        val admin = userDao.getUserByPhone(adminPhone)
        if (admin == null || admin.role != UserRole.ADMIN || targetPhone == adminPhone) return false
        userDao.updateStatus(targetPhone, status)
        return true
    }

    suspend fun adminUpdateConfig(adminPhone: String, newConfig: AppConfigEntity): Boolean {
        val admin = userDao.getUserByPhone(adminPhone)
        if (admin == null || admin.role != UserRole.ADMIN) return false
        configDao.upsertConfig(newConfig.copy(id = 1))
        return true
    }

    suspend fun adminToggleMethod(adminPhone: String, methodKey: String, released: Boolean): Boolean {
        val admin = userDao.getUserByPhone(adminPhone)
        if (admin == null || admin.role != UserRole.ADMIN) return false
        methodDao.setReleased(methodKey, released)
        return true
    }

    suspend fun adminAddCustomMethod(
        adminPhone: String,
        title: String,
        subtitle: String,
        category: String,
        iconKey: String,
        releaseImmediately: Boolean
    ): Boolean {
        val admin = userDao.getUserByPhone(adminPhone)
        if (admin == null || admin.role != UserRole.ADMIN) return false
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return false
        val key = "custom_${UUID.randomUUID().toString().take(8)}"
        methodDao.upsert(
            PaymentMethodEntity(
                methodKey = key,
                title = cleanTitle,
                subtitle = subtitle.ifBlank { "Custom Payment Method" },
                category = category,
                iconKey = iconKey,
                isReleased = releaseImmediately,
                isBuiltIn = false,
                sortOrder = 50
            )
        )
        return true
    }

    suspend fun adminDeleteCustomMethod(adminPhone: String, methodKey: String): Boolean {
        val admin = userDao.getUserByPhone(adminPhone)
        if (admin == null || admin.role != UserRole.ADMIN) return false
        methodDao.deleteCustomMethod(methodKey)
        return true
    }

    suspend fun adminPublishAdBanner(
        adminPhone: String,
        title: String,
        mediaPickerUri: Uri,
        isVideo: Boolean,
        targetUrl: String,
        durationLabel: String,
        durationMillis: Long
    ): Result<Unit> {
        val admin = userDao.getUserByPhone(adminPhone)
        if (admin == null || admin.role != UserRole.ADMIN) {
            return Result.failure(IllegalAccessException("Unauthorized"))
        }
        val normalizedUrl = SecurityUtils.normalizeWebUrl(targetUrl)
        if (!SecurityUtils.isSafeWebUrl(normalizedUrl)) {
            return Result.failure(IllegalArgumentException("Enter a valid http:// or https:// website URL."))
        }
        val copiedPath = QrAndMediaUtils.copyUriToInternalStorage(
            context = context,
            uri = mediaPickerUri,
            prefix = if (isVideo) "ad_vid" else "ad_img"
        ) ?: return Result.failure(IllegalStateException("Failed to import media from Gallery."))

        val now = System.currentTimeMillis()
        adDao.insertBanner(
            AdBannerEntity(
                title = title.trim().ifBlank { "Featured Offer" },
                mediaUri = copiedPath,
                isVideo = isVideo,
                targetUrl = normalizedUrl,
                durationLabel = durationLabel,
                createdAt = now,
                expiresAt = now + durationMillis
            )
        )
        return Result.success(Unit)
    }

    suspend fun adminDeleteAdBanner(adminPhone: String, bannerId: Long): Boolean {
        val admin = userDao.getUserByPhone(adminPhone)
        if (admin == null || admin.role != UserRole.ADMIN) return false
        adDao.deleteBanner(bannerId)
        return true
    }

    suspend fun updateProfileImage(phone: String, pickerUri: Uri): String? {
        val path = QrAndMediaUtils.copyUriToInternalStorage(context, pickerUri, "dp_$phone") ?: return null
        userDao.updateProfileImage(phone, path)
        return path
    }

    suspend fun updateCardBackground(phone: String, pickerUri: Uri?): String? {
        val path = pickerUri?.let {
            QrAndMediaUtils.copyUriToInternalStorage(context, it, "card_$phone")
        }
        userDao.updateCardBackground(phone, path)
        return path
    }

    private fun generateRef(): String {
        val tail = System.currentTimeMillis().toString().takeLast(6)
        val rand = Random.nextInt(100, 999)
        return "AW-$tail-$rand"
    }
}
