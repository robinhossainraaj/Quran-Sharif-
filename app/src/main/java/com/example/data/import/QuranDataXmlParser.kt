package com.example.data.import

import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

data class ParsedSurahMeta(
    val index: Int,
    val ayas: Int,
    val name: String,
    val tname: String,
    val ename: String,
    val type: String,
    val order: Int
)

data class ParsedJuzMeta(
    val index: Int,
    val sura: Int,
    val aya: Int
)

data class QuranXmlResult(
    val suras: List<ParsedSurahMeta>,
    val juzs: List<ParsedJuzMeta>
)

object QuranDataXmlParser {

    fun parse(inputStream: InputStream): QuranXmlResult {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(inputStream)
        doc.documentElement.normalize()

        val suras = mutableListOf<ParsedSurahMeta>()
        val suraNodes = doc.getElementsByTagName("sura")
        for (i in 0 until suraNodes.length) {
            val element = suraNodes.item(i) as Element
            val index = element.getAttribute("index").toInt()
            val ayas = element.getAttribute("ayas").toInt()
            val name = element.getAttribute("name")
            val tname = element.getAttribute("tname")
            val ename = element.getAttribute("ename")
            val type = element.getAttribute("type")
            val order = element.getAttribute("order").toInt()

            suras.add(
                ParsedSurahMeta(
                    index = index,
                    ayas = ayas,
                    name = name,
                    tname = tname,
                    ename = ename,
                    type = type,
                    order = order
                )
            )
        }

        val juzs = mutableListOf<ParsedJuzMeta>()
        val juzNodes = doc.getElementsByTagName("juz")
        for (i in 0 until juzNodes.length) {
            val element = juzNodes.item(i) as Element
            val index = element.getAttribute("index").toInt()
            val sura = element.getAttribute("sura").toInt()
            val aya = element.getAttribute("aya").toInt()

            juzs.add(
                ParsedJuzMeta(
                    index = index,
                    sura = sura,
                    aya = aya
                )
            )
        }

        return QuranXmlResult(suras, juzs)
    }
}
