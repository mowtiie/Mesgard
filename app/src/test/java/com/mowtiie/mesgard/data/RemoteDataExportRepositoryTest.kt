package com.mowtiie.mesgard.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mowtiie.mesgard.data.remote.DataExportRemoteSource
import com.mowtiie.mesgard.data.remote.ExportCursor
import com.mowtiie.mesgard.domain.model.AdminError
import com.mowtiie.mesgard.domain.repository.EXPORT_PAGE_SIZE
import com.mowtiie.mesgard.domain.repository.ExportResult
import io.mockk.coEvery
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteDataExportRepositoryTest {
    private val remote = mockk<DataExportRemoteSource>()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = Clock.fixed(Instant.parse("2026-11-21T08:00:00Z"), ZoneOffset.UTC)
    private val repository = RemoteDataExportRepository(remote, context, clock)

    private fun message(number: Int) = buildJsonObject {
        put("message_id", "m$number")
        put("created_at", "2026-11-20T08:00:0$number+00:00")
        put("body", "Message $number")
    }

    private fun overview() = buildJsonObject {
        put("format_version", 1)
        put("profile", buildJsonObject { put("display_name", "Student One") })
    }

    private fun exported(result: ExportResult): File = File((result as ExportResult.Ready).file.path)

    @Test
    fun `the file is one JSON document with the overview, the notes and every page of messages`() = runTest {
        val firstPage = JsonArray((0 until EXPORT_PAGE_SIZE).map(::message))
        val lastPage = JsonArray(listOf(message(9)))
        coEvery { remote.overview("org", "u1") } returns overview()
        coEvery { remote.messages("org", "u1", null, EXPORT_PAGE_SIZE) } returns firstPage
        coEvery {
            remote.messages(
                "org",
                "u1",
                ExportCursor("2026-11-20T08:00:0${EXPORT_PAGE_SIZE - 1}+00:00", "m${EXPORT_PAGE_SIZE - 1}"),
                EXPORT_PAGE_SIZE,
            )
        } returns lastPage

        val file = exported(repository.export("org", "u1", "Student One"))

        val document = Json.parseToJsonElement(file.readText()).jsonObject
        assertEquals(
            "Student One",
            document.getValue("profile").jsonObject.getValue("display_name").jsonPrimitive.content,
        )
        assertEquals(3, document.getValue("notes").jsonArray.size)
        assertEquals(EXPORT_PAGE_SIZE + 1, document.getValue("messages").jsonArray.size)
    }

    @Test
    fun `a person without messages still gets a valid file`() = runTest {
        coEvery { remote.overview("org", "u1") } returns overview()
        coEvery { remote.messages("org", "u1", null, EXPORT_PAGE_SIZE) } returns JsonArray(emptyList())

        val file = exported(repository.export("org", "u1", "Student One"))

        assertEquals(0, Json.parseToJsonElement(file.readText()).jsonObject.getValue("messages").jsonArray.size)
    }

    @Test
    fun `the file is named after the person and the day, with nothing a file system dislikes`() = runTest {
        coEvery { remote.overview("org", "u1") } returns overview()
        coEvery { remote.messages("org", "u1", null, EXPORT_PAGE_SIZE) } returns JsonArray(emptyList())

        val file = exported(repository.export("org", "u1", "Ana de la Cruz / 7B"))

        assertEquals("mesgard-export-Ana_de_la_Cruz___7B-2026-11-21.json", file.name)
        assertTrue(file.parentFile?.name == "shared_attachments")
    }

    @Test
    fun `a failure takes the half written file away`() = runTest {
        coEvery { remote.overview("org", "u1") } returns overview()
        coEvery { remote.messages("org", "u1", null, EXPORT_PAGE_SIZE) } throws IOException("offline")

        val result = repository.export("org", "u1", "Student One")

        assertEquals(ExportResult.Failure(AdminError.NETWORK_UNAVAILABLE), result)
        assertFalse(File(context.cacheDir, "shared_attachments/mesgard-export-Student_One-2026-11-21.json").exists())
    }
}
