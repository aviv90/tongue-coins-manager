package com.krumin.tonguecoinsmanager.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.AsyncImage
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.ui.viewmodel.BroadcastViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    onBack: () -> Unit,
    viewModel: BroadcastViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.success_broadcast_saved))
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                            onClick = { viewModel.onSave() },
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
                        onValueChange = viewModel::onDisabledChanged
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

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                // Preview Section
                Text(
                    stringResource(R.string.broadcast_preview_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensionResource(R.dimen.spacing_large)),
                    elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.surface_elevation)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.disabled) MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.5f
                        ) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.disabled) {
                            Text(
                                stringResource(R.string.broadcast_preview_disabled_warning),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                        }

                        if (!state.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = state.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(dimensionResource(R.dimen.broadcast_preview_height))
                                    .background(Color.Gray),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_normal)))
                        }

                        if (state.title.isNotBlank()) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                        }

                        if (state.body.isNotBlank()) {
                            Text(
                                text = state.body,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (!state.ctaText.isNullOrBlank() && !state.ctaUrl.isNullOrBlank()) {
                                Button(onClick = { /* Do nothing in preview */ }) {
                                    Text(state.ctaText)
                                }
                                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                            }
                            Button(
                                onClick = { /* Do nothing in preview */ },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(stringResource(R.string.broadcast_preview_close_button))
                            }
                        }
                    }
                }
            }
        }
    }
}
