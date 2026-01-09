package com.faturatakip.faturatakip0.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.faturatakip.faturatakip0.R
import com.faturatakip.faturatakip0.data.AppDatabase
import com.faturatakip.faturatakip0.ui.MainActivity
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val invoices = database.invoiceDao().getAllInvoicesSync()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val tomorrowStart = calendar.timeInMillis
        val oneDayInMs = TimeUnit.DAYS.toMillis(1)
        val tomorrowEnd = tomorrowStart + oneDayInMs

        invoices.filter { !it.isPaid }.forEach { invoice ->
            if (invoice.dueDate in tomorrowStart until tomorrowEnd) {
                sendNotification(
                    invoice.category,
                    "${invoice.name} faturanızın son ödeme tarihine 1 gün kaldı! Tutar: ${invoice.amount.formatCurrency()}"
                )
            }
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "fatura_hatirlatici_kanali"

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Fatura Hatırlatıcı",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_calendar)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true) // Tıklandığında bildirimi panelden siler
            .setContentIntent(pendingIntent) // KRİTİK: Tıklama eylemini bağladık
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}