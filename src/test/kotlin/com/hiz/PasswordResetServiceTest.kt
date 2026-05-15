package com.hiz;

import com.hiz.auth.*
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

public class PasswordResetServiceTest {


    private fun getService(messenger: PasswordResetMessenger, userRepo: UserRepository): PasswordResetService {
        val signer = HMACSigner("hello")
        val generator = OTPGenerator(signer) { Instant.now() }
        return PasswordResetService(
            signer = signer,
            otpGenerator = generator,
            passwordResetMessenger = messenger,
            userRepo = userRepo
        )
    }


    @Test
    fun sendResetCodeOnNonExistentEmailReturnsSignature() {
        val userRepo: UserRepository = mock()
        val messenger: PasswordResetMessenger = mock()
        whenever(userRepo.findByEmail("test@example.com")).thenReturn(null)
        val signature = getService(messenger, userRepo).sendResetCode("test@example.com")
        assertTrue(signature.isNotEmpty())
        verify(
            messenger,
            never()
        ).sendMessage(any(), any())
    }

    @Test
    fun sendResetCodeOnUserFoundReturnsSignature() {
        val userRepo: UserRepository = mock()
        val messenger: PasswordResetMessenger = mock()
        val email = "test@example.com"
        whenever(userRepo.findByEmail(anyString())).thenReturn(
            User(
                UserId(UUID.randomUUID()),
                firstName = "firstName",
                lastName = "last name",
                email = email,
                password = ""
            )
        )
        whenever(userRepo.save(any<User>())).thenReturn(
            User(
                UserId(UUID.randomUUID()),
                firstName = "firstName",
                lastName = "last name",
                email = email,
                password = ""
            )
        )


        getService(messenger, userRepo).sendResetCode(email)
        verify(
            messenger,
        ).sendMessage(any(), any())

    }

    @Test
    fun testGivenValidCodeAndSignaturePasswordIsChanged() {
        val userRepo: UserRepository = mock()
        var capturedCode = ""
        val messenger = PasswordResetMessenger { _, code -> capturedCode = code }
        val email = "test@example.com"
        val originalPassword = "original"
        val user = User(
            UserId(UUID.randomUUID()),
            firstName = "firstName",
            lastName = "last name",
            email = email,
            password = originalPassword
        )
        whenever(userRepo.findByEmail(anyString())).thenReturn(
            user
        )
        whenever(userRepo.save(any<User>())).thenReturn(
            user
        )

        val service = getService(messenger, userRepo)
        val signature = service.sendResetCode(email)
        service.resetPassword(capturedCode, signature, "newPassword")
        assertNotEquals(user.password, originalPassword)
    }
}
