package com.xhlab.yafc.parser.data.deserializer

import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.parser.FactorioLocalization
import com.xhlab.yafc.parser.ProgressTextIndicator
import com.xhlab.yafc.parser.YAFCLogger
import com.xhlab.yafc.parser.data.SpecialNames
import com.xhlab.yafc.parser.info
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class CommonDeserializer constructor(
    private val parent: FactorioDataDeserializer,
    private val data: LuaTable,
    private val prototypes: LuaTable,
    private val logger: YAFCLogger
) {
    internal val raw = (data["raw"] as? LuaTable) ?: LuaValue.tableOf()

    internal fun getFluidFixedTemp(key: String, temperature: Int): MutableFluid {
        var temp = temperature
        val basic = parent.getObject(key, ::MutableFluid)
        if (basic.temperature == temp) {
            return basic
        }

        if (temp < basic.temperatureRange.min) {
            temp = basic.temperatureRange.min
        }

        val idWithTemp = "$key@$temp"
        val regKey = FactorioDataDeserializer.TypeWithName(typeOf<MutableFluid>(), idWithTemp)

        if (basic.temperature == 0) {
            basic.setTemperature(temp)
            parent.registeredObjects[regKey] = basic

            return basic
        }

        val fluidWithTemp = parent.registeredObjects[regKey]
        if (fluidWithTemp != null) {
            return fluidWithTemp as MutableFluid
        }

        val split = splitFluid(basic, temp)
        parent.allObjects.add(split)
        parent.registeredObjects[regKey] = split

        return split
    }

    private fun updateSplitFluids() {
        val processedFluidLists = hashSetOf<String>()

        for (fluid in parent.allObjects.filterIsInstance<MutableFluid>()) {
            if (fluid.temperature == 0) {
                fluid.temperature = fluid.temperatureRange.min
            }
            val variantName = "${fluid.type}.${fluid.name}".substringBefore("@")
            if (fluid.variants.isEmpty() || processedFluidLists.contains(variantName)) {
                continue
            }
            fluid.variants.sortWith(DataUtils.fluidTemperatureComparer)
            parent.fluidVariants[variantName] = fluid.variants
            for (variant in fluid.variants) {
                addTemperatureToFluidIcon(variant)
                variant.name = variant.name.substringBefore("@") + "@" + variant.temperature
            }
        }
    }

    private fun addTemperatureToFluidIcon(fluid: MutableFluid) {
        val iconStr = "${fluid.temperature}d"
        val subIcons = iconStr.take(4).mapIndexed { n, x ->
            FactorioIconPart("__.__/$x", x = n * 7f - 12, y = -16f, scale = 0.28f)
        }
        fluid.iconSpec = fluid.iconSpec + subIcons
    }

    fun loadData(progress: ProgressTextIndicator): YAFCDatabase {
        progress.setText("Loading", "Loading items")
        val items = (prototypes["item"] as? LuaTable) ?: LuaTable.tableOf()
        for (prototypeName in items.keys()) {
            deserializePrototypes(raw, prototypeName.tojstring(), itemDeserializer, progress)
        }
        parent.recipeModules.sealAndDeduplicate(parent.universalModules)
        parent.allModules.addAll(parent.allObjects.filterIsInstance<MutableItem>().filter { it.module != null })
        progress.setText("Loading", "Loading fluids")
        deserializePrototypes(raw, "fluid", fluidDeserializer, progress)
        progress.setText("Loading", "Loading recipes")
        deserializePrototypes(raw, "recipe", parent.recipeAndTechnology.recipeDeserializer, progress)
        progress.setText("Loading", "Loading technologies")
        deserializePrototypes(raw, "technology", parent.recipeAndTechnology.technologyDeserializer, progress)
        progress.setText("Loading", "Loading entities")
        parent.entity.rocketEntitiesDeserializer.deserialize(raw["rocket-silo-rocket"].checktable())
        val entities = prototypes["entity"].opttable(LuaValue.tableOf())
        for (prototypeName in entities.keys()) {
            deserializePrototypes(raw, prototypeName.tojstring(), parent.entity.entityDeserializer, progress)
        }

        val scriptEnabled = data["script_enabled"].opttable(LuaValue.tableOf())
        parseModYafcHandles(scriptEnabled)
        progress.setText("Post-processing", "Computing maps")

        // Deterministically sort all objects
        parent.allObjects.sortWith { x, y ->
            if (x.sortingOrder == y.sortingOrder) {
                (x.typeDotName).compareTo(y.typeDotName, true) // huh.
            } else {
                x.sortingOrder.ordinal - y.sortingOrder.ordinal
            }
        }
        parent.allObjects.forEachIndexed { idx, obj ->
            obj.id = FactorioId(idx)
        }

        updateSplitFluids()

        parent.recipeAndTechnology.updateRecipeIngredientFluids()
        parent.recipeAndTechnology.updateRecipeCatalysts()
        parent.context.calculateMaps()

        return parent.context.exportBuiltData()
    }

    private fun deserializePrototypes(
        data: LuaTable,
        type: String,
        deserializer: Deserializer,
        progress: ProgressTextIndicator
    ) {
        val table = data[type] as? LuaTable ?: return
        progress.setText("Building objects", type)
        for (key in table.keys()) {
            val entry = table[key]
            if (entry is LuaTable) {
                deserializer.deserialize(entry)
            }
        }
    }

    internal fun parseEnergy(energy: String): Float {
        val len = energy.length - 2
        if (len < 0) {
            return 0f
        }

        val energyMul = energy[len]
        // internally store energy in (megawatts / megajoules) to be closer to 1
        if (energyMul.isLetter()) {
            val energyBase = energy.substring(0, len).toFloat()
            when (energyMul) {
                'k', 'K' -> return energyBase * 1e-3f
                'M' -> return energyBase
                'G' -> return energyBase * 1e3f
                'T' -> return energyBase * 1e6f
                'P' -> return energyBase * 1e9f
                'E' -> return energyBase * 1e12f
                'Z' -> return energyBase * 1e15f
                'Y' -> return energyBase * 1e18f
            }
        }

        return energy.substring(0, len + 1).toFloat() * 1e-6f
    }

    private val itemDeserializer = object : Deserializer {
        override fun deserialize(table: LuaTable) {
            val item = deserializeCommon(table, "item", ::MutableItem)

            val placeResult = table["place_result"]
            if (placeResult.isstring()) {
                parent.placeResults[item] = placeResult.tojstring()
            }

            item.stackSize = table["stack_size"].optint(1)

            val placedAsEquipmentResult = table["placed_as_equipment_result"]
            if (item.locName.isEmpty() && placedAsEquipmentResult.isstring()) {
                localize("equipment-name.${placedAsEquipmentResult.tojstring()}", null)
                if (localeBuilder.isNotEmpty()) {
                    item.locName = finishLocalize()
                }
            }

            val fuelValueTable = table["fuel_value"]
            if (fuelValueTable.isstring()) {
                val fuelValue = parseEnergy(fuelValueTable.tojstring())
                val fuelResult = parent.getRef(table, "burnt_result", ::MutableItem).second

                val category = table["fuel_category"].tojstring()
                parent.fuels.add(category, item)

                item.fuelValue = fuelValue
                item.fuelResult = fuelResult
            }

            val moduleEffect = table["effect"]
            if (item.factorioType == "module" && moduleEffect.istable()) {
                val module = MutableModuleSpecification(
                    consumption = moduleEffect["consumption"].opttable(LuaValue.tableOf())["bonus"]
                        .optnumber(LuaValue.valueOf(0.0)).tofloat(),
                    speed = moduleEffect["speed"].opttable(LuaValue.tableOf())["bonus"]
                        .optnumber(LuaValue.valueOf(0.0)).tofloat(),
                    productivity = moduleEffect["productivity"].opttable(LuaValue.tableOf())["bonus"]
                        .optnumber(LuaValue.valueOf(0.0)).tofloat(),
                    pollution = moduleEffect["pollution"].opttable(LuaValue.tableOf())["bonus"]
                        .optnumber(LuaValue.valueOf(0.0)).tofloat(),
                )

                val limitationTable = table["limitation"]
                module.limitation = if (limitationTable.istable()) {
                    limitationTable.checktable().keys().mapNotNull {
                        val value = limitationTable[it]
                        if (value.isstring()) {
                            parent.getObject(value.tojstring(), ::MutableRecipeImpl)
                        } else {
                            null
                        }
                    }
                } else {
                    emptyList()
                }

                if (module.limitation.isEmpty()) {
                    parent.universalModules.add(item)
                } else {
                    for (recipe in module.limitation) {
                        parent.recipeModules.add(recipe, item, true)
                    }
                }

                item.module = module
            }

            val rocketLaunchProduct = table["rocket_launch_product"]
            val rocketLaunchProducts = table["rocket_launch_products"]
            val launchProducts = when {
                (rocketLaunchProduct.istable()) -> {
                    val productTable = rocketLaunchProduct.checktable()
                    val product = parent.recipeAndTechnology.loadProductWithMultiplier(productTable, item.stackSize)
                    if (product != null) {
                        listOf(product)
                    } else {
                        null
                    }
                }

                (rocketLaunchProducts.istable()) -> {
                    rocketLaunchProducts.checktable().keys().mapNotNull {
                        val element = rocketLaunchProducts[it]
                        if (element.istable()) {
                            val productTable = element.checktable()
                            parent.recipeAndTechnology.loadProductWithMultiplier(productTable, item.stackSize)
                        } else {
                            null
                        }
                    }
                }

                else -> null
            }

            if (!launchProducts.isNullOrEmpty()) {
                val recipe = parent.context.createSpecialRecipe(item, SpecialNames.rocketLaunch, "launched")
                recipe.ingredients = listOf(
                    MutableIngredient(item, item.stackSize.toFloat()),
                    MutableIngredient(parent.context.rocketLaunch, 1f)
                )
                recipe.products = launchProducts
                recipe.time = 0f // TODO what to put here?
            }
        }
    }

    private fun splitFluid(basic: MutableFluid, temperature: Int): MutableFluid {
        logger.info<CommonDeserializer>("Splitting fluid ${basic.name} at $temperature")
        if (basic.variants.isEmpty()) {
            basic.variants = arrayListOf(basic)
        }

        val copy = basic.copy().apply {
            setTemperature(temperature)
            variants = basic.variants // link basic's variants array
            variants.add(this)
        }

        if (copy.fuelValue > 0f) {
            parent.fuels.add(SpecialNames.burnableFluid, copy)
        }
        parent.fuels.add(SpecialNames.specificFluid + basic.name, copy)

        return copy
    }

    private val fluidDeserializer = object : Deserializer {
        override fun deserialize(table: LuaTable) {
            val fluid = deserializeCommon(table, "fluid", ::MutableFluid)

            fluid.originalName = fluid.name

            val fuelValue = table["fuel_value"]
            if (fuelValue.isstring()) {
                fluid.fuelValue = parseEnergy(fuelValue.tojstring())
                parent.fuels.add(SpecialNames.burnableFluid, fluid)
            }

            parent.fuels.add(SpecialNames.specificFluid + fluid.name, fluid)

            val heatCapacity = table["heat_capacity"]
            if (heatCapacity.isstring()) {
                fluid.heatCapacity = parseEnergy(heatCapacity.tojstring())
            }

            val defaultTemperature = table["default_temperature"].optint(0)
            val maxTemperature = table["max_temperature"].optint(0)
            fluid.temperatureRange = TemperatureRange(defaultTemperature, maxTemperature)
        }
    }

    private fun loadItemOrFluid(table: LuaTable, useTemperature: Boolean, nameField: String = "name"): MutableGoods? {
        val name = table[nameField].optjstring(null) ?: return null

        val type = table["type"]
        if (type.isstring() && type.tojstring() == "fluid") {
            if (useTemperature) {
                val temperature = table["temperature"].optint(0)
                return getFluidFixedTemp(name, temperature)
            }

            return parent.getObject(name, ::MutableFluid)
        }

        return parent.getObject(name, ::MutableItem)
    }

    internal fun loadItemData(table: LuaTable, useTemperature: Boolean): Triple<MutableGoods?, Float, Boolean> {
        return if (table["name"].isstring()) {
            val goods = loadItemOrFluid(table, useTemperature)
            val amount = table["amount"].optdouble(0.0).toFloat()
            Triple(goods, amount, true) // true means 'may have extra data'
        } else {
            val name = table[1].checkjstring()
            val amount = table[2].checkdouble().toFloat()
            val goods = parent.getObject(name, ::MutableItem)
            Triple(goods, amount, false)
        }
    }

    private val localeBuilder = StringBuilder()

    private fun localize(value: LuaValue) {
        if (value is LuaTable) {
            val key = value[1]
            if (!key.isstring()) {
                return
            }

            localize(key.tojstring(), value)
        } else {
            localeBuilder.append(value)
        }
    }

    private fun finishLocalize(): String {
        // Cleaning up tags using simple state machine
        // 0 = outside of tag,
        // 1 = first potential tag char,
        // 2 = inside possible tag,
        // 3 = inside definite tag.
        // tag is definite when it contains '=' or starts with '/' or '.'
        var state = 0
        var tagStart = 0
        var index = 0
        while (index < localeBuilder.length) {
            val chr = localeBuilder[index]
            when (state) {
                0 -> {
                    if (chr == '[') {
                        state = 1
                        tagStart = index
                    }
                }

                1 -> {
                    state = when {
                        (chr == ']') -> 0
                        (chr == '/' || chr == '.') -> 3
                        else -> 2
                    }
                }

                2 -> {
                    if (chr == '=') {
                        state = 3
                    } else if (chr == ']') {
                        state = 0
                    }
                }

                3 -> {
                    if (chr == ']') {
                        localeBuilder.removeRange(tagStart, index - tagStart + 1)
                        index = tagStart - 1
                        state = 0
                    }
                }
            }

            index += 1
        }

        return localeBuilder.toString().replace("\\n", "\n").apply {
            localeBuilder.clear()
        }
    }

    private fun localize(key: String, table: LuaTable?) {
        if (key == "") {
            if (table == null) {
                return
            }

            for (tableKey in table.keys()) {
                val element = table[tableKey]
                if (element.istable()) {
                    localize(element)
                } else {
                    localeBuilder.append(element)
                }
            }

            return
        }

        val localizedKey = FactorioLocalization.localize(key)

        if (localizedKey == null) {
            if (table != null) {
                val elements = table.keys().mapNotNull {
                    val value = table[it]
                    if (value.isstring()) value.tojstring() else null
                }

                localeBuilder.append(elements.joinToString(" "))
            }

            return
        }

        if (!localizedKey.contains("__")) {
            localeBuilder.append(localizedKey)

            return
        }

        with(localizedKey.split("__").iterator()) {
            while (hasNext()) {
                val prev = next()
                localeBuilder.append(prev)

                if (!hasNext()) {
                    break
                }
                val control = next()

                when {
                    (control == "ITEM" || control == "FLUID" || control == "RECIPE" || control == "ENTITY") -> {
                        val nextPart = next()
                        val subKey = "${control.lowercase()}-name.$nextPart"
                        localize(subKey, null)
                    }

                    (control == "CONTROL") -> {
                        val nextPart = next()
                        localeBuilder.append(nextPart)
                    }

                    (control == "ALT_CONTROL") -> {
                        next()
                        if (!hasNext()) {
                            break
                        }
                        val nextPart = next()
                        localeBuilder.append(nextPart)
                    }

                    (table != null && control.toIntOrNull() != null) -> {
                        val index = control.toInt()
                        val item = table[index + 1]
                        when {
                            (item.isstring()) -> {
                                localize(item.tojstring(), null)
                            }

                            (item.istable()) -> {
                                localize(item)
                            }

                            (item.isnumber()) -> {
                                localeBuilder.append(item.tofloat())
                            }
                        }
                    }

                    (control.startsWith("plural")) -> {
                        localeBuilder.append("(???)")
                        next()
                    }

                    else -> {
                        // Not supported token... Append everything else as-is
                        while (hasNext()) {
                            localeBuilder.append(next())
                        }
                        break
                    }
                }
            }
        }
    }

    internal inline fun <reified T> deserializeCommon(
        table: LuaTable,
        localeType: String,
        construct: (name: String) -> T
    ): T where T : MutableFactorioObject {
        return deserializeCommonWithNominal<T, T>(table, localeType, construct)
    }

    internal inline fun <reified Nominal, reified Actual> deserializeCommonWithNominal(
        table: LuaTable,
        localeType: String,
        construct: (name: String) -> Actual
    ): Actual where Nominal : MutableFactorioObject, Actual : MutableFactorioObject {
        val name = table["name"].tojstring()
        val obj = parent.getObjectWithNominal<Nominal, Actual>(name, construct)
        val factorioType = table["type"].optjstring("")
        obj.factorioType = factorioType

        val localizedName = table["localised_name"]
        if (localizedName != LuaValue.NIL) {
            localize(localizedName)
        } else {
            localize("$localeType-name.$name", null)
        }
        obj.locName = if (localeBuilder.isEmpty()) "" else finishLocalize()

        val localizedDescription = table["localised_description"]
        if (localizedDescription != LuaValue.NIL) {
            localize(localizedDescription)
        } else {
            localize("$localeType-description.$name", null)
        }
        obj.locDescr = if (localeBuilder.isEmpty()) "" else finishLocalize()

        val defaultIconSize = table["icon_size"].tofloat()
        val icon = table["icon"]
        obj.iconSpec = when {
            (icon.isstring()) -> {
                val path = icon.tojstring()
                listOf(FactorioIconPart(path = path, size = defaultIconSize))
            }

            (icon.istable()) -> {
                val iconList = icon.checktable()
                iconList.keys().map {
                    val item = iconList[it]
                    val path = item["icon"].tojstring()
                    val size = with(item["icon_size"]) {
                        if (this != LuaValue.NIL) this.tofloat() else defaultIconSize
                    }
                    val scale = with(item["scale"]) {
                        if (this != LuaValue.NIL) this.tofloat() else 1f
                    }

                    val shift = with(item["shift"]) {
                        if (this.istable()) {
                            get(1).tofloat() to get(2).tofloat()
                        } else {
                            null to null
                        }
                    }

                    val tint = with(item["tint"]) {
                        if (this.istable()) {
                            val r = this["r"].tofloat()
                            val g = this["g"].tofloat()
                            val b = this["b"].tofloat()
                            val a = this["a"].tofloat()

                            Color(r, g, b, a)
                        } else {
                            Color()
                        }
                    }

                    FactorioIconPart(
                        path = path,
                        size = size,
                        x = shift.first ?: 0f,
                        y = shift.second ?: 0f,
                        r = tint.r,
                        g = tint.g,
                        b = tint.b,
                        a = tint.a,
                        scale = scale
                    )
                }
            }

            else -> emptyList()
        }

        return obj
    }

    private fun parseModYafcHandles(scriptEnabled: LuaTable) {
        for (key in scriptEnabled.keys()) {
            val element = scriptEnabled[key].opttable(null)
            if (element != null) {
                val type = element["type"].tojstring()
                val name = element["name"].tojstring()

                val kotlinType = typeNameToType(type) ?: return
                val existing = parent.registeredObjects[FactorioDataDeserializer.TypeWithName(kotlinType, name)]
                if (existing != null) {
                    parent.rootAccessible.add(existing)
                }
            }
        }
    }

    private fun typeNameToType(typeName: String): KType? {
        return when (typeName) {
            "item" -> typeOf<Item>()
            "fluid" -> typeOf<Fluid>()
            "technology" -> typeOf<Technology>()
            "recipe" -> typeOf<Recipe>()
            "entity" -> typeOf<Entity>()
            else -> null
        }
    }

    data class Color(val r: Float = 1f, val g: Float = 1f, val b: Float = 1f, val a: Float = 1f)

    interface Deserializer {
        fun deserialize(table: LuaTable)
    }
}
