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
import java.lang.Exception
import kotlin.math.abs

enum class ForegroundType {
    WithoutIntermediate,
    WithoutHide,
    Mixed,
}

enum class HeadLayout {
    FRAME_LAYOUT,
    LINEAR_LAYOUT,
    RELATIVE_LAYOUT
}

enum class Direction(val int: Int) {
    UP(-1),
    DOWN(1)
}

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

        /* Getting all attrs */
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AlterableBottomSheetLayout,
            0,
            0
        ).apply {
            mForegroundBackground = getResourceId(
                R.styleable.AlterableBottomSheetLayout_foreground,
                R.drawable.bg_round_corners)
            mMarginTop = getDimensionPixelSize(
                R.styleable.AlterableBottomSheetLayout_margin_top,
                0)
            mBackgroundColor = getColor(
                R.styleable.AlterableBottomSheetLayout_background_color,
                BACKGROUND
            )
            mForegroundColor = getColor(
                R.styleable.AlterableBottomSheetLayout_foreground_color,
                FOREGROUND
            )
            mIsDraggable = getBoolean(
                R.styleable.AlterableBottomSheetLayout_isDraggable,
                true)
            mHideOnOutClick = getBoolean(
                R.styleable.AlterableBottomSheetLayout_hide_on_background_click,
                true)
            mIsHidable = getBoolean(
                R.styleable.AlterableBottomSheetLayout_isHidable,
                true)
            mHeadLayout = getInt(
                R.styleable.AlterableBottomSheetLayout_head_layout,
                0)
            mForegroundHeight = getLayoutDimension(
                R.styleable.AlterableBottomSheetLayout_foreground_height,
                -1)
            mType = getInt(
                R.styleable.AlterableBottomSheetLayout_foreground_type,
                0)
            mIntermediateHeight = getDimensionPixelSize(
                R.styleable.AlterableBottomSheetLayout_intermediate_height,
                300)
            mBackgroundTransparency = 1f - getFloat(
                R.styleable.AlterableBottomSheetLayout_transparency_percent,
                0f)
            this.recycle()
        }

        /* creating background View */
        mBackground = View(context).apply {
            alpha = mBackgroundTransparency
            setBackgroundColor(mBackgroundColor)
        }

        /* creating foreground Layout */
        when (mHeadLayout) {
            HeadLayout.LINEAR_LAYOUT.ordinal -> {
                mForeground = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                }
            }

            HeadLayout.RELATIVE_LAYOUT.ordinal -> {
                mForeground = RelativeLayout(context)
            }

            HeadLayout.FRAME_LAYOUT.ordinal -> {
                mForeground = FrameLayout(context)
            }

            else -> {
                throw Exception("non-existing head layout")
            }
        }
        mForeground.apply {
            setBackgroundResource(mForegroundBackground)
            layoutParams = createLayoutParams()
        }

        addView(mBackground, 0)
        addView(mForeground, 1)
    }

    fun show() {
        this.isVisible = true
        animateWithSpring(0f)
    }

    fun hide() {
        animateWithSpring(mForeground.height.toFloat())
    }

    /*
     * for the first two children(background and foreground) the same as for frameLayout
     * others are tossing over to foreground, and foreground is responsible for there measure and layout
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutChildren(l, t, r, b, false)

        tossOverChildViews()

        border = mForeground.top
    }

    /*
     *intercepted only background click, and move if nobody of children can process
     */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null)
            return false

        return when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(ev)

                curTranslation = mForeground.translationY
                prevTouchY = ev.y

                hide = ev.y < mForeground.y

                val interceptClick = ev.y < mForeground.y
                interceptClick
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(ev)

                val interceptScroll = (abs(ev.y - prevTouchY) > mTouchSlop
                        && !isChildScrolling(ev.rawX,
                    ev.rawY,
                    this,
                    getDirection(ev.y, prevTouchY)))
                interceptScroll
            }

            else ->
                false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null)
            return false

        velocityTracker?.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (mIsDraggable) {
                    val dY = event.y - prevTouchY // draggable distance

                    if (checkAcceptableBounds(dY))
                        dragViewAlongWithFinger(dY)
                }
            }

            MotionEvent.ACTION_DOWN -> {
                /* that event come only if background was pressed */
                if (hide && mHideOnOutClick && mIsHidable && mType != ForegroundType.WithoutHide.ordinal)
                    hide()
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                if (!hide && mIsDraggable) {
                    velocityTracker?.computeCurrentVelocity(1000)

                    val velocity = velocityTracker?.yVelocity ?: 0f

                    if (abs(velocity) > 1000) {
                        finalAnimationWithFling(velocity)
                    } else {
                        finalAnimationWithSpring()
                    }

                    velocityTracker?.clear()
                }
            }
        }

        return true
    }

    /*
     * velocity based animation
     */
    private fun finalAnimationWithFling(velocity: Float) {
        when (mType) {

            ForegroundType.WithoutIntermediate.ordinal, ForegroundType.Mixed.ordinal -> {
                animateWithFling(velocity, 0f, mForeground.height.toFloat())
            }

            ForegroundType.WithoutHide.ordinal -> {
                animateWithFling(velocity, 0f, mForeground.height - mIntermediateHeight.toFloat())
            }
        }
    }

    /*
     * final animations to place view in exact spot
     */
    private fun finalAnimationWithSpring() {
        when (mType) {
            ForegroundType.WithoutIntermediate.ordinal -> {
                val center = mForeground.height / 2 + border

                if (mForeground.y <= center && !mIsHidable)
                    animateWithSpring(0f)
                else
                    animateWithSpring(mForeground.height.toFloat())
            }

            ForegroundType.WithoutHide.ordinal -> {
                val center = (mForeground.height - mIntermediateHeight) / 2 + border

                if (mForeground.y < center)
                    animateWithSpring(0f)
                else
                    animateWithSpring(mForeground.height - mIntermediateHeight.toFloat())
            }

            ForegroundType.Mixed.ordinal -> {
                val oneThirdTop = (mForeground.height - mIntermediateHeight) / 2 + border

                if (mForeground.y <= oneThirdTop) {
                    animateWithSpring(0f)
                    return
                }

                val oneThirdBottom = border + mForeground.height - mIntermediateHeight / 2

                if (mForeground.y >= oneThirdBottom && mIsHidable)
                    animateWithSpring(mForeground.height.toFloat())
                else
                    animateWithSpring(mForeground.height - mIntermediateHeight.toFloat())
            }

            else -> {
                throw Exception("non-existing foreground type")
            }
        }
    }

    private fun checkAcceptableBounds(dY: Float): Boolean {

        return when (mType) {
            ForegroundType.WithoutIntermediate.ordinal,
            ForegroundType.Mixed.ordinal,
            -> {
                dY + curTranslation >= 0 && dY + curTranslation <= mForeground.height
            }

            ForegroundType.WithoutHide.ordinal,
            -> {
                dY + curTranslation >= 0 && dY + curTranslation <= mForeground.height - mIntermediateHeight
            }

            else -> {
                throw Exception("non-existing foreground type")
            }
        }
    }

    private fun animateWithFling(velocity: Float, min: Float, max: Float) {
        val flingAnimation = FlingAnimation(mForeground, DynamicAnimation.TRANSLATION_Y)
            .apply {
                friction = 1f

                setStartVelocity(velocity)
                setMinValue(min)
                setMaxValue(max)

                addEndListener { _, _, _, _ ->
                    finalAnimationWithSpring()
                }
            }

        flingAnimation.start()
    }

    private fun animateWithSpring(finalPos: Float) {
        val springAnimation = SpringAnimation(mForeground, DynamicAnimation.TRANSLATION_Y)
            .apply {

                setStartVelocity(2000f)

                spring = SpringForce(finalPos)
                    .apply {
                        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                        stiffness = SpringForce.STIFFNESS_MEDIUM
                    }

                /* if was fully scrolled to bottom of screen - disappear */
                addEndListener { _, _, _, _ ->
                    if (finalPos == mForeground.height.toFloat())
                        this@AlterableBottomSheetLayout.isVisible = false
                }
            }

        springAnimation.start()
    }

    /*
     * like frameLayout.onLayout, but for only 2 views
     */
    private fun layoutChildren(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        forceLeftGravity: Boolean,
    ) {
        val parentLeft = 0
        val parentRight: Int = right - left - 0
        val parentTop = 0
        val parentBottom: Int = bottom - top - 0

        val count = 2

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

    /*
    * recursive!, iterate over all children to see if they can scroll
    */
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
                if (view.canScrollVertically(Direction.UP.int) && direction == Direction.UP.int)
                    return true

                if (view.canScrollVertically(Direction.DOWN.int) && direction == Direction.DOWN.int)
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
     * checking if view under finger
     */
    private fun isViewAtLocation(rawX: Float, rawY: Float, view: View): Boolean {
        if (view.left <= rawX && view.right >= rawX) {
            if (view.top <= rawY && view.bottom >= rawY) {
                return true
            }
        }

        return false
    }

    private fun createLayoutParams(): LayoutParams {

        return LayoutParams(
            LayoutParams.MATCH_PARENT,
            mForegroundHeight,
            Gravity.BOTTOM
        ).apply {
            setMargins(0, mMarginTop, 0, 0)
        }
    }

    private fun tossOverChildViews() {
        for (i in 2 until childCount) {
            travellingView = getChildAt(i)

            travellingView?.let { view ->
                removeView(view)
                mForeground.addView(view)
            }
        }
    }

    /*
     * direction -1 - up, 1 - down
     */
    private fun getDirection(cur: Float, prev: Float): Int {

        return if (cur > prev)
            Direction.UP.int
        else
            Direction.DOWN.int
    }

    private fun dragViewAlongWithFinger(dY: Float) {
        mForeground.translationY = dY + curTranslation
    }
}
