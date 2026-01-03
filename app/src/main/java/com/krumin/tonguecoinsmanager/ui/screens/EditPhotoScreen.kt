package com.krumin.tonguecoinsmanager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.krumin.tonguecoinsmanager.ui.viewmodel.EditPhotoViewModel
import com.krumin.tonguecoinsmanager.ui.viewmodel.EditUiState
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhotoScreen(
    photoId: String?,
    onBack: () -> Unit,
    viewModel: EditPhotoViewModel = koinViewModel(parameters = { parametersOf(photoId) })
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPhoto by viewModel.currentPhoto.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var credit by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("1") }
    var categories by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(currentPhoto) {
        currentPhoto?.let {
            title = it.title
            credit = it.credit
            hint = it.hint
            difficulty = it.difficulty.toString()
            categories = it.categories
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (photoId == null) "Add Photo" else "Edit Photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is EditUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp).size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = {
                            val file = selectedImageUri?.let { uriToTempFile(context, it) }
                            viewModel.savePhoto(
                                title = title,
                                credit = credit,
                                hint = hint,
                                difficulty = difficulty.toIntOrNull() ?: 1,
                                categories = categories,
                                imageFile = file
                            )
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Preview/Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (currentPhoto?.imageUrl != null) {
                    AsyncImage(
                        model = currentPhoto?.imageUrl,
                        contentDescription = "Current image",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    OutlinedButton(onClick = { launcher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pick Image")
                    }
                }
            }
            if (selectedImageUri != null || currentPhoto?.imageUrl != null) {
                TextButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Change Image")
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title (The Phrase)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = credit,
                onValueChange = { credit = it },
                label = { Text("Credit") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = hint,
                onValueChange = { hint = it },
                label = { Text("Hint") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = difficulty,
                onValueChange = { if (it.all { char -> char.isDigit() }) difficulty = it },
                label = { Text("Difficulty (1-10)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = categories,
                onValueChange = { categories = it },
                label = { Text("Categories (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            if (uiState is EditUiState.Success) {
                LaunchedEffect(Unit) {
                    onBack()
                }
            }

            if (uiState is EditUiState.Error) {
                Text(
                    text = (uiState as EditUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

fun uriToTempFile(context: android.content.Context, uri: Uri): File {
    val inputStream = context.contentResolver.openInputStream(uri)
    val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
    val outputStream = FileOutputStream(tempFile)
    inputStream?.copyTo(outputStream)
    return tempFile
}
