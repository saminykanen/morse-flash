package com.example.morseflash

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.*

class MorseFlasher(
    context: Context,
    private val scope: CoroutineScope
) {
    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val torchCameraId: String? = findTorchCameraId()

    var onStatus: ((String) -> Unit)? = null

    private var flashingJob: Job? = null

    private fun findTorchCameraId(): String? {
        val ids = cameraManager.cameraIdList
        var fallback: String? = null
        for (id in ids) {
            val chars = cameraManager.getCameraCharacteristics(id)
            val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            if (!hasFlash) continue
            val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
            if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                return id
            }
            if (fallback == null) fallback = id
        }
        return fallback
    }

    fun flashText(text: String, unitMs: Long = 200L) {
        if (torchCameraId == null) {
            onStatus?.invoke("No flashlight available")
            return
        }
        flashingJob?.cancel()
        flashingJob = scope.launch(Dispatchers.Main.immediate) {
            try {
                onStatus?.invoke("Flashing: $text")
                performSequence(textToSequence(text), unitMs)
                onStatus?.invoke("Done")
            } catch (e: CancellationException) {
                onStatus?.invoke("Stopped")
            } catch (t: Throwable) {
                onStatus?.invoke("Error: ${t.message}")
            } finally {
                setTorch(false)
            }
        }
    }

    fun stop() {
        flashingJob?.cancel()
        flashingJob = null
        scope.launch { setTorch(false) }
    }

    private suspend fun setTorch(on: Boolean) {
        withContext(Dispatchers.Main) {
            try {
                cameraManager.setTorchMode(torchCameraId!!, on)
            } catch (_: Throwable) {
            }
        }
    }

    private suspend fun performSequence(steps: List<Step>, unitMs: Long) {
        var torchOn = false
        for (step in steps) {
            if (step.on != torchOn) {
                torchOn = step.on
                setTorch(torchOn)
            }
            delay(step.units * unitMs)
        }
        if (torchOn) setTorch(false)
    }

    private data class Step(val on: Boolean, val units: Long)

    private fun textToSequence(text: String): List<Step> {
        val sanitized = text.uppercase().filter { it in MORSE_MAP.keys || it == ' ' }
        val steps = mutableListOf<Step>()
        val words = sanitized.split(Regex("\\s+"))
        words.forEachIndexed { wIndex, word ->
            word.forEachIndexed { cIndex, ch ->
                val code = MORSE_MAP[ch] ?: return@forEachIndexed
                code.forEachIndexed { i, symbol ->
                    // signal on (dot=1, dash=3)
                    steps += Step(on = true, units = if (symbol == '.') 1 else 3)
                    // intra-character gap (1) if not last symbol of letter
                    if (i != code.lastIndex) steps += Step(on = false, units = 1)
                }
                // inter-character gap (3) if not last char of word
                if (cIndex != word.lastIndex) steps += Step(on = false, units = 3)
            }
            // inter-word gap (7) if not last word
            if (wIndex != words.lastIndex) steps += Step(on = false, units = 7)
        }
        // collapse consecutive offs by summing units
        return collapseOffs(steps)
    }

    private fun collapseOffs(steps: List<Step>): List<Step> {
        if (steps.isEmpty()) return steps
        val out = ArrayList<Step>(steps.size)
        var prev: Step? = null
        for (s in steps) {
            if (prev == null) { prev = s; continue }
            if (!s.on && prev != null && !prev.on) {
                prev = Step(false, prev.units + s.units)
            } else {
                out.add(prev!!)
                prev = s
            }
        }
        if (prev != null) out.add(prev)
        return out
    }

    companion object {
        private val MORSE_MAP: Map<Char, String> = mapOf(
            'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
            'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
            'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
            'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
            'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--", 'Z' to "--..",
            '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
            '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
        )
    }
}
