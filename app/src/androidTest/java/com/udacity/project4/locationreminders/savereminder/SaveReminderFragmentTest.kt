package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.internal.ContextUtils.getActivity
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import com.udacity.project4.util.validData
import com.udacity.project4.utils.EspressoResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock

@MediumTest
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderFragmentTest {

    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var application: Application
    private val dataBindingResource = DataBindingIdlingResource()

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    @Before
    fun resources_register(): Unit = IdlingRegistry.getInstance().run {
        register(EspressoResource.countingIdlingResource)
        register(dataBindingResource)
    }

    @After
    fun resources_not_register(): Unit = IdlingRegistry.getInstance().run {
        unregister(EspressoResource.countingIdlingResource)
        unregister(dataBindingResource)
    }

    @Before
    fun setup() {
        stopKoin()
        application = ApplicationProvider.getApplicationContext()
        val model = module {
            single { SaveReminderViewModel(application, get() as ReminderDataSource) }
            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(application) }
        }
        startKoin { androidContext(application)
            modules(listOf(model))
        }
        viewModel = GlobalContext.get().koin.get()
    }

    @Test
    fun no_title_save_reminder() {
        val navigation = mock(NavController::class.java)
        val action = launchFragmentInContainer<SaveReminderFragment>(Bundle.EMPTY, R.style.AppTheme)
        dataBindingResource.monitorFragment(action)
        action.onFragment { Navigation.setViewNavController(it.view!!, navigation) }
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_enter_title)))
    }

    @Test
    fun no_location_save_reminder() {
        val navigation = mock(NavController::class.java)
        val action = launchFragmentInContainer<SaveReminderFragment>(Bundle.EMPTY, R.style.AppTheme)
        dataBindingResource.monitorFragment(action)
        action.onFragment { Navigation.setViewNavController(it.view!!, navigation) }
        onView(withId(R.id.reminderTitle)).perform(typeText(validData.title))
        closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_select_location)))
    }
    @Test
    fun valid_data_save_reminder() {
        val latlng = LatLng(validData.latitude!!, validData.longitude!!)
        viewModel.selectedLocation(PointOfInterest(latlng, validData.location, null))
        val navigation = mock(NavController::class.java)
        val action = launchFragmentInContainer<SaveReminderFragment>(Bundle.EMPTY, R.style.AppTheme)
        dataBindingResource.monitorFragment(action)
        action.onFragment {
            Navigation.setViewNavController(it.view!!, navigation)
        }
        onView(withId(R.id.reminderTitle)).perform(typeText(validData.title))
        onView(withId(R.id.reminderDescription)).perform(typeText(validData.description))
        closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())


        onView(withText(R.string.reminder_saved))
            .inRoot(withDecorView(not(`is`(getActivity(application)?.window?.decorView))))
            .check(matches(isDisplayed()))
    }
}