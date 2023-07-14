package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoResource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingResource = DataBindingIdlingResource()

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

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel { RemindersListViewModel(appContext, get() as ReminderDataSource) }
            single { SaveReminderViewModel(appContext, get() as ReminderDataSource) }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin { modules(listOf(myModule)) }
        //Get our real repository
        repository = get()
        //clear the data to start fresh
        runBlocking { repository.deleteAllReminders() }
    }


    @Test
    fun from_save_reminder_click_back_to_list_reminder() {
        val action = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingResource.monitorActivity(action)
        onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())
        pressBack()
        onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))
        action.close()
    }


    @Test
    fun from_select_location_click_back_to_save_reminder() {
        val action = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingResource.monitorActivity(action)
        onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        pressBack()
        onView(withId(R.id.selectLocation)).check(matches(isDisplayed()))
        action.close()
    }
    @Test
    fun check_added_reminder() {
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingResource.monitorActivity(scenario)
        onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.map)).perform(click())
        onView(withId(R.id.Save_Button)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("Title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("Description"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withText("Title")).check(matches(isDisplayed()))
        onView(withText("Description")).check(matches(isDisplayed()))
        scenario.close()
    }

}
