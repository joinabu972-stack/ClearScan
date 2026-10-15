package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val folder: String = "Documents",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val notes: String = ""
)

@Entity(
    tableName = "document_pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long = 0,
    val pageIndex: Int,
    val originalImagePath: String,
    val processedImagePath: String,
    val filterType: String = FilterType.MAGIC.name,
    val rotationDegrees: Int = 0,
    val extractedText: String = ""
)

data class DocumentWithPages(
    @Embedded val document: DocumentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val pages: List<PageEntity>
) {
    val pageCount: Int get() = pages.size
    val firstPagePreviewPath: String? get() = pages.minByOrNull { it.pageIndex }?.processedImagePath
}
