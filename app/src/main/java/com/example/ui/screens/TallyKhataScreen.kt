package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PartyEntryRecord
import com.example.data.model.PartyEntryType
import com.example.data.model.PartyRecord
import com.example.data.model.PartyType
import com.example.domain.PartySummary
import com.example.domain.TallyKhataSummary
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.neumorphicCircle
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
import com.example.util.CurrencyFormat
import com.example.util.DateFormats
import com.example.util.WhatsAppShareHelper
import kotlin.math.abs

enum class TallyPartyFilter(val labelBn: String) {
  ALL("সবাই"),
  CUSTOMER("কাস্টমার"),
  SUPPLIER("সাপ্লায়ার"),
  WITH_DUE("বাকি আছে"),
}

private val WhatsAppGreen = Color(0xFF16A34A)

@Composable
fun BusinessTallyKhataSection(
  tallySummary: TallyKhataSummary,
  allEntries: List<PartyEntryRecord>,
  onAddParty: () -> Unit,
  onSelectParty: (String) -> Unit,
  onQuickAddEntry: (PartyRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val neo = HisabTheme.neumorphic
  var searchQuery by remember { mutableStateOf("") }
  var filter by remember { mutableStateOf(TallyPartyFilter.ALL) }

  val filteredParties = remember(tallySummary.partySummaries, searchQuery, filter) {
    val q = searchQuery.trim().lowercase()
    tallySummary.partySummaries.filter { summary ->
      val p = summary.party
      val matchesQuery =
        q.isEmpty() ||
          p.name.lowercase().contains(q) ||
          p.phone.orEmpty().lowercase().contains(q) ||
          p.address.orEmpty().lowercase().contains(q)
      val matchesFilter =
        when (filter) {
          TallyPartyFilter.ALL -> true
          TallyPartyFilter.CUSTOMER -> p.role == PartyType.CUSTOMER
          TallyPartyFilter.SUPPLIER -> p.role == PartyType.SUPPLIER
          TallyPartyFilter.WITH_DUE -> abs(summary.netBalance) > 0.01
        }
      matchesQuery && matchesFilter
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Hero Tally Khata Balance Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .neumorphicRaised(
          cornerRadius = 26.dp,
          shadowElevation = 8.dp,
          drawContainer = false,
        )
        .clip(RoundedCornerShape(26.dp))
        .background(Brush.linearGradient(colors = neo.heroGradient))
        .padding(20.dp),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.MenuBook,
              contentDescription = null,
              tint = Color.White.copy(alpha = 0.9f),
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "ব্যবসায়িক ট্যালি খাতা",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White.copy(alpha = 0.92f),
            )
          }
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .clickable(onClick = onAddParty)
              .testTag("tally_add_party_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.PersonAdd,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "নতুন যোগ",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        val netLabel =
          when {
            tallySummary.netBalance > 0.01 ->
              "নেট পাবো: ${CurrencyFormat.formatBdt(tallySummary.netBalance, false)}"
            tallySummary.netBalance < -0.01 ->
              "নেট দেবো: ${CurrencyFormat.formatBdt(abs(tallySummary.netBalance), false)}"
            else -> "হিসাব সমান: ৳০"
          }
        Text(
          text = netLabel,
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "কাস্টমার: ${tallySummary.customerCount} জন  •  সাপ্লায়ার: ${tallySummary.supplierCount} জন",
          style = MaterialTheme.typography.labelMedium,
          color = Color.White.copy(alpha = 0.85f),
        )

        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "মোট পাবো (বাকি পাওনা)",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = CurrencyFormat.formatBdt(tallySummary.totalReceivable),
              style = MaterialTheme.typography.titleMedium,
              color = Color(0xFF86EFAC),
              fontWeight = FontWeight.Bold,
            )
          }
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
          ) {
            Text(
              text = "মোট দেবো (দেনা)",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = CurrencyFormat.formatBdt(tallySummary.totalPayable),
              style = MaterialTheme.typography.titleMedium,
              color = Color(0xFFFDA4AF),
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    // Search Input
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("কাস্টমার বা সাপ্লায়ার খুঁজুন...") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Rounded.Clear, contentDescription = "পরিষ্কার করুন")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("tally_search_input"),
    )

    // Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      TallyPartyFilter.entries.forEach { option ->
        FilterChip(
          selected = filter == option,
          onClick = { filter = option },
          label = { Text(option.labelBn) },
        )
      }
    }

    // Party List
    if (filteredParties.isEmpty()) {
      EmptyStateCard(
        title = if (tallySummary.partySummaries.isEmpty()) "ট্যালি খাতা এখনো খালি" else "কাউকে পাওয়া যায়নি",
        subtitle = if (tallySummary.partySummaries.isEmpty()) {
          "বাকি ও জমার হিসাব রাখতে কাস্টমার বা সাপ্লায়ার যোগ করুন এবং লেনদেন হোয়াটসঅ্যাপে শেয়ার করুন।"
        } else {
          "অনুসন্ধান বা ফিল্টার পরিবর্তন করে আবার চেষ্টা করুন।"
        },
      )
      if (tallySummary.partySummaries.isEmpty()) {
        Button(
          onClick = onAddParty,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        ) {
          Icon(Icons.Rounded.PersonAdd, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("প্রথম কাস্টমার / সাপ্লায়ার যোগ করুন")
        }
      }
    } else {
      filteredParties.forEach { summary ->
        val partyEntries = allEntries.filter { it.partyId == summary.party.id }
        PartySummaryCard(
          summary = summary,
          onClick = { onSelectParty(summary.party.id) },
          onQuickAdd = { onQuickAddEntry(summary.party) },
          onWhatsAppShare = {
            WhatsAppShareHelper.sharePartyStatement(context, summary, partyEntries)
          },
        )
      }
    }
  }
}

@Composable
private fun PartySummaryCard(
  summary: PartySummary,
  onClick: () -> Unit,
  onQuickAdd: () -> Unit,
  onWhatsAppShare: () -> Unit,
) {
  val party = summary.party
  val roleColor =
    if (party.role == PartyType.CUSTOMER) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.secondary

  val (balanceLabel, balanceColor, balanceText) =
    when {
      summary.netBalance > 0.01 ->
        Triple("পাবো", IncomeGreen, CurrencyFormat.formatBdt(summary.netBalance))
      summary.netBalance < -0.01 ->
        Triple("দেবো", ExpenseRed, CurrencyFormat.formatBdt(abs(summary.netBalance)))
      else -> Triple("পরিশোধিত", MaterialTheme.colorScheme.onSurfaceVariant, "৳০")
    }

  val initial = party.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

  NeumorphicCard(
    cornerRadius = 20.dp,
    shadowElevation = 5.dp,
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("party_card_${party.id}"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .neumorphicCircle(
              shadowElevation = 3.dp,
              containerColor = roleColor.copy(alpha = 0.14f),
            ),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = roleColor,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = party.name,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = roleColor.copy(alpha = 0.12f),
            ) {
              Text(
                text = party.role.labelBn,
                style = MaterialTheme.typography.labelSmall,
                color = roleColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = buildString {
              if (!party.phone.isNullOrBlank()) {
                append(party.phone)
                append(" • ")
              }
              append("${summary.entryCount}টি এন্ট্রি")
              if (summary.lastActivityMillis > 0L) {
                append(" • ${DateFormats.formatDateKey(summary.lastActivityMillis)}")
              }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = balanceLabel,
            style = MaterialTheme.typography.labelSmall,
            color = balanceColor,
          )
          Text(
            text = balanceText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = balanceColor,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onQuickAdd),
        ) {
          Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Rounded.Add,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "দিলাম / পেলাম",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = WhatsAppGreen.copy(alpha = 0.14f),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onWhatsAppShare),
        ) {
          Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Rounded.Share,
              contentDescription = null,
              tint = WhatsAppGreen,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "হোয়াটসঅ্যাপ",
              style = MaterialTheme.typography.labelMedium,
              color = WhatsAppGreen,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}

@Composable
fun PartyDetailScreen(
  summary: PartySummary,
  entries: List<PartyEntryRecord>,
  onBack: () -> Unit,
  onEditParty: (PartyRecord) -> Unit,
  onDeleteParty: (String) -> Unit,
  onAddEntry: (PartyEntryType) -> Unit,
  onEditEntry: (PartyEntryRecord) -> Unit,
  onDeleteEntry: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val party = summary.party
  var confirmDeleteParty by remember { mutableStateOf(false) }
  var pendingDeleteEntry by remember { mutableStateOf<PartyEntryRecord?>(null) }

  // Compute chronological running balance (oldest -> newest, then reverse for display)
  val entriesWithRunningBalance = remember(entries) {
    val chronological =
      entries.sortedWith(
        compareBy<PartyEntryRecord> { it.dateMillis }
          .thenBy { it.createdAt?.seconds ?: 0L }
      )
    var running = 0.0
    chronological
      .map { entry ->
        running += if (entry.kind == PartyEntryType.GIVEN) entry.amount else -entry.amount
        entry to running
      }
      .reversed()
  }

  Column(modifier = modifier.fillMaxSize()) {
    // Top Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .neumorphicCircle(shadowElevation = 4.dp)
          .clip(CircleShape)
          .clickable(onClick = onBack)
          .testTag("party_detail_back"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "ফিরে যান",
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = party.name,
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = buildString {
            append(party.role.labelBn)
            if (!party.phone.isNullOrBlank()) append(" • ${party.phone}")
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (!party.phone.isNullOrBlank()) {
        IconButton(onClick = { WhatsAppShareHelper.openDialer(context, party.phone) }) {
          Icon(
            Icons.Rounded.Call,
            contentDescription = "কল করুন",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
      IconButton(onClick = { onEditParty(party) }) {
        Icon(Icons.Rounded.Edit, contentDescription = "সম্পাদনা")
      }
      IconButton(onClick = { confirmDeleteParty = true }) {
        Icon(
          Icons.Rounded.Delete,
          contentDescription = "মুছুন",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }

    LazyColumn(
      modifier = Modifier.weight(1f),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Balance & Contact Card
      item {
        NeumorphicCard(
          cornerRadius = 22.dp,
          shadowElevation = 6.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            if (!party.address.isNullOrBlank() || !party.note.isNullOrBlank()) {
              Text(
                text = listOfNotNull(party.address, party.note).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(10.dp))
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .neumorphicInset(cornerRadius = 16.dp),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "মোট দিলাম",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = CurrencyFormat.formatBdt(summary.totalGiven),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                }
                Column(
                  modifier = Modifier.weight(1f),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = "মোট পেলাম",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = CurrencyFormat.formatBdt(summary.totalReceived),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                }
                val balColor =
                  when {
                    summary.netBalance > 0.01 -> IncomeGreen
                    summary.netBalance < -0.01 -> ExpenseRed
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                  }
                val balTitle =
                  when {
                    summary.netBalance > 0.01 -> "পাবো"
                    summary.netBalance < -0.01 -> "দেবো"
                    else -> "সমান"
                  }
                Column(
                  modifier = Modifier.weight(1f),
                  horizontalAlignment = Alignment.End,
                ) {
                  Text(
                    text = balTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = balColor,
                  )
                  Text(
                    text = CurrencyFormat.formatBdt(abs(summary.netBalance)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = balColor,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = {
                WhatsAppShareHelper.sharePartyStatement(context, summary, entries)
              },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("party_whatsapp_statement_button"),
            ) {
              Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "হোয়াটসঅ্যাপে সম্পূর্ণ হিসাব পাঠান",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }

      // Quick Entry Buttons (দিলাম / পেলাম)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          val givenColor = if (party.role == PartyType.CUSTOMER) ExpenseRed else IncomeGreen
          val receivedColor = if (party.role == PartyType.CUSTOMER) IncomeGreen else ExpenseRed

          Button(
            onClick = { onAddEntry(PartyEntryType.GIVEN) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = givenColor),
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("party_add_given_button"),
          ) {
            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (party.role == PartyType.CUSTOMER) "দিলাম (বাকি)" else "দিলাম (পরিশোধ)",
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
            )
          }

          Button(
            onClick = { onAddEntry(PartyEntryType.RECEIVED) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = receivedColor),
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("party_add_received_button"),
          ) {
            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (party.role == PartyType.CUSTOMER) "পেলাম (জমা)" else "পেলাম (বাকি কেনা)",
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
            )
          }
        }
      }

      // Ledger History Header
      item {
        Text(
          text = "লেনদেনের বিবরণ (${entries.size}টি)",
          style = MaterialTheme.typography.titleMedium,
        )
      }

      if (entriesWithRunningBalance.isEmpty()) {
        item {
          EmptyStateCard(
            title = "কোনো লেনদেন এখনো নেই",
            subtitle = "উপরের বোতাম ব্যবহার করে দিলাম বা পেলাম এন্ট্রি যোগ করুন।",
          )
        }
      } else {
        items(entriesWithRunningBalance, key = { it.first.id }) { (entry, runningBal) ->
          val isCreditIncrease =
            (party.role == PartyType.CUSTOMER && entry.kind == PartyEntryType.GIVEN) ||
              (party.role == PartyType.SUPPLIER && entry.kind == PartyEntryType.RECEIVED)
          val accent = if (isCreditIncrease) ExpenseRed else IncomeGreen
          val runningText =
            when {
              runningBal > 0.01 -> "জের: পাবো ${CurrencyFormat.formatBdt(runningBal)}"
              runningBal < -0.01 -> "জের: দেবো ${CurrencyFormat.formatBdt(abs(runningBal))}"
              else -> "জের: পরিশোধিত ৳০"
            }

          NeumorphicCard(
            cornerRadius = 18.dp,
            shadowElevation = 5.dp,
            onClick = { onEditEntry(entry) },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .neumorphicCircle(
                      shadowElevation = 3.dp,
                      containerColor = accent.copy(alpha = 0.14f),
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector =
                      if (entry.kind == PartyEntryType.GIVEN) Icons.Rounded.ArrowUpward
                      else Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = entry.kind.contextLabel(party.role),
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                  )
                  Text(
                    text = "${entry.dateKey} • ${entry.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (!entry.note.isNullOrBlank()) {
                    Text(
                      text = entry.note,
                      style = MaterialTheme.typography.bodySmall,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = CurrencyFormat.formatBdt(entry.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                  )
                  Text(
                    text = runningText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
              ) {
                TextButton(
                  onClick = {
                    WhatsAppShareHelper.sharePartyEntry(context, party, entry, summary)
                  }
                ) {
                  Icon(
                    Icons.Rounded.Share,
                    contentDescription = null,
                    tint = WhatsAppGreen,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("হোয়াটসঅ্যাপ", color = WhatsAppGreen)
                }
                TextButton(onClick = { onEditEntry(entry) }) {
                  Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("সম্পাদনা")
                }
                TextButton(onClick = { pendingDeleteEntry = entry }) {
                  Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }
      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }

  if (confirmDeleteParty) {
    ConfirmDeleteDialog(
      title = "${party.role.labelBn} মুছবেন?",
      message = "${party.name} এবং তার সব ট্যালি খাতার লেনদেন স্থায়ীভাবে মুছে ফেলা হবে।",
      onConfirm = {
        confirmDeleteParty = false
        onDeleteParty(party.id)
      },
      onDismiss = { confirmDeleteParty = false },
    )
  }

  pendingDeleteEntry?.let { entry ->
    ConfirmDeleteDialog(
      title = "এন্ট্রি মুছবেন?",
      message = "${entry.kind.labelBn} ${CurrencyFormat.formatBdt(entry.amount)} (${entry.dateKey}) মুছে ফেলা হবে।",
      onConfirm = {
        onDeleteEntry(entry.id)
        pendingDeleteEntry = null
      },
      onDismiss = { pendingDeleteEntry = null },
    )
  }
}
