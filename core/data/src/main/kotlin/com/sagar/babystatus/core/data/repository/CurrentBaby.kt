package com.sagar.babystatus.core.data.repository

import com.sagar.babystatus.core.database.dao.BabyDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Runs [query] for the current baby, or emits [empty] while there is none (before onboarding). */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> BabyDao.forBaby(empty: T, query: (String) -> Flow<T>): Flow<T> =
    observeBaby()
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(empty) else query(id) }
