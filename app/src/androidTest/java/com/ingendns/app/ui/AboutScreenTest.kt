package com.ingendns.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ingendns.app.ui.theme.InGenDNSTheme
import org.junit.Rule
import org.junit.Test

class AboutScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun showsProductAndDeveloperInformation() {
        compose.setContent { InGenDNSTheme { AboutScreen() } }
        compose.onNodeWithText("iNGenDNS").assertIsDisplayed()
        compose.onNodeWithText("Dinesh K").assertIsDisplayed()
    }
}
