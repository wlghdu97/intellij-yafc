package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.model.data.entity.Entity
import com.xhlab.yafc.model.data.entity.EntityCrafter
import javax.swing.Icon

internal data class MutableTechnology(
    override val name: String,
    override var factorioType: String? = null,
    override var originalName: String? = null,
    override var typeDotName: String? = null,
    override var locName: String? = null,
    override var locDescr: String? = null,
    override var iconSpec: List<FactorioIconPart>? = null,
    override var icon: Icon? = null,
    override var id: FactorioId? = null,
    override var crafters: List<EntityCrafter> = emptyList(),
    override var ingredients: List<MutableIngredient> = emptyList(),
    override var products: List<MutableProduct> = emptyList(),
    override var modules: List<MutableItem> = emptyList(),
    override var sourceEntity: Entity? = null,
    override var mainProduct: MutableGoods? = null,
    override var time: Float = 0f,
    override var enabled: Boolean = false,
    override var hidden: Boolean = false,
    override var flags: RecipeFlags? = null,
    var count: Float = 0f, // TODO support formula count
    var unlockRecipes: List<MutableRecipe> = emptyList()
) : MutableRecipeOrTechnology(name) { // Technology is very similar to recipe
    var prerequisites: List<MutableTechnology> = emptyList()

    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.TECHNOLOGIES
    override val type: String = "Technology"

    override fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>) {
        super.getDependencies(collector, temp)
        if (prerequisites.isNotEmpty()) {
            collector.addObject(prerequisites, DependencyList.Flags.TECHNOLOGY_PREREQUISITES)
        }
        if (hidden && !enabled) {
            collector.addId(emptyList(), DependencyList.Flags.HIDDEN)
        }
    }
}
