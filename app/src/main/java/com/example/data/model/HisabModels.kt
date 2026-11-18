package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AccountType(val value: String, val labelBn: String) {
  PERSONAL("personal", "ব্যক্তিগত হিসাব"),
  BUSINESS("business", "ব্যবসায়িক হিসাব");

  companion object {
    fun fromValue(value: String?): AccountType =
      entries.firstOrNull { it.value == value } ?: PERSONAL
  }
}

enum class TransactionType(val value: String, val labelBn: String) {
  INCOME("income", "আয়"),
  EXPENSE("expense", "খরচ");

  companion object {
    fun fromValue(value: String?): TransactionType =
      entries.firstOrNull { it.value == value } ?: EXPENSE
  }
}

enum class PaymentMethod(val value: String, val labelBn: String) {
  CASH("Cash", "Cash (নগদ)"),
  BKASH("bKash", "bKash"),
  NAGAD("Nagad", "Nagad"),
  ROCKET("Rocket", "Rocket"),
  BANK("Bank", "Bank"),
  CARD("Card", "Card"),
  OTHER("Other", "Other (অন্যান্য)");

  companion object {
    fun fromValue(value: String?): PaymentMethod =
      entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: CASH
  }
}

enum class PartyType(val value: String, val labelBn: String) {
  CUSTOMER("customer", "কাস্টমার"),
  SUPPLIER("supplier", "সাপ্লায়ার");

  companion object {
    fun fromValue(value: String?): PartyType =
      entries.firstOrNull { it.value == value } ?: CUSTOMER
  }
}

enum class PartyEntryType(val value: String, val labelBn: String) {
  GIVEN("given", "দিলাম"),
  RECEIVED("received", "পেলাম");

  fun contextLabel(partyType: PartyType): String =
    when (partyType) {
      PartyType.CUSTOMER -> if (this == GIVEN) "দিলাম (বাকি/বিক্রি)" else "পেলাম (জমা/আদায়)"
      PartyType.SUPPLIER -> if (this == GIVEN) "দিলাম (পরিশোধ)" else "পেলাম (বাকি/মাল কেনা)"
    }

  companion object {
    fun fromValue(value: String?): PartyEntryType =
      entries.firstOrNull { it.value == value } ?: GIVEN
  }
}

object DefaultCategories {
  val EXPENSE = listOf(
    "খাবার",
    "বাজার",
    "যাতায়াত",
    "বাসা ভাড়া",
    "বিদ্যুৎ বিল",
    "গ্যাস বিল",
    "ইন্টারনেট",
    "মোবাইল রিচার্জ",
    "চিকিৎসা",
    "পোশাক",
    "বিনোদন",
    "শিক্ষা",
    "পরিবারের খরচ",
    "অন্যান্য",
  )

  val INCOME = listOf(
    "ব্যবসার আয়",
    "চাকরির বেতন",
    "অনলাইন ইনকাম",
    "অন্যান্য আয়",
  )
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String? = null,
  val email: String? = null,
  val activeAccount: String = AccountType.PERSONAL.value,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TransactionRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val accountType: String = AccountType.PERSONAL.value,
  val type: String = TransactionType.EXPENSE.value,
  val amount: Double = 0.0,
  val category: String = "",
  val paymentMethod: String = PaymentMethod.CASH.value,
  val dateMillis: Long = 0L,
  val dateKey: String = "",
  val year: Int = 2026,
  val month: Int = 1,
  val day: Int = 1,
  val note: String? = null,
  val receiptBase64: String? = null,
  val clientRequestId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val isIncome: Boolean get() = type == TransactionType.INCOME.value
  val isExpense: Boolean get() = type == TransactionType.EXPENSE.value
  val account: AccountType get() = AccountType.fromValue(accountType)
}

@IgnoreExtraProperties
data class CategoryRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val type: String = TransactionType.EXPENSE.value,
  val isCustom: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class BudgetRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val accountType: String = AccountType.PERSONAL.value,
  val year: Int = 2026,
  val month: Int = 1,
  val category: String = OVERALL_CATEGORY,
  val limitAmount: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  companion object {
    const val OVERALL_CATEGORY = "ALL"
  }
}

@IgnoreExtraProperties
data class SavingsGoalRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val accountType: String = AccountType.PERSONAL.value,
  val name: String = "",
  val targetAmount: Double = 0.0,
  val savedAmount: Double = 0.0,
  val targetDateMillis: Long = 0L,
  val targetDateKey: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val progressFraction: Float
    get() = if (targetAmount <= 0.0) 0f else (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f)

  val progressPercent: Int
    get() = if (targetAmount <= 0.0) 0 else ((savedAmount / targetAmount) * 100.0).toInt().coerceIn(0, 100)
}

@Keep
@IgnoreExtraProperties
data class PartyRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val partyType: String = PartyType.CUSTOMER.value,
  val name: String = "",
  val phone: String? = null,
  val address: String? = null,
  val note: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val role: PartyType get() = PartyType.fromValue(partyType)
}

@Keep
@IgnoreExtraProperties
data class PartyEntryRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val partyId: String = "",
  val entryType: String = PartyEntryType.GIVEN.value,
  val amount: Double = 0.0,
  val paymentMethod: String = PaymentMethod.CASH.value,
  val dateMillis: Long = 0L,
  val dateKey: String = "",
  val note: String? = null,
  val clientRequestId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val kind: PartyEntryType get() = PartyEntryType.fromValue(entryType)
}
