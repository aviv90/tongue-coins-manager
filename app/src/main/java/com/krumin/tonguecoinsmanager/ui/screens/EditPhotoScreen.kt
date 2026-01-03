package com.krumin.tonguecoinsmanager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
    viewModel: EditPhotoViewModel = koinViewModel(parameters = { parametersOf(photoId) })
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var credit by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("1") }
    var categories by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(state.photo) {
        state.photo?.let {
            title = it.title
            credit = it.credit
            hint = it.hint
            difficulty = it.difficulty.toString()
            categories = it.categories
        }
    }

    if (state.isSuccess) {
        LaunchedEffect(Unit) {
            onBack()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                    val imageSource = selectedImageUri ?: state.photo?.imageUrl
                    if (imageSource != null) {
                        AsyncImage(
                            model = imageSource,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        OutlinedButton(onClick = { launcher.launch("image/*") }) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(dimensionResource(R.dimen.spacing_medium)))
                            Text(stringResource(R.string.select_photo))
                        }
                    }
                }

                if (selectedImageUri != null || state.photo?.imageUrl != null) {
                    TextButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.change_photo))
                    }
                }

                EditTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.field_title_label)
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
                    keyboardType = KeyboardType.Number
                )

                EditTextField(
                    value = categories,
                    onValueChange = { categories = it },
                    label = stringResource(R.string.field_categories_label)
                )

                if (photoId != null) {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                    Button(
                        onClick = { showDeleteDialog = true },
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

                if (state.error != null) {
                    Text(
                        text = state.error?.asString() ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))
                    )
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}
