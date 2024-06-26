package com.example.shuffleit

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import com.example.shuffleit.models.AudioFile

class Utils {
    companion object {
        fun intToTime(time: Int): String {
            var hours = (time/3600).toString()
            var mins = (time/60).toString()
            var secs = (time%60).toString().padStart(2, '0')
            if(hours!="0"){
                mins = mins.padStart(2, '0')
                return "$hours: $mins : $secs"
            }
            return "$mins : $secs"
        }

        fun getAudioFilesFromUri(context: Context, uri: Uri, playlistId:Long): List<AudioFile> {
            val audioFiles = mutableListOf<AudioFile>()
            val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            collectAudioFiles(context, docUri, audioFiles, playlistId)
            return audioFiles
        }

        private fun collectAudioFiles(context: Context, uri: Uri, audioFiles: MutableList<AudioFile>, playlistId:Long) {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))
            val cursor: Cursor? = context.contentResolver.query(childrenUri, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            ), null, null, null)

            cursor?.use {
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)

                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        collectAudioFiles(context, documentUri, audioFiles, playlistId)
                    } else if (mimeType.startsWith("audio/")) {
                        val audioFile = AudioFile(path = documentUri, playlistId = playlistId)
                        audioFiles.add(audioFile)
                    }
                }
            }
        }
    }
}