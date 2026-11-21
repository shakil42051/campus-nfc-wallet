package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.attendance.data.dao.AttendanceConfigDao
import com.example.attendance.data.dao.AttendanceDao
import com.example.attendance.data.dao.AttendanceLogDao
import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.attendance.data.entity.AttendanceEntity
import com.example.attendance.data.entity.AttendanceLogEntity
import com.example.data.local.dao.CardDao
import com.example.data.local.dao.UsageTransactionDao
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.UsageTransactionEntity

@Database(
    entities = [
        CardEntity::class,
        UsageTransactionEntity::class,
        AttendanceEntity::class,
        AttendanceConfigEntity::class,
        AttendanceLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class CardDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun transactionDao(): UsageTransactionDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun attendanceConfigDao(): AttendanceConfigDao
    abstract fun attendanceLogDao(): AttendanceLogDao

    companion object {
        @Volatile
        private var INSTANCE: CardDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_uid` ON `cards` (`uid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_transactions_cardId` ON `usage_transactions` (`cardId`)")
            }
        }

        fun getDatabase(context: Context): CardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CardDatabase::class.java,
                    "campus_nfc_wallet_db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
