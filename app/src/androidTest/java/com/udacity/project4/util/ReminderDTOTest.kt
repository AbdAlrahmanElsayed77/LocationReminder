package com.udacity.project4.util

import com.udacity.project4.locationreminders.data.dto.ReminderDTO

val validData = ReminderDTO(
    title = "test title",
    description = "test description",
    location = "test location",
    latitude = 30.719277,
    longitude = 31.248040
)

val dataNullTitle = ReminderDTO(
    title = null,
    description = "test description",
    location = "test location",
    latitude = 30.719277,
    longitude = 31.248040
)

val dataNullLocation = ReminderDTO(
    title = "test title",
    description = "test description",
    location = null,
    latitude = 30.719277,
    longitude = 31.248040
)