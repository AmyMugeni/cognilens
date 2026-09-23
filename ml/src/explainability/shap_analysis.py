import shap


def create_shap_explainer(model):
    """Create a SHAP explainer for the trained classification model."""
    return shap.Explainer(model)


def calculate_shap_values(explainer, X):
    """Calculate SHAP values for a set of observations."""
    return explainer(X)