import pandas as pd


def add_baseline_features(df: pd.DataFrame) -> pd.DataFrame:
    """Create baseline-relative behavioural features."""

    df = df.copy()

    if "baseline_duration_ratio" not in df.columns:
        df["baseline_duration_ratio"] = (
            df["elapsed_duration"] / df["baseline_session_duration"]
        )

    if "baseline_frequency_ratio" not in df.columns:
        df["baseline_frequency_ratio"] = (
            df["sessions_today"] / df["baseline_session_frequency"]
        )

    return df