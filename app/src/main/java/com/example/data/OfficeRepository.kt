package com.example.data

import kotlinx.coroutines.flow.Flow

class OfficeRepository(private val officeDao: OfficeDao) {
    val activeDocuments: Flow<List<OfficeDocument>> = officeDao.getAllActiveDocuments()
    val trashDocuments: Flow<List<OfficeDocument>> = officeDao.getTrashDocuments()

    fun getDocumentById(id: Int): Flow<OfficeDocument?> = officeDao.getDocumentById(id)

    suspend fun insert(document: OfficeDocument): Long = officeDao.insertDocument(document)

    suspend fun update(document: OfficeDocument) = officeDao.updateDocument(document)

    suspend fun delete(document: OfficeDocument) = officeDao.deleteDocument(document)

    suspend fun deleteById(id: Int) = officeDao.deleteDocumentById(id)

    suspend fun updateTrashStatus(id: Int, isTrash: Boolean) = officeDao.updateTrashStatus(id, isTrash)

    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean) = officeDao.updateFavoriteStatus(id, isFavorite)

    suspend fun emptyTrash() = officeDao.emptyTrash()
}
