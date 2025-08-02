package com.example.androidjsoneditor

/**
 * JSON과 XML의 계층 구조를 모두 표현하기 위한 통합 데이터 모델
 */
data class DataNode(
    val id: String = java.util.UUID.randomUUID().toString(), // 각 노드를 식별할 고유 ID
    var name: String, // JSON의 key 또는 XML의 태그(tag) 이름
    var value: String?, // 데이터의 값 또는 XML 태그의 텍스트 콘텐츠
    val type: NodeType, // 데이터 타입 (OBJECT, ARRAY, ELEMENT 등)
    val level: Int, // 계층 구조의 깊이 (들여쓰기 표현용)
    var attributes: MutableMap<String, String> = mutableMapOf(), // XML의 속성(attribute) 저장용
    var isExpanded: Boolean = true, // 노드의 펼침/접힘 상태
    val children: MutableList<DataNode> = mutableListOf() // 자식 노드 리스트
)

/**
 * 데이터의 종류를 구분하기 위한 열거형 클래스
 */
enum class NodeType {
    OBJECT,  // JSON Object
    ARRAY,   // JSON Array
    ELEMENT, // XML Element
    STRING,
    NUMBER,
    BOOLEAN,
    NULL
}