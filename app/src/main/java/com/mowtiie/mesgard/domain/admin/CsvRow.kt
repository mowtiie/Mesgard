package com.mowtiie.mesgard.domain.admin

private val DELIMITERS = listOf(',', ';', '\t')
private const val QUOTE = '"'

/** A row of a CSV file with the line it starts on, counted from one. */
internal class CsvRow(val line: Int, val cells: List<String>) {
    fun cell(index: Int): String = if (index < 0) "" else cells.getOrNull(index)?.trim().orEmpty()
}

/** Splits CSV text into rows, following RFC 4180 quoting. The delimiter is the most frequent one in the first line. */
internal fun readCsvRows(text: String): List<CsvRow> = CsvTokenizer(detectDelimiter(text)).read(text)

private fun detectDelimiter(text: String): Char {
    val header = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
    return DELIMITERS.maxByOrNull { delimiter -> header.count { it == delimiter } } ?: ','
}

private class CsvTokenizer(private val delimiter: Char) {
    private val rows = mutableListOf<CsvRow>()
    private val cells = mutableListOf<String>()
    private val cell = StringBuilder()
    private var inQuotes = false
    private var line = 1
    private var rowLine = 1

    fun read(text: String): List<CsvRow> {
        var index = 0
        while (index < text.length) {
            index += consume(text[index], text.getOrNull(index + 1))
        }
        if (cell.isNotEmpty() || cells.isNotEmpty()) endRow()
        return rows
    }

    /** Returns how many characters were used: two for an escaped quote, otherwise one. */
    private fun consume(character: Char, next: Char?): Int {
        when {
            inQuotes && character == QUOTE && next == QUOTE -> {
                cell.append(QUOTE)
                return 2
            }
            character == QUOTE -> inQuotes = !inQuotes
            inQuotes -> appendQuoted(character)
            character == delimiter -> endCell()
            character == '\n' -> endLine()
            character != '\r' -> cell.append(character)
        }
        return 1
    }

    private fun appendQuoted(character: Char) {
        if (character == '\n') line++
        cell.append(character)
    }

    private fun endLine() {
        endRow()
        line++
        rowLine = line
    }

    private fun endCell() {
        cells += cell.toString()
        cell.clear()
    }

    private fun endRow() {
        endCell()
        if (cells.any { it.isNotBlank() }) rows += CsvRow(rowLine, cells.toList())
        cells.clear()
    }
}
