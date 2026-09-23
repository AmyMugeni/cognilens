package com.cognilens.app.domain.rules

sealed class RuleEvaluationResult {
    /** Hard override triggered. ML model evaluation MUST be bypassed. */
    data class TriggerIntervention(val reason: String) : RuleEvaluationResult()

    /** No hard rules broken. Defer classification to the Gradient Boosting ONNX model. */
    object PassToML : RuleEvaluationResult()
}