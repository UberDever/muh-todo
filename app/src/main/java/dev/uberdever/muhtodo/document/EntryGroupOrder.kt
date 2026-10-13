package dev.uberdever.muhtodo.document

enum class EntryGroup { INCOMPLETE, COMPLETE, BULLETS }

object EntryGroupOrder {
    val default = listOf(EntryGroup.INCOMPLETE, EntryGroup.BULLETS, EntryGroup.COMPLETE)
    fun validated(order: List<EntryGroup>): List<EntryGroup> =
        if (order.size == 3 && order.toSet().size == 3) order.toList() else default
    fun parse(tokens: List<String>): List<EntryGroup> =
        runCatching { validated(tokens.map(EntryGroup::valueOf)) }.getOrDefault(default)
    fun move(order: List<EntryGroup>, group: EntryGroup, target: Int): List<EntryGroup> =
        validated(order).toMutableList().apply { remove(group); add(target.coerceIn(0, size), group) }
    fun group(task: Todo) = when {
        !task.hasCheckbox -> EntryGroup.BULLETS
        task.completed -> EntryGroup.COMPLETE
        else -> EntryGroup.INCOMPLETE
    }
}
