package com.karvo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.karvo.R
import com.karvo.data.MAX_RATE_TITLE
import com.karvo.data.RATE_COLORS
import com.karvo.data.Rate
import com.karvo.data.titleTaken
import com.karvo.domain.RateParseResult
import com.karvo.domain.formatRateText
import com.karvo.domain.normalizeRateInput
import com.karvo.domain.parseRateCents
import com.karvo.ui.theme.AlpineTile
import com.karvo.ui.theme.alpineFieldColors

/** A rate's color as a small dot (settings, the lesson editor's flags, lesson lists). */
@Composable
fun RateDot(color: Int, modifier: Modifier = Modifier, size: Int = 12) {
    Box(modifier.size(size.dp).background(Color(color), CircleShape))
}

/** Settings' list of hourly rates: the default first, each with a menu to make it the default, edit or delete it. */
@Composable
fun RatesSection(
    rates: List<Rate>,
    onSave: (Rate) -> Unit,
    onSetDefault: (Rate) -> Unit,
    onDelete: (Rate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Rate?>(null) }
    var deleting by remember { mutableStateOf<Rate?>(null) }
    AlpineTile(modifier.fillMaxWidth()) {
        ListItem(
            colors = transparentRow(),
            headlineContent = { Text(stringResource(R.string.hourly_rates), style = MaterialTheme.typography.titleMedium) },
            supportingContent = if (rates.isEmpty()) { { Text(stringResource(R.string.rates_empty_hint)) } } else null,
        )
        rates.forEach { rate ->
            var menuOpen by remember { mutableStateOf(false) }
            ListItem(
                colors = transparentRow(),
                leadingContent = { RateDot(rate.color, size = 16) },
                headlineContent = { Text(rate.title) },
                supportingContent = {
                    val amount = stringResource(R.string.rate_per_hour, formatRateText(rate.cents))
                    Text(if (rate.isDefault) amount + " · " + stringResource(R.string.rate_default) else amount)
                },
                trailingContent = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.rate_options, rate.title))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (!rate.isDefault) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.rate_set_default)) }, onClick = { menuOpen = false; onSetDefault(rate) })
                            }
                            DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = { menuOpen = false; editing = rate })
                            // The default goes only as the last rate; otherwise another one must be made the default first.
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                enabled = !rate.isDefault || rates.size == 1,
                                onClick = { menuOpen = false; deleting = rate },
                            )
                        }
                    }
                },
                modifier = Modifier.clickable { editing = rate },
            )
        }
        TextButton(
            onClick = { editing = Rate(title = "", cents = 0, color = RATE_COLORS.firstOrNull { c -> rates.none { it.color == c } } ?: RATE_COLORS[0]) },
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.rate_add), Modifier.padding(start = 8.dp))
        }
    }

    editing?.let { rate -> RateDialog(rate, rates, onDismiss = { editing = null }, onSave = { onSave(it); editing = null }) }
    deleting?.let { rate ->
        val default = rates.firstOrNull { it.isDefault && it.id != rate.id }
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.rate_delete_question, rate.title)) },
            text = { Text(if (default != null) stringResource(R.string.rate_delete_moves, default.title) else stringResource(R.string.rate_delete_last)) },
            confirmButton = { TextButton(onClick = { onDelete(rate); deleting = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RateDialog(rate: Rate, others: List<Rate>, onDismiss: () -> Unit, onSave: (Rate) -> Unit) {
    var title by remember { mutableStateOf(rate.title) }
    var amount by remember { mutableStateOf(if (rate.cents > 0) formatRateText(rate.cents) else "") }
    var color by remember { mutableStateOf(rate.color) }
    val cents = (parseRateCents(amount) as? RateParseResult.Valid)?.cents
    val amountError = amount.isNotBlank() && cents == null
    val taken = titleTaken(others, title, rate.id)
    val valid = title.isNotBlank() && !taken && cents != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (rate.id == 0L) R.string.rate_add else R.string.rate_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    colors = alpineFieldColors(),
                    value = title,
                    onValueChange = { if (it.length <= MAX_RATE_TITLE) title = it },
                    label = { Text(stringResource(R.string.rate_title)) },
                    singleLine = true,
                    isError = taken,
                    supportingText = if (taken) { { Text(stringResource(R.string.rate_title_taken)) } } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    colors = alpineFieldColors(),
                    value = amount,
                    onValueChange = { amount = normalizeRateInput(it) },
                    label = { Text(stringResource(R.string.hourly_rate)) },
                    placeholder = { Text("00.00") },
                    suffix = { Text("€") },
                    singleLine = true,
                    isError = amountError,
                    supportingText = if (amountError) { { Text(stringResource(R.string.rate_error)) } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.rate_color), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RATE_COLORS.forEachIndexed { i, c ->
                        val chosen = c == color
                        val label = stringResource(R.string.rate_color_n, i + 1)
                        Box(
                            Modifier
                                .size(36.dp)
                                .border(if (chosen) 3.dp else 0.dp, if (chosen) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                .padding(5.dp)
                                .background(Color(c), CircleShape)
                                .clickable(role = Role.RadioButton) { color = c }
                                .semantics { contentDescription = label; selected = chosen },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(rate.copy(title = title.trim(), cents = cents!!, color = color)) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
