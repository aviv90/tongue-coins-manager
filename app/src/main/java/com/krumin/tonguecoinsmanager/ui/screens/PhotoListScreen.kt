package com.krumin.tonguecoinsmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.AsyncImage
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.ui.viewmodel.MainAction
import com.krumin.tonguecoinsmanager.ui.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoListScreen(
    onAddPhoto: () -> Unit,
    onEditPhoto: (String) -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
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
                                modifier = Modifier.fillMaxWidth(),
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
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (state.error != null) {
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
                    if (state.filteredPhotos.isEmpty()) {
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
                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large)),
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
                        ) {
                            items(
                                items = state.filteredPhotos,
                                key = { it.id }
                            ) { photo ->
                                PhotoCard(
                                    photo = photo,
                                    onClick = { onEditPhoto(photo.id) }
                                )
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
    onClick: () -> Unit
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

            Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))) {
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
        }
    }
}
