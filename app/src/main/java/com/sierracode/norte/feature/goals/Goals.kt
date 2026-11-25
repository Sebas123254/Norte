package com.sierracode.norte.feature.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sierracode.norte.data.local.entity.*
import com.sierracode.norte.data.local.repository.GoalRepository
import com.sierracode.norte.util.money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repo: GoalRepository
) : ViewModel() {
    val goals = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(title: String, targetCents: Long) = viewModelScope.launch {
        repo.upsert(Goal(title = title, targetAmountCents = targetCents))
    }
}

@Composable
fun GoalsScreen(nav: NavController, vm: GoalsViewModel = hiltViewModel()) {
    val items by vm.goals.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                text = { Text("Nueva meta") },
                icon = {}
            )
        }
    ) { p ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(p),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { g ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(g.title, style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            progress = { g.progressPercent },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                        Text("${(g.progressPercent * 100).toInt()}% · ${money(g.currentAmountCents)} / ${money(g.targetAmountCents)}")
                    }
                }
            }
        }
    }

    if (showDialog) {
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nueva meta") },
            text = {
                Column {
                    OutlinedTextField(title, { title = it }, label = { Text("Título") })
                    OutlinedTextField(amount, { amount = it }, label = { Text("Monto objetivo (MXN)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cents = (amount.toDoubleOrNull() ?: 0.0).times(100).toLong()
                    if (title.isNotBlank() && cents > 0) { vm.add(title, cents); showDialog = false }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}