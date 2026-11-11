package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.model.Track
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SmartBankManifestTest {
    private val c1 = "deck/oic_nw_c1.csv.enc"
    private val c2 = "deck/oic_nw_c2.csv.enc"
    private val c1Sha = "a".repeat(64)
    private val c2Sha = "b".repeat(64)
    private val old = mapOf(c1 to (c1Sha to 1234), c2 to (c2Sha to 2048))
    private fun server(version1: String, version2: String) =
        JSONObject().put("track", Track.DECK_OIC_NW.name)
            .put("versionFormat", "github-content-sha256-v1")
            .put("bankRevision", "c".repeat(40))
            .put("versions", JSONArray().put(JSONObject().put("path",c1).put("fullHash",version1))
                .put(JSONObject().put("path",c2).put("fullHash",version2)))

    @Test fun bundledManifestMatchesOriginalEncryptedBytesWithoutNetwork() {
        val version1 = SmartBankManifest.version(c1,c1Sha,1234)
        val version2 = SmartBankManifest.version(c2,c2Sha,2048)
        val remote = SmartBankManifest.parseRemote(
            server(version1,version2),Track.DECK_OIC_NW, old.keys
        )
        assertEquals(emptyList<SmartBankSource>(), SmartBankManifest.needsDownload(remote,old, emptyMap()))
        assertEquals(64, SmartBankManifest.digest("hello".toByteArray()).length)
    }

    @Test fun onlyModifiedCompetenceRequiresDownload() {
        val newer = "d".repeat(64)
        val remote = SmartBankManifest.parseRemote(
            server(SmartBankManifest.version(c1,c1Sha,1234),
                SmartBankManifest.version(c2,newer,2048)),
            Track.DECK_OIC_NW,old.keys
        )
        assertEquals(listOf(c2),SmartBankManifest.needsDownload(remote,old, emptyMap()).map{it.path})
        assertEquals(emptyList<SmartBankSource>(),
            SmartBankManifest.needsDownload(remote,old,mapOf(c2 to (newer to 2048))))
        assertEquals(listOf(c2),
            SmartBankManifest.needsDownload(remote,old,mapOf(c2 to (newer to 2047))).map{it.path})
    }

    @Test fun invalidOrUnlicensedManifestFailsClosed() {
        val version1=SmartBankManifest.version(c1,c1Sha,1234)
        val version2=SmartBankManifest.version(c2,c2Sha,2048)
        val valid = server(version1,version2)
        assertThrows(IllegalArgumentException::class.java) {
            SmartBankManifest.parseRemote(JSONObject(valid.toString()).put("versionFormat","storage-metadata-v1"),
                Track.DECK_OIC_NW,old.keys)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SmartBankManifest.parseRemote(server(version1,"").put("track","ENGINE_MGT"),
                Track.DECK_OIC_NW,old.keys)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SmartBankManifest.parseRemote(server(version1,version2).put("versions",
                JSONArray().put(JSONObject().put("path","../../secret.csv.enc").put("fullHash",version1))),
                Track.DECK_OIC_NW,old.keys)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SmartBankManifest.parseRemote(server(version1,version2),Track.DECK_OIC_NW,setOf(c1))
        }
        assertNull(SmartBankManifest.sourceForAssetPath("../private.csv"))
        assertEquals("gmdss/c1_part1.csv.enc",
            SmartBankManifest.sourceForAssetPath("questions/pro/c1_part1.csv"))
        assertEquals(c1,SmartBankManifest.sourceForAssetPath("questions/deck/oic_nw_c1.csv"))
    }

    @Test fun packagedAssetManifestIsBoundedAndTrackScoped() {
        val manifest=JSONObject().put("version",1).put("encrypted",true)
            .put("files",JSONArray()
                .put(JSONObject().put("path",c1).put("sha256",c1Sha).put("bytes",1234))
                .put(JSONObject().put("path",c2).put("sha256",c2Sha).put("bytes",2048))
                .put(JSONObject().put("path","engine_mgt/em_c1.csv.enc")
                    .put("sha256",c2Sha).put("bytes",2048)))
        assertEquals(old,SmartBankManifest.bundledFiles(manifest,Track.DECK_OIC_NW))
    }
}
