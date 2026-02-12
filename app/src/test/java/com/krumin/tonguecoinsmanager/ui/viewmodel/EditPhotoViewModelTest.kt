package com.krumin.tonguecoinsmanager.ui.viewmodel

import com.krumin.tonguecoinsmanager.domain.model.PendingChange
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.model.Platform
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.domain.service.CategoryGenerator
import com.krumin.tonguecoinsmanager.domain.service.ImageEditor
import com.krumin.tonguecoinsmanager.util.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EditPhotoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: PhotoRepository = mock()
    private val categoryGenerator: CategoryGenerator = mock()
    private val imageEditor: ImageEditor = mock()

    @Test
    fun `loadPhoto should prefer pending changes over server data`() = runTest {
        // Given
        val photoId = "photo1"
        val serverPhoto = PhotoMetadata(
            id = photoId,
            imageUrl = "server_url",
            title = "Server Title",
            version = 1,
            supportedPlatforms = listOf(Platform.ANDROID)
        )
        val pendingPhoto = serverPhoto.copy(
            title = "Draft Title",
            version = 2,
            supportedPlatforms = listOf(Platform.ANDROID, Platform.IOS)
        )
        val pendingChange = PendingChange.Edit(
            id = photoId,
            metadata = pendingPhoto,
            newImageFile = null
        )

        whenever(repository.getPhotos()).doReturn(listOf(serverPhoto))
        whenever(repository.pendingChanges).thenReturn(MutableStateFlow(listOf(pendingChange)))

        val viewModel = EditPhotoViewModel(
            repository = repository,
            categoryGenerator = categoryGenerator,
            imageEditor = imageEditor,
            photoId = photoId
        )

        // Then
        // Wait for initial load
        // Note: In a real test we might need advanceUntilIdle() or similar if using UnconfinedTestDispatcher isn't enough

        // This assertion is EXPECTED TO FAIL currently because the bug exists
        assertEquals("Draft Title", viewModel.state.value.photo?.title)
        assertEquals(
            listOf(Platform.ANDROID, Platform.IOS),
            viewModel.state.value.photo?.supportedPlatforms
        )
    }
}
