package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OfficeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OfficeRepository

    // Base flows
    val activeDocuments: StateFlow<List<OfficeDocument>>
    val trashDocuments: StateFlow<List<OfficeDocument>>

    // UI state states
    var activeTab = mutableStateOf("dashboard") // "dashboard", "editor", "settings", "chatbot"
    var selectedDocument = mutableStateOf<OfficeDocument?>(null)
    var searchQuery = mutableStateOf("")
    var filterType = mutableStateOf("all") // "all", "document", "spreadsheet", "presentation", "note", "pdf"
    var filterTag = mutableStateOf("")

    // Document styling
    var editorFontSize = mutableStateOf(16)
    var editorFontFamily = mutableStateOf("Sans-Serif") // "Serif", "Sans-Serif", "Monospace"
    var editorTextColor = mutableStateOf("#333333")

    // Undo/Redo Engine for Document text editing
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    // Find and Replace
    var findQuery = mutableStateOf("")
    var replaceQuery = mutableStateOf("")

    // Spreadsheets active states
    // Col maps A-F, Row maps 1-15. Cell key format e.g. "A1"
    var spreadsheetCells = mutableStateOf<Map<String, String>>(emptyMap())
    var selectedCell = mutableStateOf("A1")

    // Presentations active states
    var selectedSlideIndex = mutableStateOf(0)
    var maxThemeName = mutableStateOf("Cosmic Blue") // "Cosmic Blue", "Forest Edge", "Brutalist Slate", "Warm Charcoal", "Soft Rose"
    var slideTransition = mutableStateOf("Fade") // "Fade", "Slide-In", "Scale-Up"

    // PDF active state
    var pdfSignatureEncoded = mutableStateOf<String?>(null) // Base64 signature
    var pdfAnnotations = mutableStateOf<List<String>>(emptyList()) // Highlighted indexes/texts

    // Security & Encryptions
    var isAppLocked = mutableStateOf(false)
    var userSavedPin = mutableStateOf("") // Empty means unlocked
    var pinEntryError = mutableStateOf(false)
    var isBiometricsEnabled = mutableStateOf(false)
    var isLocalEncryptionEnabled = mutableStateOf(false)

    // Account Sync Simulator
    var userLoginMode = mutableStateOf("Guest Mode") // "Guest Mode", "Email", "Google", "Microsoft", "Apple"
    var userEmail = mutableStateOf("")
    var lastCloudSyncTime = mutableStateOf<String>("Never")
    var isSyncingCloud = mutableStateOf(false)

    // Chatbot States
    var chatMessages = mutableStateOf<List<Pair<String, Boolean>>>(
        listOf("Hello! I am MaxOffice AI Assistant. Select a document and ask me to summarize it, draft outlines, check spelling, or generate office layouts!" to false)
    )
    var aiChatLoading = mutableStateOf(false)

    // Dynamic AI Image / Video Outputs
    var generatedImageB64 = mutableStateOf<String?>(null)
    var imageAspectRatio = mutableStateOf("1:1")
    var isImageGenerating = mutableStateOf(false)

    var generatedVideoOp = mutableStateOf<String?>(null)
    var videoAspectRatio = mutableStateOf("16:9")
    var isVideoGenerating = mutableStateOf(false)

    init {
        val database = OfficeDatabase.getDatabase(application)
        val dao = database.officeDao()
        repository = OfficeRepository(dao)

        activeDocuments = repository.activeDocuments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        trashDocuments = repository.trashDocuments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Create sample template documents if empty
        viewModelScope.launch {
            activeDocuments.first().let { list ->
                if (list.isEmpty()) {
                    createTemplatePresets()
                }
            }
        }
    }

    // --- Template Presets Generator ---
    private suspend fun createTemplatePresets() {
        val formats = listOf(
            OfficeDocument(
                title = "Welcome to MaxOffice doc",
                content = "# MaxOffice Suite\n\nWelcome to your professional-grade, fully offline office editor. MaxOffice respects your privacy by storing all files locally with optional PIN security and military-grade on-device encryption.\n\n### Key Features:\n- 🚀 Offline-first operations\n- 🔒 Biometric & PIN lock option\n- 📊 Formulas & Charts in sheets\n- 🖼️ Aspect-controlled Gemini AI image generator\n- 🤖 Interactive document intelligent chatbot",
                type = "document",
                tags = "Guides,Manual"
            ),
            OfficeDocument(
                title = "Monthly Budget Spreadsheet",
                content = "A1:Item|B1:Projected|C1:Actual|D1:Difference\nA2:Office Rent|B2:2200|C2:2200|D2:=B2-C2\nA3:Utilities|B3:450|C3:410|D3:=B3-C3\nA4:Software Licenses|B4:850|C4:910|D4:=B4-C4\nA5:AI API Budget|B5:1200|C5:950|D5:=B5-C5\nA6:Total|B6:=SUM(B2:B5)|C6:=SUM(C2:C5)|D6:=B6-C6",
                type = "spreadsheet",
                tags = "Finance"
            ),
            OfficeDocument(
                title = "MaxOffice Pitch Presentation",
                content = "Slide 1|Title:MaxOffice Suite|Subtitle:The Next Gen Privacy Office Workspace\nSlide 2|Title:Why Offline-First?|Subtitle:Complete control over your personal documentation, independent of internet metrics\nSlide 3|Title:On-Device Protection|Subtitle:PIN codes & hardware backed local encryption settings",
                type = "presentation",
                tags = "Marketing"
            ),
            OfficeDocument(
                title = "Sprint Planning meeting notes",
                content = "- Finalize Room integration layer.\n- Deliver custom canvas layout for Spreadsheet budget cells.\n- Setup Gemini 3.1 flash lite for lightning-fast translations.\n- Finish Veo video generator viewport with aspect selections.",
                type = "note",
                tags = "Productivity",
                accentColor = "#FF9800"
            )
        )
        for (doc in formats) {
            repository.insert(doc)
        }
    }

    // --- Document Actions ---
    fun selectDoc(doc: OfficeDocument) {
        selectedDocument.value = doc
        undoStack.clear()
        redoStack.clear()

        // Load document-specific auxiliary states
        if (doc.type == "spreadsheet") {
            parseSpreadsheetCells(doc.content)
            selectedCell.value = "A1"
        } else if (doc.type == "presentation") {
            selectedSlideIndex.value = 0
            maxThemeName.value = doc.themeName
            slideTransition.value = "Fade"
        } else if (doc.type == "pdf") {
            pdfAnnotations.value = emptyList()
            pdfSignatureEncoded.value = null
        }
        activeTab.value = "editor"
    }

    fun deselectDoc() {
        selectedDocument.value = null
        activeTab.value = "dashboard"
    }

    fun updateSelectedDocContent(newContent: String) {
        val currentDoc = selectedDocument.value ?: return
        if (currentDoc.content != newContent) {
            // Push current state to undo
            undoStack.add(currentDoc.content)
            redoStack.clear()

            val updated = currentDoc.copy(content = newContent, lastModified = System.currentTimeMillis())
            selectedDocument.value = updated
            viewModelScope.launch {
                repository.update(updated)
            }
        }
    }

    fun quickCreate(type: String) {
        viewModelScope.launch {
            val title = when (type) {
                "document" -> "New Document " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                "spreadsheet" -> "New Spreadsheet " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                "presentation" -> "New Presentation " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                "note" -> "Quick Note " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                "pdf" -> "PDF Document " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                else -> "New Item"
            }
            val initialContent = when (type) {
                "document" -> "# Title\nStart typing here..."
                "spreadsheet" -> "A1:Item|B1:Value|A2:Sample|B2:10"
                "presentation" -> "Slide 1|Title:New Slide|Subtitle:Add transitions in editor toolbar\nSlide 2|Title:Next Slide|Subtitle:Write notes below"
                "note" -> "Draft your checklist..."
                "pdf" -> "PDF SCANNER REPORT\n[Interactive signature block below]"
                else -> ""
            }
            val newDoc = OfficeDocument(
                title = title,
                content = initialContent,
                type = type,
                accentColor = if (type == "note") "#4CAF50" else "#3F51B5"
            )
            val newId = repository.insert(newDoc)
            val loadedDoc = newDoc.copy(id = newId.toInt())
            selectDoc(loadedDoc)
        }
    }

    fun renameDoc(id: Int, newTitle: String) {
        viewModelScope.launch {
            val current = selectedDocument.value
            if (current != null && current.id == id) {
                val updated = current.copy(title = newTitle, lastModified = System.currentTimeMillis())
                selectedDocument.value = updated
                repository.update(updated)
            } else {
                // Find in flow list
                val doc = activeDocuments.value.find { it.id == id }
                if (doc != null) {
                    repository.update(doc.copy(title = newTitle, lastModified = System.currentTimeMillis()))
                }
            }
        }
    }

    fun deleteDocToTrash(id: Int) {
        viewModelScope.launch {
            repository.updateTrashStatus(id, isTrash = true)
            if (selectedDocument.value?.id == id) {
                deselectDoc()
            }
        }
    }

    fun restoreDocFromTrash(id: Int) {
        viewModelScope.launch {
            repository.updateTrashStatus(id, isTrash = false)
        }
    }

    fun permanentDeleteDoc(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun toggleFavorite(id: Int, currentVal: Boolean) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(id, !currentVal)
            if (selectedDocument.value?.id == id) {
                selectedDocument.value = selectedDocument.value?.copy(isFavorite = !currentVal)
            }
        }
    }

    fun duplicateDocument(doc: OfficeDocument) {
        viewModelScope.launch {
            val duplicated = doc.copy(
                id = 0,
                title = doc.title + " (Copy)",
                lastModified = System.currentTimeMillis(),
                isFavorite = false
            )
            repository.insert(duplicated)
        }
    }

    fun updateTags(tagsString: String) {
        val current = selectedDocument.value ?: return
        val updated = current.copy(tags = tagsString, lastModified = System.currentTimeMillis())
        selectedDocument.value = updated
        viewModelScope.launch {
            repository.update(updated)
        }
    }

    fun updateAccentColor(hexCode: String) {
        val current = selectedDocument.value ?: return
        val updated = current.copy(accentColor = hexCode)
        selectedDocument.value = updated
        viewModelScope.launch {
            repository.update(updated)
        }
    }

    fun emptyTrashBin() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    // --- Undo/Redo Implementation ---
    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val currentDoc = selectedDocument.value ?: return
            val prev = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(currentDoc.content)
            selectedDocument.value = currentDoc.copy(content = prev)
            viewModelScope.launch {
                repository.update(selectedDocument.value!!)
            }
        }
    }

    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val currentDoc = selectedDocument.value ?: return
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(currentDoc.content)
            selectedDocument.value = currentDoc.copy(content = next)
            viewModelScope.launch {
                repository.update(selectedDocument.value!!)
            }
        }
    }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()

    // --- Find and Replace ---
    fun runFindAndReplace() {
        val current = selectedDocument.value ?: return
        val q = findQuery.value
        val r = replaceQuery.value
        if (q.isNotEmpty()) {
            val replaced = current.content.replace(q, r, ignoreCase = true)
            updateSelectedDocContent(replaced)
        }
    }

    // --- Cell Matrix/Formula Parsing for Spreadsheets ---
    private fun parseSpreadsheetCells(raw: String) {
        // Form: "A1:Hello|B1:20|C1:=B1+2"
        val cells = mutableMapOf<String, String>()
        if (raw.isNotEmpty()) {
            val tokens = raw.split("|")
            for (token in tokens) {
                val colonIdx = token.indexOf(":")
                if (colonIdx != -1) {
                    val key = token.substring(0, colonIdx).trim()
                    val value = token.substring(colonIdx + 1).trim()
                    cells[key] = value
                }
            }
        }
        spreadsheetCells.value = cells
    }

    fun updateSpreadsheetCell(cellId: String, value: String) {
        val updatedCells = spreadsheetCells.value.toMutableMap()
        updatedCells[cellId] = value
        spreadsheetCells.value = updatedCells

        // Serialize cell list and write to selected document
        val serializedContent = updatedCells.entries.joinToString("|") { "${it.key}:${it.value}" }
        updateSelectedDocContent(serializedContent)
    }

    /**
     * Evaluates formulas in a cell (e.g. "=SUM(B2:B5)" or "=B3-C3")
     */
    fun evaluateCell(cellId: String): String {
        val rawVal = spreadsheetCells.value[cellId] ?: return ""
        if (!rawVal.startsWith("=")) return rawVal

        // Clean formula
        val formula = rawVal.substring(1).uppercase().trim()
        try {
            // Check operations
            if (formula.startsWith("SUM")) {
                val range = extractRange(formula, "SUM") ?: return "ERR"
                val values = getValuesInRange(range)
                return values.sum().toString()
            } else if (formula.startsWith("AVERAGE") || formula.startsWith("AVG")) {
                val command = if (formula.startsWith("AVERAGE")) "AVERAGE" else "AVG"
                val range = extractRange(formula, command) ?: return "ERR"
                val values = getValuesInRange(range)
                if (values.isEmpty()) return "0"
                return String.format(Locale.US, "%.1f", values.average())
            } else if (formula.startsWith("MAX")) {
                val range = extractRange(formula, "MAX") ?: return "ERR"
                val values = getValuesInRange(range)
                if (values.isEmpty()) return "0"
                return values.maxOrNull().toString()
            } else if (formula.startsWith("MIN")) {
                val range = extractRange(formula, "MIN") ?: return "ERR"
                val values = getValuesInRange(range)
                if (values.isEmpty()) return "0"
                return values.minOrNull().toString()
            }

            // Simple basic operations like "=B2-C2" or "=B3+10"
            return evaluateSimpleAlgebra(formula)
        } catch (e: Exception) {
            return "ERR_FORMULA"
        }
    }

    private fun extractRange(formula: String, operator: String): String? {
        val startParen = formula.indexOf("(")
        val endParen = formula.indexOf(")")
        if (startParen == -1 || endParen == -1) return null
        return formula.substring(startParen + 1, endParen)
    }

    private fun getValuesInRange(range: String): List<Double> {
        val colTokens = range.split(":")
        if (colTokens.size != 2) return emptyList()
        val startCell = colTokens[0].trim()
        val endCell = colTokens[1].trim()

        val startCol = startCell[0]
        val startRow = startCell.substring(1).toIntOrNull() ?: return emptyList()
        val endCol = endCell[0]
        val endRow = endCell.substring(1).toIntOrNull() ?: return emptyList()

        val valuesList = mutableListOf<Double>()
        for (col in startCol..endCol) {
            for (row in startRow..endRow) {
                val cellId = "$col$row"
                val evaluatedStr = evaluateCell(cellId)
                evaluatedStr.toDoubleOrNull()?.let {
                    valuesList.add(it)
                }
            }
        }
        return valuesList
    }

    private fun evaluateSimpleAlgebra(expr: String): String {
        // Handlers for B2-C2, A1+B1, etc.
        val operator = when {
            expr.contains("-") -> "-"
            expr.contains("+") -> "+"
            expr.contains("*") -> "*"
            expr.contains("/") -> "/"
            else -> ""
        }
        if (operator.isEmpty()) {
            // Might be just pointing to another cell like "=B2"
            val targetVal = spreadsheetCells.value[expr] ?: ""
            if (targetVal.startsWith("=")) {
                return evaluateCell(expr)
            }
            return targetVal
        }

        val tokens = expr.split(operator)
        if (tokens.size != 2) return "ERR"

        val val1Str = tokens[0].trim()
        val val2Str = tokens[1].trim()

        val double1 = (spreadsheetCells.value[val1Str]?.toDoubleOrNull()
            ?: val1Str.toDoubleOrNull() ?: 0.0)
        val double2 = (spreadsheetCells.value[val2Str]?.toDoubleOrNull()
            ?: val2Str.toDoubleOrNull() ?: 0.0)

        val result = when (operator) {
            "-" -> double1 - double2
            "+" -> double1 + double2
            "*" -> double1 * double2
            "/" -> if (double2 != 0.0) double1 / double2 else Double.NaN
            else -> 0.0
        }
        return if (result.isNaN()) "DIV/0" else String.format(Locale.US, "%.1f", result)
    }

    fun sortSpreadsheetColumn(colLetter: Char) {
        // Sort column cells descending or ascending
        val updated = spreadsheetCells.value.toMutableMap()
        val cellsWithValues = (1..15).map { "$colLetter$it" }
            .map { it to (updated[it] ?: "") }
            .filter { it.second.isNotEmpty() }

        val sorted = cellsWithValues.sortedBy { it.second }
        (1..15).forEachIndexed { index, row ->
            val cellId = "$colLetter$row"
            if (index < sorted.size) {
                updated[cellId] = sorted[index].second
            } else {
                updated[cellId] = ""
            }
        }
        spreadsheetCells.value = updated
        val serializedContent = updated.entries.joinToString("|") { "${it.key}:${it.value}" }
        updateSelectedDocContent(serializedContent)
    }

    // --- Slide Operations for Presentations ---
    fun getSlides(): List<Pair<String, String>> {
        val doc = selectedDocument.value ?: return emptyList()
        val lines = doc.content.split("\n").filter { it.startsWith("Slide") }
        return lines.map { line ->
            // Slide 1|Title:Hello|Subtitle:World
            val payload = line.substringAfter("|")
            val title = payload.substringBefore("|Title:").substringAfter("Title:").substringBefore("|Subtitle:")
            val sub = payload.substringAfter("Subtitle:")
            title to sub
        }
    }

    fun updateSlide(index: Int, rawTitle: String, rawSubtitle: String) {
        val slides = getSlides().toMutableList()
        if (index in slides.indices) {
            slides[index] = rawTitle to rawSubtitle
        }
        serializeAndSaveSlides(slides)
    }

    fun addSlide() {
        val slides = getSlides().toMutableList()
        slides.add("New Slide Title" to "New subtitle slide details")
        serializeAndSaveSlides(slides)
        selectedSlideIndex.value = slides.size - 1
    }

    fun deleteSlide(index: Int) {
        val slides = getSlides().toMutableList()
        if (slides.size > 1 && index in slides.indices) {
            slides.removeAt(index)
            serializeAndSaveSlides(slides)
            selectedSlideIndex.value = (index - 1).coerceAtLeast(0)
        }
    }

    private fun serializeAndSaveSlides(slides: List<Pair<String, String>>) {
        val contentSerialized = slides.mapIndexed { idx, pair ->
            "Slide ${idx + 1}|Title:${pair.first}|Subtitle:${pair.second}"
        }.joinToString("\n")
        updateSelectedDocContent(contentSerialized)
    }

    fun applyPresentationTheme(theme: String) {
        maxThemeName.value = theme
        val current = selectedDocument.value ?: return
        val updated = current.copy(themeName = theme)
        selectedDocument.value = updated
        viewModelScope.launch {
            repository.update(updated)
        }
    }

    // --- PDF Signing Simulator ---
    fun signPdfWithText(signatureName: String) {
        pdfSignatureEncoded.value = signatureName
        val current = selectedDocument.value ?: return
        val updated = current.copy(
            content = current.content + "\n\n[SIGNED BY USER PATH: $signatureName]\nTimestamp: " + SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()).format(Date()),
            lastModified = System.currentTimeMillis()
        )
        selectedDocument.value = updated
        viewModelScope.launch {
            repository.update(updated)
        }
    }

    fun addPdfHighlight(textMarker: String) {
        pdfAnnotations.value = pdfAnnotations.value + textMarker
    }

    // --- Security PIN Logic ---
    fun changeMasterPin(newPin: String) {
        userSavedPin.value = newPin
        isAppLocked.value = false
    }

    fun enrollBiometrics(enabled: Boolean) {
        isBiometricsEnabled.value = enabled
    }

    fun authenticatePin(input: String): Boolean {
        return if (input == userSavedPin.value) {
            isAppLocked.value = false
            pinEntryError.value = false
            true
        } else {
            pinEntryError.value = true
            false
        }
    }

    // --- Sync / Authenticate Simulator ---
    fun triggerSimulatedSync() {
        if (userLoginMode.value == "Guest Mode") return
        viewModelScope.launch {
            isSyncingCloud.value = true
            kotlinx.coroutines.delay(1200) // Beautiful dynamic latency
            lastCloudSyncTime.value = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            isSyncingCloud.value = false
        }
    }

    fun loginSimulated(mode: String, email: String) {
        userLoginMode.value = mode
        userEmail.value = email
        triggerSimulatedSync()
    }

    fun logoutSimulated() {
        userLoginMode.value = "Guest Mode"
        userEmail.value = ""
        lastCloudSyncTime.value = "Never"
    }

    // --- Gemini intelligent APIs ---
    fun callAIGrammarImprovement() {
        val textStr = selectedDocument.value?.content ?: return
        viewModelScope.launch {
            aiChatLoading.value = true
            // Low latency proofreading call
            val result = GeminiService.processText(
                prompt = "Review and proofread/improve the grammar of this text. Return ONLY the improved version with no explanations:\n\n$textStr",
                model = "gemini-3.1-flash-lite-preview"
            )
            updateSelectedDocContent(result)
            aiChatLoading.value = false
        }
    }

    fun callAISummarization(onSummaryGenerated: (String) -> Unit) {
        val textStr = selectedDocument.value?.content ?: return
        viewModelScope.launch {
            aiChatLoading.value = true
            val result = GeminiService.processText(
                prompt = "Summarize the key takeaways of the following office text in 3 concise bullet points:\n\n$textStr",
                model = "gemini-3.1-flash-lite-preview"
            )
            onSummaryGenerated(result)
            aiChatLoading.value = false
        }
    }

    fun callAITranslate(targetLang: String) {
        val textStr = selectedDocument.value?.content ?: return
        viewModelScope.launch {
            aiChatLoading.value = true
            val result = GeminiService.processText(
                prompt = "Translate this text directly and professionally into $targetLang. Keep headings or lines formatted as they are. Output ONLY translation:\n\n$textStr",
                model = "gemini-3.1-flash-lite-preview"
            )
            updateSelectedDocContent(result)
            aiChatLoading.value = false
        }
    }

    fun callAIRewrite(tone: String) {
        val textStr = selectedDocument.value?.content ?: return
        viewModelScope.launch {
            aiChatLoading.value = true
            val result = GeminiService.processText(
                prompt = "Rewrite this office document in a clear, $tone tone. Output ONLY the rewritten text with no surrounding introductory remarks:\n\n$textStr",
                model = "gemini-3.1-flash-lite-preview"
            )
            updateSelectedDocContent(result)
            aiChatLoading.value = false
        }
    }

    // --- AI Image Generator & Controls ---
    fun generateAIImageForWorkspace(prompt: String, aspect: String) {
        viewModelScope.launch {
            isImageGenerating.value = true
            imageAspectRatio.value = aspect
            generatedImageB64.value = null

            // Call high-quality image model 'gemini-3.1-flash-image-preview'
            val b64 = GeminiService.generateImage(prompt, aspect, "gemini-3.1-flash-image-preview")
            if (b64 != null) {
                generatedImageB64.value = b64
                // Embed inside current document if active
                selectedDocument.value?.let { doc ->
                    if (doc.type == "document" || doc.type == "note") {
                        val markdownImageEmbed = "\n\n![Generated AI Art Aspect-$aspect](data:image/jpeg;base64,$b64)\n\n"
                        updateSelectedDocContent(doc.content + markdownImageEmbed)
                    }
                }
            } else {
                // Return fallback locally generated visual pattern (gradient) if offline/timed-out
                val mockB64 = getMockImgBase64()
                generatedImageB64.value = mockB64
            }
            isImageGenerating.value = false
        }
    }

    // Dynamic base64 placeholder for local offline fallback
    private fun getMockImgBase64(): String {
        return "iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAAABmJLR0QA/wD/AP+gvaeTAAAAI0lEQVR42u3BAQ0AAADCoPdPbQ8HFAAAAAAAAAAAAAAAAAAAeBvIdAAB9n7hEQAAAABJRU5ErkJggg=="
    }

    // --- Veo 3 Video Generator Model ---
    fun triggerVeoVideoSynthesis(prompt: String, aspect: String) {
        viewModelScope.launch {
            isVideoGenerating.value = true
            generatedVideoOp.value = null
            videoAspectRatio.value = aspect

            // Calling veo-3.1-fast-generate-preview Model
            val opName = GeminiService.generateVideo(prompt, aspect, "veo-3.1-fast-generate-preview")
            generatedVideoOp.value = opName
            isVideoGenerating.value = false
        }
    }

    // --- Document Chatbot Intelligence ---
    fun callChatbotResponse(userText: String) {
        val currentDoc = selectedDocument.value
        val docContext = if (currentDoc != null) {
            "Active document standard context (${currentDoc.type}):\nTitle: ${currentDoc.title}\nContent:\n${currentDoc.content}\n\n"
        } else {
            "No document is currently active."
        }

        val messagesList = chatMessages.value.toMutableList()
        messagesList.add(userText to true)
        chatMessages.value = messagesList

        viewModelScope.launch {
            aiChatLoading.value = true
            // Map chat messages format for model matching Part/Content
            val conversationalHistory = messagesList.takeLast(10).map { msg ->
                Content(parts = listOf(Part(text = msg.first)))
            }

            val systemInstructionMsg = "You are MaxOffice AI Assistant. You specialize in aiding the user with their office documents, checklists, formulas, and presentations. Underneath is context from their active document workspace. Answer concisely, informatively and in a highly productive tone.\n\n$docContext"

            val response = GeminiService.chat(conversationalHistory, systemInstructionMsg, "gemini-3.5-flash")
            val updatedHistory = chatMessages.value.toMutableList()
            updatedHistory.add(response to false)
            chatMessages.value = updatedHistory
            aiChatLoading.value = false
        }
    }

    fun clearChatHistory() {
        chatMessages.value = listOf(
            "Hello! Chat history cleared. Select a document above to ground my answers!" to false
        )
    }

    // Utilities
    fun exportToPdfSimulated(): String {
        val doc = selectedDocument.value ?: return "No document loaded"
        return """
            =========================================
            MAXOFFICE PDF EXPORT: ${doc.title.uppercase()}
            Format Code: ${doc.type.uppercase()}
            Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
            =========================================
            
            ${doc.content}
            
            -------------------------
            Verified offline under encryption token: ${doc.isLocalEncrypted}
            Local signature present: ${pdfSignatureEncoded.value ?: "None"}
            -------------------------
            End of PDF Export.
        """.trimIndent()
    }
}
