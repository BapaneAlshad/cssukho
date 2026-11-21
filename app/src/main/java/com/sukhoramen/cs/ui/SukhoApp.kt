package com.sukhoramen.cs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.sukhoramen.cs.MainViewModel
import com.sukhoramen.cs.UiEvent

@Composable
fun SukhoApp(model: MainViewModel, onExit: () -> Unit = {}) {
    val state by model.state.collectAsStateWithLifecycle()
    SukhoTheme(state.theme) {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route ?: "dm"
        val snackbar = remember { SnackbarHostState() }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, model) {
            val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) model.flushDrafts() }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(model) {
            model.events.collect { event ->
                when (event) {
                    is UiEvent.Notice -> snackbar.showSnackbar(event.text, duration = SnackbarDuration.Short)
                    is UiEvent.Incoming -> {
                        val result = snackbar.showSnackbar(
                            if (event.added) "Interaksi demo baru dari Maya Lestari" else "Interaksi demo ini sudah ada di inbox",
                            actionLabel = "Buka", duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) nav.navigate("detail/${event.id}") { launchSingleTop = true }
                    }
                }
            }
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            snackbarHost = { SnackbarHost(snackbar, Modifier.padding(horizontal = 12.dp)) },
            bottomBar = {
                if (state.ready && !route.startsWith("detail")) BottomNavigation(route) { destination ->
                    selectPrimaryTab(nav, destination)
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (!state.ready) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("Menyiapkan inbox…", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    NavHost(nav, startDestination = "dm") {
                        composable("dm") {
                            InboxScreen(
                                state, type = com.sukhoramen.cs.data.InteractionType.DM,
                                onRefresh = model::refresh,
                                onSettings = { nav.navigate("settings") { launchSingleTop = true } },
                                onOpen = { model.open(it); nav.navigate("detail/$it") },
                            )
                        }
                        composable("comment") {
                            InboxScreen(
                                state, type = com.sukhoramen.cs.data.InteractionType.COMMENT,
                                onRefresh = model::refresh,
                                onSettings = { nav.navigate("settings") { launchSingleTop = true } },
                                onOpen = { model.open(it); nav.navigate("detail/$it") },
                            )
                        }
                        composable("settings") { SettingsScreen(state, model, onExit) }
                        composable("detail/{id}") { backStack ->
                            DetailScreen(
                                id = backStack.arguments?.getString("id").orEmpty(), state = state,
                                onBack = { model.flushDrafts(); nav.popBackStack() }, onDraft = model::draft,
                                onSend = model::send, onRetry = model::retry, onComplete = model::complete,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun selectPrimaryTab(nav: NavHostController, destination: String) {
    if (nav.currentDestination?.route == "settings") nav.popBackStack()

    // Settings is a temporary destination. Remove it before applying tab state
    // restoration so it cannot be saved as part of the DM/Comment back stack.
    if (nav.currentDestination?.route == destination) return

    nav.navigate(destination) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomNavigation(route: String, onSelect: (String) -> Unit) {
    val largeFont = LocalDensity.current.fontScale >= 1.3f
    Surface(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        shape = RoundedCornerShape(26.dp), shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Triple("dm", "DM", SukhoIcons.Chat), Triple("comment", "Comment", SukhoIcons.Comment)).forEach { (destination, label, icon) ->
                val selected = route == destination
                Surface(
                    onClick = { onSelect(destination) },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("nav_$destination"),
                    shape = RoundedCornerShape(19.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    if (largeFont) Column(Modifier.padding(horizontal = 4.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(icon, null, Modifier.size(21.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    } else Row(Modifier.padding(horizontal = 10.dp, vertical = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, Modifier.size(21.dp)); Spacer(Modifier.width(9.dp))
                        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

