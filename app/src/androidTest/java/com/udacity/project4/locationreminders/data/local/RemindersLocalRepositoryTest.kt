package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.util.validData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.hasSize
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantTask = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private lateinit var repos: RemindersLocalRepository

    @Before
    fun init() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java).allowMainThreadQueries().build()
        repos = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @Test
    fun reminderInsertion() = runBlocking {
        repos.saveReminder(validData)
        val result = repos.getReminders()
        assertThat(result, instanceOf(Result.Success::class.java))
        result as Result.Success
        assertThat(result.data, not(empty()))
        assertThat(result.data, hasSize(1))
    }

    @Test
    fun remider_deleting() = runBlocking {
        repos.saveReminder(validData)
        repos.deleteAllReminders()
        val result = repos.getReminders()
        assertThat(result, instanceOf(Result.Success::class.java))
        result as Result.Success
        assertThat(result.data, empty())
    }

    @Test
    fun reminder_insertion_by_id_while_error() = runBlocking {
        val result = repos.getReminder(validData.id)
        assertThat(result, instanceOf(Result.Error::class.java))
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }
    @Test
    fun reminder_insertion_by_id_while_success() = runBlocking {
        repos.saveReminder(validData)
        val result = repos.getReminder(validData.id)
        assertThat(result, instanceOf(Result.Success::class.java))
        result as Result.Success
        assertThat(result.data, notNullValue())
        assertThat(result.data.title, `is`(validData.title))
        assertThat(result.data.description, `is`(validData.description))
        assertThat(result.data.location, `is`(validData.location))
        assertThat(result.data.latitude, `is`(validData.latitude))
        assertThat(result.data.longitude, `is`(validData.longitude))
    }
    @After
    fun closeDatabase() = database.close()

}