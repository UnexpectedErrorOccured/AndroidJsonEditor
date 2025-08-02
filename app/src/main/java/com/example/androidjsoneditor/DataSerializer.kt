package com.example.androidjsoneditor

import org.json.JSONArray
import org.json.JSONObject

/**
 * DataNode 리스트를 JSON 또는 XML 문자열로 변환하는 클래스
 */
object DataSerializer {

    // --- JSON Serialization ---

    /**
     * DataNode 리스트를 예쁘게 포맷팅된 JSON 문자열로 변환합니다.
     * @param nodes 변환할 최상위 DataNode 리스트
     * @return JSON 형식의 문자열
     */
    fun toJsonString(nodes: List<DataNode>): String {
        if (nodes.isEmpty()) return ""
        val rootNode = nodes.first()

        return when (rootNode.type) {
            NodeType.OBJECT -> convertNodeToJsonObject(rootNode).toString(4)
            NodeType.ARRAY -> convertNodeToJsonArray(rootNode).toString(4)
            else -> rootNode.value ?: ""
        }
    }

    private fun convertNodeToJsonObject(node: DataNode): JSONObject {
        val jsonObject = JSONObject()
        node.children.forEach { child ->
            val value = getJsonValue(child)
            jsonObject.put(child.name, value)
        }
        return jsonObject
    }

    private fun convertNodeToJsonArray(node: DataNode): JSONArray {
        val jsonArray = JSONArray()
        node.children.forEach { child ->
            val value = getJsonValue(child)
            jsonArray.put(value)
        }
        return jsonArray
    }

    private fun getJsonValue(node: DataNode): Any {
        return when (node.type) {
            NodeType.OBJECT -> convertNodeToJsonObject(node)
            NodeType.ARRAY -> convertNodeToJsonArray(node)
            NodeType.STRING -> node.value ?: ""
            NodeType.NUMBER -> node.value?.toDoubleOrNull() ?: 0.0
            NodeType.BOOLEAN -> node.value?.toBoolean() ?: false
            NodeType.NULL -> JSONObject.NULL
            else -> JSONObject.NULL // XML ELEMENT는 JSON으로 변환 시 무시
        }
    }

    // --- XML Serialization (새로 추가된 부분) ---

    /**
     * DataNode 리스트를 예쁘게 포맷팅된 XML 문자열로 변환합니다.
     * @param nodes 변환할 최상위 DataNode 리스트
     * @return XML 형식의 문자열
     */
    fun toXmlString(nodes: List<DataNode>): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        nodes.forEach { node ->
            buildXmlNode(node, builder, 0)
        }
        return builder.toString()
    }

    private fun buildXmlNode(node: DataNode, builder: StringBuilder, indentLevel: Int) {
        // JSON 타입은 XML로 변환하지 않음
        if (node.type != NodeType.ELEMENT) return

        val indent = "    ".repeat(indentLevel)
        builder.append("$indent<${node.name}")

        // 속성 추가
        if (node.attributes.isNotEmpty()) {
            node.attributes.forEach { (key, value) ->
                builder.append(" $key=\"$value\"")
            }
        }

        if (node.children.isEmpty() && node.value.isNullOrEmpty()) {
            // 자식과 값이 모두 없으면 self-closing 태그
            builder.append("/>\n")
        } else {
            builder.append(">")
            if (node.children.isNotEmpty()) {
                // 자식이 있으면 재귀적으로 자식 노드 빌드
                builder.append("\n")
                node.children.forEach { child ->
                    buildXmlNode(child, builder, indentLevel + 1)
                }
                builder.append("$indent</${node.name}>\n")
            } else {
                // 값만 있으면 값을 추가
                builder.append(node.value ?: "")
                builder.append("</${node.name}>\n")
            }
        }
    }
}
