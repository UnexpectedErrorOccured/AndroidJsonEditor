package com.example.androidjsoneditor

import android.content.Context
import android.util.TypedValue
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
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
        // 새로 추가한 TextView 참조
        val childCountText: TextView = itemView.findViewById(R.id.child_count_text)

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

        holder.indentationSpace.layoutParams.width = node.level * 48

        // 이름(Key/Tag) 설정
        holder.nodeName.text = if (node.type == NodeType.ARRAY || node.value != null) "${node.name}:" else node.name
        holder.nodeName.setTextColor(context.getThemeColor(R.attr.syntaxKeyColor))

        // 값(Value) 설정
        holder.nodeValue.text = if (node.type == NodeType.STRING) "\"${node.value}\"" else node.value
        val valueColorAttr = when (node.type) {
            NodeType.STRING -> R.attr.syntaxStringColor
            NodeType.NUMBER -> R.attr.syntaxNumberColor
            NodeType.BOOLEAN -> R.attr.syntaxBooleanColor
            NodeType.NULL -> R.attr.syntaxNullColor
            else -> R.attr.syntaxDefaultTextColor
        }
        holder.nodeValue.setTextColor(context.getThemeColor(valueColorAttr))

        // 자식 노드 개수 표시 로직 (새로 추가)
        if (node.children.isNotEmpty()) {
            holder.childCountText.visibility = View.VISIBLE
            holder.childCountText.text = "[${node.children.size}]"
        } else {
            holder.childCountText.visibility = View.GONE
        }

        // 속성(Attribute) 설정
        if (node.attributes.isNotEmpty()) {
            holder.nodeAttributes.visibility = View.VISIBLE
            holder.nodeAttributes.text = node.attributes.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }
            holder.nodeAttributes.setTextColor(context.getThemeColor(R.attr.syntaxAttributeColor))
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

    @ColorInt
    private fun Context.getThemeColor(@AttrRes themeAttrId: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(themeAttrId, typedValue, true)
        return typedValue.data
    }

    override fun getItemCount(): Int = displayNodes.size

    private fun toggleNode(position: Int) {
        val node = displayNodes[position]

        if (node.isExpanded) {
            val descendantsCount = countVisibleDescendants(node)
            if (descendantsCount == 0) return
            node.isExpanded = false
            for (i in 0 until descendantsCount) {
                displayNodes.removeAt(position + 1)
            }
            notifyItemRangeRemoved(position + 1, descendantsCount)
        } else {
            node.isExpanded = true
            val children = node.children
            if (children.isEmpty()) return
            displayNodes.addAll(position + 1, children)
            notifyItemRangeInserted(position + 1, children.size)
        }
        notifyItemChanged(position)
    }

    private fun countVisibleDescendants(node: DataNode): Int {
        var count = 0
        if (node.children.isNotEmpty()) {
            for (child in node.children) {
                count += 1
                if (child.isExpanded) {
                    count += countVisibleDescendants(child)
                }
            }
        }
        return count
    }

    fun getLongClickedPosition(): Int = longClickedPosition

    private fun setLongClickedPosition(position: Int) {
        longClickedPosition = position
    }
}
