package com.sierracode.norte.feature.documents

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.sierracode.norte.data.local.entity.DocumentType
import com.sierracode.norte.data.local.repository.DocumentRepository
import com.sierracode.norte.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class DocumentFormViewModel @Inject constructor(
    private val repo: DocumentRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    fun save(
        name: String, category: String, owner: String, type: DocumentType,
        issue: LocalDate?, expiry: LocalDate?, notes: String, fileUri: String?,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        val lead = settingsStore.settings.first().defaultReminderDays
        repo.upsert(
            Document(
                ownerName = owner.ifBlank { "Yo" },
                category = category.ifBlank { "Otros" },
                name = name.trim(),
                documentType = type,
                issueDateEpochDay = issue?.toEpochDay(),
                expiryDateEpochDay = expiry?.toEpochDay(),
                reminderLeadDays = lead,
                fileUri = fileUri,
                notes = notes.ifBlank { null }
            )
        )
        onDone()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentFormScreen(nav: NavController, vm: DocumentFormViewModel = hiltViewModel()) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(DocumentType.OTRO) }
    var issue by remember { mutableStateOf<LocalDate?>(null) }
    var expiry by remember { mutableStateOf<LocalDate?>(null) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var typeMenu by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            fileUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo documento") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(owner, { owner = it }, label = { Text("Propietario") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Box {
                OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Tipo: ${type.name}")
                }
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    DocumentType.entries.forEach { t ->
                        DropdownMenuItem(text = { Text(t.name) }, onClick = { type = t; typeMenu = false })
                    }
                }
            }

            DateField("Emisión", issue) { issue = it }
            DateField("Vencimiento", expiry) { expiry = it }

            OutlinedButton(
                onClick = { picker.launch(arrayOf("image/*", "application/pdf")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AttachFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (fileUri == null) "Adjuntar foto o PDF" else "Adjunto seleccionado ✓")
            }

            OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    vm.save(name, category, owner, type, issue, expiry, notes, fileUri?.toString()) {
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: LocalDate?, onChange: (LocalDate?) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${value?.toString() ?: "—"}")
    }
    if (show) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    })
                    show = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) { DatePicker(state = state) }
    }
}