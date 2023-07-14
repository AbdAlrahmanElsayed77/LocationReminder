package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import com.udacity.project4.util.validData
import com.udacity.project4.utils.EspressoResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest :
    AutoCloseKoinTest() {

    private lateinit var repos: ReminderDataSource
    private lateinit var application: Application
    private val dataBindingResource = DataBindingIdlingResource()

    @get:Rule
        val instantTask = InstantTaskExecutorRule()

    @Before
    fun resources_register_id(): Unit = IdlingRegistry.getInstance().run {
        register(EspressoResource.countingIdlingResource)
        register(dataBindingResource)
    }

    @After
    fun resources_not_register_id(): Unit = IdlingRegistry.getInstance().run {
        unregister(EspressoResource.countingIdlingResource)
        unregister(dataBindingResource)
    }

    @Before
    fun init() {
        stopKoin()
        application = getApplicationContext()
        val model = module {
            viewModel { RemindersListViewModel(application, get() as ReminderDataSource) }
            single { SaveReminderViewModel(application, get() as ReminderDataSource) }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(application) }
        }
        startKoin { modules(listOf(model)) }
        repos = get()
        runBlocking { repos.deleteAllReminders() }
    }

    @Test
    fun click_on_add_Fab_in_reminder_list() {
        val action = launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        val navigation = Mockito.mock(NavController::class.java)
        dataBindingResource.monitorFragment(action)
        action.onFragment { Navigation.setViewNavController(it.view!!, navigation) }
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        verify(navigation).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun open_reminder_list_null() {
        val action = launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        val navigation = Mockito.mock(NavController::class.java)
        dataBindingResource.monitorFragment(action)
        action.onFragment { Navigation.setViewNavController(it.view!!, navigation) }
        onView(withText(R.string.no_data)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun open_reminder_list_not_null() {
        val remind = validData
        runBlocking { repos.saveReminder(remind) }
        val action = launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        val navigation = Mockito.mock(NavController::class.java)
        dataBindingResource.monitorFragment(action)
        action.onFragment { Navigation.setViewNavController(it.view!!, navigation) }
        onView(withText(R.string.no_data)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withText(remind.title)).check(matches(isDisplayed())).check(matches(withText(validData.title)))
        onView(withText(remind.description)).check(matches(isDisplayed())).check(matches(withText(validData.description)))
        onView(withText(remind.location)).check(matches(isDisplayed())).check(matches(withText(validData.location)))
    }


}