package com.xhlab.yafc.ide.settings.yafc

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.ide.ui.FactorioObjectCellType
import com.xhlab.yafc.ide.ui.YAFCFactorioObjectListCellRenderer
import com.xhlab.yafc.model.YAFCProject
import com.xhlab.yafc.model.YAFCProjectPreferences
import com.xhlab.yafc.model.YAFCProjectSettings
import com.xhlab.yafc.model.YAFCStorage
import com.xhlab.yafc.model.data.*
import java.awt.event.ActionEvent
import javax.swing.ComboBoxModel
import javax.swing.JButton
import javax.swing.JComponent
import kotlin.reflect.KMutableProperty0

// TODO: refresh affected uis after apply()
class YAFCPreferencesView(project: Project) {
    private val pref = project.service<YAFCProjectPreferences>()
    private val settings = project.service<YAFCProjectSettings>()

    private val defaultBeltComboBoxModel = MutableCollectionComboBoxModel<EntityBelt>().apply {
        val storage = project.service<YAFCProject>().storage
        if (storage != null) {
            add(getAllBelts(storage))
        }
    }
    private val defaultInserterComboBoxModel = MutableCollectionComboBoxModel<EntityInserter>().apply {
        val storage = project.service<YAFCProject>().storage
        if (storage != null) {
            add(getAllInserters(storage))
        }
    }

    private val root = panel {
        val initialUnitOfTime = UnitOfTime.fromValue(pref.time)

        lateinit var unitOfTimeCustomTextField: Cell<JBTextField>
        lateinit var simpleItemAmountsRadioButton: Cell<JBRadioButton>
        lateinit var simpleFluidAmountsRadioButton: Cell<JBRadioButton>

        fun newUnitOfTime(): Int {
            return unitOfTimeCustomTextField.component.text.toIntOrNull() ?: 0
        }

        fun unitPerTimeGroup(fluid: Boolean, initial: ProductionPerConsumption): Row {
            val groupTitle = YAFCBundle.message(
                when (fluid) {
                    true -> "settings.yafc.preferences.group.fluid.production.per.consumption"
                    false -> "settings.yafc.preferences.group.item.production.per.consumption"
                }
            )
            return group(groupTitle) {
                val bind = when (fluid) {
                    true -> MutableProperty({ pref.fluidUnit.toInt() }, { pref.fluidUnit = it.toFloat() })
                    false -> MutableProperty({ pref.itemUnit.toInt() }, { pref.itemUnit = it.toFloat() })
                }
                // this value is not actually used
                var unitPerTime: Int = when (fluid) {
                    true -> pref.fluidUnit.toInt()
                    false -> pref.itemUnit.toInt()
                }
                buttonsGroup {
                    lateinit var customTextField: Cell<JBTextField>

                    val actionListener = { e: ActionEvent, _: JBRadioButton ->
                        val newItemUnit = ProductionPerConsumption.valueOf(e.actionCommand)
                        customTextField.component.text = newItemUnit.value.toString()
                    }

                    row {
                        radioButton(
                            YAFCBundle.message(
                                "settings.yafc.preferences.radio.simple.amounts",
                                DataUtils.unitOfTime(newUnitOfTime()).second
                            ),
                            ProductionPerConsumption.SIMPLE_AMOUNT.value
                        ).actionListener(actionListener).apply {
                            component.actionCommand = ProductionPerConsumption.SIMPLE_AMOUNT.name
                            component.isSelected = (initial == ProductionPerConsumption.SIMPLE_AMOUNT)
                            when (fluid) {
                                true -> simpleFluidAmountsRadioButton = this
                                false -> simpleItemAmountsRadioButton = this
                            }
                        }
                    }
                    row {
                        val customCheckBox = radioButton(
                            YAFCBundle.message("settings.yafc.preferences.radio.custom.unit.equals"),
                            ProductionPerConsumption.CUSTOM.value
                        ).actionListener(actionListener).apply {
                            component.actionCommand = ProductionPerConsumption.CUSTOM.name
                            component.isSelected = (initial == ProductionPerConsumption.CUSTOM)
                        }
                        customTextField = intTextField(1..Int.MAX_VALUE)
                            .bindIntText({ bind.get() }, { bind.set(it) })
                            .enabledIf(customCheckBox.selected)
                            .onApply {
                                val newUnit = customTextField.component.text.toFloatOrNull() ?: 0f
                                when (fluid) {
                                    true -> pref.fluidUnit = newUnit
                                    false -> pref.itemUnit = newUnit
                                }
                            }
                        label(YAFCBundle.message("settings.yafc.preferences.radio.per.second"))
                        if (!fluid) {
                            lateinit var setFromBelt: Cell<JButton>
                            setFromBelt = button(YAFCBundle.message("settings.yafc.preferences.button.set.from.belt")) {
                                showBeltChooserPopup(project, setFromBelt.component, { newUnitOfTime() }) {
                                    customTextField.component.text = it.beltItemsPerSecond.toInt().toString()
                                }
                            }.enabledIf(customCheckBox.selected)
                        }
                    }
                }.bind({ unitPerTime }, { unitPerTime = it })
            }
        }

        var timeUnit: Int = pref.time // this value is not actually used
        group(YAFCBundle.message("settings.yafc.preferences.group.unit.of.time")) {
            buttonsGroup {
                row {
                    val actionListener = { e: ActionEvent, _: JBRadioButton ->
                        val newUnitOfTime = UnitOfTime.valueOf(e.actionCommand)
                        unitOfTimeCustomTextField.component.text = newUnitOfTime.value.toString()
                        val simpleAmountsText = YAFCBundle.message(
                            "settings.yafc.preferences.radio.simple.amounts",
                            DataUtils.unitOfTime(newUnitOfTime()).second
                        )
                        simpleItemAmountsRadioButton.component.text = simpleAmountsText
                        simpleFluidAmountsRadioButton.component.text = simpleAmountsText
                    }

                    radioButton(
                        YAFCBundle.message("settings.yafc.preferences.radio.second"),
                        UnitOfTime.SECOND.value
                    ).actionListener(actionListener).apply {
                        component.actionCommand = UnitOfTime.SECOND.name
                        component.isSelected = (initialUnitOfTime == UnitOfTime.SECOND)
                    }
                    radioButton(
                        YAFCBundle.message("settings.yafc.preferences.radio.minute"),
                        UnitOfTime.MINUTE.value
                    ).actionListener(actionListener).apply {
                        component.actionCommand = UnitOfTime.MINUTE.name
                        component.isSelected = (initialUnitOfTime == UnitOfTime.MINUTE)
                    }
                    radioButton(
                        YAFCBundle.message("settings.yafc.preferences.radio.hour"),
                        UnitOfTime.HOUR.value
                    ).actionListener(actionListener).apply {
                        component.actionCommand = UnitOfTime.HOUR.name
                        component.isSelected = (initialUnitOfTime == UnitOfTime.HOUR)
                    }
                    val customCheckbox = radioButton(
                        YAFCBundle.message("settings.yafc.preferences.radio.custom"),
                        UnitOfTime.CUSTOM.value
                    ).actionListener(actionListener).apply {
                        component.actionCommand = UnitOfTime.CUSTOM.name
                        component.isSelected = (initialUnitOfTime == UnitOfTime.CUSTOM)
                    }
                    unitOfTimeCustomTextField = intTextField(0..Int.MAX_VALUE)
                        .bindIntText(pref::time)
                        .enabledIf(customCheckbox.selected)
                        .onApply {
                            pref.time = newUnitOfTime()
                        }
                }
            }.bind({ timeUnit }, { timeUnit = it })
        }

        val initialItemUnit = ProductionPerConsumption.fromValue(pref.itemUnit)
        unitPerTimeGroup(false, initialItemUnit)

        val initialFluidUnit = ProductionPerConsumption.fromValue(pref.fluidUnit)
        unitPerTimeGroup(true, initialFluidUnit)

        row(YAFCBundle.message("settings.yafc.preferences.group.default.belt")) {
            chooserComboBox(project, pref::defaultBelt, defaultBeltComboBoxModel)
                .widthGroup(DEFAULTS_GROUP)
        }

        row(YAFCBundle.message("settings.yafc.preferences.group.default.inserter")) {
            chooserComboBox(project, pref::defaultInserter, defaultInserterComboBoxModel)
                .widthGroup(DEFAULTS_GROUP)
        }

        row(YAFCBundle.message("settings.yafc.preferences.group.inserter.capacity")) {
            spinner(1..Int.MAX_VALUE).bindIntValue(pref::inserterCapacity)
        }

        row(YAFCBundle.message("settings.yafc.preferences.group.reactor.layout")) {
            intTextField(1..32)
                .bindIntText({ settings.reactorSizeX.toInt() }, { settings.reactorSizeX = it.toFloat() })
                .gap(RightGap.SMALL)
                .widthGroup(REACTOR_LAYOUT_GROUP)
            label("✕")
                .gap(RightGap.SMALL)
            intTextField(1..32)
                .bindIntText({ settings.reactorSizeY.toInt() }, { settings.reactorSizeY = it.toFloat() })
                .widthGroup(REACTOR_LAYOUT_GROUP)
        }
    }

