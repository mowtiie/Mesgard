package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import java.time.LocalDate

private val HeaderVerticalPadding = 12.dp
private val HeaderChipPadding = 12.dp
private val HeaderChipVerticalPadding = 4.dp

@Composable
internal fun DateHeader(date: LocalDate, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val label = when (relativeDay(date, today)) {
        RelativeDay.TODAY -> stringResource(R.string.date_today)
        RelativeDay.YESTERDAY -> stringResource(R.string.date_yesterday)
        RelativeDay.OTHER -> formatDateHeader(date, today)
    }
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = HeaderVerticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = HeaderChipPadding, vertical = HeaderChipVerticalPadding),
            )
        }
    }
}
