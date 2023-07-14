package com.udacity.project4.locationreminders.util

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

val validData = ReminderDataItem(
    title = "test",
    description = "test",
    location = "test",
    latitude = 30.719277,
    longitude = 31.248040
)

val dataNullTitle = ReminderDataItem(
    title = null,
    description = "test",
    location = "test",
    latitude = 30.719277,
    longitude = 31.248040
)

val dataNullLocation = ReminderDataItem(
    title = "test",
    description = "test",
    location = null,
    latitude = 30.719277,
    longitude = 31.248040
)

fun ReminderDataItem.toDTO()= ReminderDTO(
    title = title,
    description = description,
    location = location,
    latitude = latitude,
    longitude = longitude
)