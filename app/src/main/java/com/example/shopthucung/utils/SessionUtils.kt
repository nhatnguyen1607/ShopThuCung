    package com.example.foodapp.utils

    import android.content.Context
    import android.util.Log

    object SessionUtils {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val BACKUP_FILE = "user_id_backup"

        fun saveUserId(context: Context, userId: String) {
            Log.d("SessionUtils", "Saving userId: $userId")
            if (userId.isEmpty()) {
                Log.w("SessionUtils", "Attempted to save empty userId")
                return
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USER_ID, userId)
                .apply()
            try {
                context.openFileOutput(BACKUP_FILE, Context.MODE_PRIVATE).use {
                    it.write(userId.toByteArray())
                }
                Log.d("SessionUtils", "UserId backed up to file")
            } catch (e: Exception) {
                Log.e("SessionUtils", "Failed to save backup: ${e.message}")
            }
        }

        fun getUserId(context: Context): String {
            var userId = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_ID, "") ?: ""
            Log.d("SessionUtils", "Retrieved userId from SharedPreferences: $userId")
            if (userId.isEmpty()) {
                Log.w("SessionUtils", "SharedPreferences user_id is empty, checking backup")
                try {
                    context.openFileInput(BACKUP_FILE).use {
                        userId = it.readBytes().toString(Charsets.UTF_8)
                        Log.d("SessionUtils", "Restored userId from backup: $userId")
                        saveUserId(context, userId)
                    }
                } catch (e: Exception) {
                    Log.e("SessionUtils", "Failed to read backup: ${e.message}")
                }
            }
            return userId
        }

        fun clearUserId(context: Context) {
            Log.d("SessionUtils", "Clearing userId")
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_USER_ID)
                .apply()
            try {
                context.deleteFile(BACKUP_FILE)
                Log.d("SessionUtils", "Deleted userId backup file")
            } catch (e: Exception) {
                Log.e("SessionUtils", "Failed to delete backup: ${e.message}")
            }
        }
    }