package com.xhlab.yafc.ide.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.OnePixelDivider
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.scale.JBUIScale
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Borders
import com.intellij.util.ui.UIUtil
import com.xhlab.yafc.model.YAFCProjectPreferences
import com.xhlab.yafc.model.YAFCStorage
import com.xhlab.yafc.model.analysis.factorio.*
import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.model.util.and
import com.xhlab.yafc.model.util.toSet
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
import javax.swing.border.TitledBorder
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.round

class FactorioObjectDetailedHint private constructor(
    project: Project,
    storage: YAFCStorage,
    element: FactorioObject
) : JBPanel<JBPanel<*>>(BorderLayout()) {
    private val preferences = project.service<YAFCProjectPreferences>()

    init {
        background = UIUtil.getToolTipBackground()
        add(
            JPanel(GridLayoutManager(2, 2, JBUI.emptyInsets(), 8, 0)).apply {
                background = UIUtil.getToolTipBackground()
                val iconSize = Dimension(IconCollection.BIG_ICON_SIZE, IconCollection.BIG_ICON_SIZE)
                add(
                    JBLabel(
                        IconLoader.createLazy {
                            IconCollection.getIconWithHighestMilestoneBadge(
                                storage,
                                element,
                                FactorioObjectCellType.BIG
                            )
                        }
                    ),
                    GridConstraints(
                        0,
                        0,
                        2,
                        1,
                        GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED,
                        GridConstraints.SIZEPOLICY_FIXED,
                        iconSize,
                        iconSize,
                        iconSize
                    )
                )
                add(
                    SimpleColoredComponent().apply {
                        append(element.locName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    },
                    GridConstraints(
                        0,
                        1,
                        1,
                        1,
                        GridConstraints.ANCHOR_WEST,
                        GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_GROW or GridConstraints.SIZEPOLICY_CAN_SHRINK,
                        GridConstraints.SIZEPOLICY_CAN_GROW or GridConstraints.SIZEPOLICY_CAN_SHRINK,
                        null,
                        null,
                        null
                    )
                )
                val costAnalysis = storage.analyses.get<FactorioCostAnalysis>(FactorioAnalysisType.COST)
                if (costAnalysis != null) {
                    val cost = costAnalysis.cost[element]
                    if (cost != null && cost.isFinite()) {
                        add(
                            SimpleColoredComponent().apply {
                                append("cost per ${element.type} : ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
                                append("¥%.2f".format(cost), SimpleTextAttributes.SYNTHETIC_ATTRIBUTES)
                            },
                            GridConstraints(
                                1,
                                1,
                                1,
                                1,
                                GridConstraints.ANCHOR_WEST,
                                GridConstraints.FILL_HORIZONTAL,
                                GridConstraints.SIZEPOLICY_CAN_GROW or GridConstraints.SIZEPOLICY_CAN_SHRINK,
                                GridConstraints.SIZEPOLICY_CAN_GROW or GridConstraints.SIZEPOLICY_CAN_SHRINK,
                                null,
                                null,
                                null
                            )
                        )
                    }
                }
            },
            BorderLayout.NORTH
        )
        add(buildCommon(storage, element))
        add(buildContents(storage, element), BorderLayout.SOUTH)
    }

    private fun buildCommon(storage: YAFCStorage, element: FactorioObject): JComponent {
        return verticalList {
            add(SeparatorComponent(4, OnePixelDivider.BACKGROUND, null))
            if (element.locDescr.isNotEmpty()) {
                add(commentLabel(element.locDescr, SimpleTextAttributes.GRAYED_ATTRIBUTES))
            }
            val milestones = storage.analyses.get<FactorioMilestones>(FactorioAnalysisType.MILESTONES)
            val automation = storage.analyses.get<FactorioAutomationAnalysis>(FactorioAnalysisType.AUTOMATION)
            if (milestones != null && !milestones.isAccessible(element)) {
                val content =
                    "This ${element.type} is inaccessible, or it is only accessible through mod or map script. Middle click to open dependency analyser to investigate."
                add(commentLabel(content))
            } else if (automation != null && !automation.isAutomatable(element)) {
                val content =
                    "This ${element.type} cannot be fully automated. This means that it requires either manual crafting, or manual labor such as cutting trees."
                add(commentLabel(content))
            }
            if (milestones != null && !milestones.isAccessibleWithCurrentMilestones(element) &&
                automation != null && !automation.isAutomatableWithCurrentMilestones(element)
            ) {
                val content = "This ${element.type} cannot be fully automated at current milestones."
                add(commentLabel(content))
            }
            if (element.specialType != FactorioObjectSpecialType.NORMAL) {
                add(commentLabel("Special: ${element.specialType}"))
            }
        }
    }

    private fun buildContents(storage: YAFCStorage, element: FactorioObject): JComponent {
        return verticalList {
            when (element) {
                is Technology -> {
                    add(buildRecipe(storage, element))
                    add(buildTechnology(storage, element))
                }

                is Recipe -> {
                    add(buildRecipe(storage, element))
                }

                is Goods -> {
                    add(buildGoods(storage, element))
                }

                is Entity -> {
                    add(buildEntity(storage, element))
                }
            }
        }
    }

    private fun buildEntity(storage: YAFCStorage, entity: Entity): JComponent {
        return verticalList {
            if (entity.loot.isNotEmpty()) {
                add(iconRow("Loot", storage, entity.loot))
            }

            if (entity.mapGenerated) {
                val estimatedDensity = if (entity.mapGenDensity <= 0f) {
                    "unknown"
                } else {
                    DataUtils.formatAmount(entity.mapGenDensity, UnitOfMeasure.Plain.NONE)
                }
                add(commentLabel("Generates on map (estimated density: $estimatedDensity)"))
            }

            if (entity is EntityCrafter) {
                if (entity.recipes.isNotEmpty()) {
                    add(iconRow("Crafts", storage, entity.recipes, 2))

                    if (entity.craftingSpeed != 1f) {
                        val craftingSpeed = DataUtils.formatAmount(
                            entity.craftingSpeed,
                            UnitOfMeasure.Plain.PERCENT,
                            "Crafting speed: "
                        )
                        add(commentLabel(craftingSpeed))
                    }
                    if (entity.productivity != 0f) {
                        val craftingProductivity = DataUtils.formatAmount(
                            entity.productivity,
                            UnitOfMeasure.Plain.PERCENT,
                            "Crafting productivity: "
                        )
                        add(commentLabel(craftingProductivity))
                    }
                    if (entity.allowedEffects == AllowedEffects.noneOf(AllowedEffect::class.java)) {
                        add(commentLabel("Module slots: ${entity.moduleSlots}"))
                        if (entity.allowedEffects != AllowedEffects.allOf(AllowedEffect::class.java)) {
                            add(commentLabel("Only allowed effects: ${entity.allowedEffects}"))
                        }
                    }
                }

                if (entity.inputs.isNotEmpty()) {
                    add(iconRow("Allowed inputs", storage, entity.inputs, 2))
                }
            }

            val energy = entity.energy
            if (energy != null) {
                var energyUsage =
                    energyDescriptions[energy.type] + DataUtils.formatAmount(entity.power, UnitOfMeasure.Plain.MEGAWATT)
                if (energy.drain > 0f) {
                    energyUsage += " + " + DataUtils.formatAmount(energy.drain, UnitOfMeasure.Plain.MEGAWATT)
                }
                if (energy.type == EntityEnergyType.FLUID_FUEL ||
                    energy.type == EntityEnergyType.SOLID_FUEL ||
                    energy.type == EntityEnergyType.FLUID_HEAT
                ) {
                    add(iconRow(energyUsage, storage, energy.fuels, 2))
                }
                if (energy.emissions != 0f) {
                    var attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES
                    if (energy.emissions < 0f) {
                        attributes = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GREEN)
                        add(commentLabel("This building absorbs pollution", attributes))
                    } else if (energy.emissions >= 20f) {
                        attributes = SimpleTextAttributes.ERROR_ATTRIBUTES
                        add(commentLabel("This building contributes to global warning!", attributes))
                    }
                    val emissions = DataUtils.formatAmount(energy.emissions, UnitOfMeasure.Plain.NONE)
                    add(commentLabel("Emissions: $emissions", attributes))
                }
            }

            val miscText: String? = when (entity) {
                is EntityBelt -> {
                    DataUtils.formatAmount(
                        preferences,
                        entity.beltItemsPerSecond,
                        UnitOfMeasure.Preference.PER_SECOND,
                        "Belt throughput (Items): "
                    )
                }

                is EntityInserter -> {
                    DataUtils.formatAmount(
                        entity.inserterSwingTime,
                        UnitOfMeasure.Plain.SECOND,
                        "Swing time: "
                    )
                }

                is EntityBeacon -> {
                    DataUtils.formatAmount(
                        entity.beaconEfficiency,
                        UnitOfMeasure.Plain.PERCENT,
                        "Beacon efficiency: "
                    )
                }

                is EntityAccumulator -> {
                    DataUtils.formatAmount(
                        entity.accumulatorCapacity,
                        UnitOfMeasure.Plain.MEGAJOULE,
                        "Accumulator charge: "
                    )
                }

                is EntityCrafter -> {
                    if (entity.craftingSpeed > 0f && entity.factorioType == "solar-panel") {
                        DataUtils.formatAmount(
                            entity.craftingSpeed,
                            UnitOfMeasure.Plain.MEGAWATT,
                            "Power production (average): "
                        )
                    } else {
                        null
                    }
                }

                else -> null
            }

            if (miscText != null) {
                add(commentLabel(miscText))
            }
        }
    }

    private fun buildGoods(storage: YAFCStorage, goods: Goods): JComponent {
        return verticalList {
            add(commentLabel("Middle mouse button to open Never Enough Items Explorer for this ${goods.type}"))

            if (goods.production.isNotEmpty()) {
                add(iconRow("Made with", storage, goods.production, 2))
            }
            if (goods.miscSources.isNotEmpty()) {
                add(iconRow("Sources", storage, goods.miscSources, 2))
            }
            if (goods.usages.isNotEmpty()) {
                add(iconRow("Needs for", storage, goods.usages, 4))
            }
            if (goods.fuelFor.isNotEmpty()) {
                val title = if (goods.fuelValue > 0f) {
                    "Fuel value ${DataUtils.formatAmount(goods.fuelValue, UnitOfMeasure.Plain.MEGAJOULE)} used for:"
                } else {
                    "Can be used as fuel for:"
                }
                add(iconRow(title, storage, goods.fuelFor, 2))
            }

            if (goods is Item) {
                val fuelResult = goods.fuelResult
                if (goods.fuelValue > 0f && fuelResult != null) {
                    add(iconRow("Fuel byproduct", storage, listOf(fuelResult), 1))
                }
                val placeResult = goods.placeResult
                if (placeResult != null) {
                    add(iconRow("Place result", storage, listOf(placeResult), 1))
                }

                val module = goods.module
                if (module != null) {
                    add(
                        verticalList {
                            if (module.productivity != 0f) {
                                val productivity = DataUtils.formatAmount(
                                    module.productivity,
                                    UnitOfMeasure.Plain.PERCENT,
                                    "Productivity: "
                                )
                                add(commentLabel(productivity))
                            }
                            if (module.speed != 0f) {
                                val speed = DataUtils.formatAmount(
                                    module.speed,
                                    UnitOfMeasure.Plain.PERCENT,
                                    "Speed: "
                                )
                                add(commentLabel(speed))
                            }
                            if (module.consumption != 0f) {
                                val consumption = DataUtils.formatAmount(
                                    module.consumption,
                                    UnitOfMeasure.Plain.PERCENT,
                                    "Consumption: "
                                )
                                add(commentLabel(consumption))
                            }
                            if (module.pollution != 0f) {
                                val pollution = DataUtils.formatAmount(
                                    module.consumption,
                                    UnitOfMeasure.Plain.PERCENT,
                                    "Pollution: "
                                )
                                add(commentLabel(pollution))
                            }
                        }.withSubHeader("Module parameters")
                    )
                    val limitation = module.limitation
                    if (limitation.isNotEmpty()) {
                        add(iconRow("Module limitation", storage, limitation, 2))
                    }
                }

                add(commentLabel("Stack size : ${goods.stackSize}"))
            }
        }
    }

    private fun buildRecipe(storage: YAFCStorage, recipe: RecipeOrTechnology): JComponent {
        return verticalList {
            add(
                commentLabel(DataUtils.formatAmount(recipe.time, UnitOfMeasure.Plain.SECOND)).apply {
                    iconTextGap = 8
                    icon = AllIcons.Vcs.History
                }
            )

            add(iconRow("Ingredients", storage, recipe.ingredients))

            val cost = storage.analyses.get<FactorioCostAnalysis>(FactorioAnalysisType.COST)
            if (cost != null && recipe is Recipe) {
                val waste = cost.recipeWastePercentage[recipe] ?: 0f
                if (waste > 0.01f) {
                    val wasteAmount = round(waste * 100f)
                    val wasteText = ". (Wasting $wasteAmount% of YAFC cost)"
                    val attributes = if (wasteAmount < 90) {
                        SimpleTextAttributes.REGULAR_ATTRIBUTES
                    } else {
                        SimpleTextAttributes.ERROR_ATTRIBUTES
                    }
                    val text = when {
                        (recipe.products.size == 1) -> {
                            "YAFC analysis: There are better recipes to create ${recipe.products[0].goods.locName}$wasteText"
                        }

                        (recipe.products.isNotEmpty()) -> {
                            "YAFC analysis: There are better recipes to create each of the products$wasteText"
                        }

                        else -> {
                            "YAFC analysis: This recipe wastes useful products. Don't do this recipe."
                        }
                    }

                    add(commentLabel(text, attributes))
                }
            }

            if (DataUtils.hasFlags(recipe.flags, RecipeFlag.USES_FLUID_TEMPERATURE.toSet())) {
                add(commentLabel("Uses fluid temperature"))
            }
            if (DataUtils.hasFlags(recipe.flags, RecipeFlag.USES_MINING_PRODUCTIVITY.toSet())) {
                add(commentLabel("Uses mining productivity"))
            }
            if (DataUtils.hasFlags(recipe.flags, RecipeFlag.SCALE_PRODUCTION_WITH_POWER.toSet())) {
                add(commentLabel("Production scaled with power"))
            }

            if (recipe.products.isNotEmpty() &&
                !(recipe.products.size == 1 &&
                        recipe.products[0].isSimple &&
                        recipe.products[0].goods is Item &&
                        recipe.products[0].amount == 1f)
            ) {
                add(iconRow("Products", storage, recipe.products))
            }

            add(iconRow("Made in", storage, recipe.crafters, 2))

            if (recipe.modules.isNotEmpty()) {
                add(iconRow("Allowed modules", storage, recipe.modules, 1))

                var crafterCommonModules = AllowedEffects.allOf(AllowedEffect::class.java)
                recipe.crafters.forEach {
                    if (it.moduleSlots > 0) {
                        crafterCommonModules = crafterCommonModules and it.allowedEffects
                    }
                }

                for (module in recipe.modules) {
                    val spec = module.module
                    if (spec != null && !EntityWithModules.canAcceptModule(spec, crafterCommonModules)) {
                        add(commentLabel("Some crafters restrict module usage"))
                        break
                    }
                }
            }

            if (recipe is Recipe && !recipe.enabled) {
                val title = "Unlocked by"
                if (recipe.technologyUnlock.size > 2) {
                    add(iconRow(title, storage, recipe.technologyUnlock, 1))
                } else {
                    val techScience =
                        storage.analyses.get<FactorioTechnologyScienceAnalysis>(FactorioAnalysisType.TECHNOLOGY_SCIENCE)
                    if (techScience != null) {
                        recipe.technologyUnlock.forEach {
                            val ingredient = techScience.getMaxTechnologyIngredient(it)
                            add(
                                tooltipPanel {
                                    add(singleIconRow(storage, it), BorderLayout.WEST)

                                    if (ingredient != null) {
                                        val amountText = DataUtils.formatAmount(
                                            ingredient.amount,
                                            UnitOfMeasure.Plain.NONE
                                        )
                                        add(
                                            JBLabel(amountText).apply {
                                                border = Borders.empty(ICON_ROW_GAP)
                                                iconTextGap = 8
                                                icon = IconLoader.createLazy {
                                                    IconCollection.getIcon(storage.dataSource, ingredient.goods)
                                                        ?: IconUtil.getEmptyIcon(false)
                                                }
                                            },
                                            BorderLayout.EAST
                                        )
                                    }
                                }.withSubHeader(title)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buildTechnology(storage: YAFCStorage, technology: Technology): JComponent {
        return verticalList {
            if (technology.hidden && !technology.enabled) {
                add(commentLabel("This technology is hidden from the list and cannot be researched."))
            }

            if (technology.prerequisites.isNotEmpty()) {
                add(iconRow("Prerequisites", storage, technology.prerequisites, 1))
            }

            if (technology.unlockRecipes.isNotEmpty()) {
                add(iconRow("Unlocks recipes", storage, technology.unlockRecipes, 2))
            }

            val techScience =
                storage.analyses.get<FactorioTechnologyScienceAnalysis>(FactorioAnalysisType.TECHNOLOGY_SCIENCE)
            if (techScience != null) {
                val packs = techScience.allSciencePacks[technology] ?: emptyList()
                if (packs.isNotEmpty()) {
                    add(iconRow("Total science required", storage, packs))
                }
            }
        }
    }

    private fun iconRow(title: String, storage: YAFCStorage, elements: List<FactorioObject>, row: Int): JComponent {
        return if (elements.isEmpty()) {
            emptyRow()
        } else {
            tooltipPanel {
                val sorted = elements.sortedWith(DataUtils.getDefaultOrdering(storage.analyses)).reversed()
                if (sorted.size <= row) {
                    add(
                        verticalList {
                            repeat(sorted.size) {
                                add(
                                    singleIconRow(storage, sorted[it]).apply {
                                        preferredSize = Dimension(
                                            JBUIScale.scale(WIDTH),
                                            JBUIScale.scale(IconCollection.ICON_SIZE + (ICON_ROW_GAP * 2))
                                        )
                                    }
                                )
                            }
                        },
                        BorderLayout.NORTH
                    )
                } else {
                    var index = 0
                    if (sorted.size - 1 < (row - 1) * MAX_ICONS_PER_ROW) {
                        add(singleIconRow(storage, sorted[0]), BorderLayout.NORTH)
                        index++
                    }

                    val sortedLeft = sorted.subList(index, min(MAX_ICONS_PER_ROW * row, sorted.size))
                    val gap = ICON_ROW_GAP
                    add(
                        JPanel(FlowLayout(FlowLayout.LEFT, gap, gap)).apply {
                            background = UIUtil.getToolTipBackground()
                            val maxRow = min(row - index, ceil(elements.size.toFloat() / MAX_ICONS_PER_ROW).toInt())
                            val height = JBUIScale.scale(IconCollection.ICON_SIZE * maxRow + gap * (maxRow + 1))
                            val dim = Dimension(JBUIScale.scale(WIDTH), height)
                            preferredSize = dim

                            sortedLeft.forEach {
                                add(JBLabel(IconLoader.createLazy {
                                    IconCollection.getIcon(storage.dataSource, it) ?: IconUtil.getEmptyIcon(false)
                                }))
                            }
                        }
                    )

                    val maxVisibleCount = MAX_ICONS_PER_ROW * row
                    if (elements.size > maxVisibleCount) {
                        add(
                            commentLabel("... and ${elements.size - maxVisibleCount} more").apply {
                                border = Borders.empty(4)
                            },
                            BorderLayout.SOUTH
                        )
                    }
                }
            }
        }.withSubHeader(title)
    }

    private fun iconRow(title: String, storage: YAFCStorage, elements: List<IFactorioObjectWrapper>): JComponent {
        return if (elements.isEmpty()) {
            emptyRow()
        } else {
            tooltipPanel {
                add(
                    verticalList {
                        val height = JBUIScale.scale((IconCollection.ICON_SIZE + ICON_ROW_GAP * 2) * elements.size)
                        val dim = Dimension(JBUIScale.scale(WIDTH), height)
                        preferredSize = dim

                        elements.forEach {
                            add(singleIconRow(storage, it))
                        }
                    }
                )
            }
        }.withSubHeader(title)
    }

    private fun emptyRow(): JComponent {
        return tooltipPanel {
            add(commentLabel("Nothing").apply {
                preferredSize = Dimension(JBUIScale.scale(WIDTH), JBUIScale.scale(IconCollection.BIG_ICON_SIZE))
            })
        }
    }

    private fun tooltipPanel(block: JComponent.() -> Unit): JComponent {
        return JPanel(BorderLayout()).apply {
            background = UIUtil.getToolTipBackground()
            alignmentX = JComponent.LEFT_ALIGNMENT
            block()
        }
    }

    private fun singleIconRow(storage: YAFCStorage, wrapper: IFactorioObjectWrapper): JComponent {
        return JBLabel(wrapper.text).apply {
            border = Borders.empty(ICON_ROW_GAP)
            iconTextGap = 8
            icon = IconLoader.createLazy {
                IconCollection.getIcon(storage.dataSource, wrapper.target) ?: IconUtil.getEmptyIcon(false)
            }
            preferredSize = Dimension(JBUIScale.scale(WIDTH), JBUIScale.scale(IconCollection.ICON_SIZE))
        }
    }

    private fun verticalList(border: Border? = null, block: JComponent.() -> Unit): JComponent {
        return JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = UIUtil.getToolTipBackground()
            if (border != null) {
                this.border = border
            }
            block()
        }
    }

    private fun JComponent.withSubHeader(title: String): JComponent {
        return this.apply {
            border = CompoundBorder(
                Borders.emptyTop(8),
                TitledBorder(RoundedLineBorder(UIUtil.getBoundsColor()), title)
            )
        }
    }

    /**
     * @see SimpleColoredComponent.formatToLabel
     */
    private fun commentLabel(
        content: String,
        attributes: SimpleTextAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES
    ): JBLabel {
        val spanStyle = if (attributes.fgColor != null) {
            val colorHex = ColorUtil.toHtmlColor(attributes.fgColor)
            """ style="color:$colorHex""""
        } else {
            ""
        }
        val xmlContent = content.replace("\n", "<br>")
        return JBLabel(
            """
                <html><body style="width:${JBUIScale.scale(WIDTH)}">
                <span$spanStyle>$xmlContent</span>
                </body></html>
            """.trimIndent()
        ).apply {
            border = Borders.empty(4)
            alignmentX = JComponent.LEFT_ALIGNMENT
        }
    }

    companion object {
        private const val WIDTH = 376
        private const val ICON_ROW_GAP = 4
        private const val MAX_ICONS_PER_ROW = (WIDTH - 12) / (ICON_ROW_GAP + IconCollection.ICON_SIZE)

        private val energyDescriptions = mapOf(
            EntityEnergyType.ELECTRIC to "Power usage: ",
            EntityEnergyType.HEAT to "Heat energy usage: ",
            EntityEnergyType.LABOR to "Labor energy usage: ",
            EntityEnergyType.VOID to "Free energy usage: ",
            EntityEnergyType.FLUID_FUEL to "Fluid fuel energy usage: ",
            EntityEnergyType.FLUID_HEAT to "Fluid heat energy usage: ",
            EntityEnergyType.SOLID_FUEL to "Solid fuel energy usage: "
        )

        fun create(project: Project, storage: YAFCStorage, element: FactorioObject): JBPanel<*> =
            FactorioObjectDetailedHint(project, storage, element).withPreferredWidth(WIDTH)
    }
}
