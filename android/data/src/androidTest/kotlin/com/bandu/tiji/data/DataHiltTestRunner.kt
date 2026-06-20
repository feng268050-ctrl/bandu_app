package com.bandu.tiji.data

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class DataHiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application =
        super.newApplication(
            classLoader,
            HiltTestApplication::class.java.name,
            context,
        )
}
