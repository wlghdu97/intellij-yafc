package com.xhlab.yafc.model.math

val ULong.highestBitSet: Int
    get() {
        var x = this
        var set = 0
        if (x > 0xffffffffu) {
            set += 32
            x = x shr 32
        }
        if (x > 0xffffu) {
            set += 16
            x = x shr 16
        }
        if (x > 0xffu) {
            set += 8
            x = x shr 8
        }
        if (x > 0xfu) {
            set += 4
            x = x shr 4
        }
        if (x > 0x3u) {
            set += 2
            x = x shr 2
        }
        if (x > 0x1u) {
            set += 1
        }
        return set
    }
