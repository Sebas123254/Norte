package com.sierracode.norte.feature.documents

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sierracode.norte.R
import com.sierracode.norte.data.local.entity.Document
import com.sierracode.norte.data.local.entity.DocumentStatus
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
    
    // Calculo de status en UI (no guardado en DB)
    val today = LocalDate.now()
    val expDate = if (doc?.expiryDateEpochDay != null) {
        LocalDate.ofEpochDay(doc.expiryDateEpochDay)
    } else null
    val status = when {
        expDate == null -> DocumentStatus.VIGENTE
        today.isBefore(expDate.minusDays(doc.reminderLeadDays.toLong())) -> DocumentStatus.VIGENTE
        today.isBefore(expDate) -> DocumentStatus.POR_VENCER
        else -> DocumentStatus.VENCIDO
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(doc?.name ?: stringResource(R.string.documents_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    doc?.let { currentDoc ->
                        IconButton(onClick = {
                            vm.delete(currentDoc) { nav.popBackStack() }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
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
                // Mostrar adjunto (foto o PDF) si existe
                currentDoc.fileUri?.let { uri ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Adjunto:", style = MaterialTheme.typography.titleMedium)
                            }
                            
                            // Mostrar imagen si es una imagen
                            if (uri.toString().contains("image")) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Documento escaneado",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .roundedCornerSize(CornerSize.ExtraLarge)
                                        .align(Alignment.End),
                                    placeholder = painterResource(android.R.drawable.ic_menu_camera),
                                    error = painterResource(android.R.drawable.ic_menu_gallery)
                                )
                            } 
                            // Mostrar icono de PDF si es un PDF
                            else if (uri.toString().contains("application/pdf")) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(Color.Gray)
                                        .align(Alignment.End)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Filled.Description, contentDescription = "PDF Preview") {
                                            // No poner texto para que el icono llene el espacio
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Documento PDF", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // Tarjeta con información del documento
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(currentDoc.name, style = MaterialTheme.typography.titleLarge)
                        
                        // Mostrar tipo con especial tratamiento para TARJETA y LICENCIA
                        currentDoc.documentType.let { type ->
                            when (type) {
                                DocumentType.TARJETA -> Text(
                                    "Tarjeta de crédito/banco - **** **** **** ${currentDoc.additionalFields["last4"] ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                DocumentType.LICENCIA -> currentDoc.additionalFields?.get("tipo")?.let { clase ->
                                    Text("$clase", style = MaterialTheme.typography.bodyMedium)
                                }
                                else -> Text(
                                    stringResource(R.string.documents_detail_type, type.name),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        currentDoc.ownerName?.let { owner ->
                            Text(
                                stringResource(R.string.documents_detail_owner, owner),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        currentDoc.category?.let { cat ->
                            Text(
                                stringResource(R.string.documents_detail_category, cat),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Estado con ícono y texto siempre visible
                        status.let { st ->
                            Text(
                                stringResource(id = R.string.documents_status, st),
                                style = if (st == DocumentStatus.VENCIDO) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
                            )
                        }

                        currentDoc.issueDateEpochDay?.let {
                            Text(
                                stringResource(R.string.documents_detail_issue_date, LocalDate.ofEpochDay(it).toString()),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            stringResource(R.string.documents_detail_expiry_date, LocalDate.ofEpochDay(currentDoc.expiryDateEpochDay).toString()),
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        currentDoc.notes?.let {
                            Text(
                                stringResource(R.string.documents_detail_notes, it),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                Text(stringResource(R.string.documents_detail_loading), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

