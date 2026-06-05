package com.weyya.app.i18n

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards translation completeness: every localized strings.xml must define exactly the same
 * resource names as the English base. A missing key falls back silently to English at runtime,
 * so this test makes desyncs fail the build instead.
 *
 * Pure JVM (no Robolectric) — it reads the XML files straight off disk.
 */
class StringsParityTest {

    private val locales = listOf("es", "hi", "in", "pt")

    @Test
    fun `every locale defines the same string keys as the base`() {
        val baseKeys = parseResourceNames(resFile(null))

        val missingByLocale = locales.associateWith { locale ->
            baseKeys - parseResourceNames(resFile(locale))
        }.filterValues { it.isNotEmpty() }

        val extraByLocale = locales.associateWith { locale ->
            parseResourceNames(resFile(locale)) - baseKeys
        }.filterValues { it.isNotEmpty() }

        assertThat(missingByLocale).isEmpty()
        assertThat(extraByLocale).isEmpty()
    }

    /** Resolves values/strings.xml (base when [locale] is null) regardless of the test working dir. */
    private fun resFile(locale: String?): File {
        val dir = if (locale == null) "values" else "values-$locale"
        val candidates = listOf(
            "src/main/res/$dir/strings.xml",
            "app/src/main/res/$dir/strings.xml",
        )
        return candidates.map(::File).firstOrNull { it.exists() }
            ?: error("strings.xml not found for '$dir' (cwd=${File(".").absolutePath})")
    }

    /** Collects the `name` of every <string>, <plurals> and <string-array> element. */
    private fun parseResourceNames(file: File): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val names = mutableSetOf<String>()
        for (tag in listOf("string", "plurals", "string-array")) {
            val nodes = doc.getElementsByTagName(tag)
            for (i in 0 until nodes.length) {
                val name = (nodes.item(i) as Element).getAttribute("name")
                if (name.isNotEmpty()) names.add(name)
            }
        }
        return names
    }
}
