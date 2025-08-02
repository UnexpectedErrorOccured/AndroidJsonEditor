package com.example.androidjsoneditor

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class EditNodeDialogFragment : DialogFragment() {

    // 다이얼로그의 결과를 액티비티로 전달하기 위한 리스너 인터페이스
    interface EditNodeDialogListener {
        fun onDialogPositiveClick(
            dialog: DialogFragment,
            targetNodeId: String?, // 수정 대상 노드의 ID (추가 모드일 경우 null)
            parentNodeId: String?, // 부모 노드의 ID (자식 추가 모드일 경우)
            name: String,
            value: String,
            type: NodeType
        )
    }

    private lateinit var listener: EditNodeDialogListener
    private lateinit var nameEditText: EditText
    private lateinit var valueEditText: EditText
    private lateinit var typeSpinner: Spinner

    // 액티비티가 리스너를 구현했는지 확인
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as EditNodeDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement EditNodeDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_node, null)

        nameEditText = view.findViewById(R.id.edit_node_name)
        valueEditText = view.findViewById(R.id.edit_node_value)
        typeSpinner = view.findViewById(R.id.type_spinner)

        // 스피너에 데이터 타입 목록 설정
        typeSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            NodeType.values().map { it.name }
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // 전달받은 인자(argument)를 기반으로 다이얼로그 초기화
        val nodeName = arguments?.getString(ARG_NAME)
        val nodeValue = arguments?.getString(ARG_VALUE)
        val nodeTypeOrdinal = arguments?.getInt(ARG_TYPE, NodeType.STRING.ordinal) ?: NodeType.STRING.ordinal

        if (nodeName != null) {
            nameEditText.setText(nodeName)
            valueEditText.setText(nodeValue)
            typeSpinner.setSelection(nodeTypeOrdinal)
        }

        val dialogTitle = arguments?.getString(ARG_TITLE) ?: "노드 추가"

        builder.setView(view)
            .setTitle(dialogTitle)
            .setPositiveButton("저장") { _, _ ->
                // "저장" 버튼 클릭 시, 입력된 데이터를 리스너를 통해 전달
                val name = nameEditText.text.toString()
                val value = valueEditText.text.toString()
                val selectedType = NodeType.values()[typeSpinner.selectedItemPosition]
                val targetNodeId = arguments?.getString(ARG_TARGET_ID)
                val parentNodeId = arguments?.getString(ARG_PARENT_ID)

                listener.onDialogPositiveClick(this, targetNodeId, parentNodeId, name, value, selectedType)
            }
            .setNegativeButton("취소") { _, _ ->
                dialog?.cancel()
            }

        return builder.create()
    }

    companion object {
        // 다이얼로그에 데이터를 전달하기 위한 키 값들
        private const val ARG_TITLE = "title"
        private const val ARG_NAME = "name"
        private const val ARG_VALUE = "value"
        private const val ARG_TYPE = "type"
        private const val ARG_TARGET_ID = "target_id" // 수정할 노드의 ID
        private const val ARG_PARENT_ID = "parent_id" // 자식을 추가할 부모 노드의 ID

        // "노드 수정"을 위한 인스턴스 생성
        fun newEditInstance(node: DataNode): EditNodeDialogFragment {
            val args = Bundle().apply {
                putString(ARG_TITLE, "노드 수정")
                putString(ARG_NAME, node.name)
                putString(ARG_VALUE, node.value)
                putInt(ARG_TYPE, node.type.ordinal)
                putString(ARG_TARGET_ID, node.id)
            }
            return EditNodeDialogFragment().apply {
                arguments = args
            }
        }

        // "자식 노드 추가"를 위한 인스턴스 생성
        fun newAddChildInstance(parentNode: DataNode): EditNodeDialogFragment {
            val args = Bundle().apply {
                putString(ARG_TITLE, "'${parentNode.name}'에 자식 추가")
                putString(ARG_PARENT_ID, parentNode.id)
            }
            return EditNodeDialogFragment().apply {
                arguments = args
            }
        }
    }
}
