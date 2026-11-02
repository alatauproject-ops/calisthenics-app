package com.edward.calisthenics.data

import kotlinx.serialization.json.Json
import java.io.File

/** Хранит всё состояние приложения в одном JSON-файле на телефоне. */
class Store(dir: File) {
    private val file = File(dir, "state-v2.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): AppState? = runCatching {
        if (file.exists()) json.decodeFromString(AppState.serializer(), file.readText()) else null
    }.getOrNull()

    @Synchronized
    fun save(state: AppState) {
        val tmp = File(file.parentFile, "state-v2.json.tmp")
        tmp.writeText(json.encodeToString(AppState.serializer(), state))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
