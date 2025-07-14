package com.furkandarakcilar.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Invoice::class],
    version = 4,             // 1→2 owner, 2→3 category
    exportSchema = false
)
abstract class InvoiceDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile private var INSTANCE: InvoiceDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE invoice_table " +
                            "ADD COLUMN owner TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE invoice_table " +
                            "ADD COLUMN category TEXT NOT NULL DEFAULT 'DIGER'"
                )
            }
        }
        private val MIGRATION_3_4 = object: Migration(3,4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE invoice_table " +
                            "ADD COLUMN isPaid INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): InvoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): InvoiceDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                InvoiceDatabase::class.java,
                "invoice_database"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3,MIGRATION_3_4)
                .build()
        }
    }
}
