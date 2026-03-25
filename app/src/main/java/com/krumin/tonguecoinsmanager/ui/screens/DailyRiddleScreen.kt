package com.krumin.tonguecoinsmanager.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.AsyncImage
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.ui.viewmodel.DailyRiddleViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRiddleScreen(
    onBack: () -> Unit,
    viewModel: DailyRiddleViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val availableRiddles by viewModel.availableRiddles.collectAsState()

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    val filteredRiddles by remember(searchQuery, availableRiddles) {
        derivedStateOf {
            if (searchQuery.isEmpty()) {
                availableRiddles
            } else {
                availableRiddles.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.id.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.clearError()
        }
    }

    // Date Picker Dialog
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            viewModel.onDateSelected(formattedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    var riddleToSet by remember { mutableStateOf<PhotoMetadata?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (riddleToSet != null) {
            AlertDialog(
                onDismissRequest = { riddleToSet = null },
                title = { Text(stringResource(R.string.riddle_dialog_title_set)) },
                text = {
                    Text(
                        stringResource(
                            R.string.riddle_dialog_message_set,
                            riddleToSet?.title ?: "",
                            state.selectedDate
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            riddleToSet?.let { viewModel.onSetRiddle(it.id) }
                            riddleToSet = null
                        }
                    ) {
                        Text(stringResource(R.string.riddle_button_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { riddleToSet = null }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.riddle_dialog_title_reset)) },
                text = { Text(stringResource(R.string.riddle_dialog_message_reset)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onResetRiddle()
                            showResetDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.riddle_button_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.riddle_title_screen)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_content_description)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(
                        top = dimensionResource(R.dimen.spacing_large),
                        start = dimensionResource(R.dimen.spacing_large),
                        end = dimensionResource(R.dimen.spacing_large),
                        bottom = dimensionResource(R.dimen.spacing_tiny)
                    )
            ) {
                // Date Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.riddle_date_label, state.selectedDate),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = { datePickerDialog.show() }) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = stringResource(R.string.content_desc_select_date)
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                        Text(stringResource(R.string.riddle_select_date_button))
                    }
                }

                Spacer(
                    modifier = Modifier.height(
                        dimensionResource(R.dimen.spacing_large) + dimensionResource(
                            R.dimen.spacing_medium
                        )
                    )
                )

                // Current Riddle Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.currentRiddle != null)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large))) {
                        Text(
                            text = stringResource(R.string.riddle_current_header),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

                        if (state.currentRiddle != null) {
                            val currentRiddleDetails =
                                availableRiddles.find { it.id == state.currentRiddle!!.contentItemId }

                            if (currentRiddleDetails != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = currentRiddleDetails.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(dimensionResource(R.dimen.riddle_image_size))
                                            .background(Color.Gray),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_normal)))
                                    Column {
                                        Text(
                                            text = currentRiddleDetails.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.riddle_id_label,
                                                currentRiddleDetails.id
                                            ),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (state.currentRiddle!!.manuallySet) {
                                            Text(
                                                text = stringResource(R.string.riddle_manually_set_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(
                                        R.string.riddle_details_not_found,
                                        state.currentRiddle!!.contentItemId
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                            OutlinedButton(
                                onClick = { showResetDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                                Text(stringResource(R.string.riddle_reset_auto_button))
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.riddle_not_set_warning),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(
                        dimensionResource(R.dimen.spacing_large) + dimensionResource(
                            R.dimen.spacing_medium
                        )
                    )
                )

                Text(
                    text = stringResource(R.string.riddle_select_header),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.riddle_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

                // Dictionary List
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            bottom = dimensionResource(
                                R.dimen.spacing_large
                            )
                        )
                    ) {
                        items(filteredRiddles, key = { it.id }) { item ->
                            val isSelected = state.currentRiddle?.contentItemId == item.id
                            RiddleSelectionItem(
                                item = item,
                                isSelected = isSelected,
                                onSet = { riddleToSet = item },
                                onSelect = { onSet -> onSet() }
                            )
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun RiddleSelectionItem(
    item: PhotoMetadata,
    isSelected: Boolean,
    onSet: () -> Unit,
    onSelect: (() -> Unit) -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.surface_elevation_high) / 6) // Approx 2dp
    ) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.spacing_medium))
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.riddle_item_image_size))
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_normal)))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.riddle_selected_badge),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(onClick = onSet) {
                    Text(stringResource(R.string.riddle_select_button))
                }
            }
        }
    }
}

