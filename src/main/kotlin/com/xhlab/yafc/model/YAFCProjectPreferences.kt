package com.xhlab.yafc.model

import com.xhlab.yafc.model.data.EntityBelt
import com.xhlab.yafc.model.data.EntityInserter

interface YAFCProjectPreferences {
    var time: Int
    var itemUnit: Float
    var fluidUnit: Float
    var defaultBelt: EntityBelt?
    var defaultInserter: EntityInserter?
    var inserterCapacity: Int
    val sourceResources: Set<String>
    val favourites: Set<String>
    var targetTechnology: String?
}
