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
    private var mBackground: View
    private var mForeground: ViewGroup
    private val mTouchSlop: Int

    private var prevTouchY: Float = 0f
    private var velocityTracker: VelocityTracker? = null
    private var hide: Boolean = false
    private var border: Int = 0
    private var travellingView: View? = null

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
                    false
                }
                else ->
                    false
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            velocityTracker?.addMovement(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (mIsDraggable) {
                        val dY = event.y - prevTouchY
                        if (dY > 0)
                            mForeground.translationY = dY
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
                            animateWithFling(velocity)
                        } else {
                            springAnimation()
                        }
                        velocityTracker?.clear()
                    }
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
            setMaxValue(mForeground.height + 100f)
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
        //Log.i("SIZE", "mForeground : ${mForeground.height}")
        //Log.i("POS", "${mForeground.y - mMarginTop} vs ${mForeground.height/2}")
        if (mForeground.y - border <= mForeground.height / 2) {
            animateWithSpring(0f)
        } else {
            if (mIsHidable)
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
}
