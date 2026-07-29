package com.ingendns.app

private val VERSION_PATTERN = Regex("\\d+(?:\\.\\d+)*")

internal fun extractAppVersion(value: String): String? =
    VERSION_PATTERN.find(value)?.value

internal fun compareAppVersions(candidate: String, installed: String): Int {
    val candidateParts = extractAppVersion(candidate)?.split('.')?.map(String::toIntOrNull)
        ?: return 0
    val installedParts = extractAppVersion(installed)?.split('.')?.map(String::toIntOrNull)
        ?: return 0
    val size = maxOf(candidateParts.size, installedParts.size)
    for (index in 0 until size) {
        val candidatePart = candidateParts.getOrNull(index) ?: 0
        val installedPart = installedParts.getOrNull(index) ?: 0
        if (candidatePart != installedPart) return candidatePart.compareTo(installedPart)
    }
    return 0
}
