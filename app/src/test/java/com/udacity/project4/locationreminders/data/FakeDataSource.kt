package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeDataSource(private val reminders: MutableList<ReminderDTO> = mutableListOf()) : ReminderDataSource {

    private var returnError = false

    fun setShouldReturnError(shouldReturn: Boolean) {
        this.returnError = shouldReturn
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> =
        if (returnError) {
            Result.Error(" Error With Test")
        } else {
            Result.Success(reminders)
        }
    override suspend fun deleteAllReminders() {
        reminders.clear()
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> =
        if (returnError) {
            Result.Error(" Error With Test")
        } else {
            val remind = reminders.find { it.id == id }
            if (remind == null) {
                Result.Error("Reminder Not found")
            } else {
                Result.Success(remind)
            }
        }


}