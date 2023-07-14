package com.udacity.project4.utils

import androidx.test.espresso.idling.CountingIdlingResource

object EspressoResource {

    private const val RESOURCE_TAG = "GLOBAL"

    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE_TAG)

    fun increment() {
        countingIdlingResource.increment()
    }

    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }
}

inline fun <T> wrapEspressoResource(function: () -> T): T {
    EspressoResource.increment()
    return try {
        function()
    } finally {
        EspressoResource.decrement()
    }
}