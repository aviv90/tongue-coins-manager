package com.krumin.tonguecoinsmanager.util

import com.krumin.tonguecoinsmanager.R
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class UserErrorMapperTest {

    @Test
    fun `IOException maps to network message`() {
        val ui = IOException("x").toFriendlyUiText() as UiText.StringResource
        assertEquals(R.string.error_network_friendly, ui.resId)
    }

    @Test
    fun `other failures map to generic message`() {
        val generic = R.string.error_generic_friendly
        assertEquals(generic, (FileNotFoundException().toFriendlyUiText() as UiText.StringResource).resId)
        assertEquals(generic, (IllegalStateException().toFriendlyUiText() as UiText.StringResource).resId)
    }
}
