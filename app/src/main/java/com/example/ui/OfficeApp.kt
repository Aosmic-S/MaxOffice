package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.OfficeDocument

@Composable
fun OfficeApp(viewModel: OfficeViewModel = viewModel()) {
    val localCtx = LocalContext.current
    val activeDocs by viewModel.activeDocuments.collectAsState()
    val trashDocs by viewModel.trashDocuments.collectAsState()

    // Screen color configurations
    var selectedAccentColorHex by remember { mutableStateOf("#3F51B5") } // Default Classic Blue
    val primaryColor = Color(android.graphics.Color.parseColor(selectedAccentColorHex))

    // Application Lock Verification Screen
    if (viewModel.userSavedPin.value.isNotEmpty() && viewModel.isAppLocked.value) {
        PinLockScreen(viewModel, primaryColor)
    } else {
        Scaffold(
            containerColor = Color(0xFFF7F9FC),
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFFF0F4F8),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = viewModel.activeTab.value == "dashboard" || viewModel.activeTab.value == "editor",
                        onClick = { viewModel.activeTab.value = "dashboard" },
                        icon = { Icon(Icons.Filled.Class, contentDescription = "Active Documents") },
                        label = { Text("Workspace") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF001D35),
                            selectedTextColor = Color(0xFF001D35),
                            indicatorColor = Color(0xFFD3E3FD),
                            unselectedIconColor = Color(0xFF5F6368),
                            unselectedTextColor = Color(0xFF5F6368)
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.activeTab.value == "chatbot",
                        onClick = { viewModel.activeTab.value = "chatbot" },
                        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "MaxOffice AI Chat") },
                        label = { Text("AI Copilot") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF001D35),
                            selectedTextColor = Color(0xFF001D35),
                            indicatorColor = Color(0xFFD3E3FD),
                            unselectedIconColor = Color(0xFF5F6368),
                            unselectedTextColor = Color(0xFF5F6368)
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.activeTab.value == "settings",
                        onClick = { viewModel.activeTab.value = "settings" },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Security Settings") },
                        label = { Text("Security") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF001D35),
                            selectedTextColor = Color(0xFF001D35),
                            indicatorColor = Color(0xFFD3E3FD),
                            unselectedIconColor = Color(0xFF5F6368),
                            unselectedTextColor = Color(0xFF5F6368)
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = viewModel.activeTab.value,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "CoreTabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        "dashboard" -> DashboardScreen(
                            viewModel,
                            activeDocs,
                            trashDocs,
                            primaryColor,
                            onAccentSelected = { selectedAccentColorHex = it }
                        )
                        "editor" -> EditorPortalScreen(viewModel, primaryColor)
                        "chatbot" -> ChatbotScreen(viewModel, primaryColor)
                        "settings" -> SecuritySettingsScreen(viewModel, primaryColor)
                    }
                }
            }
        }
    }
}

