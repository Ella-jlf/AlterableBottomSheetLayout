package com.insspring.alterablebottomsheet

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.abs

class AlterableBottomSheetLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    @Suppress("PrivatePropertyName")
    private val BACKGROUND = 0x80000000.toInt()

    @Suppress("PrivatePropertyName")
    private val FOREGROUND = 0xFFFFFFFF.toInt()

    private var mIsHidable: Boolean
    private var mForegroundBackground: Int
    private var mMarginTop: Int
    private var mBackgroundColor: Int
    private var mForegroundColor: Int
    private var mHideOnOutClick: Boolean
    private var mBackgroundTransparency: Float
    private var mHeadLayout: Int
    private var mForegroundHeight: Int
    private var mIsDraggable: Boolean
    private var mType: Int
    private var mIntermediateHeight: Int

    private var mBackground: View
    private var mForeground: ViewGroup
    private val mTouchSlop: Int

    private var prevTouchY: Float = 0f
    private var velocityTracker: VelocityTracker? = null
    private var border: Int = 0
    private var hide = false
    private var travellingView: View? = null
    private var curTranslation: Float = 0f

    init {
        this.translationZ = 10f
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AlterableBottomSheetLayout,
            0,
            0
        ).apply {
            // getting all attrs
            mForegroundBackground =
                getResourceId(R.styleable.AlterableBottomSheetLayout_foreground,
                    R.drawable.bg_round_corners)
            mMarginTop =
                getDimensionPixelSize(R.styleable.AlterableBottomSheetLayout_margin_top, 0)
            mBackgroundColor =
                getColor(
                    R.styleable.AlterableBottomSheetLayout_background_color, BACKGROUND
                )
            mForegroundColor =
                getColor(
                    R.styleable.AlterableBottomSheetLayout_foreground_color, FOREGROUND
                )
            mIsDraggable = getBoolean(R.styleable.AlterableBottomSheetLayout_isDraggable, true)
            mHideOnOutClick =
                getBoolean(R.styleable.AlterableBottomSheetLayout_hide_on_background_click, true)
            mIsHidable = getBoolean(R.styleable.AlterableBottomSheetLayout_isHidable, true)
            mHeadLayout = getInt(R.styleable.AlterableBottomSheetLayout_head_layout, 0)
            mForegroundHeight =
                getLayoutDimension(R.styleable.AlterableBottomSheetLayout_foreground_height,
                    -1)
            mType = getInt(R.styleable.AlterableBottomSheetLayout_foreground_type, 0)
            mIntermediateHeight =
                getDimensionPixelSize(R.styleable.AlterableBottomSheetLayout_intermediate_height,
                    300)
            mBackgroundTransparency =
                1f - getFloat(R.styleable.AlterableBottomSheetLayout_transparency_percent, 0f)
            // adding background
            mBackground = View(context).apply {
                alpha = mBackgroundTransparency
                setBackgroundColor(mBackgroundColor)
            }
            addView(mBackground, 0)
            //creating certain type of layout
            when (mHeadLayout) {
                1 -> {
                    mForeground = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setBackgroundResource(mForegroundBackground)
                        layoutParams =
                            LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                mForegroundHeight,
                                Gravity.BOTTOM
                            ).apply {
                                setMargins(0, mMarginTop, 0, 0)
                            }
                    }
                }
                2 -> {
                    mForeground = RelativeLayout(context).apply {
                        setBackgroundResource(mForegroundBackground)
                        layoutParams =
                            LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                mForegroundHeight,
                                Gravity.BOTTOM
                            ).apply {
                                setMargins(0, mMarginTop, 0, 0)
                            }
                    }
                }
                else -> {
                    mForeground = FrameLayout(context).apply {
                        setBackgroundResource(mForegroundBackground)
                        layoutParams =
                            LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                mForegroundHeight,
                                Gravity.BOTTOM
                            ).apply {
                                setMargins(0, mMarginTop, 0, 0)
                            }
                    }
                }
            }
            addView(mForeground, 1)
        }
        typedArray.recycle()
    }

    // for the first two children(background and foreground) the same as for frameLayout
    //others are tossing over to foreground, and foreground is responsible for there measure and layout
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutChildren(l, t, r, b, false)
        for (i in 2 until childCount) {
            travellingView = getChildAt(i)
            removeView(travellingView)
            travellingView?.let { view ->
                mForeground.addView(view)
            }
        }
        border = mForeground.top
        //Log.i("SIZE", "$border")
    }

    //intercepted only background click, and move if nobody of children can process
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            return when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(ev)
                    curTranslation = mForeground.translationY
                    prevTouchY = ev.y
                    if (ev.y < mForeground.y) {
                        hide = true
                        true
                    } else {
                        hide = false
                        false
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(ev)
                    if (abs(ev.y - prevTouchY) > mTouchSlop) {
                        //let children intercept it
                        !isChildScrolling(ev.rawX,
                            ev.rawY,
                            this,
                            getDirection(ev.y, prevTouchY))
                    } else
                        false
                }
                else ->
                    false
            }
        }
        return false
    }

    // direction -1 - up, 1 - down
    private fun getDirection(cur: Float, prev: Float): Int {
        return if (cur > prev)
            -1
        else
            1
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            velocityTracker?.addMovement(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (mIsDraggable) {
                        // drag view with finger
                        val dY = event.y - prevTouchY
                        when (mType) {
                            0, 2 -> {
                                // conditions not to get out of the acceptable bounds
                                if (dY + curTranslation >= 0 && dY + curTranslation <= mForeground.height)
                                    mForeground.translationY = dY + curTranslation
                            }
                            1 -> {
                                // conditions not to get out of the acceptable bounds
                                if (dY + curTranslation >= 0 && dY + curTranslation <= mForeground.height - mIntermediateHeight)
                                    mForeground.translationY = dY + curTranslation
                            }
                        }
                        //Log.i("VALS", "\ncurrent: ${event.y} \n previous: $prevTouchY \n translation: $dY")
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    // that event come only if background was pressed
                    if (hide && mHideOnOutClick && mIsHidable && mType != 1)
                        hide()
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> {
                    if (!hide && mIsDraggable) {
                        velocityTracker?.computeCurrentVelocity(1000)
                        val velocity = velocityTracker?.yVelocity ?: 0f
                        //check if we fling view, it has to fly based on it's velocity
                        if (abs(velocity) > 1000) {
                            when (mType) {
                                0 -> finalAnimationWithFling0(velocity)
                                1 -> finalAnimationWithFling1(velocity)
                                2 -> finalAnimationWithFling2(velocity)
                            }
                        } else {
                            when (mType) {
                                0 -> finalAnimationWithSpring0()
                                1 -> finalAnimationWithSpring1()
                                2 -> finalAnimationWithSpring2()
                            }
                        }
                        velocityTracker?.clear()
                    }
                }
            }
        }
        return true
    }

    //velocity based animations
    private fun finalAnimationWithFling0(velocity: Float) {
        animateWithFling(velocity, 0f, mForeground.height.toFloat())
    }

    private fun finalAnimationWithFling1(velocity: Float) {
        animateWithFling(velocity, 0f, mForeground.height - mIntermediateHeight.toFloat())
    }

    private fun finalAnimationWithFling2(velocity: Float) {
        finalAnimationWithFling0(velocity)
    }

    private fun animateWithFling(velocity: Float, min: Float, max: Float) {
        val flingAnimation = FlingAnimation(mForeground, DynamicAnimation.TRANSLATION_Y).apply {
            friction = 1f
            setStartVelocity(velocity)
            setMinValue(min)
            setMaxValue(max)
            addEndListener { _, _, _, _ ->
                when (mType) {
                    0 -> finalAnimationWithSpring0()
                    1 -> finalAnimationWithSpring1()
                    2 -> finalAnimationWithSpring2()
                }
            }
        }
        flingAnimation.start()
    }

    private fun animateWithSpring(finalPos: Float) {
        val springAnimation =
            SpringAnimation(mForeground, DynamicAnimation.TRANSLATION_Y).apply {
                setStartVelocity(2000f)
                spring = SpringForce(finalPos).apply {
                    dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                    stiffness = SpringForce.STIFFNESS_MEDIUM
                }
                addEndListener { _, _, _, _ ->
                    // if was fully scrolled to bottom of screen - disappear
                    if (finalPos == mForeground.height.toFloat()) {
                        this@AlterableBottomSheetLayout.isVisible = false
                    }
                }
            }
        springAnimation.start()
    }

    //final animations to place view in exact spot
    private fun finalAnimationWithSpring0() {
        if (mForeground.y <= mForeground.height / 2 + border) {
            animateWithSpring(0f)
        } else {
            if (mIsHidable)
                animateWithSpring(mForeground.height.toFloat())
            else
                animateWithSpring(0f)
        }
    }

    private fun finalAnimationWithSpring1() {
        if (mForeground.y < (mForeground.height - mIntermediateHeight) / 2 + border)
            animateWithSpring(0f)
        else {
            animateWithSpring(mForeground.height - mIntermediateHeight.toFloat())
        }
    }

    private fun finalAnimationWithSpring2() {
        if (mForeground.y <=
            (mForeground.height - mIntermediateHeight) / 2 + border
        ) {
            animateWithSpring(0f)
        } else {
            if (mForeground.y >= border + mForeground.height - mIntermediateHeight / 2) {
                if (mIsHidable)
                    animateWithSpring(mForeground.height.toFloat())
                else
                    animateWithSpring(mForeground.height - mIntermediateHeight.toFloat())
            } else {
                animateWithSpring(mForeground.height - mIntermediateHeight.toFloat())
            }
        }
    }

    fun show() {
        this.isVisible = true
        animateWithSpring(0f)
    }

    fun hide() {
        animateWithSpring(mForeground.height.toFloat())
    }

    // like frameLayout.onLayout, but for only 2 views
    private fun layoutChildren(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        forceLeftGravity: Boolean,
    ) {
        val count = 2
        val parentLeft = 0
        val parentRight: Int = right - left - 0
        val parentTop = 0
        val parentBottom: Int = bottom - top - 0
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val lp = child.layoutParams as LayoutParams
                val width = child.measuredWidth
                val height = child.measuredHeight
                var childLeft: Int
                var childTop: Int
                var gravity = lp.gravity
                if (gravity == -1) {
                    gravity = Gravity.TOP or Gravity.START
                }
                val layoutDirection = layoutDirection
                val absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection)
                val verticalGravity = gravity and Gravity.VERTICAL_GRAVITY_MASK
                when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                    Gravity.CENTER_HORIZONTAL -> childLeft =
                        parentLeft + (parentRight - parentLeft - width) / 2 +
                                lp.leftMargin - lp.rightMargin
                    Gravity.END -> {
                        if (!forceLeftGravity) {
                            childLeft = parentRight - width - lp.rightMargin
                            break
                        }
                        childLeft = parentLeft + lp.leftMargin
                    }
                    Gravity.START -> childLeft = parentLeft + lp.leftMargin
                    else -> childLeft = parentLeft + lp.leftMargin
                }
                childTop = when (verticalGravity) {
                    Gravity.TOP -> parentTop + lp.topMargin
                    Gravity.CENTER_VERTICAL -> parentTop + (parentBottom - parentTop - height) / 2 +
                            lp.topMargin - lp.bottomMargin
                    Gravity.BOTTOM -> parentBottom - height - lp.bottomMargin
                    else -> parentTop + lp.topMargin
                }
                child.layout(childLeft, childTop, childLeft + width, childTop + height)
            }
        }
    }

    // recursive!, iterate over all children to see if they can scroll
    private fun isChildScrolling(
        eventX: Float,
        eventY: Float,
        viewGroup: ViewGroup,
        direction: Int,
    ): Boolean {
        var view: View
        for (i in 0 until viewGroup.childCount) {
            view = viewGroup.getChildAt(i)
            if (isViewAtLocation(eventX, eventY, view)) {
                if (view.canScrollVertically(-1) && direction == -1)
                    return true
                if (view.canScrollVertically(1) && direction == 1)
                    return true
                if (view is ViewGroup) {
                    if (isChildScrolling(eventX - view.left,
                            eventY - view.top,
                            view,
                            direction)
                    )
                        return true
                }
            }
        }
        return false
    }

    /*
        private fun isChildGrabClick(
            eventX: Float,
            eventY: Float,
            viewGroup: ViewGroup,
        ): Boolean {
            for (i in 0 until viewGroup.childCount) {
                val view = viewGroup.getChildAt(i)
                if (isViewAtLocation(eventX, eventY, view)) {
                    if (view is ViewGroup) {
                        if (isChildGrabClick(eventX - view.left, eventY - view.top, view))
                            return true
                    }
                    if (view.performClick()) {
                        return true
                    }
                }
            }
            return false
        }
    */
    //checking if view under finger
    private fun isViewAtLocation(rawX: Float, rawY: Float, view: View): Boolean {
        if (view.left <= rawX && view.right >= rawX) {
            if (view.top <= rawY && view.bottom >= rawY) {
                return true
            }
        }
        return false
    }
}
