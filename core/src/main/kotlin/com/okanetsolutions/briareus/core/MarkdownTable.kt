package com.okanetsolutions.briareus.core

/** Copies a rendered table without losing its header or cell boundaries. */
fun Block.Table.markdown(): String {
    fun row(cells: List<String>): String = cells.joinToString(" | ", "| ", " |") { it.replace("|", "\\|").replace("\n", "<br>") }
    return (listOf(row(header), row(header.map { "---" })) + rows.map(::row)).joinToString("\n")
}
