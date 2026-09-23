import pandas as pd


def load_dataset(path: str) -> pd.DataFrame:
    """Load the ML dataset from a CSV file."""
    return pd.read_csv(path)


def separate_features_and_target(
    df: pd.DataFrame,
    target_column: str = "eventual_behaviour_class",
):
    """Separate predictor features from the target label."""
    X = df.drop(columns=[target_column])
    y = df[target_column]

    return X, y