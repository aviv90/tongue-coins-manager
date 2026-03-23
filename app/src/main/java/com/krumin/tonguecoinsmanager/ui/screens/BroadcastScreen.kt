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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
            snackbarHostState.showSnackbar("ההודעה נשמרה בהצלחה")
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("ניהול הודעה יומית") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.onSave() },
                            enabled = !state.isLoading && state.isValid
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "שמור")
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Settings Section
                Text("הגדרות הודעה", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = state.id,
                    onValueChange = viewModel::onIdChanged,
                    label = { Text("מזהה הודעה (id)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.id.trim().isEmpty(),
                    supportingText = {
                        if (state.id.trim().isEmpty()) {
                            Text("מזהה חובה", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("מומלץ פורמט: daily-YYYY-MM-DD")
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("השבת הודעה (disabled)")
                    Switch(
                        checked = state.disabled,
                        onValueChange = viewModel::onDisabledChanged
                    )
                }

                Divider()

                // Content Section
                Text("תוכן ההודעה", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChanged,
                    label = { Text("כותרת") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.body,
                    onValueChange = viewModel::onBodyChanged,
                    label = { Text("גוף ההודעה") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    supportingText = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("תומך ב-Markdown בסיסי (למשל **הדגשה**)")
                        }
                    }
                )

                OutlinedTextField(
                    value = state.imageUrl ?: "",
                    onValueChange = { viewModel.onImageUrlChanged(it.ifBlank { null }) },
                    label = { Text("URL תמונה (imageUrl)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://...") }
                )

                Divider()

                // CTA Section
                Text("כפתור פעולה (CTA)", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = state.ctaText ?: "",
                    onValueChange = { viewModel.onCtaTextChanged(it.ifBlank { null }) },
                    label = { Text("טקסט כפתור (ctaText)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = (!state.ctaText.isNullOrBlank() && state.ctaUrl.isNullOrBlank()) || (state.ctaText.isNullOrBlank() && !state.ctaUrl.isNullOrBlank())
                )

                OutlinedTextField(
                    value = state.ctaUrl ?: "",
                    onValueChange = { viewModel.onCtaUrlChanged(it.ifBlank { null }) },
                    label = { Text("קישור כפתור (ctaUrl)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://...") },
                    isError = (!state.ctaText.isNullOrBlank() && state.ctaUrl.isNullOrBlank()) || (state.ctaText.isNullOrBlank() && !state.ctaUrl.isNullOrBlank()),
                    supportingText = {
                        if ((!state.ctaText.isNullOrBlank() && state.ctaUrl.isNullOrBlank()) || (state.ctaText.isNullOrBlank() && !state.ctaUrl.isNullOrBlank())) {
                            Text("חובה למלא את שני השדות או להשאיר את שניהם ריקים", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Preview Section
                Text("תצוגה מקדימה (כפי שיופיע בדיאלוג)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.disabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.disabled) {
                            Text(
                                "ההודעה מושבתת",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (!state.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = state.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .background(Color.Gray),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (state.title.isNotBlank()) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (state.body.isNotBlank()) {
                            Text(
                                text = state.body,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (!state.ctaText.isNullOrBlank() && !state.ctaUrl.isNullOrBlank()) {
                                Button(onClick = { /* Do nothing in preview */ }) {
                                    Text(state.ctaText)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Button(
                                onClick = { /* Do nothing in preview */ },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("סגור")
                            }
                        }
                    }
                }
            }
        }
    }
}
