package com.krumin.tonguecoinsmanager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.AsyncImage
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.Platform
import com.krumin.tonguecoinsmanager.ui.viewmodel.EditAction
import com.krumin.tonguecoinsmanager.ui.viewmodel.EditPhotoViewModel
import com.krumin.tonguecoinsmanager.util.FileUtils
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhotoScreen(
    photoId: String?,
    onBack: () -> Unit,
    onResult: (String) -> Unit,
    viewModel: EditPhotoViewModel = koinViewModel(parameters = { parametersOf(photoId) })
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var credit by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("1") }
    var categories by remember { mutableStateOf("") }
    var supportedPlatforms by remember { mutableStateOf(listOf(Platform.ANDROID, Platform.IOS)) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val successMessage = stringResource(R.string.success_categories_generated)

    LaunchedEffect(state.photo) {
        state.photo?.let {
            title = it.title
            credit = it.credit
            hint = it.hint
            difficulty = it.difficulty.toString()
            categories = it.categories
            supportedPlatforms = it.supportedPlatforms
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.handleAction(EditAction.ClearError)
        }
    }

    LaunchedEffect(state.validationErrors) {
        if (state.validationErrors.containsKey("image")) {
            val errorRes = state.validationErrors["image"] ?: R.string.error_image_required
            snackbarHostState.showSnackbar(context.getString(errorRes))
        }
    }

    LaunchedEffect(state.generatedCategories) {
        state.generatedCategories?.let { genCats ->
            if (genCats.isNotEmpty()) {
                categories =
                    genCats.joinToString(com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.SEPARATOR_COMMA)
                viewModel.handleAction(EditAction.ClearGeneratedCategories)
                snackbarHostState.showSnackbar(successMessage)
            }
        }
    }

    if (state.isSuccess) {
        LaunchedEffect(Unit) {
            // Don't pass result for local (batched) saves - snackbar will show only after batch commit
            // Just navigate back silently
            onBack()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            viewModel.handleAction(EditAction.ClearConfirmedAiImage)
        }
    }

    // Checking for pending AI image to show review dialog
    if (state.pendingAiImage != null) {
        val pendingImage = state.pendingAiImage
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewModel.handleAction(EditAction.DiscardAiImage) },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.spacing_large)),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = dimensionResource(R.dimen.elevation_large)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.spacing_large)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.ai_review_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = dimensionResource(R.dimen.spacing_large))
                            .fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = pendingImage,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(dimensionResource(R.dimen.spacing_small)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.handleAction(EditAction.DiscardAiImage) },
                            modifier = Modifier
                                .weight(1f)
                                .height(dimensionResource(R.dimen.button_height_large))
                        ) {
                            Text(stringResource(R.string.ai_review_discard))
                        }

                        Button(
                            onClick = { viewModel.handleAction(EditAction.ConfirmAiImage) },
                            modifier = Modifier
                                .weight(1f)
                                .height(dimensionResource(R.dimen.button_height_large)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
                            Text(stringResource(R.string.ai_review_replace))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(state.confirmedAiImage) {
        if (state.confirmedAiImage != null) {
            selectedImageUri = null
        }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (showFullScreenImage) {
            val imageSource = state.confirmedAiImage ?: selectedImageUri ?: state.photo?.imageUrl
            if (imageSource != null) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showFullScreenImage = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dimensionResource(R.dimen.spacing_large)),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = dimensionResource(R.dimen.elevation_large)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(dimensionResource(R.dimen.spacing_large)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                AsyncImage(
                                    model = imageSource,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                            Button(
                                onClick = { showFullScreenImage = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(dimensionResource(R.dimen.button_height_large)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(stringResource(R.string.close_preview))
                            }
                        }
                    }
                }
            }
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.save_changes_title),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.save_changes_confirmation),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSaveDialog = false
                            val file =
                                selectedImageUri?.let { FileUtils.uriToTempFile(context, it) }
                            viewModel.handleAction(
                                EditAction.SavePhoto(
                                    title = title,
                                    credit = credit,
                                    hint = hint,
                                    difficulty = difficulty.toIntOrNull() ?: 1,
                                    categories = categories,
                                    supportedPlatforms = supportedPlatforms,
                                    imageFile = file
                                )
                            )
                        }
                    ) {
                        Text(
                            stringResource(R.string.save_button),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text(
                            stringResource(R.string.cancel_button),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.delete_photo_title),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.delete_photo_confirmation),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.handleAction(EditAction.DeletePhoto)
                        }
                    ) {
                        Text(
                            stringResource(R.string.delete_button),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(
                            stringResource(R.string.cancel_button),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (photoId == null) {
                                stringResource(R.string.add_photo_title)
                            } else {
                                stringResource(R.string.edit_photo_title)
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_content_description)
                            )
                        }
                    },
                    actions = {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(dimensionResource(R.dimen.spacing_large))
                                    .size(dimensionResource(R.dimen.icon_size_small)),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = dimensionResource(R.dimen.spacing_tiny)
                            )
                        } else {
                            IconButton(onClick = { showSaveDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.save_button)
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
                    .padding(dimensionResource(R.dimen.spacing_large))
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
            ) {
                // Image Preview/Picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.edit_image_height))
                        .padding(dimensionResource(R.dimen.spacing_medium)),
                    contentAlignment = Alignment.Center
                ) {
                    val imageSource =
                        state.confirmedAiImage ?: selectedImageUri ?: state.photo?.imageUrl
                    if (imageSource != null) {
                        AsyncImage(
                            model = imageSource,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showFullScreenImage = true },
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        OutlinedButton(onClick = { launcher.launch(com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.MIME_TYPE_IMAGE) }) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                            Text(stringResource(R.string.select_photo))
                        }
                    }
                }

                if (selectedImageUri != null || state.photo?.imageUrl != null) {
                    TextButton(
                        onClick = { launcher.launch(com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.MIME_TYPE_IMAGE) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.change_photo))
                    }
                }

                if (photoId != null) {
                    // AI Editing Section
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            dimensionResource(R.dimen.spacing_tiny) / 2, // 1.dp
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large)),
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.ai_edit_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            var aiPrompt by remember { mutableStateOf("") }

                            OutlinedTextField(
                                value = aiPrompt,
                                onValueChange = { aiPrompt = it },
                                placeholder = { Text(stringResource(R.string.ai_edit_prompt_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                shape = MaterialTheme.shapes.medium
                            )

                            Button(
                                onClick = {
                                    viewModel.handleAction(EditAction.EditImage(aiPrompt))
                                },
                                enabled = aiPrompt.isNotBlank() && !state.isEditingImage,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (state.isEditingImage) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(
                                            dimensionResource(R.dimen.spacing_medium)
                                        )
                                    ) {
                                        Text(stringResource(R.string.ai_editing))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(
                                                dimensionResource(R.dimen.spacing_large) + dimensionResource(
                                                    R.dimen.spacing_small
                                                )
                                            ), // ~20.dp
                                            strokeWidth = dimensionResource(R.dimen.spacing_tiny), // 2.dp
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                } else {
                                    @Suppress("IMPLICIT_CAST_TO_ANY")
                                    Text(stringResource(R.string.ai_edit_button))
                                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small))
                                    )
                                }
                            }
                        }
                    }
                }

                EditTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.field_title_label),
                    error = state.validationErrors["title"]?.let { stringResource(it) }
                )

                EditTextField(
                    value = credit,
                    onValueChange = { credit = it },
                    label = stringResource(R.string.field_credit_label)
                )

                EditTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = stringResource(R.string.field_hint_label)
                )

                EditTextField(
                    value = difficulty,
                    onValueChange = { if (it.all { char -> char.isDigit() }) difficulty = it },
                    label = stringResource(R.string.field_difficulty_label),
                    keyboardType = KeyboardType.Number,
                    error = state.validationErrors["difficulty"]?.let { stringResource(it) }
                )

                EditTextField(
                    value = categories,
                    onValueChange = { categories = it },
                    label = stringResource(R.string.field_categories_label),
                    error = state.validationErrors["categories"]?.let { stringResource(it) },
                    trailingIcon = {
                        if (title.isNotBlank()) {
                            if (state.isGeneratingCategories) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small)),
                                    strokeWidth = dimensionResource(R.dimen.spacing_tiny)
                                )
                            } else {
                                IconButton(onClick = {
                                    viewModel.handleAction(EditAction.GenerateCategories(title))
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = stringResource(R.string.generate_categories_content_description),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

                Text(
                    text = stringResource(R.string.supported_platforms_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
                ) {
                    Platform.entries.forEach { platform ->
                        val platformLabel = when (platform) {
                            Platform.ANDROID -> stringResource(R.string.platform_android)
                            Platform.IOS -> stringResource(R.string.platform_ios)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = supportedPlatforms.contains(platform),
                                onCheckedChange = { checked ->
                                    supportedPlatforms = if (checked) {
                                        supportedPlatforms + platform
                                    } else {
                                        supportedPlatforms - platform
                                    }
                                }
                            )
                            Text(text = platformLabel)
                        }
                    }
                }

                if (photoId != null) {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                    Button(
                        onClick = { showDeleteDialog = true },
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensionResource(R.dimen.button_height_large)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(dimensionResource(R.dimen.spacing_large))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small))
                        )
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacing_normal)))
                        Text(
                            text = stringResource(R.string.delete_photo_action),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun EditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null,
    error: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            trailingIcon = trailingIcon,
            isError = error != null
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.spacing_normal),
                    top = dimensionResource(R.dimen.spacing_tiny)
                )
            )
        }
    }
}
