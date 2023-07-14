package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var fakeData: FakeDataSource
    private lateinit var viewModel: SaveReminderViewModel

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        stopKoin()
        fakeData = FakeDataSource()
        viewModel = SaveReminderViewModel(getApplicationContext(), fakeData)
    }

    @Test
    fun savingSucceedValidItemInViewModel() {
        val Value = viewModel.validateAndSaveReminder(validData)
        assertThat(Value, `is`(true))
        assertThat(viewModel.showToast.getOrAwaitValue(), `is`(getString(R.string.reminder_saved)))
    }

    @Test
    fun validateAndSaveReminder_error_with_selection_location() {
        val result = viewModel.validateAndSaveReminder(dataNullLocation)
        assertThat(result, `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }

    @Test
    fun validateAndSaveReminder_check_loading_reminders() {
        mainCoroutineRule.pauseDispatcher()
        viewModel.validateAndSaveReminder(validData)
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun validateAndSaveReminder_error_with_entering_title() {
        val result = viewModel.validateAndSaveReminder(dataNullTitle)
        assertThat(result, `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }


}