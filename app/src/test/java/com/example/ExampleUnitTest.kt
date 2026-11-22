package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSha256Hashing_deterministicAndSecure() {
    val pin = "1234"
    val hash1 = com.example.data.local.AppPreferences.hashSha256(pin)
    val hash2 = com.example.data.local.AppPreferences.hashSha256(pin)
    assertEquals(hash1, hash2)
    assertNotEquals("1234", hash1)
    assertEquals(64, hash1.length) // 256 bits = 64 hex characters
  }

  @Test
  fun testPinVerification_matchesExpected() {
    val originalPin = "9876"
    val storedHash = com.example.data.local.AppPreferences.hashSha256(originalPin)
    val enteredPin = "9876"
    val wrongPin = "1111"
    assertEquals(storedHash, com.example.data.local.AppPreferences.hashSha256(enteredPin))
    assertNotEquals(storedHash, com.example.data.local.AppPreferences.hashSha256(wrongPin))
  }
}
