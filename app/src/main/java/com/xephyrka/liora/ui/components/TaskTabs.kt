package com.xephyrka.liora.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import com.xephyrka.liora.ui.components.ShowcaseState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.xephyrka.liora.data.model.TaskList

/**
 * A scrollable horizontal tab row used to switch between different task lists.
 * Includes a fixed "All Tasks" tab at the start and a "+" tab at the end to create new lists.
 * Custom lists support long-press to edit or delete.
 */
@Composable
fun TaskTabs(
    /** The index of the currently active tab. */
    selectedTab: Int,
    /** Callback triggered when the user selects a tab. */
    onTabSelected: (Int) -> Unit,
    /** The list of user-created task categories. */
    taskLists: List<TaskList>,
    /** Callback triggered when the user chooses to edit a list's name. */
    onEditList: (TaskList) -> Unit,
    /** Callback triggered when the user chooses to delete a list. */
    onDeleteList: (TaskList) -> Unit,
    showcaseState: ShowcaseState? = null
) {
    /** The total number of tabs: All Tasks + custom lists + Add button. */
    val totalTabsCount = taskLists.size + 2
    /** Ensures the selected index stays within valid bounds. */
    val selectedTabIndex = selectedTab.coerceIn(0, totalTabsCount - 1)

    Column(modifier = Modifier.fillMaxWidth()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    showcaseState?.updateTargetCoordinates("tabs_target", coords)
                },
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    )
                }
            }
        ) {
            GhostlessTab(
                selected = selectedTabIndex == 0,
                onClick = { onTabSelected(0) },
                text = { Text("All Tasks") }
            )

            taskLists.forEachIndexed { index, list ->
                /** Calculation of the tab index for the current list. */
                val tabIndex = index + 1
                /** Controls the visibility of the edit/delete context menu for this list. */
                var showMenu by remember { mutableStateOf(value = false) }

                GhostlessTab(
                    selected = selectedTabIndex == tabIndex,
                    onClick = { onTabSelected(tabIndex) },
                    onLongClick = { showMenu = true },
                    text = {
                        Box {
                            Text(text = list.name, style = MaterialTheme.typography.labelLarge)
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Name") },
                                    onClick = { showMenu = false; onEditList(list) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete List", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; onDeleteList(list) }
                                )
                            }
                        }
                    }
                )
            }

            GhostlessTab(
                selected = selectedTabIndex == (taskLists.size + 1),
                onClick = { onTabSelected(taskLists.size + 1) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add List") }
            )
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }
}

/**
 * A customized Tab component that supports long-click interactions and provides visual feedback via a ripple effect.
 * It does not include the default Material Tab's "ghosting" or complex internal padding.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GhostlessTab(
    /** Whether this tab is the currently active one. */
    selected: Boolean,
    /** Callback triggered on a standard click. */
    onClick: () -> Unit,
    /** Optional callback triggered on a long-press. */
    onLongClick: (() -> Unit)? = null,
    /** The Composable content for the tab's label. */
    text: @Composable (() -> Unit)? = null,
    /** The Composable content for the tab's icon. */
    icon: @Composable (() -> Unit)? = null
) {
    /** State for managing user interactions (presses, ripples). */
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .height(48.dp)
            .background(Color.Transparent)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics {
                this.selected = selected
                this.role = Role.Tab
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        /** The color of the tab content based on its selection state. */
        val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        CompositionLocalProvider(LocalContentColor provides color) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                icon?.invoke()
                text?.let {
                    ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                        it()
                    }
                }
            }
        }
    }
}
