package com.example.djmeter.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.io.image.ImageDataFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator {
    fun generateDecibelGraphPdf(context: Context, readings: List<Float>): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "decibel_graph_$timeStamp.pdf"
        val filePath = File(context.getExternalFilesDir(null), fileName)

        // Create PDF document
        val writer = PdfWriter(FileOutputStream(filePath))
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            // Add title
            document.add(Paragraph("Decibel Readings Report"))
            document.add(Paragraph("Generated on: $timeStamp"))
            
            // Create graph bitmap
            val graphBitmap = createGraphBitmap(readings)
            
            // Convert bitmap to PDF image
            val stream = ByteArrayOutputStream()
            graphBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val imageData = ImageDataFactory.create(stream.toByteArray())
            val image = Image(imageData)
            
            // Add image to document
            document.add(image)
            
            // Add readings data
            document.add(Paragraph("Detailed Readings:"))
            readings.forEachIndexed { index, value ->
                document.add(Paragraph("Reading ${index + 1}: $value dB"))
            }
            
        } finally {
            document.close()
        }

        return filePath.absolutePath
    }

    private fun createGraphBitmap(readings: List<Float>): Bitmap {
        val width = 800
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.BLUE
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        // Draw graph
        val path = Path()
        val points = readings.size
        val dx = width.toFloat() / (points - 1)
        
        path.moveTo(0f, height - (height * (readings[0] / 120f)))
        
        for (i in 1 until points) {
            val x = i * dx
            val y = height - (height * (readings[i] / 120f))
            path.lineTo(x, y)
        }

        canvas.drawPath(path, paint)
        return bitmap
    }
} 