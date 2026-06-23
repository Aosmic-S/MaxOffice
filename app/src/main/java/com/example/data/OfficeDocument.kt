package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "office_documents")
data class OfficeDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val type: String, // "document", "spreadsheet", "presentation", "note", "pdf"
    val lastModified: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isTrash: Boolean = false,
    val tags: String = "", // Comma-separated list of tags
    val accentColor: String = "#3F51B5", // Hex code for custom branding
    val themeName: String = "Modern Light", // Active styling theme
    val speakerNotes: String = "", // Presentational speaker notes
    val isLocalEncrypted: Boolean = false
)
