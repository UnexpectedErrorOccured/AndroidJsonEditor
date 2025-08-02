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
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val node = displayNodes[position]
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

        // 들여쓰기 설정
        holder.indentationSpace.layoutParams.width = node.level * 48

        // 이름(Key/Tag) 설정 및 색상 적용
        holder.nodeName.text = if (node.type == NodeType.ARRAY || node.value != null) "${node.name}:" else node.name
        holder.nodeName.setTextColor(ContextCompat.getColor(context, R.color.syntax_key))

        // 값(Value) 설정
        holder.nodeValue.text = if (node.type == NodeType.STRING) "\"${node.value}\"" else node.value

        // 값(Value) 타입에 따른 색상 적용
        val valueColor = when (node.type) {
            NodeType.STRING -> R.color.syntax_string
            NodeType.NUMBER -> R.color.syntax_number
            NodeType.BOOLEAN -> R.color.syntax_boolean
            NodeType.NULL -> R.color.syntax_null
            else -> R.color.syntax_default_text // OBJECT, ARRAY, ELEMENT 등은 값이 없으므로 기본 색상
        }
        holder.nodeValue.setTextColor(ContextCompat.getColor(context, valueColor))


        // 속성(Attribute) 설정 및 색상 적용
        if (node.attributes.isNotEmpty()) {
            holder.nodeAttributes.visibility = View.VISIBLE
            holder.nodeAttributes.text = node.attributes.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }
            holder.nodeAttributes.setTextColor(ContextCompat.getColor(context, R.color.syntax_attribute))
        } else {
            holder.nodeAttributes.visibility = View.GONE
        }

        // 펼치기/접기 아이콘 설정
        if (node.children.isNotEmpty()) {
            holder.expandIcon.visibility = View.VISIBLE
            holder.expandIcon.setImageResource(if (node.isExpanded) R.drawable.ic_arrow_down else R.drawable.ic_arrow_right)
        } else {
            holder.expandIcon.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int = displayNodes.size

    private fun toggleNode(position: Int) {
        val node = displayNodes[position]
        node.isExpanded = !node.isExpanded

        if (node.isExpanded) {
            val children = getAllChildren(node)
            displayNodes.addAll(position + 1, children)
            notifyItemRangeInserted(position + 1, children.size)
        } else {
            val childrenCount = countVisibleChildren(node)
            if (childrenCount > 0) {
                repeat(childrenCount) {
                    displayNodes.removeAt(position + 1)
                }
                notifyItemRangeRemoved(position + 1, childrenCount)
            }
        }
        notifyItemChanged(position)
    }

    private fun getAllChildren(node: DataNode): List<DataNode> {
        val children = mutableListOf<DataNode>()
        if (!node.isExpanded) return children
        for (child in node.children) {
            children.add(child)
            children.addAll(getAllChildren(child))
        }
        return children
    }

    private fun countVisibleChildren(node: DataNode): Int = getAllChildren(node).size

    fun getLongClickedPosition(): Int = longClickedPosition

    private fun setLongClickedPosition(position: Int) {
        longClickedPosition = position
    }

    fun updateData(newNodes: List<DataNode>) {
        displayNodes.clear()
        displayNodes.addAll(newNodes)
        notifyDataSetChanged()
    }
}
