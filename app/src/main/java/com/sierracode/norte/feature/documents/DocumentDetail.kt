package com.sierracode.norte.feature.documents

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sierracode.norte.data.local.entity.Document
import com.sierracode.norte.data.local.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    private val repo: DocumentRepository
) : ViewModel() {
    private val _document = MutableStateFlow<Document?>(null)
    val document = _document.asStateFlow()

    fun load(id: String) = viewModelScope.launch {
        _document.value = repo.getById(id)
    }

    fun delete(doc: Document, onDone: () -> Unit) = viewModelScope.launch {
        repo.delete(doc)
        onDone()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    nav: NavController,
    documentId: String,
    vm: DocumentDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(documentId) {
        vm.load(documentId)
    }

    val doc by vm.document.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(doc?.name ?: "Detalle del Documento") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    doc?.let { currentDoc ->
                        IconButton(onClick = {
                            vm.delete(currentDoc) { nav.popBackStack() }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val currentDoc = doc
            if (currentDoc != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(currentDoc.name, style = MaterialTheme.typography.titleLarge)
                        Text("Propietario: ${currentDoc.ownerName}", style = MaterialTheme.typography.bodyMedium)
                        Text("Categoría: ${currentDoc.category}", style = MaterialTheme.typography.bodyMedium)
                        Text("Tipo: ${currentDoc.documentType}", style = MaterialTheme.typography.bodyMedium)
                        Text("Estado: ${currentDoc.status}", style = MaterialTheme.typography.bodyMedium)
                        currentDoc.issueDateEpochDay?.let {
                            Text("Emisión: ${LocalDate.ofEpochDay(it)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Vencimiento: ${LocalDate.ofEpochDay(currentDoc.expiryDateEpochDay)}", style = MaterialTheme.typography.bodySmall)
                        currentDoc.notes?.let {
                            Text("Notas: $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                Text("Cargando documento...", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
