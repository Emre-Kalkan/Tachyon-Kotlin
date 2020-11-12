package com.emrekalkan.tachyon_kotlin

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import java.util.*

class DayViewTest {
    private val mDividerHeight = 7
    private val mHalfHourHeight = 28
    private val mHourLabelMarginEnd = 17
    private val mEventMargin = 3
    private val mMinuteHeight = mHalfHourHeight / 30f
    private val mParentWidth = 200

    private var context: Context = mock()

    private var attrs: AttributeSet = mock()

    private var array: TypedArray = mock()

    private var hourLabelView: View = mock()

    private var eventView: View = mock()

    private lateinit var dayView: DayView

    @Before
    fun setup() {
        whenever(context.obtainStyledAttributes(attrs, R.styleable.DayView)).thenReturn(array)

        whenever(array.getDimensionPixelSize(R.styleable.DayView_dividerHeight, 0)).thenReturn(mDividerHeight)
        whenever(array.getDimensionPixelSize(R.styleable.DayView_halfHourHeight, 0)).thenReturn(mHalfHourHeight)
        whenever(array.getDimensionPixelSize(R.styleable.DayView_hourLabelMarginEnd, 0)).thenReturn(mHourLabelMarginEnd)
        whenever(array.getDimensionPixelSize(R.styleable.DayView_eventMargin, 0)).thenReturn(mEventMargin)

        whenever(array.getInt(R.styleable.DayView_startHour, DayView.MIN_START_HOUR)).thenReturn(
            DayView.MIN_START_HOUR
        )
        whenever(array.getInt(R.styleable.DayView_endHour, DayView.MAX_END_HOUR)).thenReturn(DayView.MAX_END_HOUR)

        whenever(array.getColor(R.styleable.DayView_hourDividerColor, 0)).thenReturn(0)
        whenever(array.getColor(R.styleable.DayView_halfHourDividerColor, 0)).thenReturn(0)

        whenever(hourLabelView.measuredWidth).thenReturn(50)
        whenever(hourLabelView.measuredHeight).thenReturn(20)

        dayView = DayView(context, attrs, 0)
        dayView.createTestDayView()

        val hourLabelViews: MutableList<View> = ArrayList()
        for (i in dayView.startHour..dayView.endHour) {
            hourLabelViews.add(hourLabelView)
        }

        val eventTimeRanges: MutableList<DayView.EventTimeRange> = ArrayList()
        eventTimeRanges.add(DayView.EventTimeRange(30, 180))
        eventTimeRanges.add(DayView.EventTimeRange(90, 120))
        eventTimeRanges.add(DayView.EventTimeRange(150, 300))
        eventTimeRanges.add(DayView.EventTimeRange(150, 300))

        val eventViews: MutableList<View> = ArrayList()
        for (i in eventTimeRanges.indices) {
            eventViews.add(eventView)
        }

        val eventRects: MutableList<DirectionalRect> = ArrayList()
        for (i in eventViews.indices) {
            eventRects.add(DirectionalRect())
        }

        dayView.mHourLabelViews.addAll(hourLabelViews)
        dayView.mFilteredEventViews.addAll(eventViews)
        dayView.mFilteredEventTimeRanges.addAll(eventTimeRanges)
        dayView.mEventColumnSpansHelper = DayView.EventColumnSpansHelper(eventTimeRanges)
        dayView.mEventRects.addAll(eventRects)
        dayView.setParentWidth(mParentWidth)
    }

    @Test
    fun setHourLabelRects() {
        dayView.setHourLabelRects(25, 75, 90)
        MatcherAssert.assertThat(dayView.mHourLabelRects[0].left, CoreMatchers.`is`(25))
        MatcherAssert.assertThat(dayView.mHourLabelRects[0].top, CoreMatchers.`is`(80))
        MatcherAssert.assertThat(dayView.mHourLabelRects[0].right, CoreMatchers.`is`(75))
        MatcherAssert.assertThat(dayView.mHourLabelRects[0].bottom, CoreMatchers.`is`(100))
        MatcherAssert.assertThat(dayView.mHourLabelRects[6].left, CoreMatchers.`is`(25))
        MatcherAssert.assertThat(dayView.mHourLabelRects[6].top, CoreMatchers.`is`(500))
        MatcherAssert.assertThat(dayView.mHourLabelRects[6].right, CoreMatchers.`is`(75))
        MatcherAssert.assertThat(dayView.mHourLabelRects[6].bottom, CoreMatchers.`is`(520))
        MatcherAssert.assertThat(dayView.mHourLabelRects[13].left, CoreMatchers.`is`(25))
        MatcherAssert.assertThat(dayView.mHourLabelRects[13].top, CoreMatchers.`is`(990))
        MatcherAssert.assertThat(dayView.mHourLabelRects[13].right, CoreMatchers.`is`(75))
        MatcherAssert.assertThat(dayView.mHourLabelRects[13].bottom, CoreMatchers.`is`(1010))
        MatcherAssert.assertThat(dayView.mHourLabelRects[21].left, CoreMatchers.`is`(25))
        MatcherAssert.assertThat(dayView.mHourLabelRects[21].top, CoreMatchers.`is`(1550))
        MatcherAssert.assertThat(dayView.mHourLabelRects[21].right, CoreMatchers.`is`(75))
        MatcherAssert.assertThat(dayView.mHourLabelRects[21].bottom, CoreMatchers.`is`(1570))
    }

    @Test
    fun setDividerRects() {
        dayView.setDividerRects(10, 5, 195)
        MatcherAssert.assertThat(dayView.mHourDividerRects[0].left, CoreMatchers.`is`(5))
        MatcherAssert.assertThat(dayView.mHourDividerRects[0].top, CoreMatchers.`is`(10))
        MatcherAssert.assertThat(dayView.mHourDividerRects[0].right, CoreMatchers.`is`(195))
        MatcherAssert.assertThat(dayView.mHourDividerRects[0].bottom, CoreMatchers.`is`(17))
        MatcherAssert.assertThat(dayView.mHalfHourDividerRects[7].left, CoreMatchers.`is`(5))
        MatcherAssert.assertThat(dayView.mHalfHourDividerRects[7].top, CoreMatchers.`is`(535))
        MatcherAssert.assertThat(dayView.mHalfHourDividerRects[7].right, CoreMatchers.`is`(195))
        MatcherAssert.assertThat(dayView.mHalfHourDividerRects[7].bottom, CoreMatchers.`is`(542))
        MatcherAssert.assertThat(dayView.mHourDividerRects[19].left, CoreMatchers.`is`(5))
        MatcherAssert.assertThat(dayView.mHourDividerRects[19].top, CoreMatchers.`is`(1340))
        MatcherAssert.assertThat(dayView.mHourDividerRects[19].right, CoreMatchers.`is`(195))
        MatcherAssert.assertThat(dayView.mHourDividerRects[19].bottom, CoreMatchers.`is`(1347))
        MatcherAssert.assertThat(dayView.mHalfHourDividerRects[22].left, CoreMatchers.`is`(5))
        MatcherAssert.assertThat(dayView.mHalfHourDividerRects[22].top, CoreMatchers.`is`(1585))
        MatcherAssert.assertThat(dayView.mHalfHourDividerRects[22].right, CoreMatchers.`is`(195))
        MatcherAssert.assertThat(dayView.mHalfHourDividerRects[22].bottom, CoreMatchers.`is`(1592))
    }

    @Test
    fun setEventRects() {
        dayView.setEventRects(10, mMinuteHeight, 5, 195)
        MatcherAssert.assertThat(dayView.mEventRects, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(dayView.mEventRects[0].left, CoreMatchers.`is`(8))
        MatcherAssert.assertThat(dayView.mEventRects[0].top, CoreMatchers.`is`(48))
        MatcherAssert.assertThat(dayView.mEventRects[0].right, CoreMatchers.`is`(65))
        MatcherAssert.assertThat(dayView.mEventRects[0].bottom, CoreMatchers.`is`(175))
        MatcherAssert.assertThat(dayView.mEventRects[1].left, CoreMatchers.`is`(71))
        MatcherAssert.assertThat(dayView.mEventRects[1].top, CoreMatchers.`is`(104))
        MatcherAssert.assertThat(dayView.mEventRects[1].right, CoreMatchers.`is`(191))
        MatcherAssert.assertThat(dayView.mEventRects[1].bottom, CoreMatchers.`is`(119))
        MatcherAssert.assertThat(dayView.mEventRects[2].left, CoreMatchers.`is`(71))
        MatcherAssert.assertThat(dayView.mEventRects[2].top, CoreMatchers.`is`(160))
        MatcherAssert.assertThat(dayView.mEventRects[2].right, CoreMatchers.`is`(128))
        MatcherAssert.assertThat(dayView.mEventRects[2].bottom, CoreMatchers.`is`(287))
        MatcherAssert.assertThat(dayView.mEventRects[3].left, CoreMatchers.`is`(134))
        MatcherAssert.assertThat(dayView.mEventRects[3].top, CoreMatchers.`is`(160))
        MatcherAssert.assertThat(dayView.mEventRects[3].right, CoreMatchers.`is`(191))
        MatcherAssert.assertThat(dayView.mEventRects[3].bottom, CoreMatchers.`is`(287))
    }

    @Test
    fun setRect() {
        val rect = DirectionalRect()
        rect[false, 20, 1, 2, 3] = 4
        MatcherAssert.assertThat(rect.left, CoreMatchers.`is`(1))
        MatcherAssert.assertThat(rect.top, CoreMatchers.`is`(2))
        MatcherAssert.assertThat(rect.right, CoreMatchers.`is`(3))
        MatcherAssert.assertThat(rect.bottom, CoreMatchers.`is`(4))
        rect[true, 20, 1, 2, 3] = 4
        MatcherAssert.assertThat(rect.left, CoreMatchers.`is`(17))
        MatcherAssert.assertThat(rect.top, CoreMatchers.`is`(2))
        MatcherAssert.assertThat(rect.right, CoreMatchers.`is`(19))
        MatcherAssert.assertThat(rect.bottom, CoreMatchers.`is`(4))
    }

    @Test
    fun timeRanges() {
        val range = DayView.EventTimeRange(20, 40)
        MatcherAssert.assertThat(range.conflicts(DayView.EventTimeRange(5, 15)), CoreMatchers.`is`(false))
        MatcherAssert.assertThat(range.conflicts(DayView.EventTimeRange(50, 90)), CoreMatchers.`is`(false))
        MatcherAssert.assertThat(range.conflicts(DayView.EventTimeRange(5, 20)), CoreMatchers.`is`(false))
        MatcherAssert.assertThat(range.conflicts(DayView.EventTimeRange(40, 90)), CoreMatchers.`is`(false))
        MatcherAssert.assertThat(range.conflicts(DayView.EventTimeRange(20, 40)), CoreMatchers.`is`(true))
        MatcherAssert.assertThat(range.conflicts(DayView.EventTimeRange(10, 60)), CoreMatchers.`is`(true))
        MatcherAssert.assertThat(range.conflicts(DayView.EventTimeRange(25, 35)), CoreMatchers.`is`(true))
        MatcherAssert.assertThat(range.conflicts(DayView.EventTimeRange(10, 35)), CoreMatchers.`is`(true))
        MatcherAssert.assertThat(range.conflicts(DayView.EventTimeRange(25, 50)), CoreMatchers.`is`(true))
    }

    @Test
    fun singleEventColumnSpan() {
        val timeRanges = listOf(DayView.EventTimeRange(55, 133))
        val columnSpansHelper: DayView.EventColumnSpansHelper = DayView.EventColumnSpansHelper(timeRanges)
        MatcherAssert.assertThat(columnSpansHelper.columnSpans[0].startColumn, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(columnSpansHelper.columnSpans[0].endColumn, CoreMatchers.`is`(1))
        MatcherAssert.assertThat(columnSpansHelper.columnCount, CoreMatchers.`is`(1))
    }

    @Test
    fun multipleEventColumnSpans() {
        val timeRanges: MutableList<DayView.EventTimeRange> = ArrayList()
        timeRanges.add(DayView.EventTimeRange(30, 180))
        timeRanges.add(DayView.EventTimeRange(90, 120))
        timeRanges.add(DayView.EventTimeRange(150, 300))
        timeRanges.add(DayView.EventTimeRange(150, 300))
        val columnSpansHelper: DayView.EventColumnSpansHelper = DayView.EventColumnSpansHelper(timeRanges)
        MatcherAssert.assertThat(columnSpansHelper.columnSpans[0].startColumn, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(columnSpansHelper.columnSpans[0].endColumn, CoreMatchers.`is`(1))
        MatcherAssert.assertThat(columnSpansHelper.columnSpans[1].startColumn, CoreMatchers.`is`(1))
        MatcherAssert.assertThat(columnSpansHelper.columnSpans[1].endColumn, CoreMatchers.`is`(3))
        MatcherAssert.assertThat(columnSpansHelper.columnSpans[2].startColumn, CoreMatchers.`is`(1))
        MatcherAssert.assertThat(columnSpansHelper.columnSpans[2].endColumn, CoreMatchers.`is`(2))
        MatcherAssert.assertThat(columnSpansHelper.columnSpans[3].startColumn, CoreMatchers.`is`(2))
        MatcherAssert.assertThat(columnSpansHelper.columnSpans[3].endColumn, CoreMatchers.`is`(3))
        MatcherAssert.assertThat(columnSpansHelper.columnCount, CoreMatchers.`is`(3))
    }
}