package com.example.ui.background

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

/**
 * FEATURE 2: High-Performance Looping Video Background
 * Uses hardware-accelerated TextureView with MediaPlayer to display an alarm video:
 * 1. Looping enabled.
 * 2. Muted audio (0f) so it does not interfere with the alarm ringtone.
 * 3. Perfect aspect ratio Center-Crop matrix: fills screen without stretching or black bars.
 * 4. Automatic resource cleanup on dispose.
 */
@Composable
fun LoopingVideoBackground(
    videoPathOrUri: String,
    modifier: Modifier = Modifier,
    fallbackContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val file = remember(videoPathOrUri) { File(videoPathOrUri) }

    if (!file.exists() && !videoPathOrUri.startsWith("content://")) {
        fallbackContent()
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                TextureView(ctx).apply {
                    var mediaPlayer: MediaPlayer? = null

                    fun applyCenterCropTransform(viewWidth: Int, viewHeight: Int, videoWidth: Int, videoHeight: Int) {
                        if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return
                        val viewRatio = viewWidth.toFloat() / viewHeight
                        val videoRatio = videoWidth.toFloat() / videoHeight

                        val matrix = Matrix()
                        val scaleX: Float
                        val scaleY: Float

                        if (viewRatio > videoRatio) {
                            scaleX = 1f
                            scaleY = (viewWidth.toFloat() / videoWidth) / (viewHeight.toFloat() / videoHeight)
                        } else {
                            scaleX = (viewHeight.toFloat() / videoHeight) / (viewWidth.toFloat() / videoWidth)
                            scaleY = 1f
                        }

                        val pivotX = viewWidth / 2f
                        val pivotY = viewHeight / 2f
                        matrix.setScale(scaleX, scaleY, pivotX, pivotY)
                        setTransform(matrix)
                    }

                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            try {
                                val s = Surface(surface)
                                mediaPlayer = MediaPlayer().apply {
                                    setSurface(s)
                                    if (videoPathOrUri.startsWith("content://")) {
                                        setDataSource(ctx, Uri.parse(videoPathOrUri))
                                    } else {
                                        setDataSource(videoPathOrUri)
                                    }
                                    isLooping = true
                                    setVolume(0f, 0f) // Strictly muted per user requirements

                                    setOnVideoSizeChangedListener { mp, vidWidth, vidHeight ->
                                        applyCenterCropTransform(width, height, vidWidth, vidHeight)
                                    }

                                    setOnPreparedListener { mp ->
                                        applyCenterCropTransform(width, height, mp.videoWidth, mp.videoHeight)
                                        mp.start()
                                    }

                                    setOnErrorListener { _, _, _ ->
                                        // Ignore and handle gracefully
                                        true
                                    }

                                    prepareAsync()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                            mediaPlayer?.let { mp ->
                                applyCenterCropTransform(width, height, mp.videoWidth, mp.videoHeight)
                            }
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            try {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                            } catch (_: Exception) {}
                            mediaPlayer = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
            },
            update = { /* Updates handled internally */ }
        )
    }
}
