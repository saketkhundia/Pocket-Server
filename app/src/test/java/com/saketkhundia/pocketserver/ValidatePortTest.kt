package com.saketkhundia.pocketserver

import com.saketkhundia.pocketserver.domain.usecase.ValidatePortUseCase
import org.junit.Assert.*
import org.junit.Test

class ValidatePortTest {
    @Test
    fun `valid ports`() {
        assertTrue(ValidatePortUseCase.validate(8080) is ValidatePortUseCase.Result.Ok)
        assertTrue(ValidatePortUseCase.validate(2121) is ValidatePortUseCase.Result.Ok)
        assertTrue(ValidatePortUseCase.validate(1024) is ValidatePortUseCase.Result.Ok)
        assertTrue(ValidatePortUseCase.validate(65535) is ValidatePortUseCase.Result.Ok)
    }

    @Test
    fun `reject privileged ports`() {
        assertTrue(ValidatePortUseCase.validate(80) is ValidatePortUseCase.Result.Invalid)
        assertTrue(ValidatePortUseCase.validate(22) is ValidatePortUseCase.Result.Invalid)
        assertTrue(ValidatePortUseCase.validate(0) is ValidatePortUseCase.Result.Invalid)
    }

    @Test
    fun `reject out of range`() {
        assertTrue(ValidatePortUseCase.validate(65536) is ValidatePortUseCase.Result.Invalid)
        assertTrue(ValidatePortUseCase.validate(-1) is ValidatePortUseCase.Result.Invalid)
    }

    @Test
    fun `validate string`() {
        assertTrue(ValidatePortUseCase.validateString("8080") is ValidatePortUseCase.Result.Ok)
        assertTrue(ValidatePortUseCase.validateString("abc") is ValidatePortUseCase.Result.Invalid)
        assertTrue(ValidatePortUseCase.validateString("") is ValidatePortUseCase.Result.Invalid)
        assertTrue(ValidatePortUseCase.validateString(" 8080 ") is ValidatePortUseCase.Result.Ok)
    }
}
