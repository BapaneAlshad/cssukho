package com.sukhoramen.cs.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukhoramen.cs.R
import com.sukhoramen.cs.data.Platform
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SoftCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier, shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp,
    ) { Column(content = content) }
}

@Composable
fun BrandHeader(onRefresh: (() -> Unit)? = null, onSettings: (() -> Unit)? = null, isRefreshing: Boolean = false, showBadge: Boolean = true, isDemo: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.sukho_logo), "Logo SUKHO RAMEN", Modifier.size(42.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("SUKHO RAMEN", fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .45.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("CUSTOMER CARE", fontSize = 7.5.sp, lineHeight = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = .9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (showBadge) {
                if (isDemo) DemoBadge() else LiveBadge()
            }
            if (onRefresh != null || onSettings != null) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    onRefresh?.let { refresh ->
                        HeaderActionButton(
                            onClick = refresh, enabled = !isRefreshing, tag = "refresh",
                            description = "Perbarui inbox",
                        ) {
                            if (isRefreshing) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                            else Icon(SukhoIcons.Refresh, null, Modifier.size(19.dp))
                        }
                    }
                    onSettings?.let { settings ->
                        HeaderActionButton(onClick = settings, enabled = true, tag = "nav_settings", description = "Pengaturan") {
                            Icon(SukhoIcons.Gear, null, Modifier.size(19.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveBadge() {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
        Text("MODE CLOUD", Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold,
            letterSpacing = .6.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun HeaderActionButton(onClick: () -> Unit, enabled: Boolean, tag: String, description: String, icon: @Composable () -> Unit) {
    Surface(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.size(38.dp).testTag(tag),
        shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.semantics { contentDescription = description }) { icon() }
        }
    }
}

@Composable
fun DemoBadge() {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text("MODE DEMO", fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun Avatar(initials: String, modifier: Modifier = Modifier, alternate: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.size(48.dp).clip(RoundedCornerShape(17.dp)).background(if (alternate) colors.secondaryContainer else colors.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (alternate) colors.onSecondaryContainer else colors.onPrimaryContainer)
    }
}

@Composable
fun PlatformIcon(platform: Platform, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Icon(if (platform == Platform.INSTAGRAM) SukhoIcons.Instagram else SukhoIcons.TikTok, platform.label, modifier.size(15.dp), tint = tint)
}

@Composable
fun IconDisc(icon: ImageVector, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Box(modifier.size(42.dp).background(color.copy(alpha = .09f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(21.dp), tint = color)
    }
}

@Composable
fun NoticeCard(text: String, modifier: Modifier = Modifier, error: Boolean = false, action: String? = null, onAction: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp),
        color = if (error) colors.errorContainer else colors.primaryContainer,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (error) SukhoIcons.Offline else SukhoIcons.Info, null, Modifier.size(19.dp), tint = if (error) colors.onErrorContainer else colors.onPrimaryContainer)
            Spacer(Modifier.width(10.dp))
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = if (error) colors.onErrorContainer else colors.onPrimaryContainer)
            if (action != null) TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 9.dp)) { Text(action) }
        }
    }
}

@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) colors.primary else colors.surface,
        contentColor = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) { Icon(icon, null, Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)) }
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

fun timeLabel(timestamp: Long): String {
    val duration = ((System.currentTimeMillis() - timestamp) / 60_000).coerceAtLeast(0)
    return when {
        duration < 1 -> "Baru saja"
        duration < 60 -> "$duration mnt"
        duration < 1_440 -> "${duration / 60} jam"
        duration < 2_880 -> "Kemarin"
        else -> "${duration / 1_440} hari"
    }
}

fun messageTime(timestamp: Long): String = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID"))
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))

fun conversationDate(timestamp: Long): String = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("id-ID"))
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))

