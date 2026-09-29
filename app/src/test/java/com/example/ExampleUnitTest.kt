package com.example

import com.example.data.model.Track
import com.example.data.repository.MaritimeQuestionBank
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testDeckQuestionCountsStrict() {
    val deckQuestions = MaritimeQuestionBank.getQuestionsByTrack(Track.DECK_OIC_NW)
    org.junit.Assert.assertTrue("Deck questions must have rich dataset", deckQuestions.size >= 2268)

    val c1 = deckQuestions.filter { it.competenceId == "C1" }.size
    val c2 = deckQuestions.filter { it.competenceId == "C2" }.size
    val c3 = deckQuestions.filter { it.competenceId == "C3" }.size
    val c4 = deckQuestions.filter { it.competenceId == "C4" }.size
    val c5 = deckQuestions.filter { it.competenceId == "C5" }.size
    val c7 = deckQuestions.filter { it.competenceId == "C7" }.size
    val c9 = deckQuestions.filter { it.competenceId == "C9" }.size
    val f1 = deckQuestions.filter { it.functionId == "F1" }.size

    org.junit.Assert.assertTrue("C1 questions", c1 >= 831)
    org.junit.Assert.assertTrue("C2 questions", c2 >= 568)
    org.junit.Assert.assertTrue("C3 questions", c3 >= 442)
    org.junit.Assert.assertTrue("C4 questions", c4 >= 307)
    org.junit.Assert.assertTrue("C5 questions", c5 >= 10)
    org.junit.Assert.assertTrue("C7 questions", c7 >= 20)
    org.junit.Assert.assertTrue("C9 questions", c9 >= 15)
    org.junit.Assert.assertTrue("F1 questions", f1 >= 2193)

    val c10 = deckQuestions.filter { it.competenceId == "C10" }.size
    val c11 = deckQuestions.filter { it.competenceId == "C11" }.size
    val f2 = deckQuestions.filter { it.functionId == "F2" }.size
    org.junit.Assert.assertTrue("C10 questions", c10 >= 15)
    org.junit.Assert.assertTrue("C11 questions", c11 >= 15)
    org.junit.Assert.assertTrue("F2 questions", f2 >= 30)

    val c12 = deckQuestions.filter { it.competenceId == "C12" }.size
    val c13 = deckQuestions.filter { it.competenceId == "C13" }.size
    val c17 = deckQuestions.filter { it.competenceId == "C17" }.size
    val f3 = deckQuestions.filter { it.functionId == "F3" }.size
    org.junit.Assert.assertTrue("C12 questions", c12 >= 15)
    org.junit.Assert.assertTrue("C13 questions", c13 >= 15)
    org.junit.Assert.assertTrue("C17 questions", c17 >= 15)
    org.junit.Assert.assertTrue("F3 questions", f3 >= 45)
  }

  @Test
  fun testGmdssQuestionCountsStrict() {
    val gmdssQuestions = MaritimeQuestionBank.getQuestionsByTrack(Track.GMDSS_GOC)
    assertEquals(100, gmdssQuestions.size)

    val c1 = gmdssQuestions.filter { it.competenceId == "C1" }.size
    val c2 = gmdssQuestions.filter { it.competenceId == "C2" }.size
    assertEquals(85, c1)
    assertEquals(15, c2)
  }

  @Test
  fun testDeviceRequestCodeFormat() {
    val seed = "TEST_DEVICE_SEED_123"
    val reqCode = com.example.data.util.LicenseManager.generateRequestCode(seed)
    val regex = Regex("^HYP-[0-9A-F]{4}-[0-9A-F]{4}$")
    org.junit.Assert.assertTrue("Device request code must match HYP-XXXX-XXXX format: $reqCode", regex.matches(reqCode))
  }

  @Test
  fun testActivationKeyAlgorithmAndVerification() {
    val reqCode = "HYP-A1B2-C3D4"
    val key = com.example.data.util.LicenseManager.computeValidKey(reqCode)

    // Format should be ACTV-XXXX-XXXX-XXXX
    val keyRegex = Regex("^ACTV-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}$")
    org.junit.Assert.assertTrue("Activation key must match ACTV-XXXX-XXXX-XXXX format: $key", keyRegex.matches(key))

    // Manually calculate expected SHA-256
    val cleanReq = "HYP-A1B2-C3D4"
    val salt = "HYPERION_MARITIME_SALT_2026_SECURE_AUTH"
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(("$cleanReq:$salt").toByteArray(Charsets.UTF_8))
    val hex = hash.joinToString("") { "%02X".format(it) }
    val expectedKey = "ACTV-${hex.substring(0, 4)}-${hex.substring(4, 8)}-${hex.substring(8, 12)}"

    assertEquals(expectedKey, key)
    org.junit.Assert.assertTrue(com.example.data.util.LicenseManager.verifyKey(reqCode, key))
    org.junit.Assert.assertTrue(com.example.data.util.LicenseManager.verifyKey(reqCode, key.lowercase()))
    org.junit.Assert.assertTrue(com.example.data.util.LicenseManager.verifyKey(reqCode, key.replace("-", "")))

    // Old test keys must be rejected
    org.junit.Assert.assertFalse(com.example.data.util.LicenseManager.verifyKey(reqCode, "HYPERION-PRO-2026"))
    org.junit.Assert.assertFalse(com.example.data.util.LicenseManager.verifyKey(reqCode, "HYPERION-PRO"))
    org.junit.Assert.assertFalse(com.example.data.util.LicenseManager.verifyKey(reqCode, "MARITIME-2026"))

    // Invalid key for another device
    val otherReqCode = "HYP-FFFF-0000"
    org.junit.Assert.assertFalse(com.example.data.util.LicenseManager.verifyKey(otherReqCode, key))
  }

  @Test
  fun testObfuscatedSaltReconstruction() {
    val dynamicSalt = com.example.data.util.LicenseManager.getSecretSalt()
    assertEquals("HYPERION_MARITIME_SALT_2026_SECURE_AUTH", dynamicSalt)
    assertEquals(dynamicSalt, com.example.data.util.LicenseManager.SECRET_SALT)
  }

  @Test
  fun testScatteredActivationChecksOnQuestionBank() {
    // Unactivated questions must truncate to first 5 items
    val unactivatedDeck = MaritimeQuestionBank.getQuestionsByTrack(Track.DECK_OIC_NW, isProUnlocked = false)
    assertEquals(5, unactivatedDeck.size)

    val unactivatedCompetence = MaritimeQuestionBank.getQuestionsByCompetence("C1", Track.DECK_OIC_NW, isProUnlocked = false)
    assertEquals(5, unactivatedCompetence.size)

    // Unactivated Mock exam dataset must return empty list
    val unactivatedMock = MaritimeQuestionBank.getMockExamDataset(Track.DECK_OIC_NW, isProUnlocked = false)
    org.junit.Assert.assertTrue("Unactivated mock exam must yield empty dataset", unactivatedMock.isEmpty())

    // Activated calls return full datasets
    val activatedDeck = MaritimeQuestionBank.getQuestionsByTrack(Track.DECK_OIC_NW, isProUnlocked = true)
    org.junit.Assert.assertTrue("Activated deck questions must be loaded", activatedDeck.size >= 2268)

    val activatedMock = MaritimeQuestionBank.getMockExamDataset(Track.DECK_OIC_NW, isProUnlocked = true)
    assertEquals(200, activatedMock.size)
  }
}
