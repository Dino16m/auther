package com.hiz

import com.hiz.auth.HMACSigner
import com.hiz.auth.OTPGenerator
import com.hiz.plugins.InvalidTokenException
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

internal class OTPGeneratorTest {


    @Test
    fun testOTPGeneratorGeneratesOTP() {
        val signer = HMACSigner("hello")
        val generator = OTPGenerator(signer) { Instant.now() }
        val otp = generator.generateOTP(mapOf("userId" to "1"), "test")

        assertTrue("code is invalid") { otp.code.isNotEmpty() }
        assertTrue(otp.signature.isNotEmpty(), "signature is empty")
    }

    @Test
    fun testOTPGeneratorValidatesGeneratedOTP() {
        val signer = HMACSigner("hello")
        val generator = OTPGenerator(signer) { Instant.now() }
        val otp = generator.generateOTP(mapOf("userId" to "1"), "test")


        assertDoesNotThrow {
            generator.validateOTP(otp, "test")
        }
    }

    @Test
    fun testValidatorThrowsWhenPurposeIsMismatched() {
        val signer = HMACSigner("hello")
        val generator = OTPGenerator(signer) { Instant.now() }
        val otp = generator.generateOTP(mapOf("userId" to "1"), "test")

        assertThrows<InvalidTokenException> {
            generator.validateOTP(otp, "prod")
        }
    }

    @Test
    fun testValidatorThrowsWhenOTPIsExpired() {
        val signer = HMACSigner("hello")
        var generator = OTPGenerator(signer) { Instant.now() }
        val otp = generator.generateOTP(mapOf("userId" to "1"), "test")

        generator = OTPGenerator(signer) { Instant.now().plusSeconds(10.minutes.inWholeSeconds) }

        assertThrows<InvalidTokenException> {
            generator.validateOTP(otp, "test")
        }
    }

}