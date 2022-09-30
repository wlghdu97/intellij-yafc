package com.xhlab.yafc

import com.xhlab.yafc.model.util.*
import org.junit.Test
import java.util.*

class EnumFlagTest {
    private val x = EnumSet.of(TestEnum.ONE, TestEnum.TWO)
    private val y = EnumSet.of(TestEnum.TWO, TestEnum.THREE)
    private val z = EnumSet.of(TestEnum.THREE, TestEnum.ONE)

    private val one = EnumSet.allOf(TestEnum::class.java)
    private val zero = EnumSet.noneOf(TestEnum::class.java)

    @Test
    fun testAssociativity() {
        assert(x or (y or z) == (x or y) or z)
        assert(x and (y and z) == (x and y) and z)
    }

    @Test
    fun testCommutativity() {
        assert(x or y == y or x)
        assert(x and y == y and x)
    }

    @Test
    fun testDistributivity() {
        assert(x and (y or z) == (x and y) or (x and z))
        assert(x or (y and z) == (x or y) and (x or z))
    }

    @Test
    fun testIdentity() {
        assert(x or zero == x)
        assert(x and one == x)
    }

    @Test
    fun testAnnihilator() {
        assert(x or one == one)
        assert(x and zero == zero)
    }

    @Test
    fun testIdempotence() {
        assert(x or x == x)
        assert(x and x == x)
    }

    @Test
    fun testAbsorption() {
        assert(x and (x or y) == x)
        assert(x or (x and y) == x)
    }

    @Test
    fun testComplementation() {
        assert(x and x.inv() == zero)
        assert(x or x.inv() == one)
    }

    @Test
    fun testInv() {
        val w = TestEnum.ONE
        assert(w.inv() == w.toSet().inv())
    }

    enum class TestEnum constructor(override val value: Int) : EnumFlag {
        ONE(1 shl 0),
        TWO(1 shl 1),
        THREE(1 shl 2);
    }
}
