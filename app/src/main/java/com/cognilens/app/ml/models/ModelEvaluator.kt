package com.cognilens.app.ml.models

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

class ModelEvaluator(private val context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        // Load ONNX model directly from assets folder
        val modelBytes = context.assets.open("cognilens_gb_model.onnx").readBytes()
        session = env.createSession(modelBytes)
    }

    /**
     * Predicts whether a session is Compulsive (true) or Intentional (false).
     *
     * @param sessionDurationMins Continuous minutes in active foreground.
     * @param reopenIntervalMins Minutes since app was last closed.
     * @param bsmasScore User's baseline BSMAS survey score (6-30).
     * @param isScheduleConflict 1.0 if during user's work/study schedule, else 0.0.
     */
    fun evaluateSession(
        sessionDurationMins: Float,
        reopenIntervalMins: Float,
        bsmasScore: Float,
        isScheduleConflict: Float
    ): Boolean {
        val inputData = floatArrayOf(
            sessionDurationMins,
            reopenIntervalMins,
            bsmasScore,
            isScheduleConflict
        )

        // Shape: 1 row, 4 features
        val inputTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(inputData),
            longArrayOf(1, 4)
        )

        val outputs = session.run(mapOf("float_input" to inputTensor))

        // Retrieve predictions (class 1 = Compulsive, class 0 = Intentional)
        val resultTensor = outputs[0].value as LongArray
        val predictedClass = resultTensor[0]

        return predictedClass == 1L
    }
}