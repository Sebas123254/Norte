package com.sierracode.norte.feature.documents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sierracode.norte.R
import com.sierracode.norte.data.local.entity.Document
import com.sierracode.norte.data.local.entity.DocumentStatus
import com.sierracode.norte.data.local.entity.DocumentType
import com.sierracode.norte.data.local.repository.DocumentRepository
import com.sierracode.norte.ui.components.DocumentCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val repo: DocumentRepository
) : ViewModel() {
    val documents = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(d: Document) = viewModelScope.launch { repo.delete(d) }
}

@Composable
fun DocumentsListScreen(nav: NavController, vm: DocumentsViewModel = hiltViewModel()) {
    val docs by vm.documents.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<DocumentType?>(null) }
    var statusFilter by remember { mutableStateOf<DocumentStatus?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.documents_list_title)) },
                actions = {
                    // Aquí añadirías el selector de filtros y búsqueda
                }
            )
        }
    ) { p ->
        if (docs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.documents_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(p),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(docs.filter { doc ->
                    // Computed status from expiryDateEpochDay and reminderLeadDays
                    val expDate = doc.expiryDateEpochDay?.let { LocalDate.ofEpochDay(it) }
                    val today = LocalDate.now()
                    val computedStatus = when {
                        expDate == null -> DocumentStatus.VIGENTE
                        today.isBefore(expDate.minusDays(doc.reminderLeadDays.toLong())) -> DocumentStatus.VIGENTE
                        today.isBefore(expDate) -> DocumentStatus.POR_VENCER
                        else -> DocumentStatus.VENCIDO
                    }
                    
                    it.name.contains(searchText, ignoreCase = true) &&
                            (typeFilter == null || it.documentType == typeFilter) &&
                            (statusFilter == null || computedStatus == statusFilter)
                }, key = { it.id }) { d ->
                    DocumentCard(doc = d, onClick = { nav.navigate("document/${d.id}") })
                }
            }
        }
    }
}