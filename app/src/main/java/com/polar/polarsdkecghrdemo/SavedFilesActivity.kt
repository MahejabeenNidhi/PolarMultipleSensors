package com.polar.polarsdkecghrdemo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class SavedFilesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_files)

        val fileList = findViewById<ListView>(R.id.fileList)
        val filesDir = getExternalFilesDir(null)
        val files = filesDir?.listFiles()

        val fileNames = files?.map { it.name } ?: emptyList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileNames)
        fileList.adapter = adapter

        fileList.setOnItemClickListener { _, _, position, _ ->
            val selectedFile = files?.get(position)
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", selectedFile!!)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "text/plain")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        }
    }
}