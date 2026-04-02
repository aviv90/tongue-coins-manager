package com.krumin.tonguecoinsmanager.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.AsyncImage
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.domain.model.FcmPriority
import com.krumin.tonguecoinsmanager.domain.model.NotificationTarget
import com.krumin.tonguecoinsmanager.domain.model.Platform
import com.krumin.tonguecoinsmanager.domain.model.TestDevice
import com.krumin.tonguecoinsmanager.ui.viewmodel.FcmAction
import com.krumin.tonguecoinsmanager.ui.viewmodel.FcmState
import com.krumin.tonguecoinsmanager.ui.viewmodel.FcmViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FcmScreen(
    onBack: () -> Unit,
    viewModel: FcmViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showManageDevicesDialog by remember { mutableStateOf(false) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var showSelectDeviceDialog by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }
    var pendingTargetToConfirm by remember { mutableStateOf<NotificationTarget?>(null) }
    var advancedExpanded by remember { mutableStateOf(false) }

    // Date/Time Pickers
    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val formatted =
                    String.format(
                        Locale.US,
                        context.getString(R.string.fcm_date_format_mask),
                        year,
                        month + 1,
                        day
                    )
                viewModel.handleAction(FcmAction.UpdateScheduledDate(formatted))
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val formatted = String.format(
                    java.util.Locale.US,
                    context.getString(R.string.fcm_time_format_mask),
                    hour,
                    minute
                )
                viewModel.handleAction(FcmAction.UpdateScheduledTime(formatted))
            },
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            true
        )
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.handleAction(FcmAction.ClearStatus)
        }
    }

    LaunchedEffect(state.success) {
        state.success?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.handleAction(FcmAction.ClearStatus)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.fcm_title_screen)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showConfirmClearDialog = true }) {
                            Icon(
                                Icons.Default.RestartAlt,
                                contentDescription = stringResource(R.string.fcm_clear_form_button)
                            )
                        }
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(dimensionResource(R.dimen.spacing_large))
                                    .size(dimensionResource(R.dimen.icon_size_small)),
                                strokeWidth = dimensionResource(R.dimen.stroke_small)
                            )
                        } else {
                            val layoutDirection = LocalLayoutDirection.current
                            IconButton(onClick = {
                                if (state.title.isNotBlank() && state.body.isNotBlank()) {
                                    pendingTargetToConfirm = state.target
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConfig.Ui.ALPHA_MEDIUM)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(dimensionResource(R.dimen.spacing_large)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
                ) {
                    // AI Generation
                    FcmHeader(stringResource(R.string.fcm_idea_prompt_label))
                    OutlinedTextField(
                        value = state.ideaPrompt,
                        onValueChange = { viewModel.handleAction(FcmAction.UpdateIdeaPrompt(it)) },
                        label = { Text(stringResource(R.string.fcm_idea_prompt_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.fcm_idea_prompt_placeholder)) },
                        trailingIcon = {
                            if (state.isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(
                                        dimensionResource(
                                            R.dimen.progress_size_small
                                        )
                                    )
                                )
                            } else {
                                IconButton(
                                    onClick = { viewModel.handleAction(FcmAction.GenerateContent) },
                                    enabled = state.ideaPrompt.isNotBlank()
                                ) {
                                    Icon(
                                        Icons.Default.AutoFixHigh,
                                        contentDescription = stringResource(R.string.fcm_generate_button_content_description),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    )

                    // Notification Content
                    FcmHeader(stringResource(R.string.fcm_header_notification))

                    OutlinedTextField(
                        value = state.title,
                        onValueChange = { viewModel.handleAction(FcmAction.UpdateTitle(it)) },
                        label = { Text(stringResource(R.string.fcm_field_title_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = state.body,
                        onValueChange = { viewModel.handleAction(FcmAction.UpdateBody(it)) },
                        label = { Text(stringResource(R.string.fcm_field_body_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = state.imageUrl ?: "",
                        onValueChange = { viewModel.handleAction(FcmAction.UpdateImageUrl(it)) },
                        label = { Text(stringResource(R.string.fcm_field_image_url_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.fcm_field_image_url_placeholder)) }
                    )

                    if (!state.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = state.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimensionResource(R.dimen.broadcast_preview_height))
                                .clip(RoundedCornerShape(dimensionResource(R.dimen.card_radius_medium)))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Target
                    FcmHeader(stringResource(R.string.fcm_header_target))
                    TargetSelector(
                        selectedTarget = state.target,
                        onTargetSelected = { viewModel.handleAction(FcmAction.UpdateTarget(it)) }
                    )

                    // Data Payload
                    FcmHeader(stringResource(R.string.fcm_header_data))
                    DataPayloadEditor(
                        payload = state.dataPayload,
                        onAdd = { k, v -> viewModel.handleAction(FcmAction.AddDataPair(k, v)) },
                        onRemove = { k -> viewModel.handleAction(FcmAction.RemoveDataPair(k)) }
                    )

                    // Advanced Settings
                    AdvancedSettingsSection(
                        state = state,
                        expanded = advancedExpanded,
                        onToggle = { advancedExpanded = !advancedExpanded },
                        onPriorityChanged = { viewModel.handleAction(FcmAction.UpdatePriority(it)) },
                        onChannelIdChanged = { viewModel.handleAction(FcmAction.UpdateChannelId(it)) },
                        onSoundToggle = { viewModel.handleAction(FcmAction.UpdateSoundEnabled(it)) },
                        onBadgeChanged = { viewModel.handleAction(FcmAction.UpdateBadgeCount(it)) }
                    )

                    // Scheduling
                    FcmHeader(stringResource(R.string.fcm_header_scheduling))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = if (state.isScheduled) MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = AppConfig.Ui.ALPHA_MEDIUM
                            ) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConfig.Ui.ALPHA_MEDIUM)
                        )
                    ) {
                        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = state.isScheduled,
                                    onCheckedChange = {
                                        viewModel.handleAction(
                                            FcmAction.UpdateIsScheduled(
                                                it
                                            )
                                        )
                                    }
                                )
                                Text(stringResource(R.string.fcm_schedule_label))
                            }

                            AnimatedVisibility(visible = state.isScheduled) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(
                                        dimensionResource(
                                            R.dimen.spacing_medium
                                        )
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            dimensionResource(R.dimen.spacing_medium)
                                        )
                                    ) {
                                        OutlinedButton(
                                            onClick = { datePickerDialog.show() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.CalendarToday,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                                            Text(state.scheduledDate)
                                        }
                                        OutlinedButton(
                                            onClick = { timePickerDialog.show() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Schedule, contentDescription = null)
                                            Spacer(Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                                            Text(state.scheduledTime)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Test Devices
                    FcmHeader(stringResource(R.string.fcm_header_test_devices))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
                    ) {
                        Button(
                            onClick = { showSelectDeviceDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = state.testDevices.isNotEmpty() && state.title.isNotBlank() && state.body.isNotBlank()
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null)
                            Spacer(Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                            Text(stringResource(R.string.fcm_button_send_test))
                        }

                        OutlinedButton(
                            onClick = { showManageDevicesDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                            Text(stringResource(R.string.fcm_button_manage_devices))
                        }
                    }

                    // Dry Run
                    if (!state.isScheduled) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = state.isDryRun,
                                onCheckedChange = { viewModel.handleAction(FcmAction.UpdateDryRun(it)) }
                            )
                            Text(stringResource(R.string.fcm_dry_run_label))
                        }
                    }

                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.fab_content_padding)))
            }
        }

        // Unified Send Confirmation Dialog
        if (pendingTargetToConfirm != null) {
            val isTestSend = pendingTargetToConfirm is NotificationTarget.Token
            AlertDialog(
                onDismissRequest = { pendingTargetToConfirm = null },
                title = {
                    Text(
                        if (state.isScheduled) stringResource(R.string.fcm_send_scheduled_button)
                        else if (isTestSend) stringResource(R.string.fcm_confirm_send_token_title)
                        else stringResource(R.string.fcm_confirm_send_title)
                    )
                },
                text = {
                    Text(
                        if (isTestSend) stringResource(R.string.fcm_confirm_send_message)
                        else stringResource(R.string.fcm_confirm_send_message)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.handleAction(FcmAction.SendNotification(pendingTargetToConfirm!!))
                        pendingTargetToConfirm = null
                    }) {
                        Text(stringResource(R.string.broadcast_dialog_button_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingTargetToConfirm = null }) {
                        Text(stringResource(R.string.broadcast_dialog_button_cancel))
                    }
                }
            )
        }

        if (showConfirmClearDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmClearDialog = false },
                title = { Text(stringResource(R.string.fcm_confirm_clear_title)) },
                text = { Text(stringResource(R.string.fcm_confirm_clear_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.handleAction(FcmAction.ClearForm)
                        showConfirmClearDialog = false
                    }) {
                        Text(
                            stringResource(R.string.broadcast_dialog_button_confirm),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmClearDialog = false }) {
                        Text(stringResource(R.string.broadcast_dialog_button_cancel))
                    }
                }
            )
        }

        if (showManageDevicesDialog) {
            ManageDevicesDialog(
                devices = state.testDevices,
                onAdd = { showAddDeviceDialog = true },
                onRemove = { viewModel.handleAction(FcmAction.RemoveTestDevice(it)) },
                onDismiss = { showManageDevicesDialog = false }
            )
        }

        if (showAddDeviceDialog) {
            AddDeviceDialog(
                onConfirm = { name, token ->
                    viewModel.handleAction(FcmAction.AddTestDevice(name, token))
                    showAddDeviceDialog = false
                },
                onDismiss = { showAddDeviceDialog = false }
            )
        }

        if (showSelectDeviceDialog) {
            SelectDeviceDialog(
                devices = state.testDevices,
                onSelect = { device ->
                    pendingTargetToConfirm = NotificationTarget.Token(device.token)
                    showSelectDeviceDialog = false
                },
                onDismiss = { showSelectDeviceDialog = false }
            )
        }
    }
}

@Composable
fun FcmHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_medium))
    )
}

@Composable
fun TargetSelector(
    selectedTarget: NotificationTarget,
    onTargetSelected: (NotificationTarget) -> Unit
) {
    var selectedType by remember {
        mutableStateOf(
            when (selectedTarget) {
                is NotificationTarget.Topic -> 0
                is NotificationTarget.Condition -> 1
                is NotificationTarget.Token -> 2
            }
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(
                stringResource(R.string.fcm_target_topic),
                stringResource(R.string.fcm_target_condition),
                stringResource(R.string.fcm_target_token)
            )
            options.forEachIndexed { index, label ->
                FilterChip(
                    selected = selectedType == index,
                    onClick = {
                        selectedType = index
                        when (index) {
                            0 -> onTargetSelected(NotificationTarget.Topic("all"))
                            1 -> onTargetSelected(NotificationTarget.Condition("'all' in topics"))
                            2 -> onTargetSelected(NotificationTarget.Token(""))
                        }
                    },
                    label = {
                        Text(
                            text = label,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = dimensionResource(R.dimen.spacing_tiny))
                )
            }
        }

        when (selectedType) {
            0 -> {
                Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))) {
                    Text(
                        text = stringResource(R.string.supported_platforms_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
                    ) {
                        Platform.entries.forEach { platform ->
                            val isSelected =
                                (selectedTarget as? NotificationTarget.Topic)?.name == platform.name.lowercase() ||
                                        (selectedTarget as? NotificationTarget.Topic)?.name == "all"

                            FilterChip(
                                selected = if ((selectedTarget as? NotificationTarget.Topic)?.name == "all") true else isSelected,
                                onClick = {
                                    val currentName =
                                        (selectedTarget as? NotificationTarget.Topic)?.name ?: "all"
                                    val newName = when {
                                        currentName == "all" -> platform.name.lowercase()
                                        currentName == platform.name.lowercase() -> "all"
                                        else -> "all"
                                    }
                                    onTargetSelected(NotificationTarget.Topic(newName))
                                },
                                label = {
                                    Text(
                                        text = if (platform == Platform.ANDROID) stringResource(
                                            R.string.platform_android
                                        ) else stringResource(R.string.platform_ios),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                leadingIcon = if (isSelected || (selectedTarget as? NotificationTarget.Topic)?.name == "all") {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_tiny))
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    OutlinedTextField(
                        value = (selectedTarget as? NotificationTarget.Topic)?.name ?: "",
                        onValueChange = { onTargetSelected(NotificationTarget.Topic(it)) },
                        label = { Text(stringResource(R.string.fcm_field_topic_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            1 -> OutlinedTextField(
                value = (selectedTarget as? NotificationTarget.Condition)?.condition ?: "",
                onValueChange = { onTargetSelected(NotificationTarget.Condition(it)) },
                label = { Text(stringResource(R.string.fcm_field_condition_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            2 -> OutlinedTextField(
                value = (selectedTarget as? NotificationTarget.Token)?.token ?: "",
                onValueChange = { onTargetSelected(NotificationTarget.Token(it)) },
                label = { Text(stringResource(R.string.fcm_field_token_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DataPayloadEditor(
    payload: Map<String, String>,
    onAdd: (String, String) -> Unit,
    onRemove: (String) -> Unit
) {
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))) {
        payload.forEach { (k, v) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(dimensionResource(R.dimen.spacing_medium))
                    )
                    .padding(dimensionResource(R.dimen.spacing_medium)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$k: $v",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = { onRemove(k) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newKey,
                onValueChange = { newKey = it },
                placeholder = { Text(stringResource(R.string.fcm_data_key_placeholder)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = newValue,
                onValueChange = { newValue = it },
                placeholder = { Text(stringResource(R.string.fcm_data_value_placeholder)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (newKey.isNotBlank() && newValue.isNotBlank()) {
                        onAdd(newKey, newValue)
                        newKey = ""
                        newValue = ""
                    }
                },
                enabled = newKey.isNotBlank() && newValue.isNotBlank()
            ) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ManageDevicesDialog(
    devices: List<TestDevice>,
    onAdd: () -> Unit,
    onRemove: (TestDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fcm_button_manage_devices)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = dimensionResource(R.dimen.broadcast_preview_height)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
            ) {
                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                dimensionResource(R.dimen.stroke_thin),
                                MaterialTheme.colorScheme.outline.copy(alpha = AppConfig.Ui.ALPHA_LOW),
                                RoundedCornerShape(dimensionResource(R.dimen.card_radius_medium))
                            )
                            .padding(dimensionResource(R.dimen.spacing_medium)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = device.name, fontWeight = FontWeight.Bold)
                            Text(
                                text = device.token.takeLast(10) + "...",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { onRemove(device) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                    Text(stringResource(R.string.fcm_dialog_add_device_title))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.riddle_button_ok)) }
        }
    )
}

@Composable
fun AddDeviceDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fcm_dialog_add_device_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.fcm_field_device_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(stringResource(R.string.fcm_field_token_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, token) },
                enabled = name.isNotBlank() && token.isNotBlank()
            ) {
                Text(stringResource(R.string.broadcast_dialog_button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.broadcast_dialog_button_cancel)) }
        }
    )
}

@Composable
fun SelectDeviceDialog(
    devices: List<TestDevice>,
    onSelect: (TestDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fcm_dialog_select_device_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = dimensionResource(R.dimen.broadcast_preview_height))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
            ) {
                devices.forEach { device ->
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = {
                            Text(
                                device.token.takeLast(10) + "...",
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.clickable { onSelect(device) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.broadcast_dialog_button_cancel)) }
        }
    )
}

@Composable
fun AdvancedSettingsSection(
    state: FcmState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPriorityChanged: (FcmPriority) -> Unit,
    onChannelIdChanged: (String?) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onBadgeChanged: (Int?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = dimensionResource(R.dimen.spacing_medium)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FcmHeader(stringResource(R.string.fcm_advanced_settings_header))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_medium)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
            ) {
                // Priority
                Column {
                    Text(
                        stringResource(R.string.fcm_priority_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))) {
                        FilterChip(
                            selected = state.priority == FcmPriority.HIGH,
                            onClick = { onPriorityChanged(FcmPriority.HIGH) },
                            label = {
                                Text(
                                    text = stringResource(R.string.fcm_priority_high),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = state.priority == FcmPriority.NORMAL,
                            onClick = { onPriorityChanged(FcmPriority.NORMAL) },
                            label = {
                                Text(
                                    text = stringResource(R.string.fcm_priority_normal),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Channel ID
                OutlinedTextField(
                    value = state.androidChannelId ?: "",
                    onValueChange = { onChannelIdChanged(it.ifBlank { null }) },
                    label = { Text(stringResource(R.string.fcm_channel_id_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Sound Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.fcm_sound_label))
                    androidx.compose.material3.Switch(
                        checked = state.soundEnabled,
                        onCheckedChange = onSoundToggle
                    )
                }

                // Badge Count
                OutlinedTextField(
                    value = state.badgeCount?.toString() ?: "",
                    onValueChange = { onBadgeChanged(it.toIntOrNull()) },
                    label = { Text(stringResource(R.string.fcm_badge_count_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        }
    }
}
