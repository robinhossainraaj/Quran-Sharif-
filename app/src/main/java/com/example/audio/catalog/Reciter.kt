package com.example.audio.catalog

data class Reciter(
    val id: String,
    val edition: String,
    val nameEn: String,
    val nameBn: String,
    val style: String,
    val bitrate: Int,
    val isDefault: Boolean
) {
    fun displayName(useBangla: Boolean = false): String {
        return if (useBangla && nameBn.isNotBlank()) nameBn else nameEn
    }
}

data class ReciterCatalog(
    val cdnBase: String,
    val reciters: List<Reciter>
) {
    fun defaultReciter(): Reciter {
        return reciters.firstOrNull { it.isDefault } ?: reciters.firstOrNull() ?: fallbackReciter
    }

    fun findById(id: String): Reciter? {
        return reciters.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    companion object {
        val fallbackReciter = Reciter(
            id = "alafasy",
            edition = "ar.alafasy",
            nameEn = "Mishary Rashid Alafasy",
            nameBn = "মিশারী রশিদ আল-আফাসী",
            style = "Murattal",
            bitrate = 128,
            isDefault = true
        )
    }
}
