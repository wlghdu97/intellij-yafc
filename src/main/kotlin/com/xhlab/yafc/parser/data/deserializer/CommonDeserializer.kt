package com.xhlab.yafc.parser.data.deserializer

import com.intellij.openapi.diagnostic.Logger
import com.xhlab.yafc.model.Project
import com.xhlab.yafc.model.data.FactorioIconPart
import com.xhlab.yafc.parser.FactorioLocalization
import com.xhlab.yafc.parser.data.SpecialNames
import com.xhlab.yafc.parser.data.mutable.*
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import kotlin.reflect.typeOf

class CommonDeserializer constructor(
    private val parent: FactorioDataDeserializer,
    private val projectPath: String,
    private val data: LuaTable,
    private val prototypes: LuaTable,
    private val renderIcons: Boolean
) {
    private val logger = Logger.getInstance(CommonDeserializer::class.java)

    private val raw = (data["raw"] as? LuaTable) ?: LuaValue.tableOf()

    private fun getFluidFixedTemp(key: String, temperature: Int): MutableFluid {
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

//    private void UpdateSplitFluids()
//    {
//        var processedFluidLists = new HashSet<List<Fluid>>()
//
//        foreach (var fluid in allObjects.OfType<Fluid>())
//        {
//            if (fluid.temperature == 0)
//                fluid.temperature = fluid.temperatureRange.min
//            if (fluid.variants == null || !processedFluidLists.Add(fluid.variants)) continue
//            fluid.variants.Sort(DataUtils.FluidTemperatureComparer)
//            fluidVariants[fluid.type + "." + fluid.name] = fluid.variants
//            foreach (var variant in fluid.variants)
//            {
//                AddTemperatureToFluidIcon(variant)
//                variant.name += "@" + variant.temperature
//            }
//        }
//    }

//    private void AddTemperatureToFluidIcon(Fluid fluid)
//    {
//        var iconStr = fluid.temperature + "d"
//        fluid.iconSpec = fluid.iconSpec.Concat(iconStr.Take(4).Select((x, n) => new FactorioIconPart {path = "__.__/"+x, y=-16, x = n*7-12, scale = 0.28f})).ToArray()
//    }

    fun loadData(): Project {
//        progress.Report(("Loading", "Loading items"))

        val items = (prototypes["item"] as? LuaTable) ?: LuaTable.tableOf()
        for (prototypeName in items.keys()) {
            deserializePrototypes(raw, prototypeName.tojstring(), itemDeserializer)
        }
        parent.recipeModules.sealAndDeduplicate(parent.universalModules)
//        allModules = allObjects.OfType<Item>().Where(x => x.module != null).ToArray()

//        progress.Report(("Loading", "Loading fluids"))

//        deserializePrototypes(raw, "fluid", DeserializeFluid)

//        progress.Report(("Loading", "Loading recipes"))

//        deserializePrototypes(raw, "recipe", DeserializeRecipe)

//        progress.Report(("Loading", "Loading technologies"))

//        deserializePrototypes(raw, "technology", DeserializeTechnology)

//        progress.Report(("Loading", "Loading entities"))

//        DeserializeRocketEntities(raw["rocket-silo-rocket"] as LuaTable)
//        val entity = (prototypes["entity"] as? LuaTable) ?: LuaValue.tableOf()
//        for (prototypeName in entity.keys()) {
//            deserializePrototypes(raw, prototypeName.tojstring(), DeserializeEntity)
//        }

//        val scriptEnabled = (data["script_enabled"] as? LuaTable) ?: LuaTable.tableOf()
//        parseModYafcHandles(scriptEnabled)

//        progress.Report(("Post-processing", "Computing maps"))

        // Deterministically sort all objects

//        allObjects.Sort((a, b) => a.sortingOrder == b.sortingOrder ? string.Compare(a.typeDotName, b.typeDotName, StringComparison.Ordinal) : a.sortingOrder - b.sortingOrder)
//        for (var i = 0 i < allObjects.Count i++) {
//            allObjects[i].id = (FactorioId) i
//        }
//        UpdateSplitFluids()
//        var iconRenderTask = renderIcons ? Task.Run(RenderIcons) : Task.CompletedTask
//        UpdateRecipeIngredientFluids()
//        UpdateRecipeCatalysts()
//        CalculateMaps()
//        ExportBuiltData()

//        progress.Report(("Post-processing", "Calculating dependencies"))

//        Dependencies.Calculate()
//        TechnologyLoopsFinder.FindTechnologyLoops()

//        progress.Report(("Post-processing", "Creating project"))

//        val project = Project.ReadFromFile(projectPath, errorCollector)
//        Analysis.ProcessAnalyses(progress, project, errorCollector)

//        progress.Report(("Rendering icons", ""))

//        iconRenderedProgress = progress
//        iconRenderTask.Wait()
//
//        return project
        return Project()
    }

//    private volatile IProgress<(string, string)> iconRenderedProgress

//    private Icon CreateSimpleIcon(Dictionary<(string mod, string path),IntPtr> cache, string graphicsPath)
//    {
//        return CreateIconFromSpec(cache, new FactorioIconPart {path = "__core__/graphics/" + graphicsPath + ".png"})
//    }

//    private void RenderIcons()
//    {
//        var cache = new Dictionary<(string mod, string path), IntPtr>()
//        try
//        {
//            foreach (var digit in "0123456789d")
//            cache[(".", digit.ToString())] = SDL_image.IMG_Load("Data/Digits/" + digit + ".png")
//            DataUtils.NoFuelIcon = CreateSimpleIcon(cache, "fuel-icon-red")
//            DataUtils.WarningIcon = CreateSimpleIcon(cache, "warning-icon")
//            DataUtils.HandIcon = CreateSimpleIcon(cache, "hand")
//
//            var simpleSpritesCache = new Dictionary<string, Icon>()
//            var rendered = 0
//
//            foreach (var o in allObjects)
//            {
//                if (++rendered % 100 == 0)
//                    iconRenderedProgress?.Report(("Rendering icons", $"{rendered}/{allObjects.Count}"))
//                if (o.iconSpec != null && o.iconSpec.Length > 0)
//                {
//                    var simpleSprite = o.iconSpec.Length == 1 && o.iconSpec[0].IsSimple()
//                    if (simpleSprite && simpleSpritesCache.TryGetValue(o.iconSpec[0].path, out var icon))
//                    {
//                        o.icon = icon
//                        continue
//                    }
//
//                    try
//                    {
//                        o.icon = CreateIconFromSpec(cache, o.iconSpec)
//                        if (simpleSprite)
//                            simpleSpritesCache[o.iconSpec[0].path] = o.icon
//                    }
//                    catch (Exception ex)
//                    {
//                        Console.Error.WriteException(ex)
//                    }
//                }
//                else if (o is Recipe recipe && recipe.mainProduct != null)
//                o.icon = recipe.mainProduct.icon
//            }
//        }
//        finally
//        {
//            foreach (var (_, image) in cache)
//            {
//                if (image != IntPtr.Zero)
//                    SDL.SDL_FreeSurface(image)
//            }
//        }
//    }

//    private unsafe Icon CreateIconFromSpec(Dictionary<(string mod, string path),IntPtr> cache, params FactorioIconPart[] spec)
//    {
//        const int IconSize = IconCollection.IconSize
//        var targetSurface = SDL.SDL_CreateRGBSurfaceWithFormat(0, IconSize, IconSize, 0, SDL.SDL_PIXELFORMAT_RGBA8888)
//        SDL.SDL_SetSurfaceBlendMode(targetSurface, SDL.SDL_BlendMode.SDL_BLENDMODE_BLEND)
//        foreach (var icon in spec)
//        {
//            var modpath = FactorioDataSource.ResolveModPath("", icon.path)
//            if (!cache.TryGetValue(modpath, out var image))
//            {
//                var imageSource = FactorioDataSource.ReadModFile(modpath.mod, modpath.path)
//                if (imageSource == null)
//                    image = cache[modpath] = IntPtr.Zero
//                else
//                {
//                    fixed (byte* data = imageSource)
//                    {
//                        var src = SDL.SDL_RWFromMem((IntPtr) data, imageSource.Length)
//                        image = SDL_image.IMG_Load_RW(src, (int) SDL.SDL_bool.SDL_TRUE)
//                        if (image != IntPtr.Zero)
//                        {
//                            ref var surface = ref RenderingUtils.AsSdlSurface(image)
//                            var format = Unsafe.AsRef<SDL.SDL_PixelFormat>((void*) surface.format).format
//                            if (format != SDL.SDL_PIXELFORMAT_RGB24 && format != SDL.SDL_PIXELFORMAT_RGBA8888)
//                            {
//                                // SDL is failing to blit patelle surfaces, converting them
//                                var old = image
//                                image = SDL.SDL_ConvertSurfaceFormat(old, SDL.SDL_PIXELFORMAT_RGBA8888, 0)
//                                SDL.SDL_FreeSurface(old)
//                            }
//
//                            if (surface.h > IconSize * 2)
//                            {
//                                image = SoftwareScaler.DownscaleIcon(image, IconSize)
//                            }
//                        }
//                        cache[modpath] = image
//                    }
//                }
//            }
//            if (image == IntPtr.Zero)
//                continue
//
//            ref var sdlSurface = ref RenderingUtils.AsSdlSurface(image)
//            var targetSize = icon.scale == 1f ? IconSize : MathUtils.Ceil(icon.size * icon.scale) * (IconSize/32) // TODO research formula
//            SDL.SDL_SetSurfaceColorMod(image, MathUtils.FloatToByte(icon.r), MathUtils.FloatToByte(icon.g), MathUtils.FloatToByte(icon.b))
//            //SDL.SDL_SetSurfaceAlphaMod(image, MathUtils.FloatToByte(icon.a))
//            var basePosition = (IconSize - targetSize) / 2
//            var targetRect = new SDL.SDL_Rect
//                    {
//                        x = basePosition,
//                        y = basePosition,
//                        w = targetSize,
//                        h = targetSize
//                    }
//            if (icon.x != 0)
//                targetRect.x = MathUtils.Clamp(targetRect.x + MathUtils.Round(icon.x * IconSize / icon.size), 0, IconSize - targetRect.w)
//            if (icon.y != 0)
//                targetRect.y = MathUtils.Clamp(targetRect.y + MathUtils.Round(icon.y * IconSize / icon.size), 0, IconSize - targetRect.h)
//            var srcRect = new SDL.SDL_Rect
//                    {
//                        w = sdlSurface.h, // That is correct (cutting mip maps)
//                        h = sdlSurface.h
//                    }
//            SDL.SDL_BlitScaled(image, ref srcRect, targetSurface, ref targetRect)
//        }
//        return IconCollection.AddIcon(targetSurface)
//    }

    private fun deserializePrototypes(data: LuaTable, type: String, deserializer: Deserializer) {
        val table = data[type] as? LuaTable ?: return
//        progress.Report(("Building objects", type))
        for (key in table.keys()) {
            val entry = table[key]
            if (entry is LuaTable) {
                deserializer.deserialize(entry)
            }
        }
    }

    private fun parseEnergy(energy: String): Float {
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
            if (item.locName == null && placedAsEquipmentResult.isstring()) {
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
        logger.info("Splitting fluid ${basic.name} at $temperature")
        if (basic.variants == null) {
            basic.variants = mutableListOf(basic)
        }

        val copy = basic.copy().apply {
            setTemperature(temperature)
            variants?.add(this)
        }

        if (copy.fuelValue > 0f) {
            parent.fuels.add(SpecialNames.burnableFluid, copy)
        }
        parent.fuels.add(SpecialNames.specificFluid + basic.name, copy)

        return copy
    }

//    private void DeserializeFluid(LuaTable table)
//    {
//        var fluid = DeserializeCommon<Fluid>(table, "fluid")
//        fluid.originalName = fluid.name
//        if (table.Get("fuel_value", out string fuelValue))
//        {
//            fluid.fuelValue = ParseEnergy(fuelValue)
//            fuels.Add(SpecialNames.BurnableFluid, fluid)
//        }
//        fuels.Add(SpecialNames.SpecificFluid + fluid.name, fluid)
//        if (table.Get("heat_capacity", out string heatCap))
//            fluid.heatCapacity = ParseEnergy(heatCap)
//        fluid.temperatureRange = new TemperatureRange(table.Get("default_temperature", 0), table.Get("max_temperature", 0))
//    }

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
            val amount = table["amount"].checkdouble().toFloat()
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
            localeBuilder.append(key)

            return
        }

        with(localizedKey.split("__").iterator()) {
            while (hasNext()) {
                val control = next()
                localeBuilder.append(control)

                if (!hasNext()) {
                    break
                }

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
        val name = table["name"].tojstring()
        val obj = parent.getObject(name, construct)
        val factorioType = table["type"].optjstring("")
        obj.factorioType = factorioType

        val localizedName = table["localised_name"]
        if (localizedName != LuaValue.NIL) {
            localize(localizedName)
        } else {
            localize("$localeType-name.$name", null)
        }
        obj.locName = if (localeBuilder.isEmpty()) null else finishLocalize()

        val localizedDescription = table["localised_description"]
        if (localizedDescription != LuaValue.NIL) {
            localize(localizedDescription)
        } else {
            localize("$localeType-description.$name", null)
        }
        obj.locDescr = if (localeBuilder.isEmpty()) null else finishLocalize()

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

            else -> null
        }

        return obj
    }

    private fun parseModYafcHandles(scriptEnabled: LuaTable) {
        for (key in scriptEnabled.keys()) {
            val element = scriptEnabled[key]
            if (element is LuaTable) {
                val type = element["type"].tojstring()
                val name = element["name"].tojstring()

//                val existing = registeredObjects[name]?.tryCastToTypeByString(type)
//                if (existing != null) {
//                    rootAccessible.add(existing)
//                }
            }
        }
    }

    data class Color(val r: Float = 1f, val g: Float = 1f, val b: Float = 1f, val a: Float = 1f)

    interface Deserializer {
        fun deserialize(table: LuaTable)
    }
}
