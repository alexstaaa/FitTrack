package com.fittrack.app

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.fittrack.app.data.seed.ExerciseSeeder
import com.fittrack.app.data.seed.StarterPlanSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point. Kicks off the one-time exercise/starter-plan seed
 * and provides the app-wide Coil [ImageLoader] with GIF support (animated
 * exercise demos). Coil's default memory + disk caches stay enabled, so a GIF
 * scrolling back into view is served locally instead of re-hitting the CDN.
 */
@HiltAndroidApp
class FitTrackApplication : Application(), ImageLoaderFactory {

    @Inject lateinit var exerciseSeeder: ExerciseSeeder
    @Inject lateinit var starterPlanSeeder: StarterPlanSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            exerciseSeeder.seedIfEmpty()
            starterPlanSeeder.seedIfEmpty()
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
}
