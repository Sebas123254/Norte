package com.sierracode.norte.feature.documents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.sierracode.norte.data.local.entity.computedStatus
import com.sierracode.norte.data.local.repository.DocumentRepository
import com.sierracode.norte.ui.components.DocumentCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val repo: DocumentRepository
) : ViewModel() {
    val documents = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(d: Document) = viewModelScope.launch { repo.delete(d) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsListScreen(nav: NavController, vm: DocumentsViewModel = hiltViewModel()) {
    val docs by vm.documents.collectAsState()
    val searchText by remember { mutableStateOf("") }
    val typeFilter by remember { mutableStateOf<DocumentType?>(null) }
    val statusFilter by remember { mutableStateOf<DocumentStatus?>(null) }

    val filtered = docs.filter { d ->
        d.name.contains(searchText, ignoreCase = true) &&
                (typeFilter == null || d.documentType == typeFilter) &&
                (statusFilter == null || d.computedStatus() == statusFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.documents_list_title)) },
                actions = {
                    IconButton(onClick = { nav.navigate("scanner") }) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = stringResource(R.string.documents_scan_action))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("document_form") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add)) }
            )
        }
    ) { p ->
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.documents_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(p),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { d ->
                    DocumentCard(doc = d, onClick = { nav.navigate("document/${d.id}") })
                }
            }
        }
    }
}