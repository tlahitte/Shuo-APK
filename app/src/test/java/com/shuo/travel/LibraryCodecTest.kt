package com.shuo.travel

import com.shuo.travel.data.LibraryCodec
import com.shuo.travel.domain.*
import org.junit.Assert.*
import org.junit.Test

class LibraryCodecTest {
    private val category = Category("one", "Restaurants", "#D56A24", icon = "utensils", order = 0)
    private val phrase = Phrase("phrase", "你好", "Nǐ hǎo", "Bonjour", categoryId = "one", lastUsedAt = 1234, favorite = true)
    private val library = Library(categories = listOf(category), phrases = listOf(phrase))
    private fun rejects(block: () -> Unit) { assertThrows(Exception::class.java) { block() } }
    @Test fun roundTripPreservesAllFieldsAndUnicode() { assertEquals(library, LibraryCodec.decode(LibraryCodec.encode(library))) }
    @Test fun malformedJsonAndMissingFieldsAreRejected() {
        listOf("{", "null", "[]", "{}", "{\"version\":1,\"categories\":[],\"phrases\":[{}]}").forEach { text -> rejects { LibraryCodec.decode(text) } }
    }
    @Test fun unknownVersionIsRejected() { rejects { LibraryCodec.encode(library.copy(version = 2)) } }
    @Test fun duplicateIdsAreRejected() {
        rejects { LibraryCodec.encode(library.copy(phrases = listOf(phrase, phrase))) }
        rejects { LibraryCodec.encode(library.copy(categories = listOf(category, category))) }
    }
    @Test fun missingCategoryAndBlankTextAreRejected() {
        rejects { LibraryCodec.encode(library.copy(categories = emptyList())) }
        rejects { LibraryCodec.encode(library.copy(phrases = listOf(phrase.copy(chinese = " ")))) }
    }
    @Test fun invalidColorsIconsIdsAndTimestampsAreRejected() {
        listOf(category.copy(color = "red"), category.copy(icon = "remote-url"), category.copy(id = "../file"), category.copy(order = -1)).forEach {
            rejects { LibraryCodec.validateCategory(it) }
        }
        rejects { LibraryCodec.validatePhrase(phrase.copy(createdAt = 100, modifiedAt = 0)) }
    }
    @Test fun oversizedFilesAreRejected() { rejects { LibraryCodec.decode(" ".repeat(LibraryCodec.MAX_BYTES + 1)) } }
}
