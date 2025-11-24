package com.example.asanaapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.asanaapp.data.storage.UserManager
import com.example.asanaapp.ui.components.AsanaButton
import com.example.asanaapp.ui.theme.InterFontFamily

/**
 * Account screen
 *
 * Responsibilities:
 *  - reads user data (id, name, email, role) via [UserManager]
 *  - displays basic account information
 *  - provides navigation back to Home and to Change Password screen
 */
@Composable
fun Account(
    modifier: Modifier,
    navController: NavController,
    userManager: UserManager,
) {

    // Collect user fields as State (null until loaded)
    val name = userManager.nameFlow.collectAsState(initial = null).value
    val email = userManager.emailFlow.collectAsState(initial = null).value
    val id = userManager.idFlow.collectAsState(initial = null).value
    val role = userManager.roleFlow.collectAsState(initial = null).value

    Box(
        modifier = modifier
    ) {

        // Back button to navigate to the home screen
        AsanaButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(36.dp),
            text = "←",
            paddingStart = 16,
            paddingEnd = 16,
            paddingBottom = 16,
            paddingTop = 32,
            fontSize = 18,
            onClickAction = { navController.navigate("homeScreen") }
        )

        // Main content column centered on screen
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Title
            Text(
                "Account",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(46.dp))

            // User ID (Asana task gid from Users List project)
            Text(
                text = "ID",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 50.dp)
            )

            if (id != null) {
                Text(
                    text = id,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 50.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User's name
            Text(
                text = "Name",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 50.dp)
            )

            if (name != null) {
                Text(
                    text = name,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 50.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User's email
            Text(
                text = "Email",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 50.dp)
            )

            if (email != null) {
                Text(
                    text = email,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 50.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Role
            Text(
                text = "Role",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 50.dp)
            )

            if (role != null) {
                Text(
                    text = role,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 50.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Button to navigate to Change Password screen
            AsanaButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Change Password",
                paddingStart = 50,
                paddingEnd = 50,
                fontSize = 24,
                onClickAction = { navController.navigate("changePassword") }
            )
        }
    }
}