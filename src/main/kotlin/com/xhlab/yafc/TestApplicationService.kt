package com.xhlab.yafc

import com.intellij.openapi.diagnostic.Logger

class TestApplicationService {

    init {
        Logger.getInstance(TestApplicationService::class.java).info("service")
    }
}
