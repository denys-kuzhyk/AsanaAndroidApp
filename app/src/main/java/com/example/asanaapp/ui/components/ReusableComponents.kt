package com.example.asanaapp.ui.components

import android.util.Patterns
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asanaapp.ui.theme.ButtonColor
import com.example.asanaapp.ui.theme.InterFontFamily
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * Basic email validation using Android's [Patterns.EMAIL_ADDRESS]
 */
fun isEmailValid(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

/**
 * Strict date formatter for validating due dates in format `YYYY-MM-DD`
 */
private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("uuuu-MM-dd")
        .withResolverStyle(ResolverStyle.STRICT)

/**
 * Validates date string using [DATE_FORMATTER].
 *
 * @return true if date can be parsed and is a valid calendar date, false otherwise
 */
fun isDateValid(date: String): Boolean {
    return try {
        LocalDate.parse(date, DATE_FORMATTER)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Reusable button used throughout the app
 *
 * Features:
 *  - configurable paddings
 *  - optional leading icon
 *  - "reversed" color mode for destructive / secondary actions
 *  - uses [OutlinedButton] with app theme colors
 */
@Composable
fun AsanaButton(
    modifier: Modifier,
    text: String,
    paddingStart: Int = 0,
    paddingEnd: Int = 0,
    paddingTop: Int = 0,
    paddingBottom: Int = 0,
    fontSize: Int,
    isIcon: Boolean = false,
    icon: ImageVector? = null,
    colorReversed: Boolean = false,
    onClickAction: () -> Unit,
    enabled: Boolean = true
) {

    OutlinedButton(
        onClick = onClickAction,
        modifier = modifier
            .padding(
                start = paddingStart.dp,
                end = paddingEnd.dp,
                top = paddingTop.dp,
                bottom = paddingBottom.dp
            )
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonColors(
            containerColor = if (colorReversed) Color.White else ButtonColor,
            contentColor = if (colorReversed) ButtonColor else Color.White,
            disabledContainerColor = Color.White,
            disabledContentColor = Color.White
        ),
        enabled = enabled
    ) {
        if (isIcon) {
            Row {
                // Optional leading icon
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = text)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = text,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize.sp
                )
            }
        } else {
            Text(
                text = text,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize.sp
            )
        }
    }
}

/**
 * Reusable text field for the app
 *
 * Features:
 *  - single or "expanded" (taller) mode
 *  - optional password mode (masked input)
 *  - configurable paddings and label text
 */
@Composable
fun AsanaTextField(
    modifier: Modifier,
    text: String,
    fontSize: Int,
    paddingStart: Int = 0,
    paddingEnd: Int = 0,
    paddingTop: Int = 0,
    paddingBottom: Int = 0,
    valueText: String,
    onValueChange: (String) -> Unit,
    password: Boolean = false,
    expanded: Boolean = false
) {

    // Different heights for single-line vs expanded (multiline) field
    val height = if (expanded) 100 else 60

    OutlinedTextField(
        value = valueText,
        onValueChange = onValueChange,
        label = {
            Text(
                text = text,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = fontSize.sp
            )
        },
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        modifier = modifier
            .padding(
                start = paddingStart.dp,
                end = paddingEnd.dp,
                top = paddingTop.dp,
                bottom = paddingBottom.dp
            )
            .height(height.dp)
    )
}

/**
 * Generic message dialog that auto-dismisses after 10 seconds
 * but can also be dismissed manually with an OK button
 *
 * @param message text to show
 * @param onDismiss callback when dialog is dismissed (auto or manual)
 */
@Composable
fun MessageDialog(
    message: String,
    onDismiss: () -> Unit
) {
    var show by remember { mutableStateOf(true) }

    // Auto-dismiss after 10 seconds if still visible
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(10_000)
        if (show) {
            show = false
            onDismiss()
        }
    }

    if (show) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                show = false
                onDismiss()
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    show = false
                    onDismiss()
                }) {
                    Text("OK")
                }
            },
            text = { Text(message) }
        )
    }
}

/**
 * Card-like composable representing a single task with expandable details
 *
 * Collapsed:
 *  - shows only task name + expand icon
 *
 * Expanded:
 *  - shows description, status, due date, and optionally assignee (for Manager)
 *  - provides "Edit Task" and (for Manager) "Delete Task" actions
 */
@Composable
fun TaskItem(
    name: String,
    description: String,
    status: String,
    dueDate: String,
    assignee: String,
    onClickEdit: () -> Unit,
    onClickDelete: () -> Unit,
    role: String?
) {
    val expandIcon = Icons.Default.KeyboardArrowDown
    val collapseIcon = Icons.Default.KeyboardArrowUp
    var expand by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(horizontal = 50.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ButtonColor)
            .animateContentSize() // Smooth expand/collapse animation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row: task name + expand/collapse icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                IconButton(onClick = { expand = !expand }) {
                    Icon(
                        imageVector = if (expand) collapseIcon else expandIcon,
                        contentDescription = if (expand) "Collapse" else "Expand",
                        tint = Color.White
                    )
                }
            }

            // Expanded details
            if (expand) {
                Spacer(Modifier.height(16.dp))

                KeyValueRow(label = "Description:", value = description)
                KeyValueRow(label = "Status:",      value = status)
                KeyValueRow(label = "Due Date:",    value = dueDate)
                if (role == "Manager") {
                    KeyValueRow(label = "Assignee:", value = assignee)
                }

                Spacer(Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth()) {

                    // Edit button (visible for all roles)
                    OutlinedButton(
                        onClick = onClickEdit,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(all = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonColors(
                            containerColor = Color.White,
                            contentColor = ButtonColor,
                            disabledContainerColor = Color.White,
                            disabledContentColor = Color.White
                        )
                    ) {
                        Text(
                            "Edit Task",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Delete button only for Manager
                    if (role == "Manager") {

                        OutlinedButton(
                            onClick = onClickDelete,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(all = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White,
                                disabledContainerColor = Color.White,
                                disabledContentColor = Color.White
                            )
                        ) {
                            Text(
                                "Delete Task",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small helper row used by [TaskItem] to show a "Label: Value" layout.
 */
@Composable
private fun KeyValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            modifier = Modifier.weight(2f),
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            color = Color.White
        )
    }
}

/**
 * Generic dropdown component for selecting a single item from a list of strings
 *
 * Uses Material3 [ExposedDropdownMenuBox] pattern:
 *  - read-only text field as anchor
 *  - list of [options] displayed in dropdown
 *  - selected value is shown in the text field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDropdown(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    text: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.padding(start = 50.dp, end = 50.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                .fillMaxWidth(),
            readOnly = true,
            value = selected,
            onValueChange = {},
            label = { Text(text = text, fontSize = 18.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}