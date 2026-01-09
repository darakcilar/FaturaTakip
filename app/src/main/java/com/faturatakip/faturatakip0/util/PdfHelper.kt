package com.faturatakip.faturatakip0.util

import android.content.Context
import android.net.Uri
import com.faturatakip.faturatakip0.data.Invoice
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import java.text.SimpleDateFormat
import java.util.*

object PdfHelper {
    fun createInvoicePdf(context: Context, uri: Uri, invoices: List<Invoice>, title: String) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
            val writer = PdfWriter(outputStream)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            document.add(Paragraph(title).setTextAlignment(TextAlignment.CENTER).setFontSize(18f).setBold())
            document.add(Paragraph("Rapor Tarihi: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())}").setTextAlignment(TextAlignment.RIGHT))

            val table = Table(floatArrayOf(3f, 2f, 2f)).useAllAvailableWidth()
            table.addHeaderCell("Fatura")
            table.addHeaderCell("Tarih")
            table.addHeaderCell("Tutar")

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            invoices.forEach {
                table.addCell(it.name)
                table.addCell(sdf.format(Date(it.dueDate)))
                table.addCell(it.amount.formatCurrency())
            }

            document.add(table)
            document.add(Paragraph("\nToplam: ${invoices.sumOf { it.amount }.formatCurrency()}").setBold().setTextAlignment(TextAlignment.RIGHT))
            document.close()
        } catch (e: Exception) { e.printStackTrace() }
    }
}