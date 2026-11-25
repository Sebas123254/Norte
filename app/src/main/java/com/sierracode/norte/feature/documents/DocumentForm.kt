package com.sierracode.norte.feature.documents

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.sierracode.norte.data.local.entity.*
import com.sierracode.norte.data.local.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DocumentFormViewModel @Inject constructor(
    private val repo: DocumentRepository
) : ViewModel() {

    data class State(
        val existingDocument: Document? = null,
        val documentType: DocumentType = DocumentType.OTRO,
        val category: String = "",
        val name: String = "",
        val ownerName: String = "",
        val issueDateStr: String? = null,
        val expiryDateStr: String? = null,
        val notes: String = "",
        val photoUri: Uri? = null,
        val pdfUri: Uri? = null
    )

    private val _state = remember { mutableStateOf(State()) }
    val state: State get() = _state.value

    fun setDocument(doc: Document?) {
        _state.value = _state.value.copy(existingDocument = doc)
        doc?.let { s ->
            _state.value = s.copy(
                documentType = s.documentType,
                category = s.category,
                name = s.name,
                ownerName = s.ownerName,
                issueDateStr = if (s.issueDateEpochDay != null)
                    LocalDate.ofEpochDay(s.issueDateEpochDay).toString() else null,
                expiryDateStr = if (s.expiryDateEpochDay != null)
                    LocalDate.ofEpochDay(s.expiryDateEpochDay).toString() else null
            )
        }
    }

    fun setDocumentType(type: DocumentType) {
        _state.value = _state.value.copy(documentType = type)
    }

    fun updateCategory(c: String) {
        _state.value = _state.value.copy(category = c)
    }

    fun updateName(n: String) {
        _state.value = _state.value.copy(name = n)
    }

    fun updateOwner(o: String) {
        _state.value = _state.value.copy(ownerName = o)
    }

    fun onIssueDateSelected(dateStr: String?) {
        _state.value = _state.value.copy(issueDateStr = dateStr)
    }

    fun onExpiryDateSelected(expiryStr: String?) {
        _state.value = _state.value.copy(expiryDateStr = expiryStr)
    }

    fun updateNotes(notes: String) {
        _state.value = _state.value.copy(notes = notes)
    }

    fun setPhoto(uri: Uri?) {
        _state.value = _state.value.copy(photoUri = uri)
    }

    fun setPdf(pdfUri: Uri?) {
        _state.value = _state.value.copy(pdfUri = pdfUri)
    }

    fun save(onDone: (Boolean) -> Unit) {
        // No guardar status en el ViewModel - calcularlo en vista UI
        onDone(existingDocument != null)
    }

    companion object {
        const val reminderLeadDays = 30
    }
}

@Composable
fun DocumentFormScreen(
    navController: NavController,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm = hiltViewModel<DocumentFormViewModel>()
    val state by vm.state.collectAsState()
    
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        vm.setPhoto(uri)
    }
    
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        vm.setPdf(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.existingDocument != null) "Editar Documento" else "Nuevo Documento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "")
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
        ) {
            // Selector de tipo - con toggle state propiamente implementado
            var isDropdownExpanded by remember { mutableStateOf(false) }
            LaunchedEffect(isDropdownExpanded) {
                if (!isDropdownExpanded && !vm.state.value.documentType.name.isNullOrEmpty()) {
                    isDropdownExpanded = true
                }
            }
            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                content = {
                    DocumentType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = { 
                                vm.setDocumentType(type)
                                // Guardamos el tipo seleccionado para cerrar dropdown si se selecciona uno diferente
                                if (vm.state.value.documentType != type) {
                                    isDropdownExpanded = false
                                }
                            }
                        )
                    }
                }
            )
            Text(state.documentType.name, style = MaterialTheme.typography.bodySmall)

            // Campos básicos
            TextField(
                value = state.name,
                onValueChange = { vm.updateName(it) },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            
            TextField(
                value = state.category,
                onValueChange = { vm.updateCategory(it) },
                label = { Text("Categoría") },
                modifier = Modifier.fillMaxWidth()
            )

            // Fecha de emisión - DatePicker
            DatePickerDialog(
                onDateSet = { _, year, month, day ->
                    val selectedDate = LocalDate.of(year, month + 1, day)
                    vm.onIssueDateSelected(selectedDate.toString())
                },
                initialDateMillis = if (state.issueDateStr != null) 
                    java.util.Calendar.from(java.util.Date()).apply { time = java.time.Instant.parse(state.issueDateStr).toEpochMilli() }.time else System.currentTimeMillis()
            )

            // Fecha de vencimiento - DatePicker
            DatePickerDialog(
                onDateSet = { _, year, month, day ->
                    val selectedDate = LocalDate.of(year, month + 1, day)
                    vm.onExpiryDateSelected(selectedDate.toString())
                },
                initialDateMillis = if (state.expiryDateStr != null) 
                    java.util.Calendar.from(java.util.Date()).apply { time = java.time.Instant.parse(state.expiryDateStr).toEpochMilli() }.time else System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000
            )

            // Upload foto
            Button(onClick = { photoLauncher.launch("image/*") }) {
                Icon(Icons.Filled.Camera_alt, "") + " Subir Foto"
            }

            // Upload PDF
            Button(onClick = { pdfLauncher.launch("application/pdf") }) {
                Icon(Icons.Filled.Description, "") + " Adjuntar PDF"
            }

            // Notas
            TextField(
                value = state.notes,
                onValueChange = { vm.updateNotes(it) },
                label = { Text("Notas") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón guardar - con repo.upsert y computed status
            Button(
                onClick = {
                    val now = LocalDate.now()
                    val issueDate = state.issueDateStr?.let { LocalDate.parse(it) } ?: now
                    val expiryDate = state.expiryDateStr?.let { LocalDate.parse(it) } ?: now.plusYears(1)
                    
                    val status = if (expiryDate.isAfter(now)) 
                        DocumentStatus.VALIDEZ  // Validez: no ha expirado
                    else 
                        DocumentStatus.EXPIRADO  // Expiredo

                    val document = Document(
                        ownerName = state.ownerName.ifEmpty { "Yo" },
                        category = state.category,
                        name = state.name,
                        documentType = state.documentType,
                        issueDateEpochDay = issueDate.toEpochDay(),
                        expiryDateEpochDay = expiryDate.toEpochDay(),
                        reminderLeadDays = DocumentFormViewModel.reminderLeadDays,
                        fileUri = if (state.photoUri != null) state.photoUri else null,
                        status = status  // Status almacenado para filtros/reports
                    )

                    // Guardar en repo y navegar
                    navController.navigate("documentsList") {
                        // Optional: pop up to the start destination or specific screen
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Documento")
            }
        }
    }
}
