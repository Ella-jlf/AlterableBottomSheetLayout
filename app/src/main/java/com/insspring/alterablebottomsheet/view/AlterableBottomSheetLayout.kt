package com.insspring.alterablebottomsheet.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.insspring.alterablebottomsheet.R
import kotlin.math.abs
import kotlin.math.min

enum class ForegroundType {
    DefaultType,
    WithoutHide,
    Mixed,
}

enum class Direction(val int: Int) {
    UP(-1),
    DOWN(1)
}

class AlterableBottomSheetLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var mForegroundBackground: Int
    private var mMarginTop: Int
    private var mBackgroundColor: Int
    private var mForegroundHeight: Int
    private var mIsHideOnBgClick: Boolean
    private var mIsDraggable: Boolean
    private var mType: ForegroundType
    private var mIntermediateHeight: Int
    private var mTopCorners: Int

    private var mBackground: Background
    private var mForeground: ViewGroup
    private val mTouchSlop: Int

    private var prevTouchY: Float = 0f
    private var velocityTracker: VelocityTracker? = null
    private var border: Int = 0
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
                0xA0000000.toInt()
            )

            mIsDraggable = getBoolean(
                R.styleable.AlterableBottomSheetLayout_is_draggable,
                true)

            mIsHideOnBgClick = getBoolean(
                R.styleable.AlterableBottomSheetLayout_is_hide_on_bg_click,
                true)

            mForegroundHeight = getLayoutDimension(
                R.styleable.AlterableBottomSheetLayout_foreground_height,
                -1)

            mTopCorners = getDimensionPixelSize(
                R.styleable.AlterableBottomSheetLayout_top_corners,
                64
            )

            val mTypeValue = getInt(
                R.styleable.AlterableBottomSheetLayout_foreground_type,
                0)
            mType = when (mTypeValue) {
                0 -> ForegroundType.DefaultType
                1 -> ForegroundType.WithoutHide
                2 -> ForegroundType.Mixed
                else -> throw Exception("no such value in ForegroundType enum")
            }
            mIntermediateHeight = getDimensionPixelSize(
                R.styleable.AlterableBottomSheetLayout_intermediate_height,
                300)

            this.recycle()
        }

        /* creating background View */
        mBackground = Background(context)
            .apply {
                alpha = 1f
                setBackgroundColor(mBackgroundColor)
            }

        /* creating foreground Layout */
        mForeground = Foreground(context, mTopCorners).apply {
            setBackgroundResource(mForegroundBackground)
            layoutParams = createLayoutParams()
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            clipChildren = true
        }

        addView(mBackground, 0)
        addView(mForeground, 1)

        if (mIsHideOnBgClick)
            mBackground.setOnClickListener {
                hide()
            }
    }

    fun show() {
        this.isVisible = true
        animateWithSpring(0f)
    }

    fun hide() {
        when (mType) {
            ForegroundType.WithoutHide ->
                animateWithSpring(mForeground.height.toFloat() - mIntermediateHeight)

            ForegroundType.Mixed, ForegroundType.DefaultType ->
                animateWithSpring(mForeground.height.toFloat())
        }
    }

    fun setBackground(resId: Int) {
        mForegroundBackground = resId
        mForeground.setBackgroundResource(mForegroundBackground)
    }

    fun setIsDrawable(value: Boolean) {
        mIsDraggable = value
    }

    fun setType(value: ForegroundType) {
        mType = value
    }

    fun setIntermediate(value: Int) {
        mIntermediateHeight = value
    }

    fun setMarginTop(value: Int) {
        mMarginTop = value
        (mForeground.layoutParams as LayoutParams).setMargins(0, mMarginTop, 0, 0)
    }

    fun setHeight(value: Int) {
        mForeground.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, value, Gravity.BOTTOM)
    }

    /*
     * for the first two children(background and foreground) the same as for frameLayout
     * others are tossing over to foreground, and foreground is responsible for there measure and layout
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutChildren(l, t, r, b)

        tossOverChildViews()

        border = mForeground.top
        mBackground.border = border.toFloat()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev)
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

                false
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

                    if (checkAcceptableBounds(dY)) {
                        dragViewAlongWithFinger(dY)
                        calculateAndSetAlpha()
                    }
                }

                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                if (mIsDraggable) {
                    velocityTracker?.computeCurrentVelocity(1000)

                    val velocity = velocityTracker?.yVelocity ?: 0f

                    finalAnimationWithSpring(velocity)

                    velocityTracker?.clear()
                }

                return true
            }
        }
        if (isInterceptClickWithForeground(event.rawX, event.rawY))
            return true
        return false
    }


    /*
     * final animations to place view in exact spot
     */
    private fun finalAnimationWithSpring(velocity: Float = 0f) {
        when (mType) {
            ForegroundType.DefaultType -> {
                if (abs(velocity) > 2000) {
                    animateToRightDirection(velocity, 0f, mForeground.height.toFloat())

                    return
                }
                val center = mForeground.height / 2 + border

                if (mForeground.y > center)
                    animateWithSpring(mForeground.height.toFloat())
                else
                    animateWithSpring(0f)
            }

            ForegroundType.WithoutHide -> {
                if (abs(velocity) > 2000) {
                    animateToRightDirection(velocity,
                        0f,
                        mForeground.height - mIntermediateHeight.toFloat()
                    )

                    return
                }

                val center = (mForeground.height - mIntermediateHeight) / 2 + border

                if (mForeground.y < center)
                    animateWithSpring(0f)
                else
                    animateWithSpring(mForeground.height - mIntermediateHeight.toFloat())
            }

            ForegroundType.Mixed -> {
                if (abs(velocity) > 2000) {
                    val center = (mForeground.height - mIntermediateHeight) / 2 + border

                    if (mForeground.y < center)
                        animateToRightDirection(
                            velocity,
                            0f,
                            mForeground.height.toFloat() - mIntermediateHeight
                        )
                    else
                        animateToRightDirection(
                            velocity,
                            0f,
                            mForeground.height.toFloat())

                    return
                }

                val oneThirdTop = (mForeground.height - mIntermediateHeight) / 2 + border

                if (mForeground.y <= oneThirdTop) {
                    animateWithSpring(0f)
                    return
                }

                val oneThirdBottom = border + mForeground.height - mIntermediateHeight / 2

                if (mForeground.y >= oneThirdBottom)
                    animateWithSpring(mForeground.height.toFloat())
                else
                    animateWithSpring(mForeground.height - mIntermediateHeight.toFloat())
            }
        }
    }

    private fun checkAcceptableBounds(dY: Float): Boolean {

        return when (mType) {
            ForegroundType.DefaultType,
            ForegroundType.Mixed,
            -> {
                dY + curTranslation >= 0 && dY + curTranslation <= mForeground.height
            }

            ForegroundType.WithoutHide,
            -> {
                dY + curTranslation >= 0 && dY + curTranslation <= mForeground.height - mIntermediateHeight
            }
        }
    }

    private fun animateWithSpring(finalPos: Float) {
        val springAnimation = SpringAnimation(mForeground, DynamicAnimation.TRANSLATION_Y)
            .apply {

                setStartVelocity(2000f)

                spring = SpringForce(finalPos)
                    .apply {
                        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                        stiffness = 400f
                    }

                /* if was fully scrolled to bottom of screen - disappear */
                addEndListener { _, _, _, _ ->
                    if (finalPos == mForeground.height.toFloat())
                        this@AlterableBottomSheetLayout.isVisible = false
                }
                addUpdateListener { _, _, _ ->
                    calculateAndSetAlpha()
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

                childLeft = when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                    Gravity.CENTER_HORIZONTAL -> parentLeft + (parentRight - parentLeft - width) / 2 +
                            lp.leftMargin - lp.rightMargin

                    Gravity.END -> {
                        parentLeft + lp.leftMargin
                    }

                    Gravity.START -> parentLeft + lp.leftMargin

                    else -> parentLeft + lp.leftMargin
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
        direction: Direction,
    ): Boolean {
        var view: View

        for (i in 0 until viewGroup.childCount) {
            view = viewGroup.getChildAt(i)

            if (isViewAtLocation(eventX, eventY, view)) {
                if (view.canScrollVertically(Direction.UP.int) && direction == Direction.UP)
                    return true

                if (view.canScrollVertically(Direction.DOWN.int) && direction == Direction.DOWN)
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
        if (view.left <= rawX && view.right >= rawX
            && view.top <= rawY && view.bottom >= rawY
        ) {
            return true
        }

        return false
    }

    private fun isInterceptClickWithForeground(rawX: Float, rawY: Float): Boolean {
        if (mForeground.left <= rawX && mForeground.right >= rawX
            && mForeground.y <= rawY && mForeground.y + mForeground.height >= rawY
        ) {
            return true
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

    private fun getDirection(cur: Float, prev: Float): Direction {

        return if (cur > prev)
            Direction.UP
        else
            Direction.DOWN
    }

    private fun dragViewAlongWithFinger(dY: Float) {
        mForeground.translationY = dY + curTranslation
    }

    private fun animateToRightDirection(velocity: Float, topPos: Float, bottomPos: Float) {
        if (velocity > 0) {
            animateWithSpring(bottomPos)
        } else {
            animateWithSpring(topPos)
        }
    }

    private fun calculateAndSetAlpha() {
        val bottomEdge = when (mType) {
            ForegroundType.DefaultType -> {
                mForeground.height.toFloat()
            }

            ForegroundType.WithoutHide, ForegroundType.Mixed -> {
                mForeground.height - mIntermediateHeight.toFloat()
            }
        }

        val curAlpha =
            1 - min((mForeground.translationY / bottomEdge), 1f)
        mBackground.alpha = curAlpha

        mBackground.isVisible = curAlpha != 0f
    }
}

class Background @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    var border = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null && border > event.y) {
            performClick()
            return true
        }

        return false
    }
}

class Foreground @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private var mTopCorners: Int = 0

    constructor(context: Context, topCorners: Int) : this(context) {
        mTopCorners = topCorners
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        if (canvas != null) {
            val clipPath = Path()
            clipPath.addRoundRect(RectF(canvas.clipBounds),
                mTopCorners.toFloat(),
                mTopCorners.toFloat(),
                Path.Direction.CW)
            canvas.clipPath(clipPath)
        }
        super.onDraw(canvas)
    }
}