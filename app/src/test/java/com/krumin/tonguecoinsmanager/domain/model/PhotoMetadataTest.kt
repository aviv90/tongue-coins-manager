package com.krumin.tonguecoinsmanager.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoMetadataTest {

    @Test
    fun `trimmed() should remove leading and trailing whitespace from string fields`() {
        val metadata = PhotoMetadata(
            id = "test-id",
            imageUrl = "http://example.com/image.jpg",
            title = "  Awesome Title  ",
            credit = "\tPhotographer Name\n",
            hint = "  Some hint with spaces  ",
            difficulty = 2,
            categories = " category1, category2  ",
            version = 1,
            aspectRatio = 1.0f
        )

        val trimmed = metadata.trimmed()

        assertEquals("Awesome Title", trimmed.title)
        assertEquals("Photographer Name", trimmed.credit)
        assertEquals("Some hint with spaces", trimmed.hint)
        assertEquals("category1, category2", trimmed.categories)

        // Non-string fields should remain unchanged
        assertEquals(metadata.id, trimmed.id)
        assertEquals(metadata.imageUrl, trimmed.imageUrl)
        assertEquals(metadata.difficulty, trimmed.difficulty)
        assertEquals(metadata.version, trimmed.version)
        assertEquals(metadata.aspectRatio, trimmed.aspectRatio, 0.0f)
    }
}
