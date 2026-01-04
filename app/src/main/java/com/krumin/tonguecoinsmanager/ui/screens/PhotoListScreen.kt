package com.krumin.tonguecoinsmanager.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.krumin.tonguecoinsmanager.R
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
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
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
                    Column {
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
                                        color = MaterialTheme.colorScheme.error
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
                                        modifier = Modifier.align(Alignment.Center),
                                        style = MaterialTheme.typography.bodyLarge
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
                                                isDownloading = state.downloadingPhotoId == photo.id
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
    isDownloading: Boolean
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
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(R.dimen.spacing_medium),
                        top = dimensionResource(R.dimen.spacing_medium),
                        bottom = dimensionResource(R.dimen.spacing_medium),
                        end = 2.dp // Precisely 2dp from the left edge
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = photo.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(R.string.difficulty_label, photo.difficulty),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                IconButton(
                    onClick = onDownload,
                    enabled = !isDownloading,
                    modifier = Modifier.size(40.dp) // Explicit smaller size
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Download Photo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
