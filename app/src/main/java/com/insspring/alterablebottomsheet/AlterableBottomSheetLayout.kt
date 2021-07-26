package com.insspring.alterablebottomsheet

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
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
    private var hide: Boolean = false
    private var border: Int = 0
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

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            return when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(ev)
                    curTranslation = mForeground.translationY
                    if (ev.y < border) {
                        //Log.i("BOTTOMSHEET", "Out area")
                        hide = true
                        true
                    } else {
                        //Log.i("BOTTOMSHEET", "Scroll area")
                        hide = false
                        prevTouchY = ev.y
                        false
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(ev)
                    //abs(ev.y - prevTouchY) > touchSlop
                    if (abs(ev.y - prevTouchY) > mTouchSlop) {
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
                        val dY = event.y - prevTouchY
                        when (mType) {
                            0, 2 -> {
                                if (dY + curTranslation >= 0 && dY + curTranslation <= mForeground.height)
                                    mForeground.translationY = dY + curTranslation
                            }
                            1 -> {
                                if (dY + curTranslation >= 0 && dY + curTranslation <= mForeground.height - mIntermediateHeight)
                                    mForeground.translationY = dY + curTranslation
                            }
                        }
                        //Log.i("VALS", "\ncurrent: ${event.y} \n previous: $prevTouchY \n translation: $dY")
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    if (mIsHidable && mHideOnOutClick && hide)
                        hide()
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> {
                    if (!hide && mIsDraggable) {
                        velocityTracker?.computeCurrentVelocity(1000)
                        val velocity = velocityTracker?.yVelocity ?: 0f
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
                    if (finalPos == mForeground.height.toFloat()) {
                        this@AlterableBottomSheetLayout.isVisible = false
                    }
                }
            }
        springAnimation.start()
    }

    private fun finalAnimationWithSpring0() {
        //Log.i("SIZE", "mForeground : ${mForeground.height}")
        //Log.i("POS", "${mForeground.y - mMarginTop} vs ${mForeground.height/2}")
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

    private fun isChildScrolling(
        eventX: Float,
        eventY: Float,
        viewGroup: ViewGroup,
        direction: Int,
    ): Boolean {
        var view: View
        //Log.i("SCROLL", "called viewgroup with childCount:${viewGroup.childCount}")
        for (i in 0 until viewGroup.childCount) {
            view = viewGroup.getChildAt(i)
            //Log.i("SCROLL", "Got child at $i")
            if (isViewAtLocation(eventX, eventY, view)) {
/*                if (view is ScrollView || view is ScrollingView || view is NestedScrollView
*//*                    || view is NestedScrollingChild || view is NestedScrollingChild2
                    || view is NestedScrollingChild3 || view is NestedScrollingParent
                    || view is NestedScrollingParent2 || view is NestedScrollingParent3*//*
                )*/
                if (view.canScrollVertically(-1) && direction == -1)
                    return true
                if (view.canScrollVertically(1) && direction == 1)
                    return true
                //Log.i("SCROLL", "${view.tag} child $i is ViewGroup : ${view is ViewGroup}")
                if (view is ViewGroup) {
                    //Log.i("SCROLL", "calling isChildScrolling")
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

    private fun isViewAtLocation(rawX: Float, rawY: Float, view: View): Boolean {
        if (view.left <= rawX && view.right >= rawX) {
            if (view.top <= rawY && view.bottom >= rawY) {
                return true
            }
        }
        /*Log.i("SCROLL",
            "x:$rawX y:$rawY \n left:${view.left} right:${view.right} top:${view.top} bottom:${view.bottom} , under finger : $a")*/
        return false
    }
}
