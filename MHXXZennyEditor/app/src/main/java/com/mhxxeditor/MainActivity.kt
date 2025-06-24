
package com.mhxxeditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {
    private var selectedUri: Uri? = null
    private lateinit var zennyField: EditText
    private val zennyOffset = 0x7F4C

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedUri = result.data?.data
            selectedUri?.let { uri ->
                contentResolver.openInputStream(uri)?.use { stream ->
                    val data = stream.readBytes()
                    if (data.size > zennyOffset + 4) {
                        val zenny = ByteBuffer.wrap(data, zennyOffset, 4).order(ByteOrder.LITTLE_ENDIAN).int
                        zennyField.setText(zenny.toString())
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pickButton = findViewById<Button>(R.id.pickButton)
        val saveButton = findViewById<Button>(R.id.saveButton)
        zennyField = findViewById(R.id.zennyInput)

        pickButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            pickFileLauncher.launch(intent)
        }

        saveButton.setOnClickListener {
            val newZenny = zennyField.text.toString().toIntOrNull()
            if (selectedUri != null && newZenny != null) {
                contentResolver.openInputStream(selectedUri!!)?.use { input ->
                    val data = input.readBytes().toMutableList()
                    val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(newZenny)
                    val bytes = buffer.array()
                    for (i in 0..3) {
                        data[zennyOffset + i] = bytes[i]
                    }
                    contentResolver.openOutputStream(selectedUri!!, "rwt")?.use { output: OutputStream ->
                        output.write(data.toByteArray())
                        Toast.makeText(this, "Zenny updated!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Invalid input or file not selected", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
