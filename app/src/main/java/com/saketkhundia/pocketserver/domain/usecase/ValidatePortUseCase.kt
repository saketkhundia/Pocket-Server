package com.saketkhundia.pocketserver.domain.usecase

import com.saketkhundia.pocketserver.util.Constants

object ValidatePortUseCase {
    sealed interface Result { data object Ok : Result; data class Invalid(val reason: String) : Result }
    fun validate(port: Int): Result {
        if (port < 1 || port > 65535) return Result.Invalid("Port must be 1–65535.")
        if (port < Constants.MIN_PORT) return Result.Invalid("Use port ≥ ${Constants.MIN_PORT} (privileged ports are blocked).")
        return Result.Ok
    }
    fun validateString(raw: String): Result {
        val p = raw.trim().toIntOrNull() ?: return Result.Invalid("Port must be a number.")
        return validate(p)
    }
}
