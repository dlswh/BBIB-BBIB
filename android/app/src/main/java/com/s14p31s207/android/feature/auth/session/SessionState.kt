package com.s14p31s207.android.feature.auth.session

enum class AuthMode {
    ONLINE,
    OFFLINE
}

object SessionState {
    var authMode: AuthMode = AuthMode.ONLINE
    var name: String = ""       // ← 추가
    var role: String = ""       // ← 추가
}