package com.shuo.travel.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.shuo.travel.R
import com.shuo.travel.domain.Category
import com.shuo.travel.ui.theme.*
import androidx.core.graphics.toColorInt

@Composable fun Label(text: String, size: Int = 14, color: Color = MaterialTheme.colorScheme.onSurface,
    weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier, align: TextAlign? = null) {
    Text(text, modifier, color = color, fontSize = size.sp, lineHeight = (size * 1.35f).sp,
        fontFamily = Inter, fontWeight = weight, textAlign = align, letterSpacing = 0.sp)
}
@Composable fun categoryColor(category: Category): Color = Color((if (LocalDark.current) category.darkColor else category.color).toColorInt())

fun iconResource(name: String): Int = when (name) {
    "train-front" -> R.drawable.ic_train_front; "utensils" -> R.drawable.ic_utensils
    "bed-double" -> R.drawable.ic_bed_double; "shopping-bag" -> R.drawable.ic_shopping_bag
    "signpost" -> R.drawable.ic_signpost; "cross" -> R.drawable.ic_cross
    "messages-square" -> R.drawable.ic_messages_square; "wallet" -> R.drawable.ic_wallet
    "landmark" -> R.drawable.ic_landmark; "house" -> R.drawable.ic_house
    "star" -> R.drawable.ic_star; "layout-grid" -> R.drawable.ic_layout_grid
    "plus" -> R.drawable.ic_plus; "minus" -> R.drawable.ic_minus; "search" -> R.drawable.ic_search
    "x" -> R.drawable.ic_x; "arrow-left" -> R.drawable.ic_arrow_left
    "sliders-horizontal" -> R.drawable.ic_sliders_horizontal; "chevron-right" -> R.drawable.ic_chevron_right
    "pencil" -> R.drawable.ic_pencil; "check" -> R.drawable.ic_check
    "circle-check" -> R.drawable.ic_circle_check; "info" -> R.drawable.ic_info
    "shield-check" -> R.drawable.ic_shield_check; "sun" -> R.drawable.ic_sun
    "moon" -> R.drawable.ic_moon; "smartphone" -> R.drawable.ic_smartphone
    "wifi-off" -> R.drawable.ic_wifi_off; "rotate-ccw" -> R.drawable.ic_rotate_ccw
    "eye" -> R.drawable.ic_eye; "eye-off" -> R.drawable.ic_eye_off
    "upload" -> R.drawable.ic_upload; "download" -> R.drawable.ic_download
    "trash-2" -> R.drawable.ic_trash_2; "arrow-up" -> R.drawable.ic_arrow_up
    "arrow-down" -> R.drawable.ic_arrow_down; else -> R.drawable.ic_ellipsis
}
@Composable fun Symbol(name: String, description: String? = null, size: Int = 24,
    tint: Color = MaterialTheme.colorScheme.onSurface, modifier: Modifier = Modifier) {
    val large = when (name) {
        "train-front" -> R.drawable.ic_train_front_large; "utensils" -> R.drawable.ic_utensils_large
        "bed-double" -> R.drawable.ic_bed_double_large; "shopping-bag" -> R.drawable.ic_shopping_bag_large
        "signpost" -> R.drawable.ic_signpost_large; "cross" -> R.drawable.ic_cross_large
        "messages-square" -> R.drawable.ic_messages_square_large; "wallet" -> R.drawable.ic_wallet_large
        "landmark" -> R.drawable.ic_landmark_large; else -> R.drawable.ic_ellipsis_large
    }
    Icon(painterResource(if (size >= 48) large else iconResource(name)), description, modifier.size(size.dp), tint)
}
@Composable fun Action(icon: String, description: String, filled: Boolean = false, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(44.dp).clip(CircleShape).background(if (filled) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)) {
        Symbol(icon, description, tint = tint)
    }
}
@Composable fun Header(title: String, subtitle: String? = null, back: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (back != null) Action("arrow-left", "Retour", onClick = back)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Label(title, 27, weight = FontWeight.Bold)
            if (subtitle != null) Label(subtitle, 12, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}
@Composable fun Section(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Label(title, 17, modifier = Modifier.weight(1f))
        if (action != null) Label(action, 12, MaterialTheme.colorScheme.primary, FontWeight.SemiBold,
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onAction).padding(vertical = 4.dp))
    }
}
@Composable fun CategoryGrid(categories: List<Category>, selection: String? = null, form: Boolean = false, onClick: (Category) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(if (form) 8.dp else 10.dp)) {
        categories.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { category ->
                    val color = categoryColor(category)
                    val selected = category.id == selection
                    Column(Modifier.weight(1f).heightIn(min = if (form) 70.dp else 76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (form) Modifier.background(if (selected) color.copy(alpha = .11f) else MaterialTheme.colorScheme.surface)
                            .border(if (selected) 2.dp else 1.dp, if (selected) color else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)) else Modifier)
                        .clickable(role = Role.Button) { onClick(category) }.padding(vertical = if (form) 8.dp else 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(48.dp, if (form) 24.dp else 44.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (form) Color.Transparent else color.copy(alpha = .11f)), contentAlignment = Alignment.Center) {
                            Symbol(if (selected && form) "circle-check" else category.icon, size = 23, tint = color)
                        }
                        Label(category.name, if (form) 9 else 10, align = TextAlign.Center)
                    }
                }
                repeat(5-row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
@Composable fun EmptyState(text: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Label(text, 13, MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
    }
}
@Composable fun OfflineFooter(count: Int? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        if (count != null) Label("$count cartes prêtes à montrer", 11, MaterialTheme.colorScheme.onSurfaceVariant)
        else Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Symbol("wifi-off", size = 12, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Label("100 % hors ligne", 11, MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
