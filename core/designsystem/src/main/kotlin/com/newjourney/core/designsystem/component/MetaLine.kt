package com.newjourney.core.designsystem.component

private const val NBSP = ' '
private const val SEPARATOR = " · "

/** Short parts ("Diepe slaap", "8 min") stay whole; longer ones may still wrap between words. */
private const val KEEP_TOGETHER_MAX = 24

/**
 * Turns a "Slaap · Diepe slaap · 8 min" line into one that only wraps between its parts,
 * so a number never ends up on a line without its unit and a line never starts with "·".
 */
fun String.asMetaLine(): String = split(SEPARATOR)
    .filter { it.isNotBlank() }
    .joinToString("$NBSP· ") { part ->
        if (part.length <= KEEP_TOGETHER_MAX) part.replace(' ', NBSP) else part
    }