    private fun showBeltChooserPopup(
        project: Project,
        component: JComponent,
        time: () -> Int,
        onChosen: (EntityBelt) -> Unit
    ) {
        val storage = project.service<YAFCProject>().storage
        if (storage != null) {
            val belts = storage.db.allBelts.sortedWith(DataUtils.getDefaultOrdering(storage.analyses))
            JBPopupFactory.getInstance().createPopupChooserBuilder(belts)
                .setRenderer(
                    YAFCFactorioObjectListCellRenderer(project, FactorioObjectCellType.NORMAL) {
                        (it as? EntityBelt)?.let { belt ->
                            DataUtils.formatPerSecondAmount(time(), belt.beltItemsPerSecond)
                        }
                    }
                )
                .setItemChosenCallback(onChosen)
                .createPopup()
                .show(RelativePoint(component.locationOnScreen.apply {
                    y += component.height
                }))
        }
    }

    private fun <T : FactorioObject> Row.chooserComboBox(
        project: Project,
        bind: KMutableProperty0<T?>,
        model: ComboBoxModel<T>
    ): Cell<ComboBox<T>> {
        val renderer = YAFCFactorioObjectListCellRenderer(project, FactorioObjectCellType.NORMAL)
        return comboBox(model, renderer)
            .bindItemNullable(bind)
    }

    val component: DialogPanel = root

    fun reload(project: Project) {
        val storage = project.service<YAFCProject>().storage
        if (storage != null) {
            defaultBeltComboBoxModel.update(getAllBelts(storage))
            defaultInserterComboBoxModel.update(getAllInserters(storage))
        }
        root.reset()
    }

    private fun getAllBelts(storage: YAFCStorage): List<EntityBelt> {
        return storage.db.allBelts.sortedWith(DataUtils.getDefaultOrdering(storage.analyses))
    }

    private fun getAllInserters(storage: YAFCStorage): List<EntityInserter> {
        return storage.db.allInserters.sortedWith(DataUtils.getDefaultOrdering(storage.analyses))
    }

    private enum class UnitOfTime(val value: Int) {
        SECOND(1),
        MINUTE(60),
        HOUR(3600),
        CUSTOM(0);

        companion object {
            fun fromValue(value: Int) = when (value) {
                SECOND.value -> SECOND
                MINUTE.value -> MINUTE
                HOUR.value -> HOUR
                else -> CUSTOM
            }
        }
    }

    private enum class ProductionPerConsumption(val value: Int) {
        SIMPLE_AMOUNT(0),
        CUSTOM(1);

        companion object {
            fun fromValue(value: Float) = when (value) {
                SIMPLE_AMOUNT.value.toFloat() -> SIMPLE_AMOUNT
                else -> CUSTOM
            }
        }
    }

    companion object {
        private const val DEFAULTS_GROUP = "defaults"
        private const val REACTOR_LAYOUT_GROUP = "reactor_layout"
    }
}
