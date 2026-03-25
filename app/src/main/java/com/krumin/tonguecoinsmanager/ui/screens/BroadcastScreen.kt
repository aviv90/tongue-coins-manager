package com.krumin.tonguecoinsmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.Environment
import com.krumin.tonguecoinsmanager.ui.navigation.Screen
import com.krumin.tonguecoinsmanager.ui.viewmodel.BroadcastViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    onBack: () -> Unit,
    onResult: (String) -> Unit,
    viewModel: BroadcastViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onResult(Screen.RESULT_BROADCAST_SAVED)
            onBack()
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.clearError()
        }
    }


    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text(stringResource(R.string.broadcast_save_confirm_title)) },
                text = { Text(stringResource(R.string.broadcast_save_confirm_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.onSave()
                        showSaveDialog = false
                    }) {
                        Text(stringResource(R.string.broadcast_dialog_button_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text(stringResource(R.string.broadcast_dialog_button_cancel))
                    }
                }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.broadcast_title_screen)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_content_description)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSaveDialog = true },
                            enabled = !state.isLoading && state.isValid
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(
                                        dimensionResource(
                                            R.dimen.progress_size_small
                                        )
                                    )
                                )
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = stringResource(R.string.save_button)
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(dimensionResource(R.dimen.spacing_large))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
            ) {
                // Environment Selection
                Text(
                    stringResource(R.string.broadcast_target_env_header),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
                ) {
                    FilterChip(
                        selected = state.selectedEnvironment == Environment.PRODUCTION,
                        onClick = { viewModel.onEnvironmentChanged(Environment.PRODUCTION) },
                        label = { Text(stringResource(R.string.broadcast_env_production)) },
                        leadingIcon = if (state.selectedEnvironment == Environment.PRODUCTION) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                        enabled = !state.isLoading
                    )
                    FilterChip(
                        selected = state.selectedEnvironment == Environment.TEST,
                        onClick = { viewModel.onEnvironmentChanged(Environment.TEST) },
                        label = { Text(stringResource(R.string.broadcast_env_test)) },
                        leadingIcon = if (state.selectedEnvironment == Environment.TEST) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                        enabled = !state.isLoading
                    )
                }

                Divider()

                // Settings Section
                Text(
                    stringResource(R.string.broadcast_settings_header),
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = state.id,
                    onValueChange = viewModel::onIdChanged,
                    label = { Text(stringResource(R.string.broadcast_field_id_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.id.trim().isEmpty(),
                    supportingText = {
                        if (state.id.trim().isEmpty()) {
                            Text(
                                stringResource(R.string.broadcast_error_id_required),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(stringResource(R.string.broadcast_id_hint))
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.broadcast_field_disabled_label))
                    Switch(
                        checked = state.disabled,
                        onCheckedChange = viewModel::onDisabledChanged
                    )
                }

                Divider()

                // Content Section
                Text(
                    stringResource(R.string.broadcast_content_header),
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChanged,
                    label = { Text(stringResource(R.string.broadcast_field_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.body,
                    onValueChange = viewModel::onBodyChanged,
                    label = { Text(stringResource(R.string.broadcast_field_body_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    supportingText = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_tiny))
                            )
                            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                            Text(stringResource(R.string.broadcast_body_supporting_text))
                        }
                    }
                )

                OutlinedTextField(
                    value = state.imageUrl ?: "",
                    onValueChange = { viewModel.onImageUrlChanged(it.ifBlank { null }) },
                    label = { Text(stringResource(R.string.broadcast_field_image_url_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.broadcast_image_url_placeholder)) }
                )

                Divider()

                // CTA Section
                Text(
                    stringResource(R.string.broadcast_cta_header),
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = state.ctaText ?: "",
                    onValueChange = { viewModel.onCtaTextChanged(it.ifBlank { null }) },
                    label = { Text(stringResource(R.string.broadcast_field_cta_text_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = (!state.ctaText.isNullOrBlank() && state.ctaUrl.isNullOrBlank()) || (state.ctaText.isNullOrBlank() && !state.ctaUrl.isNullOrBlank())
                )

                OutlinedTextField(
                    value = state.ctaUrl ?: "",
                    onValueChange = { viewModel.onCtaUrlChanged(it.ifBlank { null }) },
                    label = { Text(stringResource(R.string.broadcast_field_cta_url_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.broadcast_image_url_placeholder)) },
                    isError = (!state.ctaText.isNullOrBlank() && state.ctaUrl.isNullOrBlank()) || (state.ctaText.isNullOrBlank() && !state.ctaUrl.isNullOrBlank()),
                    supportingText = {
                        if ((!state.ctaText.isNullOrBlank() && state.ctaUrl.isNullOrBlank()) || (state.ctaText.isNullOrBlank() && !state.ctaUrl.isNullOrBlank())) {
                            Text(
                                stringResource(R.string.broadcast_error_cta_mismatch),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        }
    }
}
