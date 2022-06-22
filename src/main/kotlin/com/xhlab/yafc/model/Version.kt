package com.xhlab.yafc.model

data class Version(
    val major: Int,
    val minor: Int,
    val build: Int = -1,
    val revision: Int = -1
) : Comparable<Version> {

    override fun compareTo(other: Version): Int {
        return when {
            (this.major != other.major) -> {
                this.major.compareTo(other.major)
            }

            (this.minor != other.minor) -> {
                this.minor.compareTo(other.minor)
            }

            (this.build != other.build) -> {
                this.build.compareTo(other.build)
            }

            else -> {
                this.revision.compareTo(other.revision)
            }
        }
    }

    class NoMinorException : Exception()

    class InvalidVersionFormatException : Exception()

    override fun toString(): String {
        return with(StringBuilder()) {
            append(major.toString())
            append(".")
            append(minor.toString())
            if (build >= 0) {
                append(".")
                append(build.toString())
                if (revision >= 0) {
                    append(".")
                    append(revision.toString())
                }
            }
        }.toString()
    }

    companion object {
        fun fromString(versionString: String): Version {
            val versions = versionString.split(".").map {
                it.toIntOrNull() ?: throw InvalidVersionFormatException()
            }
            return when (versions.size) {
                4 -> Version(versions[0], versions[1], versions[2], versions[3])
                3 -> Version(versions[0], versions[1], versions[2])
                2 -> Version(versions[0], versions[1])
                else -> throw NoMinorException()
            }
        }
    }
}
