package com.xhlab.yafc.ide.settings.yafc

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.xhlab.yafc.model.YAFCProject
import com.xhlab.yafc.model.YAFCProjectPreferences
import com.xhlab.yafc.model.data.EntityBelt
import com.xhlab.yafc.model.data.EntityInserter
import org.jdom.Attribute
import org.jdom.Element

@State(
    name = "YAFCProjectPreferences",
    storages = [Storage("yafc.xml")]
)
class YAFCIntellijProjectPreferences(
    private val project: Project
) : PersistentStateComponent<Element>, YAFCProjectPreferences {
    @Volatile
    override var time: Int = 1

    @Volatile
    override var itemUnit: Float = 0f

    @Volatile
    override var fluidUnit: Float = 0f

    private var defaultBeltName: String? = null

    @Volatile
    override var defaultBelt: EntityBelt? = null
        get() {
            val name = defaultBeltName
            if (field == null && name != null) {
                field = project.service<YAFCProject>().storage?.db?.allBelts?.find { it.typeDotName == name }
            }
            return field
        }
        set(value) {
            if (value == null) {
                return
            }
            defaultBeltName = value.typeDotName
            field = value
        }

    private var defaultInserterName: String? = null

    @Volatile
    override var defaultInserter: EntityInserter? = null
        get() {
            val name = defaultInserterName
            if (field == null && name != null) {
                field = project.service<YAFCProject>().storage?.db?.allInserters?.find { it.typeDotName == name }
            }
            return field
        }
        set(value) {
            if (value == null) {
                return
            }
            defaultInserterName = value.typeDotName
            field = value
        }

    @Volatile
    override var inserterCapacity: Int = 1

    private val _sourceResources = hashSetOf<String>()
    override val sourceResources: Set<String>
        get() = _sourceResources

    private val _favourites = hashSetOf<String>()
    override val favourites: Set<String>
        get() = _favourites

    @Volatile
    override var targetTechnology: String? = null

    override fun getState(): Element {
        return Element(PREFERENCES_TAG_NAME).apply {
            attributes = listOf(
                Attribute(TIME_TAG_NAME, time.toString()),
                Attribute(ITEM_UNIT_TAG_NAME, itemUnit.toString()),
                Attribute(FLUID_UNIT_TAG_NAME, fluidUnit.toString()),
                Attribute(DEFAULT_BELT, defaultBeltName),
                Attribute(DEFAULT_INSERTER, defaultInserterName),
                Attribute(INSERTER_CAPACITY_TAG_NAME, inserterCapacity.toString())
            )
        }
    }

    override fun loadState(state: Element) {
        time = state.getAttribute(TIME_TAG_NAME)?.intValue ?: 1
        itemUnit = state.getAttribute(ITEM_UNIT_TAG_NAME)?.floatValue ?: 0f
        fluidUnit = state.getAttribute(FLUID_UNIT_TAG_NAME)?.floatValue ?: 0f
        defaultBeltName = state.getAttribute(DEFAULT_BELT)?.value
        defaultInserterName = state.getAttribute(DEFAULT_INSERTER)?.value
        inserterCapacity = state.getAttribute(INSERTER_CAPACITY_TAG_NAME)?.intValue ?: 1
    }

    companion object {
        private const val PREFERENCES_TAG_NAME = "preferences"
        private const val TIME_TAG_NAME = "time"
        private const val ITEM_UNIT_TAG_NAME = "item_unit"
        private const val FLUID_UNIT_TAG_NAME = "fluid_unit"
        private const val DEFAULT_BELT = "default_belt"
        private const val DEFAULT_INSERTER = "default_inserter"
        private const val INSERTER_CAPACITY_TAG_NAME = "inserter_capacity"
    }
}
