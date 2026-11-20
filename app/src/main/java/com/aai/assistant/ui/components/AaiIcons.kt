package com.aai.assistant.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.aai.assistant.R

object AaiIcons {
    val Menu = R.drawable.ic_menu
    val Settings = R.drawable.ic_settings
    val Pip = R.drawable.ic_pip
    val Send = R.drawable.ic_send
    val Stop = R.drawable.ic_stop
    val Attach = R.drawable.ic_attach
    val Close = R.drawable.ic_close
    val Back = R.drawable.ic_back
    val Add = R.drawable.ic_add
    val Edit = R.drawable.ic_edit
    val Delete = R.drawable.ic_delete
    val Check = R.drawable.ic_check
    val ExpandMore = R.drawable.ic_expand_more
    val ExpandLess = R.drawable.ic_expand_less
    val Copy = R.drawable.ic_copy
    val Key = R.drawable.ic_key
    val Visibility = R.drawable.ic_visibility
    val VisibilityOff = R.drawable.ic_visibility_off
    val Refresh = R.drawable.ic_refresh
    val Bolt = R.drawable.ic_bolt
    val Save = R.drawable.ic_save
    val Accessibility = R.drawable.ic_accessibility
    val Layers = R.drawable.ic_layers
    val Folder = R.drawable.ic_folder
    val UnfoldMore = R.drawable.ic_unfold_more
    val UnfoldLess = R.drawable.ic_unfold_less
    val Robot = R.drawable.ic_robot
    val Terminal = R.drawable.ic_terminal
    val Globe = R.drawable.ic_globe
    val History = R.drawable.ic_history
    val Play = R.drawable.ic_play
}

@Composable
fun aaiIcon(id: Int): Painter = painterResource(id)