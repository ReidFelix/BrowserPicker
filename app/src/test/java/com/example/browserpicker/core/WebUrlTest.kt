package com.example.browserpicker.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebUrlTest {

    @Test
    fun `http and https are supported`() {
        assertTrue(WebUrl.isSupportedScheme("http"))
        assertTrue(WebUrl.isSupportedScheme("https"))
    }

    @Test
    fun `the scheme is matched case insensitively`() {
        assertTrue(WebUrl.isSupportedScheme("HTTPS"))
    }

    @Test
    fun `other schemes are rejected`() {
        assertFalse(WebUrl.isSupportedScheme("mailto"))
        assertFalse(WebUrl.isSupportedScheme("file"))
        assertFalse(WebUrl.isSupportedScheme("content"))
        assertFalse(WebUrl.isSupportedScheme("ftp"))
    }

    @Test
    fun `a missing scheme is rejected`() {
        assertFalse(WebUrl.isSupportedScheme(null))
        assertFalse(WebUrl.isSupportedScheme(""))
    }
}