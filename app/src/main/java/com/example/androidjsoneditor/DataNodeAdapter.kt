package com.example.androidjsoneditor

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DataNodeAdapter(
    private var displayNodes: MutableList<DataNode>
) : RecyclerView.Adapter<DataNodeAdapter.DataNodeViewHolder>() {

    private var longClickedPosition: Int = -1

    inner class DataNodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {
        val indentationSpace: Space = itemView.findViewById(R.id.indentation_space)
        val expandIcon: ImageView = itemView.findViewById(R.id.expand_icon)
        val nodeName: TextView = itemView.findViewById(R.id.node_name)
        val nodeValue: TextView = itemView.findViewById(R.id.node_value)
        val nodeAttributes: TextView = itemView.findViewById(R.id.node_attributes)

        init {
            itemView.setOnCreateContextMenuListener(this)
            // 아이템 클릭 시 펼치기/접기 이벤트 처리
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val node = displayNodes[position]
                    // 자식이 있는 노드만 펼치기/접기 동작 수행
                    if (node.children.isNotEmpty()) {
                        toggleNode(position)
                    }
                }
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
            val inflater = MenuInflater(v.context)
            inflater.inflate(R.menu.node_context_menu, menu)

            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                setLongClickedPosition(position)
                val node = displayNodes[position]
                val addChildItem = menu.findItem(R.id.menu_add_child)
                addChildItem.isVisible = node.type == NodeType.OBJECT || node.type == NodeType.ARRAY || node.type == NodeType.ELEMENT
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataNodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_data_node, parent, false)
        return DataNodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: DataNodeViewHolder, position: Int) {
        val node = displayNodes[position]
        val context = holder.itemView.context

        holder.indentationSpace.layoutParams.width = node.level * 48
        holder.nodeName.text = if (node.type == NodeType.ARRAY || node.value != null) "${node.name}:" else node.name
        holder.nodeName.setTextColor(ContextCompat.getColor(context, R.color.syntax_key))
        holder.nodeValue.text = if (node.type == NodeType.STRING) "\"${node.value}\"" else node.value
        val valueColor = when (node.type) {
            NodeType.STRING -> R.color.syntax_string
            NodeType.NUMBER -> R.color.syntax_number
            NodeType.BOOLEAN -> R.color.syntax_boolean
            NodeType.NULL -> R.color.syntax_null
            else -> R.color.syntax_default_text
        }
        holder.nodeValue.setTextColor(ContextCompat.getColor(context, valueColor))

        if (node.attributes.isNotEmpty()) {
            holder.nodeAttributes.visibility = View.VISIBLE
            holder.nodeAttributes.text = node.attributes.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }
            holder.nodeAttributes.setTextColor(ContextCompat.getColor(context, R.color.syntax_attribute))
        } else {
            holder.nodeAttributes.visibility = View.GONE
        }

        if (node.children.isNotEmpty()) {
            holder.expandIcon.visibility = View.VISIBLE
            holder.expandIcon.setImageResource(if (node.isExpanded) R.drawable.ic_arrow_down else R.drawable.ic_arrow_right)
        } else {
            holder.expandIcon.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int = displayNodes.size

    /**
     * 노드를 클릭했을 때 하위 노드를 펼치거나 접는 함수
     */
    private fun toggleNode(position: Int) {
        val node = displayNodes[position]
        node.isExpanded = !node.isExpanded

        if (node.isExpanded) {
            // 노드를 펼칠 때: 자식 노드들을 현재 위치 바로 아래에 추가
            val children = node.children
            displayNodes.addAll(position + 1, children)
            notifyItemRangeInserted(position + 1, children.size)
        } else {
            // 노드를 접을 때: 화면에 보이는 모든 자손 노드를 제거
            val childrenCount = countVisibleDescendants(node)
            if (childrenCount > 0) {
                // 자식 노드들을 리스트에서 제거
                for (i in 0 until childrenCount) {
                    displayNodes.removeAt(position + 1)
                }
                notifyItemRangeRemoved(position + 1, childrenCount)
            }
        }
        // 아이콘 변경을 위해 클릭된 노드 아이템 갱신
        notifyItemChanged(position)
    }

    /**
     * 현재 노드 아래에 보이는 모든 자손 노드의 개수를 재귀적으로 계산
     */
    private fun countVisibleDescendants(node: DataNode): Int {
        if (!node.isExpanded) return 0
        var count = node.children.size
        for (child in node.children) {
            count += countVisibleDescendants(child)
        }
        return count
    }


    fun getLongClickedPosition(): Int = longClickedPosition

    private fun setLongClickedPosition(position: Int) {
        longClickedPosition = position
    }
}
