package com.xhlab.yafc.model.data

import com.google.ortools.linearsolver.MPSolver
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.MathUtil.clamp
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.model.YAFCProjectPreferences
import com.xhlab.yafc.model.analysis.factorio.FactorioAnalyses
import com.xhlab.yafc.model.analysis.factorio.FactorioAnalysisType
import com.xhlab.yafc.model.analysis.factorio.FactorioCostAnalysis
import com.xhlab.yafc.model.analysis.factorio.FactorioMilestones
import com.xhlab.yafc.model.util.EnumFlag
import com.xhlab.yafc.model.util.value
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.floor
import kotlin.math.log10

object DataUtils {

    private val logger = Logger.getInstance(DataUtils::class.java)

    fun getDefaultOrdering(analyses: FactorioAnalyses) = FactorioObjectComparer(analyses, Comparator { x, y ->
        val costAnalysis = analyses.get<FactorioCostAnalysis>(FactorioAnalysisType.COST)
        if (costAnalysis != null) {
            val xFlow = costAnalysis.flow[x] ?: 0f
            val yFlow = costAnalysis.flow[y] ?: 0f
            if (xFlow != yFlow) {
                return@Comparator xFlow.compareTo(yFlow)
            }

            val rx = x as? Recipe
            val ry = y as? Recipe
            if (rx != null || ry != null) {
                val xWaste = costAnalysis.recipeWastePercentage[rx] ?: 0f
                val yWaste = costAnalysis.recipeWastePercentage[ry] ?: 0f
                return@Comparator xWaste.compareTo(yWaste)
            }

            val xCost = costAnalysis.cost[x] ?: 0f
            val yCost = costAnalysis.cost[y] ?: 0f
            yCost.compareTo(xCost)
        } else {
            deterministicComparer.compare(x, y)
        }
    })

//    public static readonly FactorioObjectComparer<Goods> FuelOrdering = new FactorioObjectComparer<Goods>((x, y) =>
//    {
//        if (x.fuelValue <= 0f && y.fuelValue <= 0f)
//        {
//            if (x is Fluid fx && y is Fluid fy)
//            return (x.Cost() / fx.heatValue).CompareTo(y.Cost() / fy.heatValue)
//            return DefaultOrdering.Compare(x, y)
//        }
//        return (x.Cost() / x.fuelValue).CompareTo(y.Cost() / y.fuelValue)
//    })
//    public static readonly FactorioObjectComparer<Recipe> DefaultRecipeOrdering = new FactorioObjectComparer<Recipe>((x, y) =>
//    {
//        var yflow = y.ApproximateFlow()
//        var xflow = x.ApproximateFlow()
//        if (yflow != xflow)
//            return yflow > xflow ? 1 : -1
//        return x.RecipeWaste().CompareTo(y.RecipeWaste())
//    })
//    public static readonly FactorioObjectComparer<EntityCrafter> CrafterOrdering = new FactorioObjectComparer<EntityCrafter>((x, y) =>
//    {
//        if (x.energy.type != y.energy.type)
//            return x.energy.type.CompareTo(y.energy.type)
//        if (x.craftingSpeed != y.craftingSpeed)
//            return y.craftingSpeed.CompareTo(x.craftingSpeed)
//        return x.Cost().CompareTo(y.Cost())
//    })

//    public static FavouritesComparer<Goods> FavouriteFuel { get private set }
//    public static FavouritesComparer<EntityCrafter> FavouriteCrafter { get private set }
//    public static FavouritesComparer<Item> FavouriteModule { get private set }

    val deterministicComparer: Comparator<FactorioObject> = FactorioObjectDeterministicComparer()

    internal val fluidTemperatureComparer = Comparator<Fluid> { x, y ->
        x.temperature.compareTo(y.temperature)
    }

    fun getMilestoneOrder(milestones: FactorioMilestones, id: FactorioId): ULong {
        return ((milestones.milestoneResult[id] ?: 0uL) - 1uL) and milestones.lockedMask
    }

    var expensiveRecipes = false

