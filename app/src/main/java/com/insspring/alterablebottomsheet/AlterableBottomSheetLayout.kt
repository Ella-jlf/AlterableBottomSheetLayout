package com.insspring.alterablebottomsheet

import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.res.getResourceIdOrThrow
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

    private var isHidable: Boolean
    private var mForegroundBackground: Int
    private var mMarginTop: Int
    private var mBackgroundColor: Int
    private var mForegroundColor: Int
    private var mHideOnOutClick: Boolean
    private var mBackgroundTransparency: Float
    private var mHeadLayout: Int
    private var mScrollableSpan: Float
    private var mAllowLimitedArea: Boolean

    private var mBackground: View
    private var mForeground: ViewGroup
    private val touchSlop: Int

    private var prevTouchY: Float = 0f
    private var velocityTracker: VelocityTracker? = null
    private var disableScrolling: Boolean = false
    private var hide: Boolean = false

    private var travellingView: View? = null

    init {
        this.translationZ = 10f
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AlterableBottomSheetLayout,
            0,
            0
        ).apply {
            // getting all attrs
            mForegroundBackground =
                getResourceIdOrThrow(R.styleable.AlterableBottomSheetLayout_foreground)
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
            mAllowLimitedArea =
                getBoolean(R.styleable.AlterableBottomSheetLayout_allow_limited_area_span, false)
            disableScrolling = mAllowLimitedArea
            mHideOnOutClick =
                getBoolean(R.styleable.AlterableBottomSheetLayout_hide_on_background_click, true)
            isHidable = getBoolean(R.styleable.AlterableBottomSheetLayout_isHidable, true)
            mScrollableSpan =
                getDimension(R.styleable.AlterableBottomSheetLayout_limited_area_span, 200f)
            mHeadLayout = getInt(R.styleable.AlterableBottomSheetLayout_head_layout, 0)
            mBackgroundTransparency =
                1f - getFloat(R.styleable.AlterableBottomSheetLayout_transparency_percent, 0f)
            // adding background
            mBackground = View(context).apply {
                alpha = mBackgroundTransparency
                setBackgroundColor(mBackgroundColor)
            }
            addView(mBackground, 0)
            when (mHeadLayout) {
                0 -> {
                    mForeground = FrameLayout(context)
                }
                1 -> {
                    mForeground = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                    }
                }
                2 -> {
                    mForeground = RelativeLayout(context)
                }
                else -> {
                    mForeground = FrameLayout(context)
                }
            }
            mForeground.apply {
                setBackgroundResource(mForegroundBackground)
                layoutParams =
                    LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT,
                        Gravity.BOTTOM
                    ).apply {
                        setMargins(0, mMarginTop, 0, 0)
                    }
            }
            addView(mForeground, 1)
        }
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
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
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        super.onInterceptTouchEvent(ev)
        if (ev != null) {
            return when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(ev)
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    //velocityTracker?.addMovement(ev)
                    abs(ev.y - prevTouchY) > touchSlop
                }
                else ->
                    false
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            velocityTracker?.addMovement(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (!(mAllowLimitedArea && disableScrolling)) {
                        val dY = event.y - prevTouchY
                        if (dY > 0)
                            mForeground.translationY = dY
                        //Log.i("VALS", "\ncurrent: ${event.y} \n previous: $prevTouchY \n translation: $dY")
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    if (event.y < mMarginTop) {
                        //Log.i("BOTTOMSHEET", "Out area")
                        hide = true
                        if (isHidable && mHideOnOutClick)
                            hide()
                    } else {
                        //Log.i("BOTTOMSHEET", "Scroll area")
                        hide = false
                        prevTouchY = event.y
                        if (mAllowLimitedArea && inMovableArea(event.y)) {
                            disableScrolling = false
                        }
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> {
                    if (!hide && !disableScrolling) {
                        velocityTracker?.computeCurrentVelocity(1000)
                        val velocity = velocityTracker?.yVelocity ?: 0f
                        if (abs(velocity) > 1000) {
                            animateWithFling(velocity)
                        } else {
                            springAnimation()
                        }
                        velocityTracker?.clear()
                    }
                    if (mAllowLimitedArea)
                        disableScrolling = true
                }
            }
        }
        return true
    }

    private fun animateWithFling(velocity: Float) {
        val flingAnimation = FlingAnimation(mForeground, DynamicAnimation.TRANSLATION_Y).apply {
            friction = 1f
            setStartVelocity(velocity)
            setMinValue(0f)
            setMaxValue(mForeground.height.toFloat())
            addEndListener { _, _, _, _ ->
                springAnimation()
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

    private fun springAnimation() {
        //Log.i("POS", "${mForeground.y - mMarginTop} vs ${mForeground.height/2}")
        if (mForeground.y - mMarginTop <= mForeground.height / 2) {
            animateWithSpring(0f)
        } else {
            if (isHidable)
                animateWithSpring(mForeground.height.toFloat())
            else
                animateWithSpring(0f)
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

    private fun inMovableArea(posY: Float): Boolean {
        // Log.i("POS", "$posY > $mMarginTop and $posY < ${mMarginTop + 150f}")
        return posY > mMarginTop && posY < mMarginTop + mScrollableSpan
    }
}
