package com.lurenjia534.skydrivex.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer

@Composable
fun DriveInfoPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(18.dp)
                        .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(32.dp)
                    .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(18.dp)
                        .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.25f)
                        .height(14.dp)
                        .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(18.dp)
                        .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.medium)
                )
            }
        }

        repeat(4) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(18.dp)
                            .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(14.dp)
                            .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
                    )
                }
            }
        }
    }
}

@Composable
fun UserInfoPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(6) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(18.dp)
                            .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(14.dp)
                            .placeholder(visible = true, highlight = PlaceholderHighlight.shimmer(), shape = MaterialTheme.shapes.small)
                    )
                }
            }
        }
    }
}

