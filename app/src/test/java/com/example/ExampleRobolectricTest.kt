package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Expense
import com.example.data.model.Member
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RoomieSplit", appName)
    }

    @Test
    fun testRoomDatabaseInsertAndQuery() = runBlocking {
        val dao = db.expenseDao()
        val memberId = dao.insertMember(
            Member(name = "Luka", phoneNumber = "+386 41 000 111", notes = "Room 1")
        )
        val fetchedMember = dao.getMemberById(memberId)
        assertNotNull(fetchedMember)
        assertEquals("Luka", fetchedMember?.name)

        val expenseId = dao.insertExpense(
            Expense(
                title = "Mercator Snacks",
                amountCents = 1520L,
                category = "Grocery",
                payerMemberId = memberId,
                splitMemberIds = "$memberId"
            )
        )
        val fetchedExpense = dao.getExpenseById(expenseId)
        assertNotNull(fetchedExpense)
        assertEquals("Mercator Snacks", fetchedExpense?.title)
        assertEquals("€15.20", fetchedExpense?.amountFormatted)
    }
}