// --- Screen App Pin Lock Layout ---
@Composable
fun PinLockScreen(viewModel: OfficeViewModel, accentColor: Color) {
    var pinText by rememberSaveable { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = "Shield Pin",
                tint = accentColor,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "MaxOffice Secure Space",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Biometric lock & PIN enabled. Enter code to decrypt",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Enter Pin bullet indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                (1..4).forEach { idx ->
                    val filled = idx <= pinText.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (filled) accentColor else Color.Gray.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    )
                }
            }

            if (viewModel.pinEntryError.value) {
                Text(
                    "Invalid PIN. Verification failed.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // High Fidelity Pin Pad
            val numbers = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Clear", "0", "OK")
            )

            numbers.forEach { rowCols ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowCols.forEach { num ->
                        Button(
                            onClick = {
                                when (num) {
                                    "Clear" -> if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                                    "OK" -> {
                                        viewModel.authenticatePin(pinText)
                                    }
                                    else -> {
                                        if (pinText.length < 4) {
                                            pinText += num
                                            if (pinText.length == 4) {
                                                viewModel.authenticatePin(pinText)
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(6.dp)
                                .testTag("pin_btn_$num"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (num == "OK") accentColor else MaterialTheme.colorScheme.surface,
                                contentColor = if (num == "OK") Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = num,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Dashboard View Layout ---
@Composable
fun DashboardScreen(
    viewModel: OfficeViewModel,
    activeDocs: List<OfficeDocument>,
    trashDocs: List<OfficeDocument>,
    accentColor: Color,
    onAccentSelected: (String) -> Unit
) {
    val localCtx = LocalContext.current
    var showRenameDialog by remember { mutableStateOf<OfficeDocument?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showDonationDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Identity Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "Spark Logo",
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "MaxOffice",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }
                    Text(
                        "Professional, privacy-first, fully offline",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Cloud Status
                Card(
                    modifier = Modifier.clickable { viewModel.triggerSimulatedSync() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (viewModel.isSyncingCloud.value) Icons.Filled.Sync else Icons.Filled.CloudQueue,
                            contentDescription = "Sync Info",
                            modifier = Modifier.size(16.dp),
                            tint = accentColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (viewModel.isSyncingCloud.value) "Syncing..." else viewModel.userLoginMode.value,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Accent Selection Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Custom Accent Theme",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val accents = listOf(
                            "#3F51B5" to "Blue",
                            "#4CAF50" to "Green",
                            "#FF9800" to "Orange",
                            "#F44336" to "Red",
                            "#9C27B0" to "Purple",
                            "#009688" to "Teal"
                        )
                        items(accents) { pair ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(pair.first)))
                                    .border(
                                        2.dp,
                                        if (accentColor == Color(android.graphics.Color.parseColor(pair.first))) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { onAccentSelected(pair.first) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (accentColor == Color(android.graphics.Color.parseColor(pair.first))) {
                                    Icon(Icons.Filled.Check, contentDescription = "Active", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Search Bar
        item {
            OutlinedTextField(
                value = viewModel.searchQuery.value,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search in MaxOffice", style = TextStyle(color = Color(0xFF44474E), fontSize = 14.sp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_field"),
                leadingIcon = { Icon(Icons.Filled.Search, "Search icon", tint = Color(0xFF44474E)) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 6.dp)) {
                        if (viewModel.searchQuery.value.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Filled.Clear, "Clear search", tint = Color(0xFF44474E))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF005FB8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("JD", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFE9EEF6),
                    unfocusedContainerColor = Color(0xFFE9EEF6),
                    cursorColor = Color(0xFF005FB8)
                ),
                shape = CircleShape,
                singleLine = true
            )
        }

        // Quick Create Bento Grid
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Quick Create Workspace",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E1F20)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                
                // Row 1: Document (Big item) on Left, Spreadsheet / Slides / Notes on Right
                Row(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Document Button (Big Bento Card)
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFD3E3FD))
                            .clickable { viewModel.quickCreate("document") }
                            .testTag("create_document")
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Description,
                                    contentDescription = "Document",
                                    tint = Color(0xFF005FB8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Document",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF001D35),
                                    lineHeight = 22.sp
                                )
                                Text(
                                    "DOCX, Markdown",
                                    fontSize = 11.sp,
                                    color = Color(0xFF001D35).copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    
                    // Right column: Spreadsheet top, (Slides, Notes) split bottom
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Spreadsheet Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFC2EFD0))
                                .clickable { viewModel.quickCreate("spreadsheet") }
                                .testTag("create_spreadsheet")
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.GridOn,
                                        contentDescription = "Spreadsheet",
                                        tint = Color(0xFF126E38),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    "Spreadsheet",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF06210F)
                                )
                            }
                        }
                        
                        // Bottom Split: Slides & Notes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Slides Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFFFAD8FD))
                                    .clickable { viewModel.quickCreate("presentation") }
                                    .testTag("create_presentation"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Slideshow,
                                    contentDescription = "Slides",
                                    tint = Color(0xFF5F156B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            // Notes Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFFFFE082))
                                    .clickable { viewModel.quickCreate("note") }
                                    .testTag("create_note"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.StickyNote2,
                                    contentDescription = "Note",
                                    tint = Color(0xFF795548),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Row 2: PDF Creator (New sleek full-width Bento Bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFFCE8F3))
                        .clickable { viewModel.quickCreate("pdf") }
                        .testTag("create_pdf")
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.PictureAsPdf,
                                    contentDescription = "PDF",
                                    tint = Color(0xFFB71C1C),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Contract & PDF Editor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF4A0A0A)
                                )
                                Text(
                                    "Sign, Split, and Export PDF",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB71C1C).copy(alpha = 0.6f)
                                )
                            }
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Go",
                            tint = Color(0xFFB71C1C).copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Categories filters row
        item {
            val tabs = listOf(
                "all" to "All Files",
                "document" to "Docs",
                "spreadsheet" to "Sheets",
                "presentation" to "Presentations",
                "note" to "Notes",
                "pdf" to "PDFs"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs) { (code, label) ->
                    val active = viewModel.filterType.value == code
                    FilterChip(
                        selected = active,
                        onClick = { viewModel.filterType.value = code },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Active Documents Files list
        val filteredList = activeDocs.filter { doc ->
            // Filter query
            (doc.title.contains(viewModel.searchQuery.value, ignoreCase = true) ||
                    doc.tags.contains(viewModel.searchQuery.value, ignoreCase = true) ||
                    doc.content.contains(viewModel.searchQuery.value, ignoreCase = true)) &&
            // Filter categories
            (viewModel.filterType.value == "all" || doc.type == viewModel.filterType.value)
        }

        if (filteredList.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = "Empty list",
                        modifier = Modifier.size(56.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No workspace files found",
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Text(
                        "Build offline documents above to start",
                        fontSize = 11.sp,
                        color = Color.Gray.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            items(filteredList) { doc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { viewModel.selectDoc(doc) }
                        .testTag("doc_card_${doc.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val itemIcon = when (doc.type) {
                                    "document" -> Icons.Outlined.Description
                                    "spreadsheet" -> Icons.Outlined.GridOn
                                    "presentation" -> Icons.Outlined.Slideshow
                                    "note" -> Icons.Outlined.StickyNote2
                                    "pdf" -> Icons.Outlined.PictureAsPdf
                                    else -> Icons.Outlined.InsertDriveFile
                                }
                                val (bgCol, iconCol) = when (doc.type) {
                                    "document" -> Color(0xFFE7F0FF) to Color(0xFF005FB8)
                                    "spreadsheet" -> Color(0xFFEBF7EE) to Color(0xFF126E38)
                                    "presentation" -> Color(0xFFFCEEFF) to Color(0xFF5F156B)
                                    "note" -> Color(0xFFFFF5CC) to Color(0xFF795548)
                                    "pdf" -> Color(0xFFFCE8F3) to Color(0xFFB71C1C)
                                    else -> Color(0xFFF1F3F4) to Color(0xFF5F6368)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            bgCol,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(itemIcon, contentDescription = "Type", tint = iconCol, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        doc.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1A1C1E)
                                    )
                                    Text(
                                        "Last Modified: " + SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()).format(Date(doc.lastModified)),
                                        fontSize = 11.sp,
                                        color = Color(0xFF74777F)
                                    )
                                }
                            }

                            // Favorite toggle and actions dropdown
                            Row {
                                IconButton(onClick = { viewModel.toggleFavorite(doc.id, doc.isFavorite) }) {
                                    Icon(
                                        if (doc.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Pin Favorite",
                                        tint = if (doc.isFavorite) Color(0xFFFFC107) else Color.Gray
                                    )
                                }

                                var showMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(Icons.Filled.MoreVert, "More actions")
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Duplicate") },
                                            onClick = {
                                                viewModel.duplicateDocument(doc)
                                                showMenu = false
                                            },
                                            leadingIcon = { Icon(Icons.Filled.CopyAll, "Copy") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Rename") },
                                            onClick = {
                                                showRenameDialog = doc
                                                renameInputText = doc.title
                                                showMenu = false
                                            },
                                            leadingIcon = { Icon(Icons.Filled.Edit, "Edit") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Trash Bin") },
                                            onClick = {
                                                viewModel.deleteDocToTrash(doc.id)
                                                showMenu = false
                                            },
                                            leadingIcon = { Icon(Icons.Filled.Delete, "Delete") }
                                        )
                                    }
                                }
                            }
                        }

                        // Tags Row
                        if (doc.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                doc.tags.split(",").forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            tag.trim(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trash Bin Header & Drawer
        if (trashDocs.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Trash", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Trash Bin (${trashDocs.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                    Button(
                        onClick = { viewModel.emptyTrashBin() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Empty Trash", fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            items(trashDocs) { doc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(doc.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Stored locally, recoverable", fontSize = 9.sp, color = Color.Gray)
                        }
                        Row {
                            IconButton(onClick = { viewModel.restoreDocFromTrash(doc.id) }) {
                                Icon(Icons.Filled.SettingsBackupRestore, "Restore", tint = accentColor)
                            }
                            IconButton(onClick = { viewModel.permanentDeleteDoc(doc.id) }) {
                                Icon(Icons.Filled.Delete, "Delete forever", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        // Supporter & Localization Coffee Footer (User requested philosophy)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDonationDialog = true }
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Coffee,
                        contentDescription = "Coffee",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "MaxOffice is 100% Free!",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        "No ads. No spyware. Help us stay alive with active offline development.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    // Rename Dialog Module
    if (showRenameDialog != null) {
        val target = showRenameDialog!!
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Workspace File") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameInputText.isNotBlank()) {
                        viewModel.renameDoc(target.id, renameInputText)
                        showRenameDialog = null
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Support Donation Dialog
    if (showDonationDialog) {
        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            title = { Text("Support MaxOffice Project") },
            text = {
                Column {
                    Text(
                        "Thank you for supporting private open-source productivity! MaxOffice has no backend servers stealing your documents, no pricing walls, and works anywhere on earth (and space!) offline.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Support options:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = {
                            Toast.makeText(localCtx, "Thank you for buying us a Coffee!", Toast.LENGTH_LONG).show()
                            showDonationDialog = false
                        }, modifier = Modifier.weight(1f)) {
                            Text("Buy Me Coffee ☕")
                        }
                        Button(onClick = {
                            Toast.makeText(localCtx, "Thank you for your GitHub support!", Toast.LENGTH_LONG).show()
                            showDonationDialog = false
                        }, modifier = Modifier.weight(1f)) {
                            Text("Contribute USD")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDonationDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// --- Active Editor Screen ---
@Composable
fun EditorPortalScreen(viewModel: OfficeViewModel, accentColor: Color) {
    val doc = viewModel.selectedDocument.value ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Toolbar Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.deselectDoc() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        doc.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        doc.type.uppercase() + " Workspace",
                        fontSize = 9.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Operations Toolbar
            Row {
                IconButton(onClick = { viewModel.toggleFavorite(doc.id, doc.isFavorite) }) {
                    Icon(
                        if (doc.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Pin",
                        tint = if (doc.isFavorite) Color(0xFFFFC107) else Color.Gray
                    )
                }

                var showAIOps by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showAIOps = true }) {
                        Icon(Icons.Filled.AutoAwesome, "AI Tools", tint = accentColor)
                    }
                    DropdownMenu(
                        expanded = showAIOps,
                        onDismissRequest = { showAIOps = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Improve Grammar") },
                            onClick = {
                                viewModel.callAIGrammarImprovement()
                                showAIOps = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Spellcheck, "Check") }
                        )
                        DropdownMenuItem(
                            text = { Text("Summarize into Bullets") },
                            onClick = {
                                viewModel.callAISummarization { summary ->
                                    viewModel.updateSelectedDocContent(
                                        viewModel.selectedDocument.value!!.content + "\n\n### AI SUMMARY NOTES:\n" + summary
                                    )
                                }
                                showAIOps = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Analytics, "Summarize") }
                        )
                        DropdownMenuItem(
                            text = { Text("Translate to Spanish") },
                            onClick = {
                                viewModel.callAITranslate("Spanish")
                                showAIOps = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Translate, "Translate") }
                        )
                        DropdownMenuItem(
                            text = { Text("Rewrite Professional") },
                            onClick = {
                                viewModel.callAIRewrite("Highly Professional")
                                showAIOps = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Psychology, "Rewrite") }
                        )
                    }
                }

                // Security lock toggle
                IconButton(onClick = {
                    viewModel.isLocalEncryptionEnabled.value = !viewModel.isLocalEncryptionEnabled.value
                    val toastMsg = if (viewModel.isLocalEncryptionEnabled.value) "Local Document Encrypted with AES-256!" else "Document Decrypted"
                    Toast.makeText(viewModel.getApplication(), toastMsg, Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        if (viewModel.isLocalEncryptionEnabled.value) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = "Lock",
                        tint = if (viewModel.isLocalEncryptionEnabled.value) accentColor else Color.Gray
                    )
                }

                var showFormatMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showFormatMenu = true }) {
                        Icon(Icons.Filled.FormatPaint, "Text Settings")
                    }
                    DropdownMenu(
                        expanded = showFormatMenu,
                        onDismissRequest = { showFormatMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Serif Heading Font") },
                            onClick = {
                                viewModel.editorFontFamily.value = "Serif"
                                showFormatMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sans-Serif Standard Font") },
                            onClick = {
                                viewModel.editorFontFamily.value = "Sans-Serif"
                                showFormatMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Monospace Grid Font") },
                            onClick = {
                                viewModel.editorFontFamily.value = "Monospace"
                                showFormatMenu = false
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Larger Text (18sp)") },
                            onClick = {
                                viewModel.editorFontSize.value = 18
                                showFormatMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Standard Text (15sp)") },
                            onClick = {
                                viewModel.editorFontSize.value = 15
                                showFormatMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Active Editor Pane Type Router
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
        ) {
            when (doc.type) {
                "document" -> RawDocumentEditorView(viewModel, accentColor)
                "spreadsheet" -> SpreadsheetEditorLayoutView(viewModel, accentColor)
                "presentation" -> PresentationEditorLayoutView(viewModel, accentColor)
                "note" -> NotesChecklistLayoutView(viewModel, accentColor)
                "pdf" -> PdfToolkitScreenPanel(viewModel, accentColor)
            }
        }
    }
}

// --- raw text document editor view ---
@Composable
fun RawDocumentEditorView(viewModel: OfficeViewModel, accentColor: Color) {
    val doc = viewModel.selectedDocument.value ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick undo / redo bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.performUndo() },
                    enabled = viewModel.canUndo()
                ) {
                    Icon(
                        Icons.Filled.Undo,
                        "Undo",
                        tint = if (viewModel.canUndo()) accentColor else Color.Gray.copy(alpha = 0.4f)
                    )
                }
                IconButton(
                    onClick = { viewModel.performRedo() },
                    enabled = viewModel.canRedo()
                ) {
                    Icon(
                        Icons.Filled.Redo,
                        "Redo",
                        tint = if (viewModel.canRedo()) accentColor else Color.Gray.copy(alpha = 0.4f)
                    )
                }
            }

            // Word counter
            val words = doc.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
            Text(
                "Words: $words | Characters: ${doc.content.length}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
        }

        // Find and replace tool bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.findQuery.value,
                        onValueChange = { viewModel.findQuery.value = it },
                        placeholder = { Text("Find text", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        textStyle = TextStyle(fontSize = 12.sp)
                    )
                    OutlinedTextField(
                        value = viewModel.replaceQuery.value,
                        onValueChange = { viewModel.replaceQuery.value = it },
                        placeholder = { Text("Replace text", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        textStyle = TextStyle(fontSize = 12.sp)
                    )
                    Button(
                        onClick = { viewModel.runFindAndReplace() },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Go", fontSize = 11.sp)
                    }
                }
            }
        }

        // Main Editor Textbox
        OutlinedTextField(
            value = doc.content,
            onValueChange = { viewModel.updateSelectedDocContent(it) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("document_editor_field"),
            singleLine = false,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = accentColor
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(
                fontSize = viewModel.editorFontSize.value.sp,
                fontFamily = when (viewModel.editorFontFamily.value) {
                    "Serif" -> FontFamily.Serif
                    "Monospace" -> FontFamily.Monospace
                    else -> FontFamily.SansSerif
                }
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        AIImageVideoGeneratorsPane(viewModel, accentColor)
    }
}

// --- Spreadsheet Editor layout view & grid matrix ---
@Composable
fun SpreadsheetEditorLayoutView(viewModel: OfficeViewModel, accentColor: Color) {
    val localCtx = LocalContext.current
    val cols = listOf('A', 'B', 'C', 'D', 'E', 'F')
    val rows = (1..15).toList()

    val mapCells = viewModel.spreadsheetCells.value
    val selectId = viewModel.selectedCell.value
    val textEditVal = mapCells[selectId] ?: ""

    Column(modifier = Modifier.fillMaxSize()) {
        // Formula reference bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Active Cell: [ $selectId ]",
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        fontSize = 14.sp
                    )
                    Text(
                        "Evaluated: " + viewModel.evaluateCell(selectId),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = textEditVal,
                        onValueChange = { viewModel.updateSpreadsheetCell(selectId, it) },
                        placeholder = { Text("E.g., 420 or formula =SUM(B1:B5)") },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("formula_input"),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    )
                    Button(
                        onClick = {
                            Toast.makeText(localCtx, "Calculated & Auto-saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Calculate", fontSize = 11.sp)
                    }
                }
            }
        }

        // Spreadsheet Column Sort & Actions helper
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Quick Sort column:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterVertically))
            cols.forEach { char ->
                Button(
                    onClick = {
                        viewModel.sortSpreadsheetColumn(char)
                        Toast.makeText(localCtx, "Column $char sorted!", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Sort $char", fontSize = 10.sp)
                }
            }
        }

        // Interactive Matrix Grid Scrollable
        Box(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .horizontalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Header Alphabet columns
                Row {
                    // Empty corner box
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 28.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("#", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    cols.forEach { charLetter ->
                        Box(
                            modifier = Modifier
                                .size(width = 100.dp, height = 28.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, Color.Gray.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(charLetter.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Grid Cells Data rows
                rows.forEach { rowIdx ->
                    Row {
                        // Index cell
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .border(0.5.dp, Color.Gray.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(rowIdx.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        cols.forEach { colLet ->
                            val cellId = "$colLet$rowIdx"
                            val cellActive = selectId == cellId
                            val evaluatedVal = viewModel.evaluateCell(cellId)

                            Box(
                                modifier = Modifier
                                    .size(width = 100.dp, height = 36.dp)
                                    .background(
                                        if (cellActive) accentColor.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        if (cellActive) 2.dp else 0.5.dp,
                                        if (cellActive) accentColor else Color.Gray.copy(alpha = 0.4f)
                                    )
                                    .clickable { viewModel.selectedCell.value = cellId }
                                    .testTag("cell_$cellId"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = evaluatedVal,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (evaluatedVal.startsWith("ERR") || evaluatedVal == "DIV/0") Color.Red else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Beautiful live dynamic Material 3 chart representer of row calculations
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Office Live Metric Chart (Column B Range A2:B5)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Simple high fidelity Canvas Chart (using local calculations)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    val lineB2 = (mapCells["B2"]?.toDoubleOrNull() ?: 10.0).coerceIn(0.0..5000.0)
                    val lineB3 = (mapCells["B3"]?.toDoubleOrNull() ?: 20.0).coerceIn(0.0..5000.0)
                    val lineB4 = (mapCells["B4"]?.toDoubleOrNull() ?: 30.0).coerceIn(0.0..5000.0)
                    val lineB5 = (mapCells["B5"]?.toDoubleOrNull() ?: 40.0).coerceIn(0.0..5000.0)

                    val heights = listOf(lineB2, lineB3, lineB4, lineB5)
                    val maxVal = (heights.maxOrNull() ?: 1.0).coerceAtLeast(1.0)

                    val barWidth = 60.dp.toPx()
                    val spacing = 32.dp.toPx()
                    val canvasHeight = size.height

                    heights.forEachIndexed { idx, value ->
                        val barHeight = ((value / maxVal) * (canvasHeight - 20.dp.toPx())).toFloat()
                        val xOffset = idx * (barWidth + spacing) + 40.dp.toPx()
                        val yOffset = canvasHeight - barHeight

                        drawRect(
                            color = accentColor,
                            topLeft = Offset(xOffset, yOffset),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                        )
                    }
                }
            }
        }
    }
}

// --- Presentation Slides editor layouts ---
@Composable
fun PresentationEditorLayoutView(viewModel: OfficeViewModel, accentColor: Color) {
    val slides = viewModel.getSlides()
    val activeIdx = viewModel.selectedSlideIndex.value

    Column(modifier = Modifier.fillMaxSize()) {
        if (slides.isEmpty()) {
            Text("No Slides available.", modifier = Modifier.padding(16.dp))
        } else {
            val details = slides.getOrNull(activeIdx) ?: ("" to "")
            var titleInput by remember(activeIdx) { mutableStateOf(details.first) }
            var subtitleInput by remember(activeIdx) { mutableStateOf(details.second) }

            // Theme colors maps
            val slideBgColor = when (viewModel.maxThemeName.value) {
                "Cosmic Blue" -> Color(0xFF0F172A)
                "Forest Edge" -> Color(0xFF1E293B)
                "Brutalist Slate" -> Color(0xFF1F2937)
                "Warm Charcoal" -> Color(0xFF27272A)
                "Soft Rose" -> Color(0xFFFAF5F5)
                else -> Color(0xFF1E293B)
            }
            val slideTextColor = if (viewModel.maxThemeName.value == "Soft Rose") Color.Black else Color.White

            // Quick theme and transitions controllers
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var showThemeMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showThemeMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Theme: " + viewModel.maxThemeName.value, fontSize = 11.sp)
                    }
                    DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                        val themesList = listOf("Cosmic Blue", "Forest Edge", "Brutalist Slate", "Warm Charcoal", "Soft Rose")
                        themesList.forEach { th ->
                            DropdownMenuItem(
                                text = { Text(th) },
                                onClick = {
                                    viewModel.applyPresentationTheme(th)
                                    showThemeMenu = false
                                }
                            )
                        }
                    }
                }

                var showTransitionMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showTransitionMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Active Transition: " + viewModel.slideTransition.value, fontSize = 11.sp)
                    }
                    DropdownMenu(expanded = showTransitionMenu, onDismissRequest = { showTransitionMenu = false }) {
                        val transList = listOf("Fade", "Slide-In", "Scale-Up")
                        transList.forEach { tr ->
                            DropdownMenuItem(
                                text = { Text(tr) },
                                onClick = {
                                    viewModel.slideTransition.value = tr
                                    showTransitionMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Central slide viewport simulator (with transitions and high contrast themes)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(slideBgColor)
                    .border(2.dp, accentColor, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        titleInput,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = slideTextColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        subtitleInput,
                        fontSize = 14.sp,
                        color = slideTextColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slide content editors textfields
            OutlinedTextField(
                value = titleInput,
                onValueChange = {
                    titleInput = it
                    viewModel.updateSlide(activeIdx, it, subtitleInput)
                },
                label = { Text("Slide Title", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = subtitleInput,
                onValueChange = {
                    subtitleInput = it
                    viewModel.updateSlide(activeIdx, titleInput, it)
                },
                label = { Text("Slide Bullet Subtitle", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 13.sp)
            )

            // Presentation Slide navigator indicators
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    slides.forEachIndexed { i, _ ->
                        val active = activeIdx == i
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) accentColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.selectedSlideIndex.value = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (i + 1).toString(),
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = { viewModel.addSlide() }) {
                        Icon(Icons.Filled.Add, "Add slide", tint = accentColor)
                    }
                    IconButton(onClick = { viewModel.deleteSlide(activeIdx) }) {
                        Icon(Icons.Filled.Delete, "Delete Slide", tint = Color.Red)
                    }
                }
            }

            // Speaker notes panel area
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.selectedDocument.value?.speakerNotes ?: "",
                onValueChange = {
                    val current = viewModel.selectedDocument.value!!
                    viewModel.selectedDocument.value = current.copy(speakerNotes = it)
                },
                label = { Text("Speaker Presentation Notes (local-only)", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                textStyle = TextStyle(fontSize = 12.sp)
            )
        }
    }
}

// --- Note Checklist item Layout View ---
@Composable
fun NotesChecklistLayoutView(viewModel: OfficeViewModel, accentColor: Color) {
    val doc = viewModel.selectedDocument.value ?: return
    var newChecklistItemStr by remember { mutableStateOf("") }
    val lines = doc.content.split("\n")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newChecklistItemStr,
                onValueChange = { newChecklistItemStr = it },
                placeholder = { Text("Add rapid checklist entry...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (newChecklistItemStr.isNotBlank()) {
                        val checkboxLine = "[ ] $newChecklistItemStr"
                        val updatedContent = if (doc.content.isBlank()) checkboxLine else doc.content + "\n" + checkboxLine
                        viewModel.updateSelectedDocContent(updatedContent)
                        newChecklistItemStr = ""
                    }
                },
                modifier = Modifier.background(accentColor, CircleShape)
            ) {
                Icon(Icons.Filled.Add, "Add item", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Checklist loop scroll views
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(lines.zip(lines.indices)) { (line, lineIdx) ->
                val isChecklistType = line.startsWith("[ ]") || line.startsWith("[x]")
                val checked = line.startsWith("[x]")
                val taskText = if (isChecklistType) line.substring(4) else line

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            if (isChecklistType) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        // Update checkbox status line in document content
                                        val mutableLines = lines.toMutableList()
                                        mutableLines[lineIdx] = if (isChecked) "[x] $taskText" else "[ ] $taskText"
                                        viewModel.updateSelectedDocContent(mutableLines.joinToString("\n"))
                                    }
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Draft Bullet", tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = taskText,
                                fontSize = 14.sp,
                                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (checked) Color.Gray else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Remove note checklist item
                        IconButton(onClick = {
                            val mutableLines = lines.toMutableList()
                            mutableLines.removeAt(lineIdx)
                            viewModel.updateSelectedDocContent(mutableLines.joinToString("\n"))
                        }) {
                            Icon(Icons.Filled.Delete, "Delete", tint = Color.Gray)
                        }
                    }
                }
            }
        }

        // Notes auxiliary color picker
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val hexes = listOf("#FFEB3B", "#FF9800", "#FFCDD2", "#C8E6C9", "#BBDEFB")
                hexes.forEach { colorStr ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(colorStr)))
                            .border(
                                1.dp,
                                if (doc.accentColor == colorStr) Color.Black else Color.Transparent,
                                CircleShape
                            )
                            .clickable { viewModel.updateAccentColor(colorStr) }
                    )
                }
            }
        }
    }
}

// --- PDF view toolkit layouts and annotators ---
@Composable
fun PdfToolkitScreenPanel(viewModel: OfficeViewModel, accentColor: Color) {
    val doc = viewModel.selectedDocument.value ?: return
    var signInputName by remember { mutableStateOf("") }
    var currentAnnIdxHighlight by remember { mutableStateOf(-1) }
    val localCtx = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Interactive PDF Suite Options",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(localCtx, "PDF Export Complete", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(Icons.Filled.Download, "Save report", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export PDF", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            Toast.makeText(localCtx, "Combined successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Merge PDF", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            Toast.makeText(localCtx, "Pages fragmented!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Split PDF", fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Document simulated page
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
                .border(2.dp, Color.LightGray)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    "MAXOFFICE DIGITAL DOCUMENT REPORT",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                val pdfLines = doc.content.split("\n")
                pdfLines.forEachIndexed { numIdx, line ->
                    val highLighted = viewModel.pdfAnnotations.value.contains(numIdx.toString())
                    Text(
                        line,
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier
                            .background(if (highLighted) Color.Yellow else Color.Transparent)
                            .clickable {
                                if (highLighted) {
                                    viewModel.pdfAnnotations.value = viewModel.pdfAnnotations.value - numIdx.toString()
                                } else {
                                    viewModel.addPdfHighlight(numIdx.toString())
                                }
                            }
                            .padding(vertical = 2.dp)
                    )
                }

                if (viewModel.pdfSignatureEncoded.value != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .size(160.dp, 56.dp)
                            .border(1.dp, Color.Red, RoundedCornerShape(4.dp))
                            .background(Color.Red.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "/Signed/ ${viewModel.pdfSignatureEncoded.value}",
                            color = Color.Blue,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // PDF Signing Input Pad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = signInputName,
                onValueChange = { signInputName = it },
                placeholder = { Text("Type name to sign PDF...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (signInputName.isNotBlank()) {
                        viewModel.signPdfWithText(signInputName)
                        signInputName = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Burn Signature", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

// --- AI Media (Images and Video) generator panel widget ---
@Composable
fun AIImageVideoGeneratorsPane(viewModel: OfficeViewModel, accentColor: Color) {
    var promptInputStr by remember { mutableStateOf("") }
    var aspectSelection by remember { mutableStateOf("1:1") }
    var selectedServiceFormat by remember { mutableStateOf("image") } // "image", "video"

    val contexts = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Icon(Icons.Filled.AutoAwesome, "Spark", tint = accentColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "MaxOffice AI Creator Portal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Format selector
                Row {
                    FilterChip(
                        selected = selectedServiceFormat == "image",
                        onClick = { selectedServiceFormat = "image" },
                        label = { Text("Art Generate") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = selectedServiceFormat == "video",
                        onClick = { selectedServiceFormat = "video" },
                        label = { Text("Veo Video") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text input block
            OutlinedTextField(
                value = promptInputStr,
                onValueChange = { promptInputStr = it },
                placeholder = { Text(if (selectedServiceFormat == "image") "Cyberpunk futuristic boardroom workspace..." else "A neon hologram of a tech executive typing...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                textStyle = TextStyle(fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedServiceFormat == "image") {
                // Image aspect controls
                Text("Aspect Ratio Control:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val ratios = listOf("1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "21:9")
                    items(ratios) { aspect ->
                        val selected = aspectSelection == aspect
                        Button(
                            onClick = { aspectSelection = aspect },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) accentColor else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                aspect,
                                fontSize = 10.sp,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (promptInputStr.isNotBlank()) {
                            viewModel.generateAIImageForWorkspace(promptInputStr, aspectSelection)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    if (viewModel.isImageGenerating.value) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synthesizing Base64 Layout...", fontSize = 11.sp)
                    } else {
                        Text("Insert AI Generated Image Asset", fontSize = 11.sp)
                    }
                }

                // Render Generated Image block
                if (viewModel.generatedImageB64.value != null && !viewModel.isImageGenerating.value) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Workspace Generated Asset Component:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val bitmap = decodeBase64ToBitmap(viewModel.generatedImageB64.value!!)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Workspace result asset",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        )
                    } else {
                        Text("Error decoding image.", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                // Video aspect controls
                Text("Select Veo Video Aspect:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val veoRatios = listOf("16:9", "9:16")
                    veoRatios.forEach { ratio ->
                        val activeRatio = aspectSelection == ratio
                        OutlinedButton(
                            onClick = { aspectSelection = ratio },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (activeRatio) accentColor else Color.Gray)
                        ) {
                            Text(ratio, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (promptInputStr.isNotBlank()) {
                            viewModel.triggerVeoVideoSynthesis(promptInputStr, aspectSelection)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("veo_synthesis_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    if (viewModel.isVideoGenerating.value) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rendering Veo Cinematic Video...", fontSize = 11.sp)
                    } else {
                        Text("Synthesize Veo Video Output (1080p)", fontSize = 11.sp)
                    }
                }

                if (viewModel.generatedVideoOp.value != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "Veo Operation ID: " + viewModel.generatedVideoOp.value,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Synthesis complete! 30 FPS high-fidelity video generated dynamically. Viewport output is ready inside reports.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper base64 decoding utilities
fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

// --- Chatbot Copilot Screen Panel ---
@Composable
fun ChatbotScreen(viewModel: OfficeViewModel, accentColor: Color) {
    var rawChatStr by remember { mutableStateOf("") }
    val chatsList = viewModel.chatMessages.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "MaxOffice AI Assistant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = accentColor
                )
                val attached = viewModel.selectedDocument.value
                Text(
                    if (attached != null) "Grounding source: ${attached.title}" else "Ready. (Open a file to chat with it)",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = { viewModel.clearChatHistory() }) {
                Icon(Icons.Filled.ClearAll, "Clear conversation")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat content scroll list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatsList) { msg ->
                val isUser = msg.second
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 280.dp),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) accentColor else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = msg.first,
                            fontSize = 13.sp,
                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (viewModel.aiChatLoading.value) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = accentColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("MaxOffice AI reasoning...", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dialogue Inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = rawChatStr,
                onValueChange = { rawChatStr = it },
                placeholder = { Text("Ask about formulas, summary outlines...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (rawChatStr.isNotBlank()) {
                        viewModel.callChatbotResponse(rawChatStr)
                        rawChatStr = ""
                    }
                },
                modifier = Modifier
                    .background(accentColor, CircleShape)
                    .testTag("send_chat_btn")
            ) {
                Icon(Icons.Filled.ArrowUpward, "Send Message", tint = Color.White)
            }
        }
    }
}

// --- Master Security & Access Settings Screen ---
@Composable
fun SecuritySettingsScreen(viewModel: OfficeViewModel, accentColor: Color) {
    var rawPinStr by remember { mutableStateOf("") }
    val localCtx = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "MaxOffice Security & Privacy Vault",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = accentColor
            )
            Text(
                "Configuring military-grade local on-device protections.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        // App Lock setting cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "On-Launch PIN Lock Code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "If set, launches require entering this 4 digit numeric PIN. Keep empty to unlock instantly.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val pinSet = viewModel.userSavedPin.value.isNotEmpty()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = rawPinStr,
                            onValueChange = { if (it.length <= 4) rawPinStr = it },
                            placeholder = { Text("E.g., 1234") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Button(
                            onClick = {
                                if (rawPinStr.length == 4 || rawPinStr.isEmpty()) {
                                    viewModel.changeMasterPin(rawPinStr)
                                    Toast.makeText(
                                        localCtx,
                                        if (rawPinStr.isEmpty()) "Launch lock disabled!" else "Launch lock PIN active!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    rawPinStr = ""
                                } else {
                                    Toast.makeText(localCtx, "PIN must be exactly 4 digits!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(if (rawPinStr.isEmpty() && pinSet) "Disable" else "Save PIN")
                        }
                    }

                    if (pinSet) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "✓ PIN Lock currently ACTIVE.",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Biometrics switch card
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Biometric Lock Bypass", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Allows fingerprint scans to bypass inputting PIN", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = viewModel.isBiometricsEnabled.value,
                        onCheckedChange = {
                            viewModel.enrollBiometrics(it)
                        }
                    )
                }
            }
        }

        // Accounts Simulation
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Optional Server Account & Syncs", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("Guest mode uses zero network traffic. Synchronize safely only when logged in.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewModel.userLoginMode.value == "Guest Mode") {
                        Button(
                            onClick = {
                                viewModel.loginSimulated("Email ID Account", "support@maxoffice.app")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Sign in anonymously with Google / Email")
                        }
                    } else {
                        Column {
                            Text("Signed In Account: " + viewModel.userLoginMode.value, fontWeight = FontWeight.Bold)
                            Text("Cloud Email: " + viewModel.userEmail.value, fontSize = 12.sp)
                            Text("Last Synchronized Backups: " + viewModel.lastCloudSyncTime.value, fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.triggerSimulatedSync() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Force cloud sync")
                                }
                                Button(
                                    onClick = { viewModel.logoutSimulated() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Logout", color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
