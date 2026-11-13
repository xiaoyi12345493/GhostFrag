package com.ghostfrag.app

import android.app.Application
import com.ghostfrag.app.data.AndroidGhostfragRepository
import com.ghostfrag.app.data.DirtyFragCompanion
import com.ghostfrag.app.domain.repository.GhostfragRepository

/** Application composition root. It is the only place that binds data implementations to domain ports. */
class GhostfragApplication : Application() {
    fun createRepository(): GhostfragRepository = AndroidGhostfragRepository(this)

    fun createDirtyFragCompanion(): DirtyFragCompanion = DirtyFragCompanion(this)
}
