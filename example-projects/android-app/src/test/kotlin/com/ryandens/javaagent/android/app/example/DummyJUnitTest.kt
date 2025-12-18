package com.ryandens.javaagent.android.app.example

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class DummyJUnitTest {
    @Mock
    private lateinit var mockList: MutableList<String>

    @Test
    fun test() {
        assertFalse(mockList.isEmpty())
    }
}
