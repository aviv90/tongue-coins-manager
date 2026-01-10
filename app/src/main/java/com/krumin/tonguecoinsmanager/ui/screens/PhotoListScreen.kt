package com.krumin.tonguecoinsmanager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.PendingChange
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.ui.navigation.Screen
import com.krumin.tonguecoinsmanager.ui.viewmodel.MainAction
import com.krumin.tonguecoinsmanager.ui.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoListScreen(
    onAddPhoto: () -> Unit,
    onEditPhoto: (String) -> Unit,
    navController: NavController? = null,
    viewModel: MainViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var isSearchActive by rememberSaveable { mutableStateOf(state.searchQuery.isNotEmpty()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // Observe result from EditPhotoScreen
    val navigationResult = navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>(Screen.RESULT_KEY, null)
        ?.collectAsState()

    LaunchedEffect(navigationResult?.value) {
        navigationResult?.value?.let { result ->
            val message = when (result) {
                Screen.RESULT_ADD -> context.getString(R.string.success_add)
                Screen.RESULT_EDIT -> context.getString(R.string.success_edit)
                Screen.RESULT_DELETE -> context.getString(R.string.success_delete)
                else -> null
            }
            message?.let {
                snackbarHostState.showSnackbar(it)
            }
            // Clear result and refresh
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>(Screen.RESULT_KEY)
            viewModel.handleAction(MainAction.LoadPhotos)
        }
    }

    LaunchedEffect(state.downloadSuccess) {
        state.downloadSuccess?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.handleAction(MainAction.ClearDownloadStatus)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            if (state.photos.isNotEmpty()) {
                snackbarHostState.showSnackbar(it.asString(context))
                viewModel.handleAction(MainAction.ClearError)
            }
        }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        viewModel.handleAction(MainAction.SearchQueryChanged(""))
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.handleAction(MainAction.LoadPhotos)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = state.searchQuery,
                                onValueChange = {
                                    viewModel.handleAction(
                                        MainAction.SearchQueryChanged(
                                            it
                                        )
                                    )
                                },
                                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.handleAction(MainAction.SearchQueryChanged(""))
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.search_close_content_description)
                                )
                            }
                        }
                    )
                } else {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_title_main),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search_content_description)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.handleAction(MainAction.LoadPhotos) }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.refresh_content_description)
                                )
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddPhoto) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_photo_content_description)
                    )
                }
            },
            bottomBar = {
                // Batch Action Bar
                AnimatedVisibility(
                    visible = state.pendingChanges.isNotEmpty() && !state.isCommitting,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    BatchActionBar(
                        count = state.pendingChanges.size,
                        onCommit = { viewModel.handleAction(MainAction.CommitChanges) },
                        onDiscard = { viewModel.handleAction(MainAction.DiscardChanges) }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (state.isLoading && state.photos.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state.isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (state.error != null && state.photos.isEmpty()) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = state.error?.asString() ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                                    Button(onClick = { viewModel.handleAction(MainAction.LoadPhotos) }) {
                                        Text(stringResource(R.string.try_again))
                                    }
                                }
                            } else {
                                if (state.filteredPhotos.isEmpty() && !state.isLoading) {
                                    Text(
                                        text = if (state.searchQuery.isEmpty()) {
                                            stringResource(R.string.no_photos_found)
                                        } else {
                                            stringResource(R.string.no_photos_match_search)
                                        },
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .fillMaxWidth()
                                            .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(dimensionResource(R.dimen.grid_column_min)),
                                        contentPadding = PaddingValues(dimensionResource(R.dimen.spacing_large)),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            dimensionResource(R.dimen.spacing_large)
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(
                                            dimensionResource(
                                                R.dimen.spacing_large
                                            )
                                        ),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            items = state.filteredPhotos,
                                            key = { it.id }
                                        ) { photo ->
                                            PhotoCard(
                                                photo = photo,
                                                onClick = { onEditPhoto(photo.id) },
                                                onDownload = {
                                                    viewModel.handleAction(
                                                        MainAction.DownloadPhoto(
                                                            photo
                                                        )
                                                    )
                                                },
                                                isDownloading = state.downloadingPhotoId == photo.id,
                                                pendingChange = state.pendingChanges.find { it.id == photo.id }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Commit Loading Overlay
                AnimatedVisibility(
                    visible = state.isCommitting,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            Text(
                                text = stringResource(R.string.batch_saving_progress),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }


            }
        }
    }
}

@Composable
fun BatchActionBar(
    count: Int,
    onCommit: () -> Unit,
    onDiscard: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(dimensionResource(R.dimen.spacing_large))
            .navigationBarsPadding()
            .fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius_large)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        tonalElevation = dimensionResource(R.dimen.surface_elevation_high),
        border = androidx.compose.foundation.BorderStroke(
            dimensionResource(R.dimen.stroke_thin),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.spacing_medium))
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.batch_pending_changes, count),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
                IconButton(onClick = onDiscard) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = stringResource(R.string.batch_discard),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = onCommit,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.card_radius_medium))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small))
                    )
                    Spacer(modifier = Modifier.size(dimensionResource(R.dimen.spacing_small)))
                    Text(stringResource(R.string.batch_commit))
                }
            }
        }
    }
}

@Composable
fun PhotoCard(
    photo: PhotoMetadata,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    isDownloading: Boolean,
    pendingChange: PendingChange?
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = photo.imageUrl,
                    contentDescription = photo.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.card_image_height))
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.spacing_medium))
                        .align(Alignment.TopEnd),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.badge_radius)),
                    tonalElevation = dimensionResource(R.dimen.surface_elevation)
                ) {
                    Text(
                        text = photo.id,
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.badge_padding_horizontal),
                            vertical = dimensionResource(R.dimen.spacing_tiny)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (pendingChange != null) {
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                text = stringResource(R.string.pending_badge),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (pendingChange) {
                                    is PendingChange.Add -> Icons.Default.Add
                                    is PendingChange.Edit -> Icons.Default.Check
                                    is PendingChange.Delete -> Icons.Default.Delete
                                },
                                contentDescription = null,
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_tiny))
                            )
                        },
                        modifier = Modifier
                            .padding(dimensionResource(R.dimen.spacing_medium))
                            .align(Alignment.BottomStart),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                        ),
                        border = null,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.badge_radius))
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(R.dimen.spacing_medium),
                        top = dimensionResource(R.dimen.spacing_medium),
                        bottom = dimensionResource(R.dimen.spacing_medium),
                        end = dimensionResource(R.dimen.spacing_tiny_horizontal)
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = photo.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.difficulty_label, photo.difficulty),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                IconButton(
                    onClick = onDownload,
                    enabled = !isDownloading,
                    modifier = Modifier.size(dimensionResource(R.dimen.download_button_size))
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(dimensionResource(R.dimen.progress_size_small)),
                            strokeWidth = dimensionResource(R.dimen.stroke_small),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.download_photo_content_description),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
