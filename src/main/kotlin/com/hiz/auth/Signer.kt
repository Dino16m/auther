package com.hiz.auth

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

interface Signer {
    fun sign(data: String): ByteArray
    fun verify(data: String, signature: ByteArray): Boolean
}

class HMACSigner(secretKey: String) : Signer {
    private val algorithm = "HmacSHA256"
    private val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), algorithm)

    private val mac: Mac
        get() {
            val mac = Mac.getInstance(algorithm)
            mac.init(secretKeySpec)
            return mac
        }

    override fun sign(data: String): ByteArray {
        return mac.doFinal(data.toByteArray())
    }

    override fun verify(data: String, signature: ByteArray): Boolean {
        return sign(data).contentEquals(signature)
    }

}