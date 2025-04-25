package com.example.shopthucung.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object CloudinaryUtils {
    private const val CLOUD_NAME = "dvkty2ewp"
    private const val API_KEY = "654133236111989"
    private const val API_SECRET = "xyMMddSRjMP34CYBOhKImpo1TBA"
    private const val UPLOAD_PRESET = "shopthucung"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun uploadToCloudinary(imageUri: Uri, context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val filePath = getPathFromUri(imageUri, context)
            if (filePath == null) {
                Log.e("CloudinaryUtils", "Lỗi: Không thể lấy đường dẫn từ Uri")
                return@withContext null
            }

            val file = File(filePath)
            if (!file.exists()) {
                Log.e("CloudinaryUtils", "Lỗi: Tệp không tồn tại")
                return@withContext null
            }

            Log.d("CloudinaryUtils", "Đang upload ảnh: $filePath")

            // Tạo yêu cầu multipart
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, okhttp3.RequestBody.create("image/*".toMediaType(), file))
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("upload_preset", UPLOAD_PRESET) // Dùng preset nếu upload không ký
                // Nếu dùng signed upload, thêm timestamp và signature (xem bên dưới)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val secureUrl = json.optString("secure_url")
                Log.d("CloudinaryUtils", "Upload hoàn tất: $secureUrl")
                secureUrl.takeIf { it.isNotEmpty() }
            } else {
                Log.e("CloudinaryUtils", "Lỗi upload: ${response.code} - ${response.message}")
                null
            }
        } catch (e: Exception) {
            Log.e("CloudinaryUtils", "Lỗi khi upload: ${e.message}", e)
            null
        }
    }

    private fun getPathFromUri(uri: Uri, context: Context): String? {
        return try {
            val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("CloudinaryUtils", "Lỗi lấy đường dẫn: ${e.message}", e)
            null
        }
    }
}