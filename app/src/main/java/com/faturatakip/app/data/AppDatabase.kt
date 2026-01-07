package com.faturatakip.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.faturatakip.app.data.db.InvoiceDao


@Database(entities = [Invoice::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // YENİ EKLENDİ: Veritabanı versiyon 1'den 2'ye geçiş için migrasyon
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 'invoices' tablosuna 'category' adında yeni bir sütun ekle.
                database.execSQL("ALTER TABLE invoices ADD COLUMN category TEXT NOT NULL DEFAULT 'Diğer'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "invoice_database"
                )
                    .addMigrations(MIGRATION_1_2) // Migrasyonu veritabanına ekle
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

