package com.sierracode.norte.feature.documents

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sierracode.norte.R
import com.sierracode.norte.data.local.entity.Document
import com.sierracode.norte.data.local.entity.computedStatus
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

    fun load(id: String) = viewModelScope.launch { _document.value = repo.getById(id) }

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
    LaunchedEffect(documentId) { vm.load(documentId) }
    val doc by vm.document.collectAsState()
    val currentDoc = doc   // copia local para permitir smart cast

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentDoc?.name ?: stringResource(R.string.documents_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (currentDoc != null) {
                        IconButton(onClick = { vm.delete(currentDoc) { nav.popBackStack() } }) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentDoc == null) {
                Text(stringResource(R.string.documents_detail_loading), style = MaterialTheme.typography.bodyLarge)
            } else {
                currentDoc.fileUri?.let { AttachmentCard(it) }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(currentDoc.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.documents_detail_type, currentDoc.documentType.name),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            stringResource(R.string.documents_detail_owner, currentDoc.ownerName),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            stringResource(R.string.documents_detail_category, currentDoc.category),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            stringResource(R.string.documents_detail_status, currentDoc.computedStatus().name),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        currentDoc.issueDateEpochDay?.let {
                            Text(
                                stringResource(R.string.documents_detail_issue_date, LocalDate.ofEpochDay(it).toString()),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        currentDoc.expiryDateEpochDay?.let {
                            Text(
                                stringResource(R.string.documents_detail_expiry_date, LocalDate.ofEpochDay(it).toString()),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        currentDoc.notes?.let {
                            Text(
                                stringResource(R.string.documents_detail_notes, it),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentCard(uri: String) {
    val context = LocalContext.current
    val isPdf = remember(uri) {
        uri.endsWith(".pdf", ignoreCase = true) ||
                runCatching { context.contentResolver.getType(Uri.parse(uri)) }.getOrNull() == "application/pdf"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Adjunto:", style = MaterialTheme.typography.titleMedium)
            if (isPdf) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Description, contentDescription = "PDF")
                    Spacer(Modifier.width(8.dp))
                    Text("Documento PDF", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                AsyncImage(
                    model = uri,
                    contentDescription = "Documento escaneado",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
    }
}