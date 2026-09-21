package com.saketkhundia.pocketserver.util

object Constants {
    const val DEFAULT_HTTP_PORT = 8080
    const val DEFAULT_FTP_PORT = 2121
    const val MIN_PORT = 1024
    const val MAX_PORT = 65535
    const val DEFAULT_SERVER_NAME = "Pocket Server"
    const val DEFAULT_USERNAME = "admin"
    const val SESSION_TIMEOUT_MIN_DEFAULT = 60L
    const val MAX_FILENAME_LENGTH = 255
    const val MAX_UPLOAD_BYTES = 10L * 1024 * 1024 * 1024 // 10 GB guard (streamed, not buffered)
    const val MAX_CONCURRENT_UPLOADS = 3
    const val SESSION_TOKEN_BYTES = 32
    const val MAX_LOGIN_ATTEMPTS = 5
    const val LOGIN_WINDOW_MS = 60_000L
    const val LOGIN_BLOCK_MS = 5 * 60_000L
    const val NOTIFICATION_ID = 1001
    const val NOTIFICATION_CHANNEL_ID = "pocket_server_channel"
}
