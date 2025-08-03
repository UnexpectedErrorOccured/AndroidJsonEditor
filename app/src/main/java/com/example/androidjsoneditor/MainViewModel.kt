package com.example.androidjsoneditor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    // 파일 종류를 저장하는 LiveData (UI가 관찰할 수 있음)
    private val _fileType = MutableLiveData<MainActivity.FileType>()
    val fileType: LiveData<MainActivity.FileType> = _fileType

    // 실제 데이터(Node Tree)를 저장하는 LiveData
    private val _rootNodes = MutableLiveData<MutableList<DataNode>>()
    val rootNodes: LiveData<MutableList<DataNode>> = _rootNodes

    /**
     * 파일 내용을 파싱하여 데이터를 로드하는 함수
     */
    fun loadData(content: String) {
        val trimmedContent = content.trim()
        val type = when {
            trimmedContent.startsWith("{") || trimmedContent.startsWith("[") -> MainActivity.FileType.JSON
            trimmedContent.startsWith("<") -> MainActivity.FileType.XML
            else -> throw IllegalArgumentException("지원하지 않는 파일 형식입니다.")
        }
        _fileType.value = type

        val parsedNodes = DataParser.parse(content).toMutableList()
        collapseAllNodes(parsedNodes) // 모든 노드를 접은 상태로 시작
        _rootNodes.value = parsedNodes
    }

    /**
     * 노드를 추가, 수정, 삭제하는 함수들
     * 이제 데이터 변경은 모두 ViewModel에서 이루어집니다.
     */
    fun editNode(targetNodeId: String, name: String, value: String) {
        _rootNodes.value?.let { nodes ->
            findNodeById(nodes, targetNodeId)?.apply {
                this.name = name
                this.value = value
            }
            // LiveData에 변경사항을 알리기 위해 값을 다시 할당
            _rootNodes.value = nodes
        }
    }

    fun addChildNode(parentNodeId: String, name: String, value: String, type: NodeType) {
        _rootNodes.value?.let { nodes ->
            findNodeById(nodes, parentNodeId)?.apply {
                val newNode = DataNode(name = name, value = value, type = type, level = this.level + 1)
                this.children.add(newNode)
            }
            _rootNodes.value = nodes
        }
    }

    fun deleteNode(nodeToDelete: DataNode) {
        _rootNodes.value?.let { nodes ->
            findAndRemoveNode(nodes, nodeToDelete)
            _rootNodes.value = nodes
        }
    }

    // --- Helper Functions ---
    private fun collapseAllNodes(nodes: List<DataNode>) {
        nodes.forEach { node ->
            if (node.children.isNotEmpty()) {
                node.isExpanded = false
                collapseAllNodes(node.children)
            }
        }
    }

    private fun findNodeById(nodes: List<DataNode>, id: String): DataNode? {
        for (node in nodes) {
            if (node.id == id) return node
            val found = findNodeById(node.children, id)
            if (found != null) return found
        }
        return null
    }

    private fun findAndRemoveNode(nodes: MutableList<DataNode>, nodeToRemove: DataNode): Boolean {
        if (nodes.remove(nodeToRemove)) return true
        for (node in nodes) {
            if (node.children.isNotEmpty() && findAndRemoveNode(node.children, nodeToRemove)) {
                return true
            }
        }
        return false
    }
}
