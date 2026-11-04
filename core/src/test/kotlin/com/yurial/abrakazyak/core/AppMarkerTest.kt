package com.yurial.abrakazyak.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AppMarkerTest {

    @Test
    fun markerIsASingletonInstance() {
        assertSame(AppMarker, AppMarker)
    }

    @Test
    fun markerHasStableStringForm() {
        assertEquals("AppMarker", AppMarker.toString())
    }
}
