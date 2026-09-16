# CogniLens

An adaptive machine learning and RAG-based digital wellbeing system for classifying social media usage behaviour and delivering personalized interventions among university students.

## Research Objective

The project investigates whether behavioural patterns derived from social media usage, individual behavioural baselines, academic schedules, and personal wake/sleep context can be used to classify potentially compulsive social media use and support timely, personalized digital wellbeing interventions.

## System Overview

CogniLens is designed to:

- Monitor social media usage through Android system APIs
- Detect social media usage sessions
- Extract behavioural and contextual features
- Establish individual behavioural baselines
- Classify emerging usage behaviour using machine learning
- Provide model confidence and explainability
- Deliver personalized interventions based on behavioural classification and context
- Provide educational assistance through Retrieval-Augmented Generation (RAG)
- Collect intervention feedback for system evaluation
- Assess behavioural patterns against the Bergen Social Media Addiction Scale (BSMAS) as a participant-level benchmark

The system is intended as a digital wellbeing research prototype and does not diagnose addiction or mental health conditions.

## Research Methodology

The project follows an experimental research approach for evaluating behavioural classification and intervention effectiveness.

Machine learning models will be experimentally compared using the same dataset and evaluation protocol. Candidate models include:

- Logistic Regression
- Random Forest
- XGBoost

The final model will be selected based on empirical evaluation.

Primary evaluation metrics:

- Accuracy
- Precision
- Recall
- F1-Score

F1-Score will serve as the primary metric because it balances precision and recall and is appropriate when behavioural classes may be imbalanced.

## Repository Structure

`	ext
cognilens/
+-- android/        # Android application
+-- ml/             # Machine learning pipeline
+-- rag/            # Retrieval-Augmented Generation components
+-- docs/           # Architecture and research documentation
+-- .gitignore
+-- README.md
+-- LICENSE
