package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE phoneNumber = :phone LIMIT 1")
    fun observeUserByPhone(phone: String): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun observePendingUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserIfAbsent(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET status = :status WHERE phoneNumber = :phone")
    suspend fun updateStatus(phone: String, status: AccountStatus)

    @Query("UPDATE users SET balancePkr = :newBalance WHERE phoneNumber = :phone")
    suspend fun updateBalance(phone: String, newBalance: Double)

    @Query("UPDATE users SET profileImageUri = :uri WHERE phoneNumber = :phone")
    suspend fun updateProfileImage(phone: String, uri: String?)

    @Query("UPDATE users SET cardBackgroundUri = :uri WHERE phoneNumber = :phone")
    suspend fun updateCardBackground(phone: String, uri: String?)
}

@Dao
interface TransactionDao {
    @Query(
        "SELECT * FROM transactions WHERE senderPhone = :phone OR receiverPhone = :phone ORDER BY timestamp DESC"
    )
    fun observeUserTransactions(phone: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity): Long
}

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY sortOrder ASC, title ASC")
    fun observeAllMethods(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE isReleased = 1 ORDER BY sortOrder ASC, title ASC")
    fun observeReleasedMethods(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE methodKey = :key LIMIT 1")
    suspend fun getByKey(key: String): PaymentMethodEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(methods: List<PaymentMethodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(method: PaymentMethodEntity)

    @Query("UPDATE payment_methods SET isReleased = :released WHERE methodKey = :key")
    suspend fun setReleased(key: String, released: Boolean)

    @Query("DELETE FROM payment_methods WHERE methodKey = :key AND isBuiltIn = 0")
    suspend fun deleteCustomMethod(key: String)
}

@Dao
interface AdBannerDao {
    @Query("SELECT * FROM ad_banners ORDER BY createdAt DESC")
    fun observeAllBanners(): Flow<List<AdBannerEntity>>

    @Query("SELECT * FROM ad_banners WHERE expiresAt > :nowMillis ORDER BY createdAt DESC")
    fun observeActiveBanners(nowMillis: Long): Flow<List<AdBannerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: AdBannerEntity): Long

    @Query("DELETE FROM ad_banners WHERE id = :id")
    suspend fun deleteBanner(id: Long)
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1")
    fun observeConfig(): Flow<AppConfigEntity?>

    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun getConfig(): AppConfigEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(config: AppConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: AppConfigEntity)
}
