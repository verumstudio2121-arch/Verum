package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarm.ui.AlarmScreen
import com.example.data.AlarmRepository
import com.example.data.PreferencesManager
import com.example.settings.SettingsScreen
import com.example.stopwatch.StopwatchScreen
import com.example.stopwatch.StopwatchViewModel
import com.example.timer.TimerScreen
import com.example.timer.TimerViewModel
import com.example.ui.theme.AccentColorTheme
import com.example.ui.theme.ClockNavTab
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.HiTechClockTheme
import com.example.ui.theme.LiquidFloatingNavBar
import com.example.util.LocalHapticManager
import com.example.util.rememberHapticManager
import com.example.worldclock.WorldClockRepository
import com.example.worldclock.WorldClockScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencesManager = PreferencesManager(applicationContext)
        val alarmRepository = AlarmRepository(applicationContext)
        val worldClockRepository = WorldClockRepository(applicationContext)

        setContent {
            val settings by preferencesManager.settings.collectAsState()

            val isDarkTheme = when (settings.appearance) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            val currentAccent = remember(settings.accentColor) {
                AccentColorTheme.fromId(settings.accentColor)
            }

            HiTechClockTheme(darkTheme = isDarkTheme, accentTheme = currentAccent) {
                val hapticManager = rememberHapticManager()

                CompositionLocalProvider(LocalHapticManager provides hapticManager) {
                    val tabs = remember { ClockNavTab.values() }
                    val pagerState = rememberPagerState(
                        initialPage = tabs.indexOf(ClockNavTab.WORLD),
                        pageCount = { tabs.size }
                    )
                    val coroutineScope = rememberCoroutineScope()
                    val selectedTab = tabs[pagerState.currentPage]
                    var inSettings by remember { mutableStateOf(false) }

                    // Shared ViewModels that preserve state across tab changes
                    val stopwatchViewModel: StopwatchViewModel = viewModel()
                    val timerViewModel: TimerViewModel = remember { TimerViewModel(applicationContext) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeepBlack)
                    ) {
                        if (inSettings) {
                            SettingsScreen(
                                preferencesManager = preferencesManager,
                                onBack = { inSettings = false }
                            )
                        } else {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = DeepBlack,
                                bottomBar = {
                                    LiquidFloatingNavBar(
                                        selectedTab = selectedTab,
                                        onTabSelected = { tab ->
                                            val targetPage = tabs.indexOf(tab)
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = targetPage,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.82f,
                                                        stiffness = 380f
                                                    )
                                                )
                                            }
                                        },
                                        pagerOffsetFractionProvider = {
                                            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                        }
                                    )
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->
                                        when (tabs[page]) {
                                            ClockNavTab.WORLD -> WorldClockScreen(
                                                repository = worldClockRepository,
                                                settings = settings,
                                                onOpenSettings = { inSettings = true }
                                            )

                                            ClockNavTab.ALARMS -> AlarmScreen(
                                                repository = alarmRepository,
                                                settings = settings,
                                                onOpenSettings = { inSettings = true }
                                            )

                                            ClockNavTab.STOPWATCH -> StopwatchScreen(
                                                viewModel = stopwatchViewModel
                                            )

                                            ClockNavTab.TIMERS -> TimerScreen(
                                                viewModel = timerViewModel,
                                                onOpenSettings = { inSettings = true }
                                            )
                                        }
                                    }

                                    // Quick Floating Settings Gear Button (Top-Right on Stopwatch)
                                    val currentTab = tabs[pagerState.currentPage]
                                    if (currentTab == ClockNavTab.STOPWATCH) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .statusBarsPadding()
                                                .padding(top = 16.dp, end = 20.dp)
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(GlassSurface)
                                                .border(1.dp, GlassBorder, CircleShape)
                                                .clickable {
                                                    hapticManager.buttonClick()
                                                    inSettings = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Settings",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
