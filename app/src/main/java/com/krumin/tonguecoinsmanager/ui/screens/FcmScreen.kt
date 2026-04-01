package com.krumin.tonguecoinsmanager.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.NotificationTarget
import com.krumin.tonguecoinsmanager.domain.model.TestDevice
import com.krumin.tonguecoinsmanager.ui.viewmodel.FcmAction
import com.krumin.tonguecoinsmanager.ui.viewmodel.FcmViewModel
import org.koin.androidx.compose.koinViewModel

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
    var showConfirmSendDialog by remember { mutableStateOf(false) }

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
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(dimensionResource(R.dimen.spacing_large))
                                    .size(dimensionResource(R.dimen.icon_size_small)),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = {
                                if (state.title.isBlank() || state.body.isBlank()) {
                                    // Validation handled in VM or locally
                                    if (state.title.isBlank()) viewModel.handleAction(
                                        FcmAction.UpdateTitle(
                                            ""
                                        )
                                    ) // Trigger error?
                                } else {
                                    showConfirmSendDialog = true
                                }
                            }) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(R.dimen.spacing_large)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
            ) {
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
                    placeholder = { Text("https://...") }
                )

                if (!state.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = state.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
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
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.fcm_button_send_test))
                    }

                    OutlinedButton(
                        onClick = { showManageDevicesDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.fcm_button_manage_devices))
                    }
                }

                // Dry Run
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Dialogs
        if (showConfirmSendDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmSendDialog = false },
                title = { Text(stringResource(R.string.fcm_confirm_send_title)) },
                text = { Text(stringResource(R.string.fcm_confirm_send_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmSendDialog = false
                        viewModel.handleAction(FcmAction.SendNotification())
                    }) {
                        Text(
                            stringResource(R.string.broadcast_dialog_button_confirm),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmSendDialog = false }) {
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
                    viewModel.handleAction(
                        FcmAction.SendNotification(
                            NotificationTarget.Token(
                                device.token
                            )
                        )
                    )
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
        modifier = Modifier.padding(top = 8.dp)
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    label = { Text(label) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                )
            }
        }

        when (selectedType) {
            0 -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.supported_platforms_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        com.krumin.tonguecoinsmanager.domain.model.Platform.entries.forEach { platform ->
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
                                        if (platform == com.krumin.tonguecoinsmanager.domain.model.Platform.ANDROID) stringResource(
                                            R.string.platform_android
                                        ) else stringResource(R.string.platform_ios)
                                    )
                                },
                                leadingIcon = if (isSelected || (selectedTarget as? NotificationTarget.Topic)?.name == "all") {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        payload.forEach { (k, v) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp),
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = device.name, fontWeight = FontWeight.Bold)
                            Text(
                                text = device.token,
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
                    Spacer(Modifier.width(8.dp))
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                devices.forEach { device ->
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = { Text(device.token, maxLines = 1) },
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
