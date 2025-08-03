package com.example.androidjsoneditor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity(), EditNodeDialogFragment.EditNodeDialogListener {

    // MainActivity는 이제 파일 종류를 직접 알 필요가 없으므로,
    // ViewModel에서 사용할 수 있도록 public enum으로 변경하거나,
    // ViewModel 내부에 정의하고 MainActivity에서는 직접 참조하지 않도록 합니다.
    // 여기서는 간단하게 public enum으로 만듭니다.
    enum class FileType { JSON, XML, UNKNOWN }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DataNodeAdapter
    private var displayNodes = mutableListOf<DataNode>()

    // by viewModels()를 사용하여 ViewModel 인스턴스를 생성합니다.
    // 이 인스턴스는 화면 회전에도 유지됩니다.
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 어댑터 및 리사이클러뷰 설정
        adapter = DataNodeAdapter(displayNodes)
        recyclerView = findViewById(R.id.dataRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        registerForContextMenu(recyclerView)

        // ViewModel의 데이터 변경을 관찰(Observe)합니다.
        mainViewModel.rootNodes.observe(this) { nodes ->
            // ViewModel의 데이터가 변경될 때마다 화면을 새로고침합니다.
            refreshDisplayList(nodes)
        }
    }

    /**
     * 화면에 보여줄 리스트를 갱신하는 함수
     */
    private fun refreshDisplayList(rootNodes: List<DataNode>) {
        displayNodes.clear()
        addNodesToDisplayList(rootNodes)
        adapter.notifyDataSetChanged()
    }

    /**
     * 재귀적으로 노드를 순회하며 화면에 표시될 노드만 리스트에 추가하는 함수
     */
    private fun addNodesToDisplayList(nodes: List<DataNode>) {
        for (node in nodes) {
            displayNodes.add(node)
            if (node.isExpanded && node.children.isNotEmpty()) {
                addNodesToDisplayList(node.children)
            }
        }
    }

    // --- 파일 처리 로직: 이제 ViewModel을 호출합니다. ---
    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                try {
                    val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use(BufferedReader::readText)
                    if (content != null) {
                        mainViewModel.loadData(content) // ViewModel에 데이터 로드 요청
                    }
                } catch (e: Exception) {
                    showErrorDialog("파일을 여는 중 오류가 발생했습니다.\n\n오류: ${e.localizedMessage}")
                }
            }
        }
    }

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                try {
                    val contentToSave = when (mainViewModel.fileType.value) {
                        FileType.JSON -> DataSerializer.toJsonString(mainViewModel.rootNodes.value ?: emptyList())
                        FileType.XML -> DataSerializer.toXmlString(mainViewModel.rootNodes.value ?: emptyList())
                        else -> {
                            Toast.makeText(this, "저장할 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                            return@also
                        }
                    }
                    contentResolver.openOutputStream(uri)?.use { it.write(contentToSave.toByteArray()) }
                    Toast.makeText(this, "파일이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    showErrorDialog("파일을 저장하는 중 오류가 발생했습니다.\n\n오류: ${e.localizedMessage}")
                }
            }
        }
    }

    // --- 다이얼로그 결과 처리: 이제 ViewModel을 호출합니다. ---
    override fun onDialogPositiveClick(dialog: DialogFragment, targetNodeId: String?, parentNodeId: String?, name: String, value: String, type: NodeType) {
        if (targetNodeId != null) {
            mainViewModel.editNode(targetNodeId, name, value)
        } else if (parentNodeId != null) {
            mainViewModel.addChildNode(parentNodeId, name, value, type)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = adapter.getLongClickedPosition()
        if (position == -1) return super.onContextItemSelected(item)
        val selectedNode = displayNodes[position]

        return when (item.itemId) {
            R.id.menu_edit -> {
                EditNodeDialogFragment.newEditInstance(selectedNode).show(supportFragmentManager, "EditNodeDialog")
                true
            }
            R.id.menu_add_child -> {
                EditNodeDialogFragment.newAddChildInstance(selectedNode).show(supportFragmentManager, "AddChildDialog")
                true
            }
            R.id.menu_delete -> {
                mainViewModel.deleteNode(selectedNode) // ViewModel에 삭제 요청
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    // --- 메뉴 및 UI 관련 함수들 (데이터 처리 로직 없음) ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_open_file -> { openFile(); true }
            R.id.menu_save_as -> { saveFile(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        openFileLauncher.launch(intent)
    }

    private fun saveFile() {
        val (mimeType, fileName) = when (mainViewModel.fileType.value) {
            FileType.JSON -> "application/json" to "untitled.json"
            FileType.XML -> "application/xml" to "untitled.xml"
            else -> "text/plain" to "untitled.txt"
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        saveFileLauncher.launch(intent)
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this).setTitle("오류 발생").setMessage(message).setPositiveButton("확인", null).show()
    }
}
