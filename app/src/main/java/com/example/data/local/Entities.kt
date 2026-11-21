package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED,
    FROZEN
}

enum class UserRole {
    USER,
    ADMIN
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val phoneNumber: String,
    val fullName: String,
    val cnic: String,
    val mpinHash: String,
    val role: UserRole = UserRole.USER,
    val status: AccountStatus = AccountStatus.PENDING,
    val balancePkr: Double = 0.0,
    val isUnlimitedBalance: Boolean = false,
    val profileImageUri: String? = null,
    val cardBackgroundUri: String? = null,
    val debitCardNumber: String,
    val debitCardExpiry: String,
    val debitCardCvc: String,
    val creditCardNumber: String,
    val creditCardExpiry: String,
    val creditCardCvc: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionRef: String,
    val senderPhone: String,
    val senderName: String,
    val receiverPhone: String,
    val receiverName: String,
    val amountPkr: Double,
    val feePkr: Double = 0.0,
    val methodKey: String,
    val methodTitle: String,
    val note: String = "",
    val status: String = "SUCCESS",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey val methodKey: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val iconKey: String,
    val isReleased: Boolean,
    val isBuiltIn: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "ad_banners")
data class AdBannerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val mediaUri: String,
    val isVideo: Boolean = false,
    val targetUrl: String,
    val durationLabel: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1,
    val walletName: String = "APNA WALLET",
    val homeIconKey: String = "wallet",
    val primaryColorHex: Long = 0xFFC8102EL,
    val accentTextColorHex: Long = 0xFFFFD100L,
    val pageBackgroundHex: Long = 0xFFFFFFFFL,
    val inputTextColorHex: Long = 0xFF000000L,
    val serviceTileSizeDp: Int = 68,
    val isQrScannerEnabled: Boolean = true,
    val isPhysicalCardEnabled: Boolean = true,
    val globalPaymentsEnabled: Boolean = true
)
