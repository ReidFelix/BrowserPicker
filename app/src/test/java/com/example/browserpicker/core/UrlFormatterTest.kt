package com.example.browserpicker.core

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlFormatterTest {

    @Test
    fun `the https scheme is hidden`() {
        assertEquals("example.com/page", UrlFormatter.display("https://example.com/page"))
    }

    @Test
    fun `the http scheme is hidden`() {
        assertEquals("example.com/page", UrlFormatter.display("http://example.com/page"))
    }

    @Test
    fun `a trailing slash is removed`() {
        assertEquals("example.com", UrlFormatter.display("https://example.com/"))
    }

    @Test
    fun `several trailing slashes are removed`() {
        assertEquals("example.com", UrlFormatter.display("https://example.com//"))
    }

    @Test
    fun `the path and query are kept`() {
        assertEquals(
            "example.com/search?q=a%20b&page=2",
            UrlFormatter.display("https://example.com/search?q=a%20b&page=2")
        )
    }

    @Test
    fun `the scheme is matched case insensitively`() {
        assertEquals("example.com", UrlFormatter.display("HTTPS://example.com"))
    }

    @Test
    fun `a url without a scheme is returned unchanged`() {
        assertEquals("example.com/page", UrlFormatter.display("example.com/page"))
    }

    @Test
    fun `an url consisting only of a scheme falls back to the original`() {
        assertEquals("https://", UrlFormatter.display("https://"))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("example.com", UrlFormatter.display("  https://example.com  "))
    }
}