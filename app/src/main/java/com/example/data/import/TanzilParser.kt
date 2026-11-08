package com.example.data.import

import java.io.InputStream

data class ParsedAyah(
    val surahId: Int,
    val ayahNo: Int,
    val text: String
)

data class TanzilFileResult(
    val ayahs: List<ParsedAyah>,
    val comments: List<String>
)

object TanzilParser {

    /**
     * Parses a Tanzil text file (quran-uthmani.txt, quran-simple-clean.txt, bn.bengali.txt).
     * Line format: <sura>|<aya>|<text>
     * - Lines starting with '#' are comments/copyright.
     * - Blank lines are skipped.
     * - Strips UTF-8 BOM if present on the first line.
     * - Throws IllegalArgumentException on invalid format.
     */
    fun parse(inputStream: InputStream, filenameForError: String = "data"): TanzilFileResult {
        val ayahs = ArrayList<ParsedAyah>(6236)
        val comments = mutableListOf<String>()

        inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            var lineNumber = 0
            for (rawLine in lines) {
                lineNumber++
                var line = rawLine
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1)
                }
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.startsWith("#")) {
                    comments.add(trimmed)
                    continue
                }

                val parts = line.split("|", limit = 3)
                require(parts.size == 3) {
                    "Invalid format in $filenameForError at line $lineNumber: expected 3 parts, got ${parts.size}"
                }

                val surahId = parts[0].trim().toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid surah id in $filenameForError at line $lineNumber: '${parts[0]}'")
                val ayahNo = parts[1].trim().toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid ayah no in $filenameForError at line $lineNumber: '${parts[1]}'")
                val text = parts[2].trim()

                require(text.isNotEmpty()) {
                    "Empty text in $filenameForError at line $lineNumber for $surahId:$ayahNo"
                }

                ayahs.add(ParsedAyah(surahId, ayahNo, text))
            }
        }

        return TanzilFileResult(ayahs, comments)
    }
}
