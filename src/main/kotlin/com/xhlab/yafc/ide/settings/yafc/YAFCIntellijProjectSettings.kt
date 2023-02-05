package com.xhlab.yafc.ide.settings.yafc

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.xhlab.yafc.model.ProjectPerItemFlag
import com.xhlab.yafc.model.ProjectPerItemFlags
import com.xhlab.yafc.model.YAFCProjectSettings
import com.xhlab.yafc.model.util.and
import com.xhlab.yafc.model.util.inv
import com.xhlab.yafc.model.util.or
import com.xhlab.yafc.model.util.value
import org.jdom.Attribute
import org.jdom.Element
import java.util.*

@State(
    name = "YAFCProjectSettings",
    storages = [Storage("yafc.xml")]
)
class YAFCIntellijProjectSettings : PersistentStateComponent<Element>, YAFCProjectSettings {
    private val _milestones = arrayListOf<String>()
    override val milestones: List<String>
        get() = _milestones

    override val itemFlags: SortedMap<String, ProjectPerItemFlags> = TreeMap()

    @Volatile
    override var miningProductivity = 0f

    @Volatile
    override var reactorSizeX = 2f

    @Volatile
    override var reactorSizeY = 2f

    override fun setMilestones(newMilestones: List<Pair<String, Boolean>>) {
        _milestones.subtract(newMilestones.map { it.first }.toSet()).forEach { itemKey ->
            setFlag(itemKey, EnumSet.noneOf(ProjectPerItemFlag::class.java), false)
        }
        _milestones.clear()
        newMilestones.forEach { (itemKey, milestoneUnlocked) ->
            _milestones.add(itemKey)
            setFlag(itemKey, EnumSet.of(ProjectPerItemFlag.MILESTONE_UNLOCKED), milestoneUnlocked)
        }
    }

    override fun setFlag(itemKey: String, flag: ProjectPerItemFlags, set: Boolean) {
        val flags = itemFlags[itemKey] ?: EnumSet.noneOf(ProjectPerItemFlag::class.java)
        val newFlags = if (set) {
            flags or flag
        } else {
            flags and flag.inv()
        }
        if (newFlags != flags) {
            itemFlags[itemKey] = newFlags
        }
    }

    override fun getState(): Element {
        return Element(SETTINGS_TAG_NAME).apply {
            val milestones = Element(MILESTONES_TAG_NAME).apply {
                milestones.forEach { typeDotName ->
                    val milestone = Element(MILESTONE_TAG_NAME).apply {
                        addContent(typeDotName)
                    }
                    addContent(milestone)
                }
            }
            addContent(milestones)
            val itemFlags = Element(ITEM_FLAGS_TAG_NAME).apply {
                itemFlags.forEach { (itemKey, flags) ->
                    val itemFlag = Element(ITEM_FLAG_TAG_NAME).apply {
                        val attributes = listOf(
                            Attribute(KEY_TAG_NAME, itemKey),
                            Attribute(FLAGS_TAG_NAME, flags.value.toString())
                        )
                        setAttributes(attributes)
                    }
                    addContent(itemFlag)
                }
            }
            addContent(itemFlags)
            val attributes = listOf(
                Attribute(MINING_PRODUCTIVITY_TAG_NAME, miningProductivity.toString()),
                Attribute(REACTOR_SIZE_X_TAG_NAME, reactorSizeX.toString()),
                Attribute(REACTOR_SIZE_Y_TAG_NAME, reactorSizeY.toString()),
            )
            setAttributes(attributes)
        }
    }

    override fun loadState(state: Element) {
        val milestoneTags = state.getChild(MILESTONES_TAG_NAME)?.children ?: emptyList()
        val milestones = milestoneTags.map { it.text }
        _milestones.clear()
        _milestones.addAll(milestones)

        val itemFlagTags = state.getChild(ITEM_FLAGS_TAG_NAME)?.children ?: emptyList()
        itemFlagTags.forEach {
            val itemKey = it.getAttributeValue(KEY_TAG_NAME)
            val flags = ProjectPerItemFlag.fromInt(it.getAttribute(FLAGS_TAG_NAME).intValue)
            setFlag(itemKey, flags, true)
        }

        miningProductivity = state.getAttribute(MINING_PRODUCTIVITY_TAG_NAME)?.floatValue ?: 0f
        reactorSizeX = state.getAttribute(REACTOR_SIZE_X_TAG_NAME)?.floatValue ?: 2f
        reactorSizeY = state.getAttribute(REACTOR_SIZE_Y_TAG_NAME)?.floatValue ?: 2f
    }

    companion object {
        private const val SETTINGS_TAG_NAME = "settings"
        private const val MILESTONES_TAG_NAME = "milestones"
        private const val MILESTONE_TAG_NAME = "milestone"
        private const val ITEM_FLAGS_TAG_NAME = "item_flags"
        private const val ITEM_FLAG_TAG_NAME = "item_flag"
        private const val KEY_TAG_NAME = "key"
        private const val FLAGS_TAG_NAME = "flags"
        private const val MINING_PRODUCTIVITY_TAG_NAME = "mining_productivity"
        private const val REACTOR_SIZE_X_TAG_NAME = "reactor_size_x"
        private const val REACTOR_SIZE_Y_TAG_NAME = "reactor_size_y"
    }
}
