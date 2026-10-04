package com.example.carebrief

import android.app.Application
import androidx.room.Room
import com.example.carebrief.core.database.CareBriefDatabase
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.CarePlanStore
import com.example.carebrief.data.RoomCareBriefRepository
import com.example.carebrief.data.SettingsStore
import com.example.carebrief.notifications.ReminderScheduler
import com.example.carebrief.notifications.ensureChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CareBriefApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CarePlanStore.shared.restore(applicationContext)
        ensureChannel(this)
        val database = Room.databaseBuilder(
            applicationContext,
            CareBriefDatabase::class.java,
            "carebrief.db"
        ).addMigrations(CareBriefDatabase.MIGRATION_1_2).build()
        val repository = RoomCareBriefRepository(database)
        DemoCareBriefRepository.shared = repository
        applicationScope.launch {
            repository.seedIfEmpty()
            val enabled = SettingsStore(applicationContext).notificationsEnabled.first()
            ReminderScheduler.setEnabled(applicationContext, enabled)
        }
    }
}
