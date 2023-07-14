package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.util.validData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

    @get:Rule
    val instantTask = InstantTaskExecutorRule()
    private lateinit var database: RemindersDatabase


    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(getApplicationContext(),RemindersDatabase::class.java).allowMainThreadQueries().build()
    }

    @Test
    fun reminder_insertion() = runBlockingTest {
        database.reminderDao().saveReminder(validData)
        val remind = database.reminderDao().getReminders()
        assertThat<List<ReminderDTO>>(remind, notNullValue())
        assertThat(remind, contains(validData))
    }

    @Test
    fun remider_deleting() = runBlockingTest {
        database.reminderDao().saveReminder(validData)
        assertThat(database.reminderDao().getReminders(), hasSize(1))
        database.reminderDao().deleteAllReminders()
        assertThat(database.reminderDao().getReminders(), empty())
    }

    @Test
    fun reminder_insertion_by_id() = runBlockingTest {
        database.reminderDao().saveReminder(validData)
        val remind = database.reminderDao().getReminderById(validData.id)
        assertThat<ReminderDTO>(remind as ReminderDTO, notNullValue())
        assertThat(remind.id, `is`(validData.id))
        assertThat(remind.title, `is`(validData.title))
        assertThat(remind.description, `is`(validData.description))
        assertThat(remind.location, `is`(validData.location))
        assertThat(remind.latitude, `is`(validData.latitude))
        assertThat(remind.longitude, `is`(validData.longitude))
    }

    @After
    fun closeDatabase() = database.close()

}