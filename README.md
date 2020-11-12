# Tachyon-Kotlin

## Based on: https://github.com/linkedin/Tachyon

Adapted to Kotlin.

Usage is all the same except you must manually call _createDayView()_ method to create the view and can now re-enter new events to the DayView even if the view is already created.

```
dayView.apply {
    // If dayView is not created, create it first and set it's hour labels
    if (isCreated.not()) {
        createDayView()
        setHourLabelViews(createHourLabelViews())
    }
    // Show created events. You can re-enter new events by calling this method again.
    setEventViews(dayEventViews, dayEventTimeRanges)
}
```
