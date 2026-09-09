package com.fittrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.fittrack.app.data.remote.MediaUrlProvider

/**
 * Icon shown whenever the exercise GIF can't be shown: null mediaId, offline,
 * 404/rate limit, or still loading. Keyed by the dataset's body_part values.
 */
fun bodyPartPlaceholderIcon(bodyPart: String): ImageVector = when (bodyPart.lowercase()) {
    "cardio" -> Icons.AutoMirrored.Filled.DirectionsRun
    "upper legs", "lower legs" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "waist", "neck" -> Icons.Filled.Accessibility
    "back", "shoulders" -> Icons.Filled.SportsGymnastics
    else -> Icons.Filled.FitnessCenter // chest, upper/lower arms, unknown
}

/**
 * Loads the exercise's animated GIF from the RapidAPI ExerciseDB via Coil
 * (memory + disk cached, lazy — only requested when this composable enters
 * composition). Degrades to a body-part placeholder icon when no API key is
 * configured, the device is offline, or the request fails; exercise data
 * itself never depends on the network.
 */
@Composable
fun ExerciseImage(
    exerciseId: String?,
    bodyPart: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val url = MediaUrlProvider.gifUrl(exerciseId)
    if (url == null) {
        PlaceholderIcon(bodyPart, modifier)
        return
    }
    val request = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .setHeader("x-rapidapi-key", MediaUrlProvider.apiKey)
        .setHeader("x-rapidapi-host", MediaUrlProvider.RAPIDAPI_HOST)
        .crossfade(true)
        .build()
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        },
        error = { PlaceholderIcon(bodyPart, Modifier.fillMaxSize()) },
    )
}

@Composable
private fun PlaceholderIcon(bodyPart: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = bodyPartPlaceholderIcon(bodyPart),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
