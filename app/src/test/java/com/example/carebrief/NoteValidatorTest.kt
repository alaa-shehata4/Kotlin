package com.example.carebrief

import com.example.carebrief.data.NoteValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteValidatorTest {
    @Test
    fun emptyObservation_isInvalid() {
        val result = NoteValidator.validate("   ", listOf("Nutrition"))
        assertTrue(result is NoteValidator.Result.Invalid)
        assertEquals("Please describe what you observed.", (result as NoteValidator.Result.Invalid).message)
    }

    @Test
    fun shortObservation_isInvalid() {
        val result = NoteValidator.validate("ate ok", listOf("Nutrition"))
        assertTrue(result is NoteValidator.Result.Invalid)
    }

    @Test
    fun validObservation_withoutCategory_isValid() {
        val result = NoteValidator.validate("Sarah ate half her breakfast and seemed tired.", emptyList())
        assertTrue(result is NoteValidator.Result.Valid)
    }
}
