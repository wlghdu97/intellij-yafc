package com.xhlab.yafc.model.util

import java.util.*

interface EnumFlag {
    val value: Int
}

val <T> EnumSet<T>.value: Int where T : Enum<T>, T : EnumFlag
    get() = this.map { it.value }.reduceOrNull { acc, i -> acc or i } ?: 0

inline infix fun <reified T> EnumSet<T>?.and(other: T?): EnumSet<T> where T : Enum<T>, T : EnumFlag {
    return this and other?.toSet()
}

inline infix fun <reified T> T?.and(other: T?): EnumSet<T> where T : Enum<T>, T : EnumFlag {
    return this?.toSet() and other?.toSet()
}

inline infix fun <reified T> T?.and(other: EnumSet<T>?): EnumSet<T> where T : Enum<T>, T : EnumFlag {
    return this?.toSet() and other
}

inline infix fun <reified T> EnumSet<T>?.and(other: EnumSet<T>?): EnumSet<T> where T : Enum<T>, T : EnumFlag {
    return if (this == null || other == null) {
        EnumSet.noneOf(T::class.java)
    } else {
        val value = this.value and other.value
        if (value == 0) {
            EnumSet.noneOf(T::class.java)
        } else {
            setOfMatching { it and value == it }
        }
    }
}

inline infix fun <reified T> EnumSet<T>?.or(other: T?): EnumSet<T> where T : Enum<T>, T : EnumFlag {
    return when {
        (this == null) -> {
            EnumSet.of(other)
        }

        (other == null) -> {
            this
        }

        else -> {
            val value = this.value or other.value
            setOfMatching { it and value != 0 }
        }
    }
}

inline infix fun <reified T> T?.or(other: T?): EnumSet<T> where T : Enum<T>, T : EnumFlag {
    return when {
        (this == null) -> {
            EnumSet.of(other)
        }

        (other == null) -> {
            EnumSet.of(this)
        }

        else -> {
            val value = this.value or other.value
            setOfMatching { it and value != 0 }
        }
    }
}

inline infix fun <reified T> T?.or(other: EnumSet<T>?): EnumSet<T> where T : Enum<T>, T : EnumFlag {
    return when {
        (this == null) -> {
            other ?: EnumSet.noneOf(T::class.java)
        }

        (other == null) -> {
            EnumSet.of(this)
        }

        else -> {
            val value = value or other.value
            setOfMatching { it and value != 0 }
        }
    }
}

inline infix fun <reified T> EnumSet<T>?.or(other: EnumSet<T>?): EnumSet<T> where T : Enum<T>, T : EnumFlag {
    return when {
        (this == null) -> {
            other ?: EnumSet.noneOf(T::class.java)
        }

        (other == null) -> {
            this
        }

        else -> {
            val value = this.value or other.value
            setOfMatching { it and value != 0 }
        }
    }
}

inline fun <reified T> T?.inv(): EnumSet<T> where T : Enum<T>, T : EnumFlag {
    return this?.toSet()?.inv() ?: EnumSet.allOf(T::class.java)
}

inline fun <reified T> EnumSet<T>?.inv(): EnumSet<T> where T : Enum<T>, T : EnumFlag {
    return if (this == null) {
        EnumSet.allOf(T::class.java)
    } else {
        EnumSet.complementOf(this)
    }
}

inline fun <reified T> setOfMatching(flag: (Int) -> Boolean): EnumSet<T> where T : Enum<T>, T : EnumFlag {
    val values = enumValues<T>().filter { flag.invoke(it.value) }
    return if (values.isNotEmpty()) {
        EnumSet.of(values.first(), *values.drop(1).toTypedArray())
    } else {
        EnumSet.noneOf(T::class.java)
    }
}

fun <T : Enum<T>> T.toSet(): EnumSet<T> = EnumSet.of(this)
