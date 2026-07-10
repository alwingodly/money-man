package com.alwin.moneymanager.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Filled.Home,
        title = "Welcome to Money Manager",
        description = "Everything about your money in one simple app. No sign-up, works fully " +
            "offline, and your data stays only on your phone.",
    ),
    OnboardingPage(
        icon = Icons.Filled.CreditCard,
        title = "Never miss a loan payment",
        description = "Add a loan once — monthly, weekly, or daily. We track every payment, remind " +
            "you before it's due, and show exactly how much is left.",
    ),
    OnboardingPage(
        icon = Icons.Filled.Receipt,
        title = "See where your money goes",
        description = "Jot down what you spend under simple categories, search any past expense, " +
            "and see your totals by day, month, or year at a glance.",
    ),
    OnboardingPage(
        icon = Icons.Filled.SwapHoriz,
        title = "Your digital debt book",
        description = "Track money you'll get and money you'll give, person by person — like a " +
            "paper debt book, but one that never gets lost.",
    ),
    OnboardingPage(
        icon = Icons.Filled.Settings,
        title = "Make it yours",
        description = "Pick your currency and theme, lock the app with your fingerprint, and back " +
            "up your data anytime. You're all set!",
    ),
)

@Composable
private fun accentFor(page: Int): Pair<Color, Color> {
    // Cycle the theme's container roles so each page has its own colour while still matching the
    // user's chosen theme (no hard-coded colours that could clash).
    val palette = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
    )
    return palette[page % palette.size]
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    Scaffold(
        topBar = {
            // Skip lives top-right (a common, unobtrusive onboarding convention); reserve its
            // height on the last page so the pages don't shift when it disappears.
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.CenterEnd) {
                if (isLastPage) {
                    Spacer(Modifier.height(48.dp))
                } else {
                    TextButton(onClick = onFinish) { Text("Skip") }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    onboardingPages.indices.forEach { index ->
                        PageIndicator(isSelected = index == pagerState.currentPage)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(if (isLastPage) "Get Started" else "Next")
                }
            }
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) { page ->
            // How far this page is from settled (0 = centred, 1 = a full page away). Drives a
            // gentle fade + scale so pages feel alive as you swipe.
            val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                .absoluteValue.coerceIn(0f, 1f)
            OnboardingPageContent(page = onboardingPages[page], pageIndex = page, offset = offset)
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, pageIndex: Int, offset: Float) {
    val (container, content) = accentFor(pageIndex)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .graphicsLayer {
                alpha = 1f - offset * 0.6f
                val scale = lerp(0.88f, 1f, 1f - offset)
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(120.dp).background(container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = content,
            )
        }
        Spacer(Modifier.height(36.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicator(isSelected: Boolean) {
    // Active page shows a wider "pill" that animates in/out as you move between pages.
    val width by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "indicatorWidth")
    val color by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "indicatorColor",
    )
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(8.dp)
            .width(width)
            .background(color, CircleShape)
    )
}
