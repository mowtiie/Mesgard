package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The height of a filled text field with one line, which the buttons beside it match. */
internal val MessageFieldHeight = 56.dp
private val MessageFieldCorner = 28.dp

/** A filled text field with fully rounded corners and no active indicator line, for writing a message. */
@Composable
internal fun MessageTextField(
    value: String,
    onValueChange: (String) -> Unit,
    maxLines: Int,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = placeholder?.let { { Text(it) } },
        trailingIcon = trailingIcon,
        maxLines = maxLines,
        shape = RoundedCornerShape(MessageFieldCorner),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        ),
    )
}
