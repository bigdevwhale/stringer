package app.stringer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.stringer.R
import app.stringer.ui.theme.Sans
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType
import kotlinx.coroutines.delay

@Composable
fun SCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(S.RMd)
            .background(S.c.surface)
            .border(1.dp, S.c.line, S.RMd)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = SType.Eyebrow, modifier = modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp))
}

/** The red pencil, for the thing to do next. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = S.RLg,
        colors = ButtonDefaults.buttonColors(containerColor = S.c.red, contentColor = S.c.onRed, disabledContainerColor = S.c.line, disabledContentColor = S.c.faint),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = SType.Button.copy(fontSize = 17.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Quiet, for the second thing. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Row(
        modifier
            .height(50.dp)
            .clip(S.RMd)
            .background(if (enabled) S.c.redSoft else S.c.surface)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val tint = if (enabled) S.c.red else S.c.faint
        if (icon != null) {
            Icon(icon, null, Modifier.size(19.dp), tint = tint)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = SType.Button, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Plain text that can be tapped, for a way out. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = S.c.muted) {
    Text(text, style = SType.Button.copy(fontSize = 15.sp), color = color, modifier = modifier.clip(S.RSm).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp))
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp) },
        shape = S.RSm,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = S.c.surface,
            labelColor = S.c.muted,
            selectedContainerColor = S.c.redSoft,
            selectedLabelColor = S.c.red,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = S.c.line, selectedBorderColor = S.c.red.copy(alpha = 0.55f)),
    )
}

@Composable
fun <T> Segmented(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(S.RSm).background(S.c.surface2).border(1.dp, S.c.line, S.RSm).padding(4.dp)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(S.RSm)
                    .background(if (on) S.c.redSoft else Color.Transparent)
                    .clickable { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) { Text(label(o), style = SType.Button.copy(fontSize = 15.sp), color = if (on) S.c.red else S.c.muted, maxLines = 1) }
        }
    }
}

@Composable
fun IconDisc(icon: ImageVector, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(S.c.redSoft), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.5f), tint = S.c.red)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = S.c.ink) }
        } else Spacer(Modifier.width(14.dp))
        Text(title, style = SType.Title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDisc(icon, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = SType.Muted)
            Spacer(Modifier.height(2.dp))
            Text(value, style = SType.Strong)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = S.c.faint)
    }
}

@Composable
fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = SType.Strong)
            if (sub.isNotEmpty()) Text(sub, style = SType.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = S.c.red, checkedThumbColor = S.c.onRed, uncheckedTrackColor = S.c.surface2, uncheckedBorderColor = S.c.line, uncheckedThumbColor = S.c.faint))
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true, style: TextStyle = SType.Body, minLines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        textStyle = style,
        modifier = modifier.fillMaxWidth(),
        shape = S.RMd,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = S.c.red,
            unfocusedBorderColor = S.c.line,
            focusedLabelColor = S.c.red,
            unfocusedLabelColor = S.c.faint,
            cursorColor = S.c.red,
            focusedContainerColor = S.c.surface,
            unfocusedContainerColor = S.c.surface,
        ),
    )
}

/** Wall clock that ticks every [stepMs] while the screen is visible. */
@Composable
fun rememberNow(stepMs: Long = 1000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var visible by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { visible = true; now = System.currentTimeMillis() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { visible = false }
    LaunchedEffect(visible, stepMs) {
        while (visible) {
            now = System.currentTimeMillis()
            delay(stepMs - now % stepMs)
        }
    }
    return now
}
