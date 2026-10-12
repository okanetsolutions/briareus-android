package com.okanetsolutions.briareus.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTableTest {
    @Test fun copiesTableAsMarkdownWithEscapedCellSeparators() {
        val table = Block.Table(listOf("Name", "Value"), listOf(listOf("a | b", "**bold**"), listOf("x", "")))
        assertEquals("| Name | Value |\n| --- | --- |\n| a \\| b | **bold** |\n| x |  |", table.markdown())
        assertEquals(table, Markdown.parse(table.markdown()).single())
    }
}
