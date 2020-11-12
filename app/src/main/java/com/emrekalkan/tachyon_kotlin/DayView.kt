package com.emrekalkan.tachyon_kotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.core.view.ViewCompat
import kotlin.math.max
import kotlin.math.min

/** Based on: https://github.com/linkedin/Tachyon
 *
 * Customized to set start hour and end hour programmatically.
 * */
open class DayView : ViewGroup {
    /**
     * Because of daylight saving time, some days are shorter or longer than 24 hours. Most calendar
     * apps assume there are 24 hours in each day, and then to handle events that span a daylight
     * saving time switch those events are adjusted. For example, when daylight saving time begins,
     * an event from 1:00 AM to 3:00 AM would only last an hour since the switch happens at 2:00 AM.
     * This means for events that span the beginning of daylight saving time, they will be drawn
     * with an extra hour. For events that span the end of daylight saving time, they'll be drawn at
     * the minimum height for an event if the event's duration is roughly an hour or less.
     */
    companion object {
        const val MIN_START_HOUR = 0

        const val MAX_END_HOUR = 24

        const val MINUTES_PER_HOUR = 60
        const val MIN_DURATION_MINUTES = 15
    }

    /** Boolean flag to hold measuring of views. */
    private var mIsWaitMeasureEnabled = true
    private var mStartMinute = 0
    private var mEndMinute = 0
    private var mMinuteCount = 0
    private var mHourLabelsCount = 0
    private var mHourDividersCount = 0
    private var mHalfHourDividersCount = 0

    private var mHourDividerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mHalfHourDividerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var mDividerHeight = 0

    private var mUsableHalfHourHeight = 0
    private var mHourLabelWidth = 0
    private var mHourLabelMarginEnd = 0
    private var mEventMargin = 0

    private var mIsRtl = false
    private var mParentWidth = 0
    private var mMinuteHeight = 0f

    private val mEventViews: ArrayList<View> = arrayListOf()

    /** Desired start hour of the day. Default is 0 */
    var startHour = 0

    /** Desired end hour of the day. Default is 24 */
    var endHour = 24

    /** Holds a flag to determine if the view is created/showed. Event views and hour labels are not included. */
    var isCreated = false

    @VisibleForTesting
    var mHourLabelRects: ArrayList<DirectionalRect> = arrayListOf()

    @VisibleForTesting
    var mHourDividerRects: ArrayList<DirectionalRect> = arrayListOf()

    @VisibleForTesting
    var mEventColumnSpansHelper: EventColumnSpansHelper? = null

    @VisibleForTesting
    var mHalfHourDividerRects: ArrayList<DirectionalRect> = arrayListOf()

    @VisibleForTesting
    val mHourLabelViews: ArrayList<View> = arrayListOf()

    @VisibleForTesting
    val mFilteredEventViews: ArrayList<View> = arrayListOf()

    @VisibleForTesting
    val mFilteredEventTimeRanges: ArrayList<EventTimeRange> = arrayListOf()

    @VisibleForTesting
    val mEventRects: ArrayList<DirectionalRect> = arrayListOf()

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val array = context!!.obtainStyledAttributes(attrs, R.styleable.DayView)

        // The total number of usable minutes in this day
        startHour = max(
            array.getInt(R.styleable.DayView_startHour, MIN_START_HOUR),
            MIN_START_HOUR
        )
        endHour = min(
            array.getInt(R.styleable.DayView_endHour, MAX_END_HOUR),
            MAX_END_HOUR
        )

        calculateCounts()

        mDividerHeight = array.getDimensionPixelSize(R.styleable.DayView_dividerHeight, 0)
        mUsableHalfHourHeight = mDividerHeight + array.getDimensionPixelSize(R.styleable.DayView_halfHourHeight, 0)

        mHourDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mHalfHourDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        mHourDividerPaint.color = array.getColor(R.styleable.DayView_hourDividerColor, 0)
        mHalfHourDividerPaint.color = array.getColor(R.styleable.DayView_halfHourDividerColor, 0)

        mHourLabelWidth = array.getDimensionPixelSize(R.styleable.DayView_hourLabelWidth, 0)
        mHourLabelMarginEnd = array.getDimensionPixelSize(R.styleable.DayView_hourLabelMarginEnd, 0)
        mEventMargin = array.getDimensionPixelSize(R.styleable.DayView_eventMargin, 0)

        array.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var measuredHeight = 0
        if (!mIsWaitMeasureEnabled) {
            validateChildViews()
            mIsRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL

            // Start with the default measured dimension
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            mParentWidth = measuredWidth

            // Measure the hour labels using two passes, this first pass is only to figure out the
            // heights
            val hourLabelStart = if (mIsRtl) paddingRight else paddingLeft
            val hourLabelEnd = hourLabelStart + mHourLabelWidth
            var firstDividerTop = 0
            var lastDividerMarginBottom = 0
            val hourLabelViewsSize = mHourLabelViews.size
            for (i in 0 until hourLabelViewsSize) {
                val view = mHourLabelViews[i]
                measureChild(view, widthMeasureSpec, heightMeasureSpec)
                if (i == 0) {
                    firstDividerTop = view.measuredHeight / 2
                } else if (i == hourLabelViewsSize - 1) {
                    lastDividerMarginBottom = view.measuredHeight / 2
                }
            }
            // Calculate the measured height
            val usableHeight = (mHourDividerRects.size + mHalfHourDividerRects.size - 1) * mUsableHalfHourHeight
            mMinuteHeight = usableHeight.toFloat() / mMinuteCount
            firstDividerTop += paddingTop
            val verticalPadding = firstDividerTop + lastDividerMarginBottom + paddingBottom + mDividerHeight
            measuredHeight = usableHeight + verticalPadding

            // Calculate the horizontal positions of the dividers
            val dividerStart = hourLabelEnd + mHourLabelMarginEnd
            val dividerEnd = measuredWidth - if (mIsRtl) paddingLeft else paddingRight

            // Set the rects for hour labels, dividers, and events
            setHourLabelRects(hourLabelStart, hourLabelEnd, firstDividerTop)
            setDividerRects(firstDividerTop, dividerStart, dividerEnd)
            setEventRects(firstDividerTop, mMinuteHeight, dividerStart, dividerEnd)

            // Measure the hour labels and events for a final time
            measureHourLabels()
            measureEvents()
        }
        setMeasuredDimension(widthMeasureSpec, measuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (i in mHourLabelViews.indices) {
            val view = mHourLabelViews[i]
            val rect = mHourLabelRects[i]
            view.layout(rect.left, rect.top, rect.right, rect.bottom)
        }

        for (i in mFilteredEventViews.indices) {
            val view = mFilteredEventViews[i]
            val rect = mEventRects[i]
            view.layout(rect.left, rect.top, rect.right, rect.bottom)
        }
    }

