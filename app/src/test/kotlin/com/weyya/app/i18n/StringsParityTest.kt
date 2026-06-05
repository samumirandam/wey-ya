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
 * Locale directories are discovered from disk (not hardcoded), so a newly added `values-xx/`
 * is checked automatically — the test can't silently skip a language it doesn't know about.
 *
 * Pure JVM (no Robolectric) — it reads the XML files straight off disk.
 */
class StringsParityTest {

    // Android locale qualifiers: language (2-3 lowercase) with an optional -rXX region.
    // Excludes non-locale qualifiers like values-night, values-v29, values-w600dp.
    private val localeDirPattern = Regex("^values-[a-z]{2,3}(-r[A-Z]{2})?$")

    @Test
    fun `every locale defines the same string keys as the base`() {
        val res = resDir()
        val baseKeys = parseResourceNames(File(res, "values/strings.xml"))

        val localeDirs = (res.listFiles { f -> f.isDirectory && localeDirPattern.matches(f.name) } ?: emptyArray())
            .filter { File(it, "strings.xml").exists() }
            .sortedBy { it.name }

        // Guard: if path resolution ever breaks, fail loudly instead of passing vacuously.
        assertThat(localeDirs).isNotEmpty()

        val missingByLocale = localeDirs.associate { dir ->
            dir.name to (baseKeys - parseResourceNames(File(dir, "strings.xml")))
        }.filterValues { it.isNotEmpty() }

        val extraByLocale = localeDirs.associate { dir ->
            parseResourceNames(File(dir, "strings.xml")).let { dir.name to (it - baseKeys) }
        }.filterValues { it.isNotEmpty() }

        assertThat(missingByLocale).isEmpty()
        assertThat(extraByLocale).isEmpty()
    }

    /** Resolves the res/ directory regardless of the test working dir (module root or repo root). */
    private fun resDir(): File {
        val candidates = listOf("src/main/res", "app/src/main/res")
        return candidates.map(::File).firstOrNull { it.isDirectory }
            ?: error("res directory not found (cwd=${File(".").absolutePath})")
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