    val random = Random()

//    public static bool SelectSingle<T>(this T[] list, out T element) where T:FactorioObject
//    {
//        var userFavourites = Project.current.preferences.favourites
//        var acceptOnlyFavourites = false
//        element = null
//        foreach (var elem in list)
//        {
//            if (!elem.IsAccessibleWithCurrentMilestones() || elem.specialType != FactorioObjectSpecialType.Normal)
//                continue
//            if (userFavourites.Contains(elem))
//            {
//                if (!acceptOnlyFavourites || element == null)
//                {
//                    element = elem
//                    acceptOnlyFavourites = true
//                }
//                else
//                {
//                    element = null
//                    return false
//                }
//            }
//            else if (!acceptOnlyFavourites)
//            {
//                if (element == null)
//                    element = elem
//                else
//                {
//                    element = null
//                    acceptOnlyFavourites = true
//                }
//            }
//        }
//
//        return element != null
//    }

//    public static void SetupForProject(Project project)
//    {
//        FavouriteFuel = new FavouritesComparer<Goods>(project, FuelOrdering)
//        FavouriteCrafter = new FavouritesComparer<EntityCrafter>(project, CrafterOrdering)
//        FavouriteModule = new FavouritesComparer<Item>(project, DefaultOrdering)
//    }

    private class FactorioObjectDeterministicComparer : Comparator<FactorioObject> {
        override fun compare(x: FactorioObject?, y: FactorioObject?): Int {
            return when {
                (x == null) -> {
                    if (y == null) 0 else 1
                }

                (y == null) -> {
                    -1
                }

                else -> {
                    x.id.compareTo(y.id) // id comparison is deterministic because objects are sorted deterministically
                }
            }
        }
    }

    class FactorioObjectComparer<T> constructor(
        private val analyses: FactorioAnalyses,
        private val similarComparator: Comparator<T>
    ) : Comparator<T> where T : FactorioObject {

        override fun compare(x: T?, y: T?): Int {
            return when {
                (x == null) -> {
                    if (y == null) 0 else 1
                }

                (y == null) -> {
                    -1
                }

                (x.specialType != y.specialType) -> {
                    x.specialType.ordinal - y.specialType.ordinal
                }

                else -> {
                    val milestones = analyses.get<FactorioMilestones>(FactorioAnalysisType.MILESTONES)
                    if (milestones != null) {
                        val msx = getMilestoneOrder(milestones, x.id)
                        val msy = getMilestoneOrder(milestones, y.id)
                        if (msx != msy) {
                            msx.compareTo(msy)
                        } else {
                            similarComparator.compare(x, y)
                        }
                    } else {
                        similarComparator.compare(x, y)
                    }
                }
            }
        }
    }

    fun createSolver(name: String): MPSolver {
        val solver = MPSolver(name, MPSolver.OptimizationProblemType.GLOP_LINEAR_PROGRAMMING)
        // Relax solver parameters as returning imprecise solution is better than no solution at all
        // It is not like we need 8 digits of precision after all, most computations in YAFC are done in singles
        // see all properties here: https://github.com/google/or-tools/blob/stable/ortools/glop/parameters.proto
        solver.setSolverSpecificParametersAsString("solution_feasibility_tolerance:1e-1")
        return solver
    }

