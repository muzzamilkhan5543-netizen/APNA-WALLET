package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AccountStatus
import com.example.data.local.AppDatabase
import com.example.data.local.SessionPreferences
import com.example.data.local.UserRole
import com.example.data.repository.AuthResult
import com.example.data.repository.TransferResult
import com.example.data.repository.WalletRepository
import com.example.security.SecurityUtils
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: WalletRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = WalletRepository(context, db, SessionPreferences(context))
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app name matches Apna Wallet`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Apna Wallet", appName)
    }

    @Test
    fun `admin login registration approval bonus and atomic transfer work end to end`() = runTest {
        repo.ensureInitialized()

        // 1. Verify Admin login with exact credentials
        val adminLogin = repo.loginWithMpin("03705590963", "7174")
        assertTrue(adminLogin is AuthResult.Success)
        val admin = (adminLogin as AuthResult.Success).user
        assertEquals("ABID EJAZ", admin.fullName)
        assertEquals(UserRole.ADMIN, admin.role)
        assertTrue(admin.isUnlimitedBalance)

        // 2. Register two users manually -> both start as PENDING
        val reg1 = repo.registerUser("Ali Raza", "03001112233", "35202-1234567-1", "1234", "1234", null)
        val reg2 = repo.registerUser("Sara Khan", "03004445566", "35202-7654321-2", "5678", "5678", null)
        assertTrue(reg1 is AuthResult.PendingApproval)
        assertTrue(reg2 is AuthResult.PendingApproval)

        // Pending user cannot enter app yet
        val pendingLogin = repo.loginWithMpin("03001112233", "1234")
        assertTrue(pendingLogin is AuthResult.PendingApproval)

        // 3. Admin approves both users and sends bonus to Ali
        assertTrue(repo.adminSetUserStatus(admin.phoneNumber, "03001112233", AccountStatus.APPROVED))
        assertTrue(repo.adminSetUserStatus(admin.phoneNumber, "03004445566", AccountStatus.APPROVED))
        val bonusRes = repo.adminSendBonus(admin.phoneNumber, "03001112233", 5000.0, "Welcome")
        assertTrue(bonusRes is TransferResult.Success)

        // 4. Cannot send to unregistered number
        val badTransfer = repo.executeTransfer(
            senderPhone = "03001112233",
            receiverInput = "03999999999",
            amountPkr = 500.0,
            mpin = "1234",
            methodKey = "transfer_money"
        )
        assertTrue(badTransfer is TransferResult.Error)

        // 5. Real atomic transfer between approved users
        val goodTransfer = repo.executeTransfer(
            senderPhone = "03001112233",
            receiverInput = "03004445566",
            amountPkr = 1200.0,
            mpin = "1234",
            methodKey = "transfer_money"
        )
        assertTrue(goodTransfer is TransferResult.Success)

        val aliAfter = repo.getUser("03001112233")!!
        val saraAfter = repo.getUser("03004445566")!!
        assertEquals(3800.0, aliAfter.balancePkr, 0.01)
        assertEquals(1200.0, saraAfter.balancePkr, 0.01)

        // 6. Frozen account cannot transfer
        assertTrue(repo.adminSetUserStatus(admin.phoneNumber, "03001112233", AccountStatus.FROZEN))
        val frozenTransfer = repo.executeTransfer(
            senderPhone = "03001112233",
            receiverInput = "03004445566",
            amountPkr = 100.0,
            mpin = "1234",
            methodKey = "transfer_money"
        )
        assertTrue(frozenTransfer is TransferResult.Error)
    }

    @Test
    fun `mpin hashing and qr payload parsing work securely`() {
        val hash1 = SecurityUtils.hashMpin("7174")
        val hash2 = SecurityUtils.hashMpin("7174")
        assertFalse(hash1 == hash2) // Random per-user salt
        assertTrue(SecurityUtils.verifyMpin("7174", hash1))
        assertFalse(SecurityUtils.verifyMpin("0000", hash1))

        val payload = SecurityUtils.buildQrPayload("03705590963", "ABID EJAZ")
        val parsed = SecurityUtils.parseQrPayload(payload)
        assertNotNull(parsed)
        assertEquals("03705590963", parsed?.phone)
        assertEquals("ABID EJAZ", parsed?.name)
    }
}
