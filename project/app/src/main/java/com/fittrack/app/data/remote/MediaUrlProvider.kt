package com.fittrack.app.data.remote

import com.fittrack.app.BuildConfig

/**
 * Builds exercise GIF requests against the RapidAPI-hosted ExerciseDB.
 *
 * History: the original free CDN (`static.exercisedb.dev`) went NXDOMAIN in
 * July 2026, so media now comes from the maintained RapidAPI deployment,
 * which serves GIFs by the dataset's 4-digit exercise id and requires an API
 * key (see `EXERCISEDB_API_KEY` in local.properties).
 *
 * GIFs are always fetched at runtime (via Coil in the UI layer) — never
 * bundled into the APK. With no key configured, [gifUrl] returns null and
 * every image slot falls back to the body-part placeholder icon; exercise
 * data itself never depends on the network. Don't prefetch in bulk — the
 * free tier is rate-limited; let Coil load lazily and cache on disk.
 */
object MediaUrlProvider {
    private const val BASE_URL = "https://exercisedb.p.rapidapi.com/image"
    const val RAPIDAPI_HOST = "exercisedb.p.rapidapi.com"

    val apiKey: String get() = BuildConfig.EXERCISEDB_API_KEY

    /** GIF URL for a dataset exercise id (e.g. "0001"), or null if unavailable. */
    fun gifUrl(exerciseId: String?): String? {
        if (apiKey.isBlank()) return null
        val id = exerciseId?.takeIf { it.isNotBlank() } ?: return null
        return "$BASE_URL?exerciseId=$id&resolution=360"
    }
}
