package com.xhlab.yafc.model.data

sealed interface IFactorioObjectWrapper {
    val text: String
    val target: FactorioObject
    val amount: Float
}
