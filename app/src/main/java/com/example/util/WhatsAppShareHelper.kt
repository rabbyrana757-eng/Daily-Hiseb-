package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.PartyEntryRecord
import com.example.data.model.PartyEntryType
import com.example.data.model.PartyRecord
import com.example.data.model.PartyType
import com.example.data.model.TransactionRecord
import com.example.domain.PartySummary
import java.net.URLEncoder
import kotlin.math.abs

object WhatsAppShareHelper {

  fun shareTransaction(context: Context, tx: TransactionRecord) {
    val typeLabel = if (tx.isIncome) "আয় (+)" else "খরচ (-)"
    val message = buildString {
      appendLine("📒 *My Daily হিসাব — লেনদেনের রসিদ*")
      appendLine("━━━━━━━━━━━━━━━━")
      appendLine("ধরন: $typeLabel")
      appendLine("খাত: ${tx.category}")
      appendLine("পরিমাণ: *${CurrencyFormat.formatBdt(tx.amount)}*")
      appendLine("তারিখ: ${tx.dateKey}")
      appendLine("মাধ্যম: ${tx.paymentMethod}")
      if (!tx.note.isNullOrBlank()) {
        appendLine("বিবরণ: ${tx.note}")
      }
      append("━━━━━━━━━━━━━━━━")
    }
    launchWhatsAppOrShare(context = context, message = message, phone = null)
  }

  fun sharePartyEntry(
    context: Context,
    party: PartyRecord,
    entry: PartyEntryRecord,
    summary: PartySummary?,
  ) {
    val actionLabel = entry.kind.contextLabel(party.role)
    val message = buildString {
      appendLine("📒 *ট্যালি খাতা — লেনদেনের রসিদ*")
      appendLine("━━━━━━━━━━━━━━━━")
      appendLine("${party.role.labelBn}: *${party.name}*")
      appendLine("লেনদেন: $actionLabel")
      appendLine("পরিমাণ: *${CurrencyFormat.formatBdt(entry.amount)}*")
      appendLine("তারিখ: ${entry.dateKey}")
      appendLine("মাধ্যম: ${entry.paymentMethod}")
      if (!entry.note.isNullOrBlank()) {
        appendLine("বিবরণ: ${entry.note}")
      }
      if (summary != null) {
        appendLine("────────────────")
        appendLine(formatBalanceLine(summary.netBalance))
      }
      append("━━━━━━━━━━━━━━━━")
    }
    launchWhatsAppOrShare(context = context, message = message, phone = party.phone)
  }

  fun sharePartyStatement(
    context: Context,
    summary: PartySummary,
    entries: List<PartyEntryRecord>,
  ) {
    val party = summary.party
    val message = buildString {
      appendLine("📒 *ট্যালি খাতা — হিসাব বিবরণী*")
      appendLine("━━━━━━━━━━━━━━━━")
      appendLine("${party.role.labelBn}: *${party.name}*")
      if (!party.phone.isNullOrBlank()) {
        appendLine("মোবাইল: ${party.phone}")
      }
      appendLine("মোট দিলাম: ${CurrencyFormat.formatBdt(summary.totalGiven)}")
      appendLine("মোট পেলাম: ${CurrencyFormat.formatBdt(summary.totalReceived)}")
      appendLine("*${formatBalanceLine(summary.netBalance)}*")
      val recent = entries.take(8)
      if (recent.isNotEmpty()) {
        appendLine("────────────────")
        appendLine("সাম্প্রতিক লেনদেন:")
        recent.forEach { entry ->
          val tag = if (entry.kind == PartyEntryType.GIVEN) "দিলাম" else "পেলাম"
          val notePart = if (!entry.note.isNullOrBlank()) " (${entry.note})" else ""
          appendLine("• ${entry.dateKey} | $tag ${CurrencyFormat.formatBdt(entry.amount)}$notePart")
        }
      }
      append("━━━━━━━━━━━━━━━━")
    }
    launchWhatsAppOrShare(context = context, message = message, phone = party.phone)
  }

  fun openDialer(context: Context, rawPhone: String) {
    val clean = rawPhone.trim()
    if (clean.isEmpty()) return
    try {
      val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
      context.startActivity(intent)
    } catch (_: Exception) {
      Toast.makeText(context, "কল অ্যাপ খোলা যায়নি", Toast.LENGTH_SHORT).show()
    }
  }

  internal fun formatBalanceLine(netBalance: Double): String =
    when {
      netBalance > 0.01 -> "বর্তমান পাওনা (পাবো): ${CurrencyFormat.formatBdt(netBalance)}"
      netBalance < -0.01 -> "বর্তমান দেনা (দেবো): ${CurrencyFormat.formatBdt(abs(netBalance))}"
      else -> "বর্তমান হিসাব: পরিশোধিত (৳০)"
    }

  internal fun normalizeWhatsAppPhone(rawPhone: String?): String? {
    if (rawPhone.isNullOrBlank()) return null
    val digits = rawPhone.filter { it.isDigit() }
    if (digits.length < 8) return null
    return when {
      digits.startsWith("01") && digits.length == 11 -> "88$digits"
      digits.startsWith("880") -> digits
      else -> digits
    }
  }

  private fun launchWhatsAppOrShare(
    context: Context,
    message: String,
    phone: String?,
  ) {
    val normalizedPhone = normalizeWhatsAppPhone(phone)
    if (normalizedPhone != null) {
      val encoded = URLEncoder.encode(message, "UTF-8")
      val waUri = Uri.parse("https://wa.me/$normalizedPhone?text=$encoded")
      val viewIntent = Intent(Intent.ACTION_VIEW, waUri)
      try {
        context.startActivity(viewIntent)
        return
      } catch (_: ActivityNotFoundException) {
        // Fall back to package-targeted ACTION_SEND / chooser below
      }
    }

    val pm = context.packageManager
    for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
      val waIntent =
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_TEXT, message)
          setPackage(pkg)
        }
      if (waIntent.resolveActivity(pm) != null) {
        try {
          context.startActivity(waIntent)
          return
        } catch (_: ActivityNotFoundException) {
          // Try next option
        }
      }
    }

    val genericIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
      }
    try {
      context.startActivity(Intent.createChooser(genericIntent, "হোয়াটসঅ্যাপ / শেয়ার করুন"))
    } catch (_: Exception) {
      Toast.makeText(context, "শেয়ার করার মতো কোনো অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
    }
  }
}
