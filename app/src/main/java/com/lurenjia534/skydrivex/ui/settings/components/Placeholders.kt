package com.lurenjia534.skydrivex.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer

@Composable
private fun Modifier.settingsSkeleton(shape: Shape = MaterialTheme.shapes.small): Modifier =
    this.placeholder(
        visible = true,
        highlight = PlaceholderHighlight.shimmer(),
        shape = shape
    )

@Composable
fun SettingsSkeletonListItem(
    modifier: Modifier = Modifier,
    headlineRatio: Float,
    supportingRatios: List<Float>,
    showTrailing: Boolean = false,
    trailingWidth: Dp = 72.dp,
    trailingHeight: Dp = 28.dp
) {
    val safeHeadlineRatio = headlineRatio.coerceIn(0.15f, 1f)
    val safeSupporting = supportingRatios.map { it.coerceIn(0.2f, 1f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(safeHeadlineRatio)
                        .fillMaxHeight()
                        .settingsSkeleton()
                )
            }
            if (showTrailing) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(trailingWidth)
                        .height(trailingHeight)
                        .settingsSkeleton(MaterialTheme.shapes.extraSmall)
                )
            }
        }

        safeSupporting.forEachIndexed { index, ratio ->
            Spacer(Modifier.height(if (index == 0) 8.dp else 6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(14.dp)
                    .settingsSkeleton()
            )
        }
    }
}

@Composable
fun SettingsSkeletonStorage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(20.dp)
                .settingsSkeleton()
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(14.dp)
                .settingsSkeleton()
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .settingsSkeleton(MaterialTheme.shapes.extraSmall)
        )
    }
}
