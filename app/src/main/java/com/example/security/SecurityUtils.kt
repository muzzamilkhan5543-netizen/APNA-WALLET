package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.random.Random

object SecurityUtils {

    private val secureRandom = SecureRandom()

    fun hashMpin(mpin: String, existingSaltHex: String? = null): String {
        val saltHex = existingSaltHex ?: ByteArray(16).also { secureRandom.nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$saltHex:$mpin".toByteArray(Charsets.UTF_8)
        val hashBytes = digest.digest(input)
        val hashHex = hashBytes.joinToString("") { "%02x".format(it) }
        return "$saltHex:$hashHex"
    }

    fun verifyMpin(mpin: String, storedSaltAndHash: String): Boolean {
        val parts = storedSaltAndHash.split(":")
        if (parts.size != 2) return false
        val saltHex = parts[0]
        val expectedHash = parts[1].toByteArray(Charsets.UTF_8)
        val computed = hashMpin(mpin, saltHex).split(":")[1].toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(expectedHash, computed)
    }

    fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("92") && digits.length == 12 -> "0" + digits.drop(2)
            digits.startsWith("03") && digits.length == 11 -> digits
            else -> digits
        }
    }

    fun isValidPakistaniPhone(phone: String): Boolean {
        val normalized = normalizePhone(phone)
        return normalized.length == 11 && normalized.startsWith("03")
    }

    fun formatCnic(raw: String): String {
        val d = raw.filter { it.isDigit() }.take(13)
        return buildString {
            d.forEachIndexed { idx, ch ->
                if (idx == 5 || idx == 12) append('-')
                append(ch)
            }
        }
    }

    fun isValidCnic(raw: String): Boolean {
        return raw.filter { it.isDigit() }.length == 13
    }

    fun generateCardNumber(prefix: String): String {
        val body = buildString {
            append(prefix)
            while (length < 15) {
                append(Random.nextInt(0, 10))
            }
        }
        val checkDigit = computeLuhnCheckDigit(body)
        return "$body$checkDigit"
    }

    private fun computeLuhnCheckDigit(partial: String): Int {
        var sum = 0
        var doubleDigit = true
        for (i in partial.length - 1 downTo 0) {
            var d = partial[i].digitToInt()
            if (doubleDigit) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            doubleDigit = !doubleDigit
        }
        return (10 - (sum % 10)) % 10
    }

    fun generateExpiry(): String {
        val month = Random.nextInt(1, 13).toString().padStart(2, '0')
        val year = Random.nextInt(28, 33).toString()
        return "$month/$year"
    }

    fun generateCvc(): String = Random.nextInt(100, 1000).toString()

    fun formatCardMasked(cardNumber: String): String {
        val clean = cardNumber.filter { it.isDigit() }
        if (clean.length < 4) return "•••• •••• •••• ••••"
        return "•••• •••• •••• ${clean.takeLast(4)}"
    }

    fun formatCardGrouped(cardNumber: String): String {
        return cardNumber.filter { it.isDigit() }.chunked(4).joinToString(" ")
    }

    fun isSafeWebUrl(url: String): Boolean {
        val trimmed = url.trim().lowercase()
        return trimmed.startsWith("https://") || trimmed.startsWith("http://")
    }

    fun normalizeWebUrl(url: String): String {
        val trimmed = url.trim()
        return if (!trimmed.lowercase().startsWith("http://") && !trimmed.lowercase().startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    fun buildQrPayload(phone: String, name: String): String {
        return "APNAWALLET|v1|${normalizePhone(phone)}|${name.replace("|", " ").trim()}"
    }

    data class ParsedQrPayload(val phone: String, val name: String?)

    fun parseQrPayload(raw: String): ParsedQrPayload? {
        val trimmed = raw.trim()
        if (trimmed.startsWith("APNAWALLET|v1|")) {
            val parts = trimmed.split("|")
            if (parts.size >= 3) {
                val phone = normalizePhone(parts[2])
                if (isValidPakistaniPhone(phone)) {
                    return ParsedQrPayload(phone, parts.getOrNull(3)?.takeIf { it.isNotBlank() })
                }
            }
        }
        val normalized = normalizePhone(trimmed)
        if (isValidPakistaniPhone(normalized)) {
            return ParsedQrPayload(normalized, null)
        }
        return null
    }
}
