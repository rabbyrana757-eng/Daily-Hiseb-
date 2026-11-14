package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.AccountType
import com.example.data.model.BudgetRecord
import com.example.data.model.CategoryRecord
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.domain.MonthlyReportData
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
  val transactions: List<TransactionRecord>,
  val categories: List<CategoryRecord>,
  val budgets: List<BudgetRecord>,
  val goals: List<SavingsGoalRecord>,
)

object ExportBackupManager {

  fun writeReportPdf(
    context: Context,
    outputStream: OutputStream,
    accountType: AccountType,
    report: MonthlyReportData,
    transactions: List<TransactionRecord>,
  ) {
    val pdf = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 40f

    val typeface =
      try {
        ResourcesCompat.getFont(context, R.font.hind_siliguri) ?: Typeface.DEFAULT
      } catch (_: Exception) {
        Typeface.DEFAULT
      }

    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0B6E4F")
        textSize = 20f
        this.typeface = Typeface.create(typeface, Typeface.BOLD)
      }
    val headerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F1F1A")
        textSize = 13f
        this.typeface = Typeface.create(typeface, Typeface.BOLD)
      }
    val bodyPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F2937")
        textSize = 10.5f
        this.typeface = typeface
      }
    val mutedPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280")
        textSize = 9.5f
        this.typeface = typeface
      }
    val linePaint =
      Paint().apply {
        color = Color.parseColor("#D1FAE5")
        strokeWidth = 1.5f
      }

    var pageNumber = 1
    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = 48f

    fun newPage() {
      pdf.finishPage(page)
      pageNumber++
      page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
      canvas = page.canvas
      y = 48f
    }

    canvas.drawText("My Daily হিসাব — মাসিক রিপোর্ট", margin, y, titlePaint)
    y += 22f
    canvas.drawText(
      "${accountType.labelBn} | ${DateFormats.monthNameBn(report.month)} ${report.year}",
      margin,
      y,
      headerPaint,
    )
    y += 14f
    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
    y += 22f

    // Summary KPIs
    val summaryLines =
      listOf(
        "মোট আয়: ${CurrencyFormat.formatBdt(report.totalIncome, true)}",
        "মোট খরচ: ${CurrencyFormat.formatBdt(report.totalExpense, true)}",
        "মোট সঞ্চয়: ${CurrencyFormat.formatBdt(report.totalSavings, true)}",
        "দৈনিক গড় খরচ: ${CurrencyFormat.formatBdt(report.averageDailyExpense, true)}",
        "সর্বোচ্চ খরচের খাত: ${report.highestCategory?.let { "${it.category} (${CurrencyFormat.formatBdt(it.amount)})" } ?: "নেই"}",
        "আগের মাস (${DateFormats.monthNameBn(DateFormats.previousMonth(report.year, report.month).second)}): আয় ${CurrencyFormat.formatBdt(report.prevIncome)} | খরচ ${CurrencyFormat.formatBdt(report.prevExpense)}",
      )
    summaryLines.forEach { line ->
      canvas.drawText(line, margin, y, bodyPaint)
      y += 18f
    }

    y += 10f
    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
    y += 20f
    canvas.drawText("লেনদেনের বিবরণ (${transactions.size} টি)", margin, y, headerPaint)
    y += 18f

    canvas.drawText("তারিখ", margin, y, headerPaint)
    canvas.drawText("ধরন", margin + 82f, y, headerPaint)
    canvas.drawText("ক্যাটাগরি", margin + 135f, y, headerPaint)
    canvas.drawText("মাধ্যম", margin + 275f, y, headerPaint)
    canvas.drawText("পরিমাণ", margin + 360f, y, headerPaint)
    y += 14f
    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
    y += 16f

    transactions.forEach { tx ->
      if (y > pageHeight - 50f) {
        newPage()
      }
      val typeLabel = if (tx.isIncome) "আয়" else "খরচ"
      canvas.drawText(tx.dateKey, margin, y, bodyPaint)
      canvas.drawText(typeLabel, margin + 82f, y, bodyPaint)
      canvas.drawText(tx.category.take(20), margin + 135f, y, bodyPaint)
      canvas.drawText(tx.paymentMethod, margin + 275f, y, bodyPaint)
      canvas.drawText(CurrencyFormat.formatBdt(tx.amount), margin + 360f, y, bodyPaint)
      y += 16f
      if (!tx.note.isNullOrBlank()) {
        canvas.drawText("নোট: ${tx.note.take(65)}", margin + 16f, y, mutedPaint)
        y += 14f
      }
    }

    pdf.finishPage(page)
    pdf.writeTo(outputStream)
    pdf.close()
  }

  /**
   * Generates a real OOXML .xlsx Excel workbook with two worksheets (Summary & Transactions).
   */
  fun writeReportXlsx(
    outputStream: OutputStream,
    accountType: AccountType,
    report: MonthlyReportData,
    transactions: List<TransactionRecord>,
  ) {
    val summaryRows =
      listOf(
        listOf("My Daily হিসাব - Monthly Financial Report", ""),
        listOf("Account", accountType.labelBn),
        listOf("Period", "${DateFormats.monthNameBn(report.month)} ${report.year}"),
        listOf("Total Income (BDT)", report.totalIncome.toString()),
        listOf("Total Expenses (BDT)", report.totalExpense.toString()),
        listOf("Total Savings (BDT)", report.totalSavings.toString()),
        listOf("Average Daily Expense (BDT)", String.format("%.2f", report.averageDailyExpense)),
        listOf("Highest Spending Category", report.highestCategory?.category ?: "None"),
        listOf("Previous Month Income (BDT)", report.prevIncome.toString()),
        listOf("Previous Month Expense (BDT)", report.prevExpense.toString()),
      )

    val txRows = mutableListOf(
      listOf("Date (DD-MM-YYYY)", "Account", "Type", "Category", "Payment Method", "Amount (BDT)", "Note"),
    )
    transactions.forEach { tx ->
      txRows +=
        listOf(
          tx.dateKey,
          tx.accountType,
          if (tx.isIncome) "Income (আয়)" else "Expense (খরচ)",
          tx.category,
          tx.paymentMethod,
          tx.amount.toString(),
          tx.note.orEmpty(),
        )
    }

    val baos = ByteArrayOutputStream()
    ZipOutputStream(baos).use { zip ->
      fun putEntry(name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
      }

      putEntry(
        "[Content_Types].xml",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""",
      )

      putEntry(
        "_rels/.rels",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""",
      )

      putEntry(
        "xl/workbook.xml",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Summary" sheetId="1" r:id="rId1"/>
    <sheet name="Transactions" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>""",
      )

      putEntry(
        "xl/_rels/workbook.xml.rels",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
</Relationships>""",
      )

      putEntry("xl/worksheets/sheet1.xml", buildSheetXml(summaryRows))
      putEntry("xl/worksheets/sheet2.xml", buildSheetXml(txRows))
    }

    outputStream.write(baos.toByteArray())
  }

  private fun buildSheetXml(rows: List<List<String>>): String {
    val sb = StringBuilder()
    sb.append(
      """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>"""
    )
    rows.forEachIndexed { rowIdx, cols ->
      val rNum = rowIdx + 1
      sb.append("<row r=\"$rNum\">")
      cols.forEachIndexed { colIdx, value ->
        val colLetter = ('A' + colIdx)
        val numeric = value.toDoubleOrNull()
        if (numeric != null && rowIdx > 0 && (colIdx == 1 || colIdx == 5)) {
          sb.append("<c r=\"$colLetter$rNum\"><v>$numeric</v></c>")
        } else {
          sb.append("<c r=\"$colLetter$rNum\" t=\"inlineStr\"><is><t>${escapeXml(value)}</t></is></c>")
        }
      }
      sb.append("</row>")
    }
    sb.append("</sheetData></worksheet>")
    return sb.toString()
  }

  private fun escapeXml(input: String): String =
    input
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")

  fun serializeBackupJson(
    transactions: List<TransactionRecord>,
    categories: List<CategoryRecord>,
    budgets: List<BudgetRecord>,
    goals: List<SavingsGoalRecord>,
  ): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("exportedAtMillis", System.currentTimeMillis())

    val txArray = JSONArray()
    transactions.forEach { tx ->
      txArray.put(
        JSONObject().apply {
          put("id", tx.id)
          put("accountType", tx.accountType)
          put("type", tx.type)
          put("amount", tx.amount)
          put("category", tx.category)
          put("paymentMethod", tx.paymentMethod)
          put("dateMillis", tx.dateMillis)
          put("dateKey", tx.dateKey)
          put("year", tx.year)
          put("month", tx.month)
          put("day", tx.day)
          put("note", tx.note.orEmpty())
          put("clientRequestId", tx.clientRequestId.orEmpty())
        }
      )
    }
    root.put("transactions", txArray)

    val catArray = JSONArray()
    categories.forEach { cat ->
      catArray.put(
        JSONObject().apply {
          put("name", cat.name)
          put("type", cat.type)
        }
      )
    }
    root.put("categories", catArray)

    val budgetArray = JSONArray()
    budgets.forEach { b ->
      budgetArray.put(
        JSONObject().apply {
          put("accountType", b.accountType)
          put("year", b.year)
          put("month", b.month)
          put("category", b.category)
          put("limitAmount", b.limitAmount)
        }
      )
    }
    root.put("budgets", budgetArray)

    val goalArray = JSONArray()
    goals.forEach { g ->
      goalArray.put(
        JSONObject().apply {
          put("accountType", g.accountType)
          put("name", g.name)
          put("targetAmount", g.targetAmount)
          put("savedAmount", g.savedAmount)
          put("targetDateMillis", g.targetDateMillis)
          put("targetDateKey", g.targetDateKey)
        }
      )
    }
    root.put("savingsGoals", goalArray)

    return root.toString(2)
  }

  fun parseBackupJson(jsonText: String): BackupPayload {
    val root = JSONObject(jsonText)
    val txList = mutableListOf<TransactionRecord>()
    val txArr = root.optJSONArray("transactions") ?: JSONArray()
    for (i in 0 until txArr.length()) {
      val obj = txArr.getJSONObject(i)
      val amount = obj.optDouble("amount", 0.0)
      if (amount <= 0.0) continue
      txList +=
        TransactionRecord(
          id = obj.optString("id", ""),
          accountType = obj.optString("accountType", AccountType.PERSONAL.value),
          type = obj.optString("type", TransactionType.EXPENSE.value),
          amount = amount,
          category = obj.optString("category", "অন্যান্য"),
          paymentMethod = obj.optString("paymentMethod", "Cash"),
          dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
          dateKey = obj.optString("dateKey", ""),
          year = obj.optInt("year", 2026),
          month = obj.optInt("month", 1),
          day = obj.optInt("day", 1),
          note = obj.optString("note", "").takeIf { it.isNotBlank() },
          clientRequestId = obj.optString("clientRequestId", "").takeIf { it.isNotBlank() },
        )
    }

    val catList = mutableListOf<CategoryRecord>()
    val catArr = root.optJSONArray("categories") ?: JSONArray()
    for (i in 0 until catArr.length()) {
      val obj = catArr.getJSONObject(i)
      val name = obj.optString("name", "")
      if (name.isBlank()) continue
      catList +=
        CategoryRecord(
          name = name,
          type = obj.optString("type", TransactionType.EXPENSE.value),
          isCustom = true,
        )
    }

    val budgetList = mutableListOf<BudgetRecord>()
    val bArr = root.optJSONArray("budgets") ?: JSONArray()
    for (i in 0 until bArr.length()) {
      val obj = bArr.getJSONObject(i)
      val limit = obj.optDouble("limitAmount", 0.0)
      if (limit <= 0.0) continue
      budgetList +=
        BudgetRecord(
          accountType = obj.optString("accountType", AccountType.PERSONAL.value),
          year = obj.optInt("year", 2026),
          month = obj.optInt("month", 1),
          category = obj.optString("category", BudgetRecord.OVERALL_CATEGORY),
          limitAmount = limit,
        )
    }

    val goalList = mutableListOf<SavingsGoalRecord>()
    val gArr = root.optJSONArray("savingsGoals") ?: JSONArray()
    for (i in 0 until gArr.length()) {
      val obj = gArr.getJSONObject(i)
      val target = obj.optDouble("targetAmount", 0.0)
      if (target <= 0.0) continue
      goalList +=
        SavingsGoalRecord(
          accountType = obj.optString("accountType", AccountType.PERSONAL.value),
          name = obj.optString("name", "সঞ্চয়"),
          targetAmount = target,
          savedAmount = obj.optDouble("savedAmount", 0.0).coerceAtLeast(0.0),
          targetDateMillis = obj.optLong("targetDateMillis", System.currentTimeMillis()),
          targetDateKey = obj.optString("targetDateKey", ""),
        )
    }

    return BackupPayload(txList, catList, budgetList, goalList)
  }
}
