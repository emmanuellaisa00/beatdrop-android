package com.beatdrop.app.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beatdrop.app.data.cloud.CloudSync
import com.beatdrop.app.data.cloud.Notification
import com.beatdrop.app.data.cloud.NotificationsRealtimeManager
import com.beatdrop.app.data.cloud.NotificationsRepository
import com.beatdrop.app.ui.components.StickyBackBar
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Notifications inbox + realtime updates. Live only when signed in. */
class NotificationsAppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NotificationsRepository()
    private val realtime = NotificationsRealtimeManager()

    private val _items = MutableStateFlow<List<Notification>>(emptyList())
    val items: StateFlow<List<Notification>> = _items

    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread

    fun refresh() {
        if (!CloudSync.isSignedIn) { _items.value = emptyList(); _unread.value = 0; return }
        viewModelScope.launch {
            runCatching {
                _items.value = repo.getNotifications()
                _unread.value = _items.value.count { !it.read }
            }
        }
    }

    fun listenRealtime(userId: String) {
        realtime.subscribe(userId)
        viewModelScope.launch {
            realtime.notifications.collect { n ->
                _items.value = listOf(n) + _items.value
                _unread.value += 1
            }
        }
    }

    fun stopRealtime() = realtime.unsubscribe()

    fun markAllRead() {
        viewModelScope.launch {
            runCatching { repo.markAllAsRead() }
            _items.value = _items.value.map { it.copy(read = true) }
            _unread.value = 0
        }
    }
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    vm: NotificationsAppViewModel = viewModel(),
) {
    BackHandler(onBack = onBack)
    val items by vm.items.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refresh() }

    Box(Modifier.fillMaxSize().background(BeatColors.Background)) {
        if (items.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.NotificationsNone, null, tint = Color(0x55FFFFFF), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("You're all caught up", style = BeatType.SectionTitle, color = BeatColors.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (CloudSync.isSignedIn) "New activity will show up here." else "Sign in to get notifications.",
                    style = BeatType.CardSub, color = BeatColors.TextSecondary, textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 64.dp, bottom = 40.dp)) {
                items(items, key = { it.id }) { n -> NotificationRow(n) }
            }
        }

        StickyBackBar(
            title = "Notifications",
            frosted = true,
            onBack = onBack,
            onMore = { vm.markAllRead() },
            moreIcon = Icons.Rounded.DoneAll,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun NotificationRow(n: Notification) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(8.dp).clip(CircleShape)
                .background(if (n.read) Color.Transparent else BeatColors.Accent)
        )
        Column(Modifier.weight(1f)) {
            Text(
                n.title,
                style = BeatType.TrackTitle.copy(fontWeight = if (n.read) FontWeight.SemiBold else FontWeight.Bold),
                color = BeatColors.TextPrimary
            )
            if (!n.body.isNullOrBlank()) {
                Text(n.body!!, style = BeatType.TrackSub, color = Color(0x99FFFFFF))
            }
        }
    }
}
