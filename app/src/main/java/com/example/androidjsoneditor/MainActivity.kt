package com.example.androidjsoneditor

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader

// 필요한 모든 클래스들을 import 합니다.
import com.example.androidjsoneditor.DataNode
import com.example.androidjsoneditor.NodeType
import com.example.androidjsoneditor.EditNodeDialogFragment
import com.example.androidjsoneditor.DataNodeAdapter
import com.example.androidjsoneditor.DataParser
import com.example.androidjsoneditor.DataSerializer

class MainActivity : AppCompatActivity(), EditNodeDialogFragment.EditNodeDialogListener {

    private enum class FileType { JSON, XML, UNKNOWN }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DataNodeAdapter

    private var rootNodes = mutableListOf<DataNode>()
    private var displayNodes = mutableListOf<DataNode>()
    private var currentFileType: FileType = FileType.UNKNOWN

    // 파일 열기 결과를 처리하는 ActivityResultLauncher
    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                try {
                    val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use(BufferedReader::readText)
                    if (content != null) {
                        // 파일 종류 판별
                        val trimmedContent = content.trim()
                        currentFileType = when {
                            trimmedContent.startsWith("{") || trimmedContent.startsWith("[") -> FileType.JSON
                            trimmedContent.startsWith("<") -> FileType.XML
                            else -> FileType.UNKNOWN
                        }

                        if (currentFileType == FileType.UNKNOWN) {
                            throw IllegalArgumentException("지원하지 않는 파일 형식입니다.")
                        }

                        rootNodes = DataParser.parse(content).toMutableList()
                        refreshDisplayList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showErrorDialog("파일을 여는 중 오류가 발생했습니다.\n\n오류: ${e.localizedMessage}")
                }
            }
        }
    }

    // 파일 저장 결과를 처리하는 ActivityResultLauncher
    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                try {
                    // 파일 종류에 따라 다른 직렬화 함수 호출
                    val contentToSave = when (currentFileType) {
                        FileType.JSON -> DataSerializer.toJsonString(rootNodes)
                        FileType.XML -> DataSerializer.toXmlString(rootNodes)
                        FileType.UNKNOWN -> {
                            Toast.makeText(this, "저장할 데이터가 없거나 파일 형식을 알 수 없습니다.", Toast.LENGTH_SHORT).show()
                            return@also
                        }
                    }

                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(contentToSave.toByteArray())
                    }
                    Toast.makeText(this, "파일이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    showErrorDialog("파일을 저장하는 중 오류가 발생했습니다.\n\n오류: ${e.localizedMessage}")
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Toast.makeText(this, "setContentView 성공", Toast.LENGTH_SHORT).show()

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        Toast.makeText(this, "툴바 초기화 성공", Toast.LENGTH_SHORT).show()
        recyclerView = findViewById(R.id.dataRecyclerView)
        if (recyclerView == null) {
            // 만약 이 토스트가 뜬다면, findViewById가 실패한 것입니다.
            Toast.makeText(this, "오류: 리사이클러뷰를 찾지 못했습니다!", Toast.LENGTH_LONG).show()
        } else {
            // 만약 이 토스트가 뜬다면, findViewById는 성공한 것입니다.
            Toast.makeText(this, "성공: 리사이클러뷰를 찾았습니다!", Toast.LENGTH_LONG).show()

            adapter = DataNodeAdapter(displayNodes)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter

            // 이 토스트가 보인 후에 앱이 튕긴다면, 범인은 100% registerForContextMenu 입니다.
            Toast.makeText(this, "이제 메뉴를 등록합니다...", Toast.LENGTH_SHORT).show()
            registerForContextMenu(recyclerView)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_open_file -> {
                openFile()
                true
            }
            R.id.menu_save_as -> {
                saveFile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("오류 발생")
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        openFileLauncher.launch(intent)
    }

    private fun saveFile() {
        // 파일 종류에 따라 기본 저장 형식과 파일명을 동적으로 변경
        val (defaultMimeType, defaultFileName) = when (currentFileType) {
            FileType.JSON -> "application/json" to "untitled.json"
            FileType.XML -> "application/xml" to "untitled.xml"
            FileType.UNKNOWN -> "text/plain" to "untitled.txt" // 데이터가 없을 경우 기본값
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = defaultMimeType
            putExtra(Intent.EXTRA_TITLE, defaultFileName)
        }
        saveFileLauncher.launch(intent)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = adapter.getLongClickedPosition()
        if (position == -1) return super.onContextItemSelected(item)

        when (item.itemId) {
            R.id.menu_edit -> {
                val nodeToEdit = displayNodes[position]
                val dialog = EditNodeDialogFragment.newEditInstance(nodeToEdit)
                dialog.show(supportFragmentManager, "EditNodeDialog")
                return true
            }
            R.id.menu_add_child -> {
                val parentNode = displayNodes[position]
                val dialog = EditNodeDialogFragment.newAddChildInstance(parentNode)
                dialog.show(supportFragmentManager, "AddChildDialog")
                return true
            }
            R.id.menu_delete -> {
                deleteNode(position)
                return true
            }
            else -> return super.onContextItemSelected(item)
        }
    }

    override fun onDialogPositiveClick(
        dialog: DialogFragment,
        targetNodeId: String?,
        parentNodeId: String?,
        name: String,
        value: String,
        type: NodeType
    ) {
        if (targetNodeId != null) {
            findNodeById(rootNodes, targetNodeId)?.apply {
                this.name = name
                this.value = value
            }
        } else if (parentNodeId != null) {
            findNodeById(rootNodes, parentNodeId)?.apply {
                val newNode = DataNode(
                    name = name,
                    value = value,
                    type = type,
                    level = this.level + 1
                )
                this.children.add(newNode)
            }
        }
        refreshDisplayList()
    }

    private fun findNodeById(nodes: List<DataNode>, id: String): DataNode? {
        for (node in nodes) {
            if (node.id == id) {
                return node
            }
            val foundInChildren = findNodeById(node.children, id)
            if (foundInChildren != null) {
                return foundInChildren
            }
        }
        return null
    }

    private fun deleteNode(position: Int) {
        val nodeToDelete = displayNodes[position]
        findAndRemoveNode(rootNodes, nodeToDelete)
        refreshDisplayList()
    }

    private fun findAndRemoveNode(nodes: MutableList<DataNode>, nodeToRemove: DataNode): Boolean {
        if (nodes.remove(nodeToRemove)) {
            return true
        }
        for (node in nodes) {
            if (node.children.isNotEmpty() && findAndRemoveNode(node.children, nodeToRemove)) {
                return true
            }
        }
        return false
    }

    private fun refreshDisplayList() {
        displayNodes.clear()
        addNodesToDisplayList(rootNodes)
        adapter.notifyDataSetChanged()
    }

    private fun addNodesToDisplayList(nodes: List<DataNode>) {
        for (node in nodes) {
            displayNodes.add(node)
            if (node.isExpanded && node.children.isNotEmpty()) {
                addNodesToDisplayList(node.children)
            }
        }
    }
}
