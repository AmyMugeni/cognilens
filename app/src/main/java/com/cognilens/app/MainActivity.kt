package com.cognilens.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cognilens.app.ui.onboarding.OnboardingViewModel
import com.cognilens.app.ui.onboarding.PermissionsScreen
import com.cognilens.app.ui.onboarding.ProfileGatewayScreen
import com.cognilens.app.ui.onboarding.ProfileSetupHubScreen
import com.cognilens.app.ui.onboarding.SplashScreen
import com.cognilens.app.ui.theme.CognilensTheme

class MainActivity : ComponentActivity() {

    private val onboardingViewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CognilensTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = OnboardingRoutes.SPLASH
                    ) {
                        composable(OnboardingRoutes.SPLASH) {
                            SplashScreen(
                                onNavigateNext = {
                                    navController.navigate(OnboardingRoutes.PERMISSIONS)
                                }
                            )
                        }

                        composable(OnboardingRoutes.PERMISSIONS) {
                            PermissionsScreen(
                                onNavigateNext = {
                                    navController.navigate(OnboardingRoutes.GATEWAY)
                                }
                            )
                        }

                        composable(OnboardingRoutes.GATEWAY) {
                            ProfileGatewayScreen(
                                onNavigateNext = {
                                    navController.navigate(OnboardingRoutes.SETUP_HUB)
                                }
                            )
                        }

                        composable(OnboardingRoutes.SETUP_HUB) {
                            ProfileSetupHubScreen(
                                viewModel = onboardingViewModel,
                                onComplete = {
                                    // Complete onboarding flow and exit activity or launch main dashboard
                                    finish()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private object OnboardingRoutes {
    const val SPLASH = "splash"
    const val PERMISSIONS = "permissions"
    const val GATEWAY = "gateway"
    const val SETUP_HUB = "setup_hub"
}