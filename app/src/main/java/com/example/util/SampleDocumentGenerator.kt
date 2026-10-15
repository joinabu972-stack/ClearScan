package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.data.dao.DocumentDao
import com.example.data.model.DocumentEntity
import com.example.data.model.FilterType
import com.example.data.model.PageEntity

object SampleDocumentGenerator {

    suspend fun seedSampleDocumentsIfEmpty(context: Context, dao: DocumentDao) {
        if (dao.getDocumentCount() > 0) return

        val now = System.currentTimeMillis()

        // Document 1: Commercial Tax Invoice
        val invoiceBitmap1 = createSampleInvoiceBitmap(1)
        val invoiceBitmap2 = createSampleInvoiceBitmap(2)
        val pathInv1 = ImageProcessingUtils.saveBitmapToInternalStorage(context, invoiceBitmap1, "inv_p1")
        val pathInv2 = ImageProcessingUtils.saveBitmapToInternalStorage(context, invoiceBitmap2, "inv_p2")

        val invDocId = dao.insertDocument(
            DocumentEntity(
                title = "Tax Invoice #INV-2026-089",
                folder = "Invoices",
                createdAt = now - 3600_000 * 5,
                updatedAt = now - 3600_000 * 5,
                isFavorite = true,
                notes = "Hardware & Cloud hosting invoice for Q3. Tax deductable."
            )
        )
        dao.insertPages(
            listOf(
                PageEntity(
                    documentId = invDocId,
                    pageIndex = 0,
                    originalImagePath = pathInv1,
                    processedImagePath = pathInv1,
                    filterType = FilterType.MAGIC.name,
                    extractedText = "INVOICE #INV-2026-089\nBilling To: Apex Enterprise Ltd\nSubtotal: $1,450.00\nVAT (15%): $217.50\nTotal Due: $1,667.50\nStatus: PAID"
                ),
                PageEntity(
                    documentId = invDocId,
                    pageIndex = 1,
                    originalImagePath = pathInv2,
                    processedImagePath = pathInv2,
                    filterType = FilterType.MAGIC.name,
                    extractedText = "TERMS & CONDITIONS\nPayment was processed via Bank Transfer. License terms active until Oct 2027."
                )
            )
        )

        // Document 2: National ID Card
        val idCardFront = createSampleIdCardBitmap("FRONT")
        val idCardBack = createSampleIdCardBitmap("BACK")
        val pathIdFront = ImageProcessingUtils.saveBitmapToInternalStorage(context, idCardFront, "id_front")
        val pathIdBack = ImageProcessingUtils.saveBitmapToInternalStorage(context, idCardBack, "id_back")

        val idDocId = dao.insertDocument(
            DocumentEntity(
                title = "National ID & Driver License",
                folder = "ID Cards",
                createdAt = now - 3600_000 * 26,
                updatedAt = now - 3600_000 * 24,
                isFavorite = true,
                notes = "High-resolution color scan of official identification card."
            )
        )
        dao.insertPages(
            listOf(
                PageEntity(
                    documentId = idDocId,
                    pageIndex = 0,
                    originalImagePath = pathIdFront,
                    processedImagePath = pathIdFront,
                    filterType = FilterType.MAGIC.name,
                    extractedText = "CITIZEN IDENTITY CARD\nName: RAHMAN, ARIF\nDOB: 14 JAN 1994\nID No: 9842-1094-8210\nIssued: 12 AUG 2021"
                ),
                PageEntity(
                    documentId = idDocId,
                    pageIndex = 1,
                    originalImagePath = pathIdBack,
                    processedImagePath = pathIdBack,
                    filterType = FilterType.MAGIC.name,
                    extractedText = "ADDRESS & EMERGENCY CONTACT\nDistrict: Dhaka\nBlood Group: B+\nMagnetic Strip & Microchip verified."
                )
            )
        )

        // Document 3: Meeting Notes & Architecture
        val notesBitmap = createSampleNotesBitmap()
        val pathNotes = ImageProcessingUtils.saveBitmapToInternalStorage(context, notesBitmap, "notes_p1")
        val notesDocId = dao.insertDocument(
            DocumentEntity(
                title = "Product Strategy & Architecture",
                folder = "Notes",
                createdAt = now - 3600_000 * 72,
                updatedAt = now - 3600_000 * 72,
                isFavorite = false,
                notes = "Brainstorming session notes with diagram sketch."
            )
        )
        dao.insertPage(
            PageEntity(
                documentId = notesDocId,
                pageIndex = 0,
                originalImagePath = pathNotes,
                processedImagePath = pathNotes,
                filterType = FilterType.BLACK_WHITE.name,
                extractedText = "CLEARSCAN MOBILE ENGINE\n1. Edge detection algorithm\n2. Color balance & shadow removal\n3. High-DPI Vector PDF export\n4. Smart folder organization"
            )
        )
    }

