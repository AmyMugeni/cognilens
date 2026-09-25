package com.cognilens.app.ml.models

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

class ModelEvaluator(private val context: Context) {

    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null

    init {
        try {
            env = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open("cognilens_gb_model.onnx").use { it.readBytes() }
            session = env?.createSession(modelBytes)
            println(" ModelEvaluator: ONNX model loaded successfully.")
        } catch (t: Throwable) {
            // Catches OrtException, UnsupportedModelVersion, and Native library load errors
            println("⚠ ModelEvaluator: Failed to load ONNX model (${t.message}). Falling back to RuleEngine.")
            env = null
            session = null
        }
    }

    fun evaluateSession(
        sessionDurationMins: Float,
        reopenIntervalMins: Float,
        bsmasScore: Float,
        isScheduleConflict: Float
    ): Boolean {
        val ortSession = session ?: return false
        val ortEnv = env ?: return false

        return try {
            val inputData = floatArrayOf(
                sessionDurationMins,
                reopenIntervalMins,
                bsmasScore,
                isScheduleConflict
            )

            val shape = longArrayOf(1, 4)
            val tensor = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(inputData), shape)

            tensor.use {
                val results = ortSession.run(mapOf("float_input" to tensor))
                val outputTensor = results[0].value as Array<LongArray>
                outputTensor[0][0] == 1L
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}