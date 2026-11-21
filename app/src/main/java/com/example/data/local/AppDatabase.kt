package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class WalletTypeConverters {
    @TypeConverter
    fun fromAccountStatus(status: AccountStatus): String = status.name

    @TypeConverter
    fun toAccountStatus(value: String): AccountStatus =
        runCatching { AccountStatus.valueOf(value) }.getOrDefault(AccountStatus.PENDING)

    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole =
        runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.USER)
}

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        PaymentMethodEntity::class,
        AdBannerEntity::class,
        AppConfigEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(WalletTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun adBannerDao(): AdBannerDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_config ADD COLUMN inputTextColorHex INTEGER NOT NULL DEFAULT 4278190080"
                )
                db.execSQL(
                    "ALTER TABLE app_config ADD COLUMN serviceTileSizeDp INTEGER NOT NULL DEFAULT 68"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apna_wallet_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