    fun createSampleDocument(context: Context, sampleType: String): Bitmap {
        return when (sampleType.uppercase()) {
            "ID_CARD" -> createSampleIdCardBitmap("FRONT")
            "NOTES" -> createSampleNotesBitmap()
            "RECEIPT" -> createSampleReceiptBitmap()
            else -> createSampleInvoiceBitmap(1)
        }
    }

    private fun createSampleInvoiceBitmap(page: Int): Bitmap {
        val width = 800
        val height = 1100
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Subtle document paper background
        canvas.drawColor(Color.rgb(253, 253, 254))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header band
        paint.color = Color.rgb(2, 132, 199)
        canvas.drawRect(0f, 0f, width.toFloat(), 140f, paint)

        // Header text
        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.isFakeBoldText = true
        canvas.drawText("CLEARSCAN GLOBAL SERVICES", 50f, 65f, paint)

        paint.textSize = 20f
        paint.isFakeBoldText = false
        canvas.drawText("Official Tax Document • Page $page of 2", 50f, 105f, paint)

        // Invoice metadata card
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(RectF(50f, 170f, (width - 50).toFloat(), 280f), 12f, 12f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("Invoice To: Apex Technologies Corp.", 70f, 215f, paint)
        paint.textSize = 18f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Date: October 15, 2026   |   Invoice: #INV-2026-089", 70f, 250f, paint)

        // Table header
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(50f, 320f, (width - 50).toFloat(), 365f, paint)

        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("DESCRIPTION", 70f, 350f, paint)
        canvas.drawText("QTY", 450f, 350f, paint)
        canvas.drawText("RATE", 550f, 350f, paint)
        canvas.drawText("AMOUNT", 660f, 350f, paint)

        // Table items
        val items = if (page == 1) {
            listOf(
                Triple("Enterprise Cloud Server Subscription", "1", "$650.00"),
                Triple("SSL Multi-Domain Certificate", "2", "$200.00"),
                Triple("Database Replication Engine License", "1", "$400.00"),
                Triple("24/7 Dedicated Priority Support", "1", "$200.00")
            )
        } else {
            listOf(
                Triple("Disaster Recovery Cold Storage", "1", "$150.00"),
                Triple("Security Audit & Pen-Test Report", "1", "$500.00")
            )
        }

        var y = 410f
        paint.textSize = 17f
        paint.isFakeBoldText = false
        for (item in items) {
            paint.color = Color.rgb(51, 65, 85)
            canvas.drawText(item.first, 70f, y, paint)
            canvas.drawText(item.second, 460f, y, paint)
            canvas.drawText(item.third, 550f, y, paint)
            canvas.drawText(item.third, 660f, y, paint)

            paint.color = Color.rgb(241, 245, 249)
            canvas.drawLine(50f, y + 20f, (width - 50).toFloat(), y + 20f, paint)
            y += 65f
        }

        // Total box
        val totalBoxTop = y + 30f
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(400f, totalBoxTop, (width - 50).toFloat(), totalBoxTop + 130f), 10f, 10f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 18f
        canvas.drawText("Subtotal:", 430f, totalBoxTop + 40f, paint)
        canvas.drawText("$1,450.00", 640f, totalBoxTop + 40f, paint)

        canvas.drawText("VAT (15%):", 430f, totalBoxTop + 75f, paint)
        canvas.drawText("$217.50", 640f, totalBoxTop + 75f, paint)

        paint.textSize = 22f
        paint.isFakeBoldText = true
        paint.color = Color.rgb(2, 132, 199)
        canvas.drawText("TOTAL DUE:", 430f, totalBoxTop + 115f, paint)
        canvas.drawText("$1,667.50", 630f, totalBoxTop + 115f, paint)

        // Stamp badge
        paint.color = Color.rgb(16, 185, 129)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRoundRect(RectF(100f, height - 200f, 280f, height - 120f), 8f, 8f, paint)
        paint.style = Paint.Style.FILL
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("PAID", 145f, height - 150f, paint)

        // Barcode lines at bottom
        paint.color = Color.rgb(100, 116, 139)
        paint.strokeWidth = 2f
        for (i in 0 until 40) {
            val bx = 450f + i * 7f
            if (i % 3 != 0) {
                canvas.drawLine(bx, height - 160f, bx, height - 110f, paint)
            }
        }

        return bmp
    }

    private fun createSampleIdCardBitmap(side: String): Bitmap {
        val width = 850
        val height = 540
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Plastic card background
        canvas.drawColor(Color.rgb(240, 249, 255))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top security banner
        paint.color = Color.rgb(3, 105, 161)
        canvas.drawRect(0f, 0f, width.toFloat(), 90f, paint)

        paint.color = Color.WHITE
        paint.textSize = 26f
        paint.isFakeBoldText = true
        canvas.drawText("NATIONAL IDENTITY & SMART CITIZEN CARD", 40f, 55f, paint)

        if (side == "FRONT") {
            // Photo placeholder
            paint.color = Color.rgb(203, 213, 225)
            canvas.drawRoundRect(RectF(50f, 130f, 250f, 380f), 12f, 12f, paint)

            paint.color = Color.rgb(100, 116, 139)
            canvas.drawCircle(150f, 220f, 45f, paint)
            canvas.drawRoundRect(RectF(100f, 275f, 200f, 360f), 20f, 20f, paint)

            // Gold Microchip graphic
            paint.color = Color.rgb(234, 179, 8)
            canvas.drawRoundRect(RectF(290f, 140f, 390f, 215f), 8f, 8f, paint)
            paint.color = Color.rgb(161, 98, 7)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(310f, 140f, 370f, 215f, paint)
            canvas.drawLine(290f, 177f, 390f, 177f, paint)
            paint.style = Paint.Style.FILL

            // Text fields
            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 22f
            paint.isFakeBoldText = true
            canvas.drawText("NAME: RAHMAN, ARIF", 290f, 260f, paint)

            paint.textSize = 18f
            paint.isFakeBoldText = false
            paint.color = Color.rgb(71, 85, 105)
            canvas.drawText("NATIONAL ID: 9842-1094-8210", 290f, 300f, paint)
            canvas.drawText("DATE OF BIRTH: 14 JAN 1994", 290f, 340f, paint)
            canvas.drawText("NATIONALITY: BANGLADESHI", 290f, 380f, paint)

            // Holographic strip
            paint.color = Color.argb(80, 56, 189, 248)
            canvas.drawRect(0f, height - 70f, width.toFloat(), height - 40f, paint)
        } else {
            // Back side: Address, Emergency contact, and MRZ barcode band
            paint.color = Color.rgb(30, 41, 59)
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("PERMANENT ADDRESS:", 50f, 140f, paint)
            paint.textSize = 17f
            paint.isFakeBoldText = false
            paint.color = Color.rgb(71, 85, 105)
            canvas.drawText("House 42, Road 11, Banani, Dhaka-1213", 50f, 175f, paint)
            canvas.drawText("Blood Group: B+  |  Organ Donor: YES", 50f, 215f, paint)

            // MRZ Machine Readable Zone lines
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawRect(30f, 320f, (width - 30).toFloat(), height - 40f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("I<BGD984210948210<<<<<<<<<<<<<<", 50f, 370f, paint)
            canvas.drawText("9401148M3110156BGD<<<<<<<<<<<4", 50f, 410f, paint)
            canvas.drawText("RAHMAN<<ARIF<<<<<<<<<<<<<<<<<<", 50f, 450f, paint)
        }

        return bmp
    }

    private fun createSampleNotesBitmap(): Bitmap {
        val width = 800
        val height = 1100
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Warm notebook paper color
        canvas.drawColor(Color.rgb(255, 253, 245))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Vertical margin line in light coral
        paint.color = Color.rgb(248, 113, 113)
        paint.strokeWidth = 2f
        canvas.drawLine(100f, 0f, 100f, height.toFloat(), paint)

        // Horizontal ruled lines
        paint.color = Color.rgb(224, 231, 255)
        paint.strokeWidth = 1.5f
        for (y in 140 until height step 45) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
        }

        // Handwritten style title
        paint.color = Color.rgb(30, 58, 138)
        paint.textSize = 30f
        paint.isFakeBoldText = true
        canvas.drawText("ClearScan Mobile Architecture", 130f, 125f, paint)

        // Notes content
        paint.textSize = 21f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(15, 23, 42)

        val lines = listOf(
            "• CameraX Viewfinder with real-time framing guide",
            "• ColorMatrix filters: Magic Color, B&W, Grayscale",
            "• Native Android PdfDocument generator (A4 72 DPI)",
            "• Multi-page batch scan with reorder & rotation",
            "• Offline Room SQLite persistence for complete privacy",
            "• Instant Share Sheet via FileProvider (PDF / JPG)"
        )

        var textY = 195f
        for (line in lines) {
            canvas.drawText(line, 130f, textY, paint)
            textY += 90f
        }

        return bmp
    }

    private fun createSampleReceiptBitmap(): Bitmap {
        val width = 600
        val height = 900
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.BLACK
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("ORGANIC GROCERY MART", 120f, 70f, paint)

        paint.textSize = 18f
        paint.isFakeBoldText = false
        canvas.drawText("Store #402 - Cashier: Maria", 170f, 110f, paint)
        canvas.drawText("Date: 15-Oct-2026  11:32 AM", 175f, 140f, paint)

        paint.strokeWidth = 2f
        canvas.drawLine(40f, 170f, (width - 40).toFloat(), 170f, paint)

        val items = listOf(
            Pair("Almond Milk 1L", "$4.20"),
            Pair("Whole Wheat Bread", "$3.50"),
            Pair("Fresh Organic Apples", "$6.10"),
            Pair("Greek Yogurt 500g", "$5.40"),
            Pair("Dark Roast Coffee Beans", "$14.80")
        )

        var y = 220f
        for (item in items) {
            canvas.drawText(item.first, 50f, y, paint)
            canvas.drawText(item.second, 480f, y, paint)
            y += 50f
        }

        canvas.drawLine(40f, y + 20f, (width - 40).toFloat(), y + 20f, paint)
        y += 70f

        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("TOTAL AMOUNT:", 50f, y, paint)
        canvas.drawText("$34.00", 470f, y, paint)

        paint.textSize = 18f
        paint.isFakeBoldText = false
        canvas.drawText("Payment: Contactless Card (**** 4812)", 110f, y + 60f, paint)
        canvas.drawText("THANK YOU FOR SHOPPING!", 150f, y + 110f, paint)

        return bmp
    }
}
