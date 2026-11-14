package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormats {
  private const val PATTERN_DD_MM_YYYY = "dd-MM-yyyy"

  fun formatDateKey(epochMillis: Long): String {
    val sdf = SimpleDateFormat(PATTERN_DD_MM_YYYY, Locale.ENGLISH)
    return sdf.format(Date(epochMillis))
  }

  fun extractYmd(epochMillis: Long): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    return Triple(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun startOfTodayMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
  }

  fun daysAgoMillis(days: Int): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = startOfTodayMillis()
    cal.add(Calendar.DAY_OF_YEAR, -days)
    return cal.timeInMillis
  }

  fun daysInMonth(year: Int, month: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  }

  fun previousMonth(year: Int, month: Int): Pair<Int, Int> {
    return if (month <= 1) (year - 1) to 12 else year to (month - 1)
  }

  fun monthNameBn(month: Int): String =
    when (month) {
      1 -> "জানুয়ারি"
      2 -> "ফেব্রুয়ারি"
      3 -> "মার্চ"
      4 -> "এপ্রিল"
      5 -> "মে"
      6 -> "জুন"
      7 -> "জুলাই"
      8 -> "আগস্ট"
      9 -> "সেপ্টেম্বর"
      10 -> "অক্টোবর"
      11 -> "নভেম্বর"
      12 -> "ডিসেম্বর"
      else -> "মাস $month"
    }

  fun shortMonthLabel(month: Int): String =
    when (month) {
      1 -> "Jan"
      2 -> "Feb"
      3 -> "Mar"
      4 -> "Apr"
      5 -> "May"
      6 -> "Jun"
      7 -> "Jul"
      8 -> "Aug"
      9 -> "Sep"
      10 -> "Oct"
      11 -> "Nov"
      12 -> "Dec"
      else -> "$month"
    }
}

object CurrencyFormat {
  private val symbols = DecimalFormatSymbols(Locale.ENGLISH)
  private val wholeFormatter = DecimalFormat("#,##0", symbols)
  private val decimalFormatter = DecimalFormat("#,##0.00", symbols)

  fun formatBdt(amount: Double, showDecimalsIfWhole: Boolean = false): String {
    val sign = if (amount < 0) "-" else ""
    val absVal = kotlin.math.abs(amount)
    val formatted =
      if (!showDecimalsIfWhole && absVal % 1.0 == 0.0) {
        wholeFormatter.format(absVal)
      } else {
        decimalFormatter.format(absVal)
      }
    return "${sign}৳ $formatted"
  }

  fun formatCompactBdt(amount: Double): String {
    val absVal = kotlin.math.abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
      absVal >= 1_000_000 -> "${sign}৳${String.format(Locale.ENGLISH, "%.1fM", absVal / 1_000_000)}"
      absVal >= 1_000 -> "${sign}৳${String.format(Locale.ENGLISH, "%.1fK", absVal / 1_000)}"
      else -> "${sign}৳${wholeFormatter.format(absVal)}"
    }
  }
}