    override fun shouldDelayChildPressedState(): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the hour and half-hour divider lines directly onto the canvas
        for (rect in mHourDividerRects) {
            canvas.drawRect(
                rect.left.toFloat(),
                rect.top.toFloat(),
                rect.right.toFloat(),
                rect.bottom.toFloat(),
                mHourDividerPaint
            )
        }
        for (rect in mHalfHourDividerRects) {
            canvas.drawRect(
                rect.left.toFloat(),
                rect.top.toFloat(),
                rect.right.toFloat(),
                rect.bottom.toFloat(),
                mHalfHourDividerPaint
            )
        }
    }

    private fun calculateCounts() {
        mStartMinute = startHour * MINUTES_PER_HOUR
        mEndMinute = endHour * MINUTES_PER_HOUR
        val hourCount = endHour - startHour
        mMinuteCount = hourCount * MINUTES_PER_HOUR

        // The hour labels and dividers count here is one more than the hours count so we can
        // include the start of the midnight hour of the next day, setHourLabelViews() expects
        // exactly this many labels
        mHourLabelsCount = hourCount + 1
        mHourDividersCount = hourCount + 1
        mHalfHourDividersCount = hourCount
    }

    /**
     * Creates dividers and add them to the related list
     */
    private fun createDividerRects() {
        mHourDividerRects.clear()
        mHalfHourDividerRects.clear()
        mHourLabelRects.clear()

        for (i in 0 until mHourDividersCount) {
            mHourDividerRects.add(DirectionalRect())
        }

        for (i in 0 until mHalfHourDividersCount) {
            mHalfHourDividerRects.add(DirectionalRect())
        }

        for (i in 0 until mHourLabelsCount) {
            mHourLabelRects.add(DirectionalRect())
        }
    }

    private fun measureHourLabels() {
        for (i in mHourLabelViews.indices) {
            measureExactly(mHourLabelViews[i], mHourLabelRects[i])
        }
    }

    private fun measureEvents() {
        for (i in mFilteredEventViews.indices) {
            measureExactly(mFilteredEventViews[i], mEventRects[i])
        }
    }

    /**
     * Sets the dimensions of a rect while factoring in whether or not right-to-left mode is on.
     *
     * @param rect   the rect to update
     * @param start  the start of the rect in left-to-right mode
     * @param top    the top of the rect, it will not be translated
     * @param end    the end of the rect in left-to-right mode
     * @param bottom the bottom of the rect, it will not be translated
     */
    private fun setRect(rect: DirectionalRect, start: Int, top: Int, end: Int, bottom: Int) {
        rect[mIsRtl, mParentWidth, start, top, end] = bottom
    }

    private fun measureExactly(view: View, rect: DirectionalRect) {
        view.measure(
            MeasureSpec.makeMeasureSpec(rect.right - rect.left, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(rect.bottom - rect.top, MeasureSpec.EXACTLY)
        )
    }

    private fun setHourRange(start: Int, end: Int) {
        startHour = start
        endHour = end
    }

    /**
     * Creates day view with limiting start and end hour by giving params.
     *
     * @param start Start hour of the day. Default is 0
     * @param end End hour of the day. Default is 24
     */
    fun createDayView(start: Int = startHour, end: Int = endHour) {
        // Enable measuring
        mIsWaitMeasureEnabled = false
        // Set hour range
        setHourRange(start, end)
        // Calculates necessary item's counts
        calculateCounts()
        // Creates divider rects
        createDividerRects()
        // Enables drawing
        setWillNotDraw(false)
        // Create layout again
        requestLayout()
        // Set isCreated as true
        isCreated = true
    }

    /**
     * @param hourLabelViews the list of views to show as labels for each hour, this list must not
     * be null and its length must be [.hourLabelsCount]
     */
    fun setHourLabelViews(hourLabelViews: List<View>) {
        for (view in this.mHourLabelViews) {
            removeView(view)
        }
        this.mHourLabelViews.clear()
        this.mHourLabelViews.addAll(hourLabelViews)
        for (view in this.mHourLabelViews) {
            addView(view)
        }
    }

    /**
     * @param eventViews      the list of event views to display
     * @param eventTimeRanges the list of event params that describe each event view's start/end
     * times, this list must be equal in length to the list of event views,
     * or both should be null
     */
    fun setEventViews(eventViews: List<View>?, eventTimeRanges: List<EventTimeRange>?) {
        for (view in mFilteredEventViews) {
            removeView(view)
        }
        this.mEventViews.clear()
        mFilteredEventViews.clear()
        mFilteredEventTimeRanges.clear()
        mEventRects.clear()
        mEventColumnSpansHelper = null
        if (eventViews != null && eventTimeRanges != null) {
            this.mEventViews.addAll(eventViews)
            for (i in eventTimeRanges.indices) {
                val eventTimeRange = eventTimeRanges[i]
                if (eventTimeRange.endMinute > mStartMinute && eventTimeRange.startMinute < mEndMinute) {
                    mFilteredEventViews.add(this.mEventViews[i])
                    mFilteredEventTimeRanges.add(eventTimeRange)
                }
            }
        }
        if (mFilteredEventViews.isNotEmpty() && mFilteredEventTimeRanges.isNotEmpty()) {
            mEventColumnSpansHelper = EventColumnSpansHelper(mFilteredEventTimeRanges)
            for (view in mFilteredEventViews) {
                addView(view)
                mEventRects.add(DirectionalRect())
            }
        }
    }

    /**
     * Removes all of the existing event views.
     *
     * @return the event views that have been removed, they are safe to recycle and reuse at this
     * point
     */
    fun removeEventViews(): List<View> {
        val eventViews: List<View> = mEventViews
        setEventViews(null, null)
        return eventViews
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the top of the given hour.
     *
     * @param hour the hour of the day, should be between 0 (12:00 AM of the current day) and 24
     * (12:00 AM of the next day)
     * @return the vertical offset of the top of the given hour in pixels
     */
    fun getHourTop(hour: Int): Int {
        check(!(hour < 0 || hour >= (mHourLabelsCount + startHour))) { "Hour must be between 0 and $mHourLabelsCount" }
        return mHourDividerRects[hour].bottom
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the bottom of the given hour.
     *
     * @param hour the hour of the day, should be between 0 (12:00 AM of the current day) and 24
     * (12:00 AM of the next day)
     * @return the vertical offset of the bottom of the given hour in pixels
     */
    fun getHourBottom(hour: Int): Int {
        check((hour < 0 || hour >= (mHourLabelsCount + startHour)).not()) { "Hour must be between 0 and $mHourLabelsCount" }
        return if (hour == mHourLabelsCount - 1) {
            mHourDividerRects[hour].bottom
        } else mHourDividerRects[hour + 1].top
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the top of the first event.
     *
     * @return the vertical offset of the top of the first event in pixels, or zero if there are no
     * events
     */
    fun getFirstEventTop(): Int {
        return if (mEventRects.isNotEmpty()) mEventRects[0].top else 0
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the bottom of the first event.
     *
     * @return the vertical offset of the bottom of the first event in pixels, or zero if there are
     * no events
     */
    fun getFirstEventBottom(): Int {
        return if (mEventRects.isNotEmpty()) mEventRects[0].bottom else 0
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the top of the last event.
     *
     * @return the vertical offset of the top of the last event in pixels, or zero if there are no
     * events
     */
    fun getLastEventTop(): Int {
        return if (mEventRects.isNotEmpty()) mEventRects[mEventRects.size - 1].top else 0
    }

    /**
     * Useful if this view is hosted in a scroll view, the y coordinate returned can be used to
     * scroll to the bottom of the last event.
     *
     * @return the vertical offset of the bottom of the last event in pixels, or zero if there are
     * no events
     */
    fun getLastEventBottom(): Int {
        return if (mEventRects.isNotEmpty()) mEventRects[mEventRects.size - 1].bottom else 0
    }

    @VisibleForTesting
    fun createTestDayView() {
        calculateCounts()
        createDividerRects()
    }

    @VisibleForTesting
    fun setHourLabelRects(hourLabelStart: Int, hourLabelEnd: Int, firstDividerTop: Int) {
        for (i in mHourLabelViews.indices) {
            val view = mHourLabelViews[i]
            val height = view.measuredHeight
            val top = firstDividerTop + mUsableHalfHourHeight * i * 2 - height / 2
            val bottom = top + height
            setRect(mHourLabelRects[i], hourLabelStart, top, hourLabelEnd, bottom)
        }
    }

    @VisibleForTesting
    fun setDividerRects(firstDividerTop: Int, dividerStart: Int, dividerEnd: Int) {
        for (i in mHourDividerRects.indices) {
            val top = firstDividerTop + i * 2 * mUsableHalfHourHeight
            val bottom = top + mDividerHeight
            setRect(mHourDividerRects[i], dividerStart, top, dividerEnd, bottom)
        }
        for (i in mHalfHourDividerRects.indices) {
            val top = firstDividerTop + (i * 2 + 1) * mUsableHalfHourHeight
            val bottom = top + mDividerHeight
            setRect(mHalfHourDividerRects[i], dividerStart, top, dividerEnd, bottom)
        }
    }

    @VisibleForTesting
    fun setEventRects(firstDividerTop: Int, minuteHeight: Float, dividerStart: Int, dividerEnd: Int) {
        if (mEventColumnSpansHelper == null) {
            return
        }
        val eventColumnWidth = if (mEventColumnSpansHelper!!.columnCount > 0) (dividerEnd - dividerStart) / mEventColumnSpansHelper!!.columnCount else 0
        for (i in mFilteredEventViews.indices) {
            val timeRange: EventTimeRange = mFilteredEventTimeRanges[i]
            val columnSpan: EventColumnSpan = mEventColumnSpansHelper!!.columnSpans[i]
            var filteredStartMinute = max(mStartMinute, timeRange.startMinute)
            var duration = min(mEndMinute, timeRange.endMinute) - filteredStartMinute
            if (duration < MIN_DURATION_MINUTES) {
                duration = MIN_DURATION_MINUTES
                filteredStartMinute = mEndMinute - duration
            }
            val start = columnSpan.startColumn * eventColumnWidth + dividerStart + mEventMargin
            val end = start + (columnSpan.endColumn - columnSpan.startColumn) * eventColumnWidth - mEventMargin * 2
            val topOffset = ((filteredStartMinute - mStartMinute) * minuteHeight).toInt()
            val top = firstDividerTop + topOffset + mDividerHeight + mEventMargin
            val bottom = top + (duration * minuteHeight).toInt() - mEventMargin * 2 - mDividerHeight
            setRect(mEventRects[i], start, top, end, bottom)
        }
    }

    @VisibleForTesting
    fun setParentWidth(parentWidth: Int) {
        this.mParentWidth = parentWidth
    }

    /**
     * Validates the state of the child views during [.onMeasure].
     *
     * @throws IllegalStateException thrown when one or more of the child views are not in a valid
     * state
     */
    @CallSuper
    @Throws(IllegalStateException::class)
    protected fun validateChildViews() {
        check(mHourLabelViews.size != 0) { "No hour label views, setHourLabelViews() must be called before this view is rendered" }
        check(mHourLabelViews.size == mHourLabelsCount) { "Inconsistent number of hour label views, there should be " + mHourLabelsCount + " but " + mHourLabelViews.size + " were found" }
        check(mFilteredEventViews.size == mFilteredEventTimeRanges.size) { "Inconsistent number of event views or event time ranges, they should either be equal in length or both should be null" }
    }

    /**
     * Represents the start and end time of a calendar event. Both times are in minutes since the
     * start of the day.
     */
    class EventTimeRange(val startMinute: Int, val endMinute: Int) {

        /**
         * @param range the time range to compare
         * @return true if the time range to compare overlaps in any way with this time range
         */
        @VisibleForTesting
        fun conflicts(range: EventTimeRange): Boolean {
            return startMinute >= range.startMinute && startMinute < range.endMinute || endMinute > range.startMinute && endMinute <= range.endMinute || range.startMinute >= startMinute && range.startMinute < endMinute || range.endMinute > startMinute && range.endMinute <= endMinute
        }

    }

    /**
     * Represents the start and end columns a calendar event should span between.
     */
    @VisibleForTesting
    class EventColumnSpan {
        var startColumn = -1
        var endColumn = -1
    }

    /**
     * Helps calculate the start and end columns for a collection of calendar events.
     */
    @VisibleForTesting
    class EventColumnSpansHelper @VisibleForTesting constructor(private val timeRanges: List<EventTimeRange>) {
        val columnSpans: MutableList<EventColumnSpan>
        var columnCount = 0
        private fun findStartColumn(position: Int) {
            for (i in timeRanges.indices) {
                if (isColumnEmpty(i, position)) {
                    val columnSpan = EventColumnSpan()
                    columnSpan.startColumn = i
                    columnSpan.endColumn = i + 1
                    columnSpans.add(columnSpan)
                    columnCount = Math.max(columnCount, i + 1)
                    break
                }
            }
        }

        private fun findEndColumn(position: Int) {
            val columnSpan = columnSpans[position]
            for (i in columnSpan.endColumn until columnCount) {
                if (!isColumnEmpty(i, position)) {
                    break
                }
                columnSpan.endColumn++
            }
        }

        private fun isColumnEmpty(column: Int, position: Int): Boolean {
            val timeRange = timeRanges[position]
            for (i in columnSpans.indices) {
                if (position == i) {
                    continue
                }
                val compareTimeRange = timeRanges[i]
                val compareColumnSpan = columnSpans[i]
                if (compareColumnSpan.startColumn == column && compareTimeRange.conflicts(timeRange)) {
                    return false
                }
            }
            return true
        }

        init {
            columnSpans = java.util.ArrayList(timeRanges.size)

            // Find the start and end columns for each event
            for (i in timeRanges.indices) {
                findStartColumn(i)
            }
            for (i in timeRanges.indices) {
                findEndColumn(i)
            }
        }
    }
}