package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Expense
import com.example.data.model.Member
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Member::class, Expense::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roomie_split_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.expenseDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: ExpenseDao) {
            // Check if already populated
            if (dao.getMemberCount() > 0) return

            // 5 housemates living together in Slovenia
            val member1 = Member(id = 1L, name = "Ajaz", phoneNumber = "+386 40 123 456", colorHex = "#1E88E5", notes = "Room 1")
            val member2 = Member(id = 2L, name = "Luka", phoneNumber = "+386 41 234 567", colorHex = "#43A047", notes = "Room 2")
            val member3 = Member(id = 3L, name = "Matej", phoneNumber = "+386 51 345 678", colorHex = "#FB8C00", notes = "Room 3")
            val member4 = Member(id = 4L, name = "Ana", phoneNumber = "+386 70 456 789", colorHex = "#E53935", notes = "Room 4")
            val member5 = Member(id = 5L, name = "Jan", phoneNumber = "+386 31 567 890", colorHex = "#8E24AA", notes = "Living Room")

            dao.insertMembers(listOf(member1, member2, member3, member4, member5))

            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L

            // Initial realistic shared apartment expenses in Slovenia
            val allIds = "1,2,3,4,5"
            val expenses = listOf(
                Expense(
                    id = 1L,
                    title = "Apartment Rent (Ljubljana)",
                    amountCents = 125000L, // €1,250.00
                    category = "Room Rent",
                    dateMillis = now - (5 * dayMillis),
                    payerMemberId = 1L, // Ajaz paid the landlord
                    notes = "Monthly flat rent transfer via NLB bank",
                    splitMemberIds = allIds
                ),
                Expense(
                    id = 2L,
                    title = "Spar Weekly Groceries",
                    amountCents = 14850L, // €148.50
                    category = "Grocery",
                    dateMillis = now - (3 * dayMillis),
                    payerMemberId = 2L, // Luka paid
                    notes = "Vegetables, milk, eggs, bread, pasta and olive oil",
                    splitMemberIds = allIds
                ),
                Expense(
                    id = 3L,
                    title = "Telemach High-Speed Internet & TV",
                    amountCents = 4290L, // €42.90
                    category = "Bills & Utilities",
                    dateMillis = now - (2 * dayMillis),
                    payerMemberId = 3L, // Matej paid
                    notes = "Monthly fiber wifi invoice",
                    splitMemberIds = allIds
                ),
                Expense(
                    id = 4L,
                    title = "Dm / Muller Household & Cleaning",
                    amountCents = 3540L, // €35.40
                    category = "Household Items",
                    dateMillis = now - (1 * dayMillis),
                    payerMemberId = 4L, // Ana paid
                    notes = "Dishwasher tablets, laundry detergent, trash bags, kitchen towels",
                    splitMemberIds = allIds
                ),
                Expense(
                    id = 5L,
                    title = "Snaga Waste & Building Maintenance",
                    amountCents = 6200L, // €62.00
                    category = "Bills & Utilities",
                    dateMillis = now - (12 * 60 * 60 * 1000L),
                    payerMemberId = 1L, // Ajaz paid
                    notes = "City utilities invoice for October",
                    splitMemberIds = allIds
                )
            )

            dao.insertExpenses(expenses)
        }
    }
}
