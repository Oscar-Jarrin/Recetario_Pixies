package com.pixies.recetario

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherRule(private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()) {
    val dispatcher = StandardTestDispatcher(scheduler)

    fun before() = Dispatchers.setMain(dispatcher)
    fun after() = Dispatchers.resetMain()
}
