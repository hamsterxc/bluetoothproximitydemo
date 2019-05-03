package com.lonebytesoft.hamster.bluetoothproximitydemo

import android.util.Base64
import kotlin.random.Random

internal fun generateNameFromId(id: String): String {
    val bytes = mutableListOf<Byte>()

    bytes.add(0xAD.toByte())
    bytes.add(0xC0.toByte())
    bytes.add(0xDE.toByte())

    val deviceId = Random.nextInt()
    (0..3).forEach { bytes.add(((deviceId shr (8 * it)) and 0xFF).toByte()) }

    val idValue = id.toLong()
    (0..7).forEach { bytes.add(((idValue shr (8 * it)) and 0xFF).toByte()) }

    return Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
}

internal fun extractIdFromName(name: String): String? {
    val bytes: ByteArray?
    try {
        bytes = Base64.decode(name, Base64.NO_WRAP)
    } catch (e: Exception) {
        return null
    }

    if ((bytes.size != 15) ||
        (bytes[0] != 0xAD.toByte()) ||
        (bytes[1] != 0xC0.toByte()) ||
        (bytes[2] != 0xDE.toByte())) {
        return null
    }

    return (0..7)
        .map { (bytes[7 + it].toUByte().toLong() shl (8 * it)) }
        .sum()
        .toString()
}