    fun trySolveWithDifferentSeeds(solver: MPSolver): MPSolver.ResultStatus {
        for (i in 0 until 3) {
            val time = System.currentTimeMillis()
            val result = solver.solve()
            val elapsedTime = System.currentTimeMillis() - time
            println("Solution completed in $elapsedTime ms with result $result")
            if (result == MPSolver.ResultStatus.ABNORMAL) {
                solver.setSolverSpecificParametersAsString("random_seed:" + random.nextInt())
                continue
            } /*else
                VerySlowTryFindBadObjective(solver)*/
            return result
        }

        return MPSolver.ResultStatus.ABNORMAL
    }

//    public static void VerySlowTryFindBadObjective(Solver solver)
//    {
//        var vars = solver.variables()
//        var obj = solver.Objective()
//        Console.WriteLine(solver.ExportModelAsLpFormat(false))
//        foreach (var v in vars)
//        {
//            obj.SetCoefficient(v, 0)
//            var result = solver.Solve()
//            if (result == Solver.ResultStatus.OPTIMAL)
//            {
//                Console.WriteLine("Infeasibility candidate: "+v.Name())
//                return
//            }
//        }
//    }

//    public static bool RemoveValue<TKey, TValue>(this Dictionary<TKey, TValue> dict, TValue value)
//    {
//        var comparer = EqualityComparer<TValue>.Default
//        foreach (var (k, v) in dict)
//        {
//            if (comparer.Equals(v, value))
//            {
//                dict.Remove(k)
//                return true
//            }
//        }
//
//        return false
//    }

//    public class FavouritesComparer<T> : IComparer<T> where T : FactorioObject
//    {
//        private readonly Dictionary<T, int> bumps = new Dictionary<T, int>()
//        private readonly IComparer<T> def
//        private readonly HashSet<FactorioObject> userFavourites
//        public FavouritesComparer(Project project, IComparer<T> def)
//        {
//            this.def = def
//            userFavourites = project.preferences.favourites
//        }
//
//        public void AddToFavourite(T x, int amount = 1)
//        {
//            if (x == null)
//                return
//            bumps.TryGetValue(x, out var prev)
//            bumps[x] = prev+amount
//        }
//        public int Compare(T x, T y)
//        {
//            var hasX = userFavourites.Contains(x)
//            var hasY = userFavourites.Contains(y)
//            if (hasX != hasY)
//                return hasY.CompareTo(hasX)
//
//            bumps.TryGetValue(x, out var ix)
//            bumps.TryGetValue(y, out var iy)
//            if (ix == iy)
//                return def.Compare(x, y)
//            return iy.CompareTo(ix)
//        }
//    }

//    public static float GetProduction(this Recipe recipe, Goods product)
//    {
//        var amount = 0f
//        foreach (var p in recipe.products)
//        {
//            if (p.goods == product)
//                amount += p.amount
//        }
//        return amount
//    }
//
//    public static float GetProduction(this Recipe recipe, Goods product, float productivity)
//    {
//        var amount = 0f
//        foreach (var p in recipe.products)
//        {
//            if (p.goods == product)
//                amount += p.GetAmount(productivity)
//        }
//        return amount
//    }

    internal fun MutableRecipe.getConsumption(product: Goods): Float {
        var amount = 0f
        ingredients.forEach { ingredient ->
            if (ingredient.containsVariant(product)) {
                amount += ingredient.amount
            }
        }

        return amount
    }
//
//    public static FactorioObjectComparer<Recipe> GetRecipeComparerFor(Goods goods)
//    {
//        return new FactorioObjectComparer<Recipe>((x, y) => (x.Cost(true)/x.GetProduction(goods)).CompareTo(y.Cost(true)/y.GetProduction(goods)))
//    }
//
//    public static Icon NoFuelIcon
//    public static Icon WarningIcon
//    public static Icon HandIcon

//    public static T AutoSelect<T>(this IEnumerable<T> list, IComparer<T> comparer = default)
//    {
//        if (comparer == null)
//        {
//            if (DefaultOrdering is IComparer<T> defaultComparer)
//            comparer = defaultComparer
//            else comparer = Comparer<T>.Default
//        }
//        var first = true
//        T best = default
//        foreach (var elem in list)
//        {
//            if (first || comparer.Compare(best, elem) > 0)
//            {
//                first = false
//                best = elem
//            }
//        }
//        return best
//    }

//    public static void MoveListElementIndex<T>(this IList<T> list, int from, int to)
//    {
//        var moving = list[from]
//        if (from > to)
//        {
//            for (var i = from-1 i >= to i--)
//            list[i + 1] = list[i]
//        }
//        else
//        {
//            for (var i = from i < to i++)
//            list[i] = list[i + 1]
//        }
//
//        list[to] = moving
//    }

//    public static T RecordUndo<T>(this T target, bool visualOnly = false) where T : ModelObject
//    {
//        target.CreateUndoSnapshot(visualOnly)
//        return target
//    }
//
//    public static void MoveListElement<T>(this IList<T> list, T from, T to)
//    {
//        var fromIndex = list.IndexOf(from)
//        var toIndex = list.IndexOf(to)
//        if (fromIndex >= 0 && toIndex >= 0)
//            MoveListElementIndex(list, fromIndex, toIndex)
//    }

    private const val NO = 0.toChar()

    val formatSpec = arrayOf(
        Format('μ', 1e6f, DecimalFormat("0.##")),
        Format('μ', 1e6f, DecimalFormat("0.##")),
        Format('μ', 1e6f, DecimalFormat("0.#")),
        Format('μ', 1e6f, DecimalFormat("0")),
        Format('μ', 1e6f, DecimalFormat("0")), // skipping m (milli-) because too similar to M (mega-)
        Format(NO, 1e0f, DecimalFormat("0.####")),
        Format(NO, 1e0f, DecimalFormat("0.###")),
        Format(NO, 1e0f, DecimalFormat("0.##")),
        Format(NO, 1e0f, DecimalFormat("0.##")), // [1-10]
        Format(NO, 1e0f, DecimalFormat("0.#")),
        Format(NO, 1e0f, DecimalFormat("0")),
        Format('k', 1e-3f, DecimalFormat("0.##")),
        Format('k', 1e-3f, DecimalFormat("0.#")),
        Format('k', 1e-3f, DecimalFormat("0")),
        Format('M', 1e-6f, DecimalFormat("0.##")),
        Format('M', 1e-6f, DecimalFormat("0.#")),
        Format('M', 1e-6f, DecimalFormat("0")),
        Format('G', 1e-9f, DecimalFormat("0.##")),
        Format('G', 1e-9f, DecimalFormat("0.#")),
        Format('G', 1e-9f, DecimalFormat("0")),
        Format('T', 1e-12f, DecimalFormat("0.##")),
        Format('T', 1e-12f, DecimalFormat("0.#"))
    )

    private val symbols = DecimalFormatSymbols.getInstance(
        Locale.getDefault(Locale.Category.FORMAT)
    ).apply {
        groupingSeparator = ' '
    }

    val preciseFormat = arrayOf(
        Format('μ', 1e6f, DecimalFormat("0.000000")),
        Format('μ', 1e6f, DecimalFormat("0.000000")),
        Format('μ', 1e6f, DecimalFormat("0.00000")),
        Format('μ', 1e6f, DecimalFormat("0.0000")),
        Format('μ', 1e6f, DecimalFormat("0.0000")), // skipping m (milli-) because too similar to M (mega-)
        Format(NO, 1e0f, DecimalFormat("0.00000000")),
        Format(NO, 1e0f, DecimalFormat("0.0000000")),
        Format(NO, 1e0f, DecimalFormat("0.000000")),
        Format(NO, 1e0f, DecimalFormat("0.000000")), // [1-10]
        Format(NO, 1e0f, DecimalFormat("00.00000")),
        Format(NO, 1e0f, DecimalFormat("000.0000")),
        Format(NO, 1e0f, DecimalFormat("0,000.000", symbols)),
        Format(NO, 1e0f, DecimalFormat("00,000.00", symbols)),
        Format(NO, 1e0f, DecimalFormat("000,000.0", symbols)),
        Format(NO, 1e0f, DecimalFormat("0,000,000", symbols))
    )

    private val amountBuilder = StringBuilder()

    inline fun <reified T> hasFlags(target: EnumSet<T>, flags: EnumSet<T>): Boolean where T : Enum<T>, T : EnumFlag {
        return if (target.isEmpty() || flags.isEmpty()) {
            false
        } else {
            (target.value and flags.value) == flags.value
        }
    }

    inline fun <reified T> hasFlagAny(target: EnumSet<T>, flags: EnumSet<T>): Boolean where T : Enum<T>, T : EnumFlag {
        return if (target.isEmpty() || flags.isEmpty()) {
            false
        } else {
            (target.value and flags.value) != 0
        }
    }

    fun formatTime(time: Float): String {
        amountBuilder.clear()
        return when {
            (time < 10f) -> String.format("%.1f seconds", time)
            (time < 60f) -> String.format("%d seconds", time.toInt())
            (time < 600f) -> String.format("%.1f minutes", time / 60f)
            (time < 3600f) -> String.format("%d minutes", (time / 60f).toInt())
            (time < 36000f) -> String.format("%.1f hours", time / 3600f)
            else -> String.format("%d hours", (time / 3600f).toInt())
        }
    }

    fun formatAmount(
        amount: Float,
        unit: UnitOfMeasure.Plain,
        prefix: String? = null,
        suffix: String? = null,
        precise: Boolean = false
    ): String {
        val (multiplier, unitSuffix) = resolveUnitOfMeasure(unit)
        return formatAmountRaw(
            amount,
            multiplier,
            unitSuffix,
            prefix,
            suffix,
            if (precise) preciseFormat else formatSpec
        )
    }

    fun formatPerSecondAmount(
        time: Int,
        amount: Float,
        prefix: String? = null,
        suffix: String? = null,
        precise: Boolean = false
    ): String {
        val (multiplier, unitSuffix) = unitOfTime(time)
        return formatAmountRaw(
            amount,
            multiplier,
            unitSuffix,
            prefix,
            suffix,
            if (precise) preciseFormat else formatSpec
        )
    }

    fun formatAmount(
        preferences: YAFCProjectPreferences,
        amount: Float,
        unit: UnitOfMeasure.Preference,
        prefix: String? = null,
        suffix: String? = null,
        precise: Boolean = false
    ): String {
        val (multiplier, unitSuffix) = resolveUnitOfMeasure(preferences, unit)
        return formatAmountRaw(
            amount,
            multiplier,
            unitSuffix,
            prefix,
            suffix,
            if (precise) preciseFormat else formatSpec
        )
    }

    private fun formatAmountRaw(
        amount: Float,
        unitMultiplier: Float,
        unitSuffix: String?,
        prefix: String? = null,
        suffix: String? = null,
        formatSpec: Array<Format>
    ): String {
        var mutableAmount = amount
        if (mutableAmount.isNaN() || mutableAmount.isInfinite()) {
            return "-"
        }
        if (mutableAmount == 0f) {
            return "0"
        }

        amountBuilder.clear()
        if (prefix != null) {
            amountBuilder.append(prefix)
        }
        if (mutableAmount < 0) {
            amountBuilder.append('-')
            mutableAmount = -mutableAmount
        }

        mutableAmount *= unitMultiplier

        val idx = clamp(floor(log10(mutableAmount)).toInt() + 8, 0, formatSpec.size - 1)
        val value = formatSpec[idx]
        amountBuilder.append(value.formatter.format(mutableAmount * value.multiplier))

        if (value.suffix != NO) {
            amountBuilder.append(value.suffix)
        }
        amountBuilder.append(unitSuffix)
        if (suffix != null) {
            amountBuilder.append(suffix)
        }

        return amountBuilder.toString()
    }

//    public static bool TryParseAmount(string str, out float amount, UnitOfMeasure unit)
//    {
//        var (mul, _) = Project.current.ResolveUnitOfMeasure(unit)
//        var lastValidChar = 0
//        var multiplier = unit == UnitOfMeasure.Megawatt ? 1e6f : 1f
//        amount = 0
//        foreach (var c in str)
//        {
//            if (c >= '0' && c <= '9' || c == '.' || c == '-' || c == 'e')
//                ++lastValidChar
//            else
//            {
//                if (lastValidChar == 0)
//                    return false
//                switch (c)
//                {
//                    case 'k': case 'K':
//                    multiplier = 1e3f
//                    break
//                    case 'm': case 'M':
//                    multiplier = 1e6f
//                    break
//                    case 'g': case 'G':
//                    multiplier = 1e9f
//                    break
//                    case 't': case 'T':
//                    multiplier = 1e12f
//                    break
//                    case 'μ': case 'u':
//                    multiplier = 1e-6f
//                    break
//                }
//                break
//            }
//        }
//        multiplier /= mul
//        var substr = str.Substring(0, lastValidChar)
//        if (!float.TryParse(substr, out amount)) return false
//        amount *= multiplier
//        if (amount > 1e15)
//            return false
//        return true
//    }

    private fun resolveUnitOfMeasure(unit: UnitOfMeasure.Plain): Pair<Float, String?> {
        return when (unit) {
            UnitOfMeasure.Plain.NONE -> {
                1f to ""
            }

            UnitOfMeasure.Plain.PERCENT -> {
                100f to YAFCBundle.message("yafc.suffix.percent")
            }

            UnitOfMeasure.Plain.SECOND -> {
                1f to YAFCBundle.message("yafc.suffix.second")
            }

            UnitOfMeasure.Plain.MEGAWATT -> {
                1e6f to YAFCBundle.message("yafc.suffix.megawatt")
            }

            UnitOfMeasure.Plain.MEGAJOULE -> {
                1e6f to YAFCBundle.message("yafc.suffix.megajoule")
            }

            UnitOfMeasure.Plain.CELSIUS -> {
                1f to YAFCBundle.message("yafc.suffix.celsius")
            }
        }
    }

    private fun resolveUnitOfMeasure(
        preferences: YAFCProjectPreferences,
        unit: UnitOfMeasure.Preference
    ): Pair<Float, String> {
        return when (unit) {
            UnitOfMeasure.Preference.PER_SECOND -> {
                unitOfTime(preferences.time)
            }

            UnitOfMeasure.Preference.ITEM_PER_SECOND -> {
                itemPerTimeUnit(preferences.time, preferences.itemUnit)
            }

            UnitOfMeasure.Preference.FLUID_PER_SECOND -> {
                fluidPerTimeUnit(preferences.time, preferences.fluidUnit)
            }
        }
    }

    fun unitOfTime(time: Int): Pair<Float, String> {
        return when (time) {
            0, 1 -> {
                1f to YAFCBundle.message("yafc.suffix.per.second")
            }

            60 -> {
                60f to YAFCBundle.message("yafc.suffix.per.minute")
            }

            3600 -> {
                3600f to YAFCBundle.message("yafc.suffix.per.hour")
            }

            else -> {
                time.toFloat() to YAFCBundle.message("yafc.suffix.per.custom")
            }
        }
    }

    private fun itemPerTimeUnit(time: Int, itemUnit: Float): Pair<Float, String> {
        return if (itemUnit == 0f) {
            return unitOfTime(time)
        } else {
            (1f / itemUnit) to YAFCBundle.message("yafc.suffix.item")
        }
    }

    private fun fluidPerTimeUnit(time: Int, fluidUnit: Float): Pair<Float, String> {
        return if (fluidUnit == 0f) {
            return unitOfTime(time)
        } else {
            (1f / fluidUnit) to YAFCBundle.message("yafc.suffix.fluid")
        }
    }

    private fun writeException(exception: Throwable) {
        logger.error(exception)
    }

//    public static string ReadLine(byte[] buffer, ref int position)
//    {
//        if (position > buffer.Length)
//            return null
//        var nextPosition = Array.IndexOf(buffer, (byte) '\n', position)
//        if (nextPosition == -1)
//            nextPosition = buffer.Length
//        var str = Encoding.UTF8.GetString(buffer, position, nextPosition - position)
//        position = nextPosition+1
//        return str
//    }
//
//    public static bool Match(this FactorioObject obj, SearchQuery query)
//    {
//        if (query.empty)
//            return true
//        if (obj == null)
//            return false
//        foreach (var token in query.tokens)
//        {
//            if (obj.name.IndexOf(token, StringComparison.OrdinalIgnoreCase) < 0 &&
//                obj.locName.IndexOf(token, StringComparison.InvariantCultureIgnoreCase) < 0 &&
//                (obj.locDescr == null || obj.locDescr.IndexOf(token, StringComparison.InvariantCultureIgnoreCase) < 0))
//                return false
//        }
//
//        return true
//    }

//    fun IsSourceResource(this FactorioObject obj): Boolean {
//        return Project.current.preferences.sourceResources.Contains(obj)
//    }

    data class Format(val suffix: Char, val multiplier: Float, val formatter: DecimalFormat)
}

sealed interface UnitOfMeasure {
    enum class Plain : UnitOfMeasure {
        NONE,
        PERCENT,
        SECOND,
        MEGAWATT,
        MEGAJOULE,
        CELSIUS;
    }

    enum class Preference : UnitOfMeasure {
        PER_SECOND,
        ITEM_PER_SECOND,
        FLUID_PER_SECOND;
    }
}
