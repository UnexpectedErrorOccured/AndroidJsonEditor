package com.example.androidjsoneditor

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.ArrayDeque

/**
 * JSON 및 XML 문자열을 파싱하여 DataNode 리스트로 변환하는 클래스
 */
object DataParser {

    /**
     * 입력된 문자열을 분석하여 JSON 또는 XML 파서를 호출하는 메인 함수
     * @param data 파싱할 원본 문자열
     * @return 변환된 DataNode의 루트 리스트
     */
    fun parse(data: String): List<DataNode> {
        val trimmedData = data.trim()
        return when {
            // 문자열이 '{' 또는 '[' 로 시작하면 JSON으로 간주
            trimmedData.startsWith("{") || trimmedData.startsWith("[") -> {
                val jsonElement = JsonParser.parseString(trimmedData)
                parseJsonElement("root", jsonElement, 0)
            }
            // 문자열이 '<' 로 시작하면 XML으로 간주
            trimmedData.startsWith("<") -> {
                parseXml(trimmedData)
            }
            else -> throw IllegalArgumentException("Unsupported data format")
        }
    }

    // --- JSON 파싱 로직 ---

    /**
     * JsonElement를 재귀적으로 분석하여 DataNode 리스트로 변환
     * @param name 현재 요소의 이름 (Key)
     * @param element 분석할 JsonElement
     * @param level 현재 계층의 깊이
     * @return 변환된 DataNode 리스트
     */
    private fun parseJsonElement(name: String, element: JsonElement, level: Int): List<DataNode> {
        val nodes = mutableListOf<DataNode>()
        when {
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val parentNode = DataNode(name = name, value = null, type = NodeType.OBJECT, level = level)
                obj.entrySet().forEach { (key, value) ->
                    parentNode.children.addAll(parseJsonElement(key, value, level + 1))
                }
                nodes.add(parentNode)
            }
            element.isJsonArray -> {
                val arr = element.asJsonArray
                val parentNode = DataNode(name = name, value = null, type = NodeType.ARRAY, level = level)
                arr.forEachIndexed { index, item ->
                    parentNode.children.addAll(parseJsonElement("[$index]", item, level + 1))
                }
                nodes.add(parentNode)
            }
            element.isJsonPrimitive -> {
                val primitive = element.asJsonPrimitive
                val nodeType = when {
                    primitive.isString -> NodeType.STRING
                    primitive.isNumber -> NodeType.NUMBER
                    primitive.isBoolean -> NodeType.BOOLEAN
                    else -> NodeType.NULL
                }
                // JsonPrimitive는 항상 따옴표를 포함한 값을 반환하므로, asString 사용
                nodes.add(DataNode(name = name, value = primitive.asString, type = nodeType, level = level))
            }
            element.isJsonNull -> {
                nodes.add(DataNode(name = name, value = "null", type = NodeType.NULL, level = level))
            }
        }
        return nodes
    }

    // --- XML 파싱 로직 ---

    /**
     * XML 문자열을 분석하여 DataNode 리스트로 변환
     * @param xmlData 파싱할 XML 문자열
     * @return 변환된 DataNode의 루트 리스트
     */
    private fun parseXml(xmlData: String): List<DataNode> {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlData))

        val rootNodes = mutableListOf<DataNode>()
        val nodeStack = ArrayDeque<DataNode>() // 계층 구조 관리를 위한 스택
        var eventType = parser.eventType
        var currentLevel = 0

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val newNode = DataNode(
                        name = parser.name,
                        value = null,
                        type = NodeType.ELEMENT,
                        level = currentLevel
                    )
                    // 속성(Attribute) 파싱
                    for (i in 0 until parser.attributeCount) {
                        newNode.attributes[parser.getAttributeName(i)] = parser.getAttributeValue(i)
                    }

                    if (nodeStack.isNotEmpty()) {
                        nodeStack.peek()?.children?.add(newNode)
                    } else {
                        rootNodes.add(newNode)
                    }
                    nodeStack.push(newNode)
                    currentLevel++
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text.trim()
                    if (text.isNotEmpty() && nodeStack.isNotEmpty()) {
                        nodeStack.peek()?.value = text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (nodeStack.isNotEmpty()) {
                        nodeStack.pop()
                    }
                    currentLevel--
                }
            }
            eventType = parser.next()
        }
        return rootNodes
    }
}
