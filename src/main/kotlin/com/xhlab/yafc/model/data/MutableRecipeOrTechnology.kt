package com.xhlab.yafc.model.data

internal sealed interface MutableRecipeOrTechnology : RecipeOrTechnology, MutableFactorioObject {
    override var crafters: List<MutableEntityCrafter>
    override var ingredients: List<MutableIngredient>
    override var products: List<MutableProduct>
    override var modules: List<MutableItem>
    override var sourceEntity: MutableEntity?
    override var mainProduct: MutableGoods?
    override var time: Float
    override var enabled: Boolean
    override var hidden: Boolean
    override var flags: RecipeFlags?
}
