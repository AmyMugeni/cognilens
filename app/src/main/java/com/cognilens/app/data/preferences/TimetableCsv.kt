package com.cognilens.app.data.preferences

import com.cognilens.app.data.local.entity.TimetableEntry

object TimetableCsv {
    private val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    private val timeRegex = Regex("""^(\d{1,2}):(\d{2})$""")

    fun parse(raw: String): List<TimetableEntry> {
        val text = raw.trim()
            .removePrefix("```csv").removePrefix("```").removeSuffix("```").trim()
        return text.lines()
            .filterNot { it.isBlank() || it.trim().lowercase().startsWith("day,") }
            .mapNotNull { line ->
                val c = splitLine(line)
                if (c.size < 3) return@mapNotNull null
                val day = days.indexOf(c[0].trim().take(3).uppercase()) + 1
                val start = minutes(c[1])
                val end = minutes(c[2])
                if (day == 0 || start == null || end == null || end <= start) return@mapNotNull null
                TimetableEntry(
                    dayOfWeek = day,
                    startMinutes = start,
                    endMinutes = end,
                    courseCode = c.getOrElse(3) { "" }.trim().take(32),
                    courseName = c.getOrElse(4) { "" }.trim().take(120),
                    location = c.getOrElse(5) { "" }.trim().take(80)
                )
            }
    }

    private fun minutes(s: String): Int? {
        val m = timeRegex.matchEntire(s.trim()) ?: return null
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        return if (h in 0..23 && min in 0..59) h * 60 + min else null
    }

    private fun splitLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { out.add(sb.toString()); sb.clear() }
                else -> sb.append(ch)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}