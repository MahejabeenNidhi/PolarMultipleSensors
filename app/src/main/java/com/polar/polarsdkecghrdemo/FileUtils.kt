package com.polar.polarsdkecghrdemo

import android.content.Context
import java.io.File

object FileUtils {
    fun saveDataToFile(context: Context, data: String, fileName: String) {
        val file = File(context.getExternalFilesDir(null), fileName)
        file.writeText(data)
    }
}