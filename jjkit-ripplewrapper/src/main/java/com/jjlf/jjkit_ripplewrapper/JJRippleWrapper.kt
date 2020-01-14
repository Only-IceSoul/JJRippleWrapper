package com.jjlf.jjkit_ripplewrapper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import com.jjlf.jjkit_layoututils.JJMargin
import com.jjlf.jjkit_layoututils.JJPadding
import com.jjlf.jjkit_layoututils.JJScreen

import java.lang.Exception
import kotlin.math.min

class JJRippleWrapper : RelativeLayout {

    private var mWIDTH = 0
    private var mHEIGHT = 0
    private var mFrameRate = 10
    private var mRippleDuration = 400
    private var mRippleAlpha = 90
    private val mCanvasHandler = Handler()
    private var mRadiusMax = 0f
    private var mAnimationRunning = false
    private var mTimer = 0
    private var mTimerEmpty = 0
    private var durationEmpty = -1
    private var mX = -1f
    private var mY = -1f
    private var mZoomDuration = 200
    private var mZoomScale = 1.03f
    private lateinit var mScaleAnimation: ScaleAnimation
    private var mHasToZoom: Boolean = false
    private var mIsCentered: Boolean = false
    private var mRippleType = 0
    private var mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mOriginBitmap: Bitmap? = null
    private var mRippleColor = Color.WHITE
    private val gestureDetector: GestureDetector =  GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onLongPress(event: MotionEvent) {
            super.onLongPress(event)
            animateRipple(event)
        }
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean { return true }
        override fun onSingleTapUp(e: MotionEvent): Boolean { return true } 
    })
    private val runnable = Runnable { invalidate() }
    private var onCompletionListener: ((JJRippleWrapper)-> Unit)? = null


    //region init

    constructor(context: Context) : super(context) {
        this.id = View.generateViewId()
        mConstraintSet.constrainWidth(id,0)
        mConstraintSet.constrainHeight(id,0)
        mConstraintSetLandScape.constrainWidth(id,0)
        mConstraintSetLandScape.constrainHeight(id,0)
        init()
        mInit = false
    }

    private var mSupportLandScape = false
    private var mIgnoreCl = false
    @SuppressLint("ResourceType")
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs){

        mConstraintSet.constrainWidth(id,0)
        mConstraintSet.constrainHeight(id,0)

        mConstraintSetLandScape.constrainWidth(id,0)
        mConstraintSetLandScape.constrainHeight(id,0)

        val attrsArray = intArrayOf(
            android.R.attr.id,
            android.R.attr.layout_width, // 1
            android.R.attr.layout_height // 2
        )
        val ba = context.obtainStyledAttributes(attrs,
            attrsArray, 0, 0)

        val attrWidth = ba.getLayoutDimension(1, 0)
        val attrHeight = ba.getLayoutDimension(2, 0)

        val attrId = ba.getResourceId(0,View.NO_ID)
        if(attrId == View.NO_ID) id = View.generateViewId()
        ba.recycle()


        val a = context.obtainStyledAttributes(attrs,
            R.styleable.JJRippleWrapper, 0, 0)


        mIgnoreCl = a.getBoolean(R.styleable.JJRippleWrapper_layout_ignoreCl,false)

        mConfigurationChanged = a.getBoolean(R.styleable.JJRippleWrapper_support_configuration_changed,false)

        mSupportLandScape = a.getBoolean(R.styleable.JJRippleWrapper_support_landscape,false)

        initAttrs(context,attrs)

        //region attrs portrait

        val aFillParent = a.getBoolean(R.styleable.JJRippleWrapper_clFillParent,false)
        val aFillParentHorizontal = a.getBoolean(R.styleable.JJRippleWrapper_clFillParentHorizontally,false)
        val aFillParentVertical = a.getBoolean(R.styleable.JJRippleWrapper_clFillParentVertically,false)

        val aCenterInParent = a.getBoolean(R.styleable.JJRippleWrapper_clCenterInParent,false)
        val aCenterInParentHorizontal = a.getBoolean(R.styleable.JJRippleWrapper_clCenterInParentHorizontally,false)
        val aCenterInParentVertical = a.getBoolean(R.styleable.JJRippleWrapper_clCenterInParentVertically,false)
        val aCenterInParentTopVertical = a.getBoolean(R.styleable.JJRippleWrapper_clCenterInParentTopVertically,false)
        val aCenterInParentBottomVertical = a.getBoolean(R.styleable.JJRippleWrapper_clCenterInParentBottomVertically,false)
        val aCenterInParentStartHorizontal = a.getBoolean(R.styleable.JJRippleWrapper_clCenterInParentStartHorizontally,false)
        val aCenterInParentEndHorizontal = a.getBoolean(R.styleable.JJRippleWrapper_clCenterInParentEndHorizontally,false)

        val aCenterInTopVerticalOf = a.getResourceId(R.styleable.JJRippleWrapper_clCenterInTopVerticallyOf,View.NO_ID)
        val aCenterInBottomVerticalOf = a.getResourceId(R.styleable.JJRippleWrapper_clCenterInBottomVerticallyOf,View.NO_ID)
        val aCenterInStartHorizontalOf= a.getResourceId(R.styleable.JJRippleWrapper_clCenterInStartHorizontallyOf,View.NO_ID)
        val aCenterInEndHorizontalOf = a.getResourceId(R.styleable.JJRippleWrapper_clCenterInEndHorizontallyOf,View.NO_ID)

        val aCenterVerticalOf = a.getResourceId(R.styleable.JJRippleWrapper_clCenterVerticallyOf,View.NO_ID)
        val aCenterHorizontalOf = a.getResourceId(R.styleable.JJRippleWrapper_clCenterHorizontallyOf,View.NO_ID)

        val aMarginEnd = a.getDimension(R.styleable.JJRippleWrapper_clMarginEnd,0f)
        val aMarginStart = a.getDimension(R.styleable.JJRippleWrapper_clMarginStart,0f)
        val aMarginTop = a.getDimension(R.styleable.JJRippleWrapper_clMarginTop,0f)
        val aMarginBottom = a.getDimension(R.styleable.JJRippleWrapper_clMarginBottom,0f)
        val aMarginEndPercent = a.getFloat(R.styleable.JJRippleWrapper_clMarginEndPercentScreenHeight,0f)
        val aMarginStartPercent = a.getFloat(R.styleable.JJRippleWrapper_clMarginStartPercentScreenHeight,0f)
        val aMarginTopPercent = a.getFloat(R.styleable.JJRippleWrapper_clMarginTopPercentScreenHeight,0f)
        val aMarginBottomPercent = a.getFloat(R.styleable.JJRippleWrapper_clMarginBottomPercentScreenHeight,0f)
        val aMarginEndPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_clMarginEndPercentScreenWidth,0f)
        val aMarginStartPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_clMarginStartPercentScreenWidth,0f)
        val aMarginTopPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_clMarginTopPercentScreenWidth,0f)
        val aMarginBottomPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_clMarginBottomPercentScreenWidth,0f)

        val aMargin = a.getDimension(R.styleable.JJRippleWrapper_clMargin,0f)
        val aMarginPercentHeight = a.getFloat(R.styleable.JJRippleWrapper_clMarginPerScHeight,0f)
        val aMarginPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_clMarginPerScWidth,0f)
        val aMarginResponsive = a.getResourceId(R.styleable.JJRippleWrapper_clMarginResponsive,
            ConstraintLayout.NO_ID
        )
        val aMarginResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_clMarginResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val aMarginResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_clMarginResPerScWidth,
            ConstraintLayout.NO_ID
        )

        val aMarginEndResponsive = a.getResourceId(R.styleable.JJRippleWrapper_clMarginEndResponsive,
            ConstraintLayout.NO_ID
        )
        val aMarginStartResponsive = a.getResourceId(R.styleable.JJRippleWrapper_clMarginStartResponsive,
            ConstraintLayout.NO_ID
        )
        val aMarginTopResponsive = a.getResourceId(R.styleable.JJRippleWrapper_clMarginTopResponsive,
            ConstraintLayout.NO_ID
        )
        val aMarginBottomResponsive = a.getResourceId(R.styleable.JJRippleWrapper_clMarginBottomResponsive,
            ConstraintLayout.NO_ID
        )
        val aMarginEndResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_clMarginEndResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val aMarginStartResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_clMarginStartResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val aMarginTopResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_clMarginTopResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val aMarginBottomResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_clMarginBottomResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val aMarginEndResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_clMarginEndResPerScWidth,
            ConstraintLayout.NO_ID
        )
        val aMarginStartResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_clMarginStartResPerScWidth,
            ConstraintLayout.NO_ID
        )
        val aMarginTopResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_clMarginTopResPerScWidth,
            ConstraintLayout.NO_ID
        )
        val aMarginBottomResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_clMarginBottomResPerScWidth,
            ConstraintLayout.NO_ID
        )

        val aMarginVertical = a.getDimension(R.styleable.JJRippleWrapper_clMarginVertical,0f)
        val aMarginVerticalPercentHeight = a.getFloat(R.styleable.JJRippleWrapper_clMarginVerticalPerScHeight,0f)
        val aMarginVerticalPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_clMarginVerticalPerScWidth,0f)
        val aMarginVerticalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_clMarginVerticalResponsive,
            ConstraintLayout.NO_ID
        )
        val aMarginVerticalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_clMarginVerticalResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val aMarginVerticalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_clMarginVerticalResPerScWidth,
            ConstraintLayout.NO_ID
        )

        val aMarginHorizontal = a.getDimension(R.styleable.JJRippleWrapper_clMarginHorizontal,0f)
        val aMarginHorizontalPercentHeight = a.getFloat(R.styleable.JJRippleWrapper_clMarginHorizontalPerScHeight,0f)
        val aMarginHorizontalPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_clMarginHorizontalPerScWidth,0f)
        val aMarginHorizontalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_clMarginHorizontalResponsive,
            ConstraintLayout.NO_ID
        )
        val aMarginHorizontalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_clMarginHorizontalResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val aMarginHorizontalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_clMarginHorizontalResPerScWidth,
            ConstraintLayout.NO_ID
        )


        val aVerticalBias = a.getFloat(R.styleable.JJRippleWrapper_clVerticalBias,0.5f)
        val aHorizontalBias = a.getFloat(R.styleable.JJRippleWrapper_clHorizontalBias,0.5f)

        val aStartToStartParent = a.getBoolean(R.styleable.JJRippleWrapper_clStartToStartParent,false)
        val aStartToEndParent = a.getBoolean(R.styleable.JJRippleWrapper_clStartToEndParent,false)
        val aEndToEndParent = a.getBoolean(R.styleable.JJRippleWrapper_clEndToEndParent,false)
        val aEndToStartParent = a.getBoolean(R.styleable.JJRippleWrapper_clEndToStartParent,false)
        val aTopToTopParent = a.getBoolean(R.styleable.JJRippleWrapper_clTopToTopParent,false)
        val aTopToBottomParent = a.getBoolean(R.styleable.JJRippleWrapper_clTopToBottomParent,false)
        val aBottomToBottomParent = a.getBoolean(R.styleable.JJRippleWrapper_clBottomToBottomParent,false)
        val aBottomToTopParent = a.getBoolean(R.styleable.JJRippleWrapper_clBottomToTopParent,false)

        val aStartToStartOf = a.getResourceId(R.styleable.JJRippleWrapper_clStartToStartOf,View.NO_ID)
        val aStartToEndOf = a.getResourceId(R.styleable.JJRippleWrapper_clStartToEndOf,View.NO_ID)
        val aEndToEndOf = a.getResourceId(R.styleable.JJRippleWrapper_clEndToEndOf,View.NO_ID)
        val aEndToStartOf = a.getResourceId(R.styleable.JJRippleWrapper_clEndToStartOf,View.NO_ID)
        val aTopToTopOf = a.getResourceId(R.styleable.JJRippleWrapper_clTopToTopOf,View.NO_ID)
        val aTopToBottomOf = a.getResourceId(R.styleable.JJRippleWrapper_clTopToBottomOf,View.NO_ID)
        val aBottomToBottomOf = a.getResourceId(R.styleable.JJRippleWrapper_clBottomToBottomOf,View.NO_ID)
        val aBottomToTopOf = a.getResourceId(R.styleable.JJRippleWrapper_clBottomToTopOf,View.NO_ID)

        val aHeightPercent = a.getFloat(R.styleable.JJRippleWrapper_clHeightPercent,0f)
        val aWidthPercent = a.getFloat(R.styleable.JJRippleWrapper_clWidthPercent,0f)
        val aHeightPercentScreenWidth = a.getFloat(R.styleable.JJRippleWrapper_clHeightPercentScreenWidth,0f)
        val aWidthPercentScreenWidth = a.getFloat(R.styleable.JJRippleWrapper_clWidthPercentScreenWidth,0f)
        val aHeightPercentScreenHeight = a.getFloat(R.styleable.JJRippleWrapper_clHeightPercentScreenHeight,0f)
        val aWidthPercentScreenHeight = a.getFloat(R.styleable.JJRippleWrapper_clWidthPercentScreenHeight,0f)

        val aHeightResponsive = a.getResourceId(R.styleable.JJRippleWrapper_clHeightResponsive,View.NO_ID)
        val aWidthResponsive = a.getResourceId(R.styleable.JJRippleWrapper_clWidthResponsive,View.NO_ID)

        val aHeightResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_clHeightResponsivePercentScreenHeight,View.NO_ID)
        val aWidthResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_clWidthResponsivePercentScreenWidth,View.NO_ID)
        val aHeightResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_clHeightResponsivePercentScreenWidth,View.NO_ID)
        val aWidthResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_clWidthResponsivePercentScreenHeight,View.NO_ID)

        val lHeightPercentScreenWidth = a.getFloat(R.styleable.JJRippleWrapper_lpHeightPercentScreenWidth,0f)
        val lWidthPercentScreenWidth = a.getFloat(R.styleable.JJRippleWrapper_lpWidthPercentScreenWidth,0f)
        val lHeightPercentScreenHeight = a.getFloat(R.styleable.JJRippleWrapper_lpHeightPercentScreenHeight,0f)
        val lWidthPercentScreenHeight = a.getFloat(R.styleable.JJRippleWrapper_lpWidthPercentScreenHeight,0f)

        val lHeightResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpHeightResponsive,View.NO_ID)
        val lWidthResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpWidthResponsive,View.NO_ID)
        val lHeightResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpHeightResponsivePercentScreenHeight,View.NO_ID)
        val lWidthResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpWidthResponsivePercentScreenWidth,View.NO_ID)
        val lHeightResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpHeightResponsivePercentScreenWidth,View.NO_ID)
        val lWidthResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpWidthResponsivePercentScreenHeight,View.NO_ID)

        val lMarginTopPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpMarginTopPerScHeight,0f)
        val lMarginLeftPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpMarginLeftPerScHeight,0f)
        val lMarginRightPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpMarginRightPerScHeight,0f)
        val lMarginBottomPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpMarginBottomPerScHeight,0f)

        val lMarginTopPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpMarginTopPerScWidth,0f)
        val lMarginLeftPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpMarginLeftPerScWidth,0f)
        val lMarginRightPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpMarginRightPerScWidth,0f)
        val lMarginBottomPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpMarginBottomPerScWidth,0f)

        val lMarginTopResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginTopResponsive,View.NO_ID)
        val lMarginLeftResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginLeftResponsive,View.NO_ID)
        val lMarginRightResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginRightResponsive,View.NO_ID)
        val lMarginBottomResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginBottomResponsive,View.NO_ID)

        val lMarginTopResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginTopResPerScWidth,View.NO_ID)
        val lMarginLeftResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginLeftResPerScWidth,View.NO_ID)
        val lMarginRightResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginRightResPerScWidth,View.NO_ID)
        val lMarginBottomResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginBottomResPerScWidth,View.NO_ID)

        val lMarginTopResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginTopResPerScHeight,View.NO_ID)
        val lMarginLeftResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginLeftResPerScHeight,View.NO_ID)
        val lMarginRightResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginRightResPerScHeight,View.NO_ID)
        val lMarginBottomResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginBottomResPerScHeight,View.NO_ID)

        val lMarginPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpMarginPercentScHeight,0f)
        val lMarginPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpMarginPercentScWidth,0f)
        val lMarginResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginResponsive,View.NO_ID)
        val lMarginResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginResPerScHeight,View.NO_ID)
        val lMarginResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginResPerScWidth,View.NO_ID)

        val lMarginVerticalPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpMarginVerticalPerScHeight,0f)
        val lMarginVerticalPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpMarginVerticalPerScWidth,0f)
        val lMarginVerticalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginVerticalResponsive,View.NO_ID)
        val lMarginVerticalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginVerticalResPerScWidth,View.NO_ID)
        val lMarginVerticalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginVerticalResPerScHeight,View.NO_ID)

        val lMarginHorizontalPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpMarginHorizontalPerScHeight,0f)
        val lMarginHorizontalPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpMarginHorizontalPerScWidth,0f)
        val lMarginHorizontalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginHorizontalResponsive,View.NO_ID)
        val lMarginHorizontalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginHorizontalResPerScWidth,View.NO_ID)
        val lMarginHorizontalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpMarginHorizontalResPerScHeight,View.NO_ID)


        val lPaddingTopPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingTopPerScHeight,0f)
        val lPaddingLeftPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingLeftPerScHeight,0f)
        val lPaddingRightPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingRightPerScHeight,0f)
        val lPaddingBottomPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingBottomPerScHeight,0f)

        val lPaddingTopPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingTopPerScWidth,0f)
        val lPaddingLeftPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingLeftPerScWidth,0f)
        val lPaddingRightPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingRightPerScWidth,0f)
        val lPaddingBottomPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingBottomPerScWidth,0f)

        val lPaddingTopResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingTopResponsive,View.NO_ID)
        val lPaddingLeftResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingLeftResponsive,View.NO_ID)
        val lPaddingRightResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingRightResponsive,View.NO_ID)
        val lPaddingBottomResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingBottomResponsive,View.NO_ID)

        val lPaddingTopResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingTopResPerScWidth,View.NO_ID)
        val lPaddingLeftResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingLeftResPerScWidth,View.NO_ID)
        val lPaddingRightResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingRightResPerScWidth,View.NO_ID)
        val lPaddingBottomResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingBottomResPerScWidth,View.NO_ID)

        val lPaddingTopResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingTopResPerScHeight,View.NO_ID)
        val lPaddingLeftResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingLeftResPerScHeight,View.NO_ID)
        val lPaddingRightResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingRightResPerScHeight,View.NO_ID)
        val lPaddingBottomResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingBottomResPerScHeight,View.NO_ID)

        val lPaddingPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingPercentScHeight,0f)
        val lPaddingPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingPercentScWidth,0f)
        val lPaddingResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingResponsive,View.NO_ID)
        val lPaddingResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingResPerScHeight,View.NO_ID)
        val lPaddingResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingResPerScWidth,View.NO_ID)

        val lPaddingVerticalPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingVerticalPerScHeight,0f)
        val lPaddingVerticalPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingVerticalPerScWidth,0f)
        val lPaddingVerticalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingVerticalResponsive,View.NO_ID)
        val lPaddingVerticalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingVerticalResPerScWidth,View.NO_ID)
        val lPaddingVerticalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingVerticalResPerScHeight,View.NO_ID)

        val lPaddingHorizontalPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingHorizontalPerScHeight,0f)
        val lPaddingHorizontalPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lpPaddingHorizontalPerScWidth,0f)
        val lPaddingHorizontalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingHorizontalResponsive,View.NO_ID)
        val lPaddingHorizontalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingHorizontalResPerScWidth,View.NO_ID)
        val lPaddingHorizontalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lpPaddingHorizontalResPerScHeight,View.NO_ID)


        //endregion

        //region attrs landscape

        val alsFillParent = a.getBoolean(R.styleable.JJRippleWrapper_cllFillParent,false)
        val alsFillParentHorizontal = a.getBoolean(R.styleable.JJRippleWrapper_cllFillParentHorizontally,false)
        val alsFillParentVertical = a.getBoolean(R.styleable.JJRippleWrapper_cllFillParentVertically,false)

        val alsCenterInParent = a.getBoolean(R.styleable.JJRippleWrapper_cllCenterInParent,false)
        val alsCenterInParentHorizontal = a.getBoolean(R.styleable.JJRippleWrapper_cllCenterInParentHorizontally,false)
        val alsCenterInParentVertical = a.getBoolean(R.styleable.JJRippleWrapper_cllCenterInParentVertically,false)
        val alsCenterInParentTopVertical = a.getBoolean(R.styleable.JJRippleWrapper_cllCenterInParentTopVertically,false)
        val alsCenterInParentBottomVertical = a.getBoolean(R.styleable.JJRippleWrapper_cllCenterInParentBottomVertically,false)
        val alsCenterInParentStartHorizontal = a.getBoolean(R.styleable.JJRippleWrapper_cllCenterInParentStartHorizontally,false)
        val alsCenterInParentEndHorizontal = a.getBoolean(R.styleable.JJRippleWrapper_cllCenterInParentEndHorizontally,false)

        val alsCenterInTopVerticalOf = a.getResourceId(R.styleable.JJRippleWrapper_cllCenterInTopVerticallyOf,View.NO_ID)
        val alsCenterInBottomVerticalOf = a.getResourceId(R.styleable.JJRippleWrapper_cllCenterInBottomVerticallyOf,View.NO_ID)
        val alsCenterInStartHorizontalOf= a.getResourceId(R.styleable.JJRippleWrapper_cllCenterInStartHorizontallyOf,View.NO_ID)
        val alsCenterInEndHorizontalOf = a.getResourceId(R.styleable.JJRippleWrapper_cllCenterInEndHorizontallyOf,View.NO_ID)

        val alsCenterVerticalOf = a.getResourceId(R.styleable.JJRippleWrapper_cllCenterVerticallyOf,View.NO_ID)
        val alsCenterHorizontalOf = a.getResourceId(R.styleable.JJRippleWrapper_cllCenterHorizontallyOf,View.NO_ID)

        val alsMarginEnd = a.getDimension(R.styleable.JJRippleWrapper_cllMarginEnd,0f)
        val alsMarginStart = a.getDimension(R.styleable.JJRippleWrapper_cllMarginStart,0f)
        val alsMarginTop = a.getDimension(R.styleable.JJRippleWrapper_cllMarginTop,0f)
        val alsMarginBottom = a.getDimension(R.styleable.JJRippleWrapper_cllMarginBottom,0f)
        val alsMarginEndPercent = a.getFloat(R.styleable.JJRippleWrapper_cllMarginEndPercentScreenHeight,0f)
        val alsMarginStartPercent = a.getFloat(R.styleable.JJRippleWrapper_cllMarginStartPercentScreenHeight,0f)
        val alsMarginTopPercent = a.getFloat(R.styleable.JJRippleWrapper_cllMarginTopPercentScreenHeight,0f)
        val alsMarginBottomPercent = a.getFloat(R.styleable.JJRippleWrapper_cllMarginBottomPercentScreenHeight,0f)
        val alsMarginEndPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_cllMarginEndPercentScreenWidth,0f)
        val alsMarginStartPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_cllMarginStartPercentScreenWidth,0f)
        val alsMarginTopPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_cllMarginTopPercentScreenWidth,0f)
        val alsMarginBottomPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_cllMarginBottomPercentScreenWidth,0f)

        val alsMargin = a.getDimension(R.styleable.JJRippleWrapper_cllMargin,0f)
        val alsMarginPercentHeight = a.getFloat(R.styleable.JJRippleWrapper_cllMarginPerScHeight,0f)
        val alsMarginPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_cllMarginPerScWidth,0f)
        val alsMarginResponsive = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginResponsive,
            ConstraintLayout.NO_ID
        )
        val alsMarginResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val alsMarginResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginResPerScWidth,
            ConstraintLayout.NO_ID
        )

        val alsMarginEndResponsive = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginEndResponsive,
            ConstraintLayout.NO_ID
        )
        val alsMarginStartResponsive = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginStartResponsive,
            ConstraintLayout.NO_ID
        )
        val alsMarginTopResponsive = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginTopResponsive,
            ConstraintLayout.NO_ID
        )
        val alsMarginBottomResponsive = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginBottomResponsive,
            ConstraintLayout.NO_ID
        )
        val alsMarginEndResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginEndResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val alsMarginStartResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginStartResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val alsMarginTopResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginTopResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val alsMarginBottomResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginBottomResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val alsMarginEndResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginEndResPerScWidth,
            ConstraintLayout.NO_ID
        )
        val alsMarginStartResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginStartResPerScWidth,
            ConstraintLayout.NO_ID
        )
        val alsMarginTopResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginTopResPerScWidth,
            ConstraintLayout.NO_ID
        )
        val alsMarginBottomResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginBottomResPerScWidth,
            ConstraintLayout.NO_ID
        )

        val alsMarginVertical = a.getDimension(R.styleable.JJRippleWrapper_cllMarginVertical,0f)
        val alsMarginVerticalPercentHeight = a.getFloat(R.styleable.JJRippleWrapper_cllMarginVerticalPerScHeight,0f)
        val alsMarginVerticalPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_cllMarginVerticalPerScWidth,0f)
        val alsMarginVerticalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginVerticalResponsive,
            ConstraintLayout.NO_ID
        )
        val alsMarginVerticalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginVerticalResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val alsMarginVerticalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginVerticalResPerScWidth,
            ConstraintLayout.NO_ID
        )

        val alsMarginHorizontal = a.getDimension(R.styleable.JJRippleWrapper_cllMarginHorizontal,0f)
        val alsMarginHorizontalPercentHeight = a.getFloat(R.styleable.JJRippleWrapper_cllMarginHorizontalPerScHeight,0f)
        val alsMarginHorizontalPercentWidth = a.getFloat(R.styleable.JJRippleWrapper_cllMarginHorizontalPerScWidth,0f)
        val alsMarginHorizontalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginHorizontalResponsive,
            ConstraintLayout.NO_ID
        )
        val alsMarginHorizontalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginHorizontalResPerScHeight,
            ConstraintLayout.NO_ID
        )
        val alsMarginHorizontalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_cllMarginHorizontalResPerScWidth,
            ConstraintLayout.NO_ID
        )


        val alsVerticalBias = a.getFloat(R.styleable.JJRippleWrapper_cllVerticalBias,0.5f)
        val alsHorizontalBias = a.getFloat(R.styleable.JJRippleWrapper_cllHorizontalBias,0.5f)

        val alsStartToStartParent = a.getBoolean(R.styleable.JJRippleWrapper_cllStartToStartParent,false)
        val alsStartToEndParent = a.getBoolean(R.styleable.JJRippleWrapper_cllStartToEndParent,false)
        val alsEndToEndParent = a.getBoolean(R.styleable.JJRippleWrapper_cllEndToEndParent,false)
        val alsEndToStartParent = a.getBoolean(R.styleable.JJRippleWrapper_cllEndToStartParent,false)
        val alsTopToTopParent = a.getBoolean(R.styleable.JJRippleWrapper_cllTopToTopParent,false)
        val alsTopToBottomParent = a.getBoolean(R.styleable.JJRippleWrapper_cllTopToBottomParent,false)
        val alsBottomToBottomParent = a.getBoolean(R.styleable.JJRippleWrapper_cllBottomToBottomParent,false)
        val alsBottomToTopParent = a.getBoolean(R.styleable.JJRippleWrapper_cllBottomToTopParent,false)

        val alsStartToStartOf = a.getResourceId(R.styleable.JJRippleWrapper_cllStartToStartOf,View.NO_ID)
        val alsStartToEndOf = a.getResourceId(R.styleable.JJRippleWrapper_cllStartToEndOf,View.NO_ID)
        val alsEndToEndOf = a.getResourceId(R.styleable.JJRippleWrapper_cllEndToEndOf,View.NO_ID)
        val alsEndToStartOf = a.getResourceId(R.styleable.JJRippleWrapper_cllEndToStartOf,View.NO_ID)
        val alsTopToTopOf = a.getResourceId(R.styleable.JJRippleWrapper_cllTopToTopOf,View.NO_ID)
        val alsTopToBottomOf = a.getResourceId(R.styleable.JJRippleWrapper_cllTopToBottomOf,View.NO_ID)
        val alsBottomToBottomOf = a.getResourceId(R.styleable.JJRippleWrapper_cllBottomToBottomOf,View.NO_ID)
        val alsBottomToTopOf = a.getResourceId(R.styleable.JJRippleWrapper_cllBottomToTopOf,View.NO_ID)

        val alsHeightPercent = a.getFloat(R.styleable.JJRippleWrapper_cllHeightPercent,0f)
        val alsWidthPercent = a.getFloat(R.styleable.JJRippleWrapper_cllWidthPercent,0f)
        val alsHeightPercentScreenWidth = a.getFloat(R.styleable.JJRippleWrapper_cllHeightPercentScreenWidth,0f)
        val alsWidthPercentScreenWidth = a.getFloat(R.styleable.JJRippleWrapper_cllWidthPercentScreenWidth,0f)
        val alsHeightPercentScreenHeight = a.getFloat(R.styleable.JJRippleWrapper_cllHeightPercentScreenHeight,0f)
        val alsWidthPercentScreenHeight = a.getFloat(R.styleable.JJRippleWrapper_cllWidthPercentScreenHeight,0f)

        val alsHeightResponsive = a.getResourceId(R.styleable.JJRippleWrapper_cllHeightResponsive,View.NO_ID)
        val alsWidthResponsive = a.getResourceId(R.styleable.JJRippleWrapper_cllWidthResponsive,View.NO_ID)

        val alsHeightResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_cllHeightResponsivePercentScreenHeight,View.NO_ID)
        val alsWidthResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_cllWidthResponsivePercentScreenWidth,View.NO_ID)
        val alsHeightResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_cllHeightResponsivePercentScreenWidth,View.NO_ID)
        val alsWidthResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_cllWidthResponsivePercentScreenHeight,View.NO_ID)


        val attrHeightLs = a.getLayoutDimension(R.styleable.JJRippleWrapper_layout_height_landscape,0)
        val attrWidthLs = a.getLayoutDimension(R.styleable.JJRippleWrapper_layout_width_landscape,0)

        val llsPadding = a.getDimension(R.styleable.JJRippleWrapper_lplPadding,-100f)
        val llsPaddingVertical = a.getDimension(R.styleable.JJRippleWrapper_lplPaddingVertical,-100f)
        val llsPaddingHorizontal = a.getDimension(R.styleable.JJRippleWrapper_lplPaddingHorizontal,-100f)
        val llsPaddingStart = a.getDimension(R.styleable.JJRippleWrapper_lplPaddingStart,-100f)
        val llsPaddingEnd = a.getDimension(R.styleable.JJRippleWrapper_lplPaddingEnd,-100f)
        val llsPaddingBottom = a.getDimension(R.styleable.JJRippleWrapper_lplPaddingBottom,-100f)
        val llsPaddingTop = a.getDimension(R.styleable.JJRippleWrapper_lplPaddingTop,-100f)

        val llsMargin = a.getDimension(R.styleable.JJRippleWrapper_lplMargin,-100f)
        val llsMarginVertical = a.getDimension(R.styleable.JJRippleWrapper_lplMarginVertical,-100f)
        val llsMarginHorizontal = a.getDimension(R.styleable.JJRippleWrapper_lplMarginHorizontal,-100f)
        val llsMarginStart = a.getDimension(R.styleable.JJRippleWrapper_lplMarginStart,-100f)
        val llsMarginEnd = a.getDimension(R.styleable.JJRippleWrapper_lplMarginEnd,-100f)
        val llsMarginBottom = a.getDimension(R.styleable.JJRippleWrapper_lplMarginBottom,-100f)
        val llsMarginTop = a.getDimension(R.styleable.JJRippleWrapper_lplMarginTop,-100f)

        val llsHeightPercentScreenWidth = a.getFloat(R.styleable.JJRippleWrapper_lplHeightPercentScreenWidth,0f)
        val llsWidthPercentScreenWidth = a.getFloat(R.styleable.JJRippleWrapper_lplWidthPercentScreenWidth,0f)
        val llsHeightPercentScreenHeight = a.getFloat(R.styleable.JJRippleWrapper_lplHeightPercentScreenHeight,0f)
        val llsWidthPercentScreenHeight = a.getFloat(R.styleable.JJRippleWrapper_lplWidthPercentScreenHeight,0f)

        val llsHeightResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplHeightResponsive,View.NO_ID)
        val llsWidthResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplWidthResponsive,View.NO_ID)
        val llsHeightResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplHeightResponsivePercentScreenHeight,View.NO_ID)
        val llsWidthResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplWidthResponsivePercentScreenWidth,View.NO_ID)
        val llsHeightResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplHeightResponsivePercentScreenWidth,View.NO_ID)
        val llsWidthResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplWidthResponsivePercentScreenHeight,View.NO_ID)

        val llsMarginTopPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplMarginTopPerScHeight,0f)
        val llsMarginLeftPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplMarginLeftPerScHeight,0f)
        val llsMarginRightPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplMarginRightPerScHeight,0f)
        val llsMarginBottomPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplMarginBottomPerScHeight,0f)

        val llsMarginTopPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplMarginTopPerScWidth,0f)
        val llsMarginLeftPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplMarginLeftPerScWidth,0f)
        val llsMarginRightPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplMarginRightPerScWidth,0f)
        val llsMarginBottomPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplMarginBottomPerScWidth,0f)

        val llsMarginTopResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginTopResponsive,View.NO_ID)
        val llsMarginLeftResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginLeftResponsive,View.NO_ID)
        val llsMarginRightResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginRightResponsive,View.NO_ID)
        val llsMarginBottomResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginBottomResponsive,View.NO_ID)

        val llsMarginTopResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginTopResPerScWidth,View.NO_ID)
        val llsMarginLeftResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginLeftResPerScWidth,View.NO_ID)
        val llsMarginRightResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginRightResPerScWidth,View.NO_ID)
        val llsMarginBottomResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginBottomResPerScWidth,View.NO_ID)

        val llsMarginTopResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginTopResPerScHeight,View.NO_ID)
        val llsMarginLeftResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginLeftResPerScHeight,View.NO_ID)
        val llsMarginRightResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginRightResPerScHeight,View.NO_ID)
        val llsMarginBottomResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginBottomResPerScHeight,View.NO_ID)

        val llsMarginPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplMarginPercentScHeight,0f)
        val llsMarginPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplMarginPercentScWidth,0f)
        val llsMarginResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginResponsive,View.NO_ID)
        val llsMarginResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginResPerScHeight,View.NO_ID)
        val llsMarginResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginResPerScWidth,View.NO_ID)

        val llsMarginVerticalPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplMarginVerticalPerScHeight,0f)
        val llsMarginVerticalPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplMarginVerticalPerScWidth,0f)
        val llsMarginVerticalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginVerticalResponsive,View.NO_ID)
        val llsMarginVerticalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginVerticalResPerScWidth,View.NO_ID)
        val llsMarginVerticalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginVerticalResPerScHeight,View.NO_ID)

        val llsMarginHorizontalPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplMarginHorizontalPerScHeight,0f)
        val llsMarginHorizontalPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplMarginHorizontalPerScWidth,0f)
        val llsMarginHorizontalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginHorizontalResponsive,View.NO_ID)
        val llsMarginHorizontalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginHorizontalResPerScWidth,View.NO_ID)
        val llsMarginHorizontalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplMarginHorizontalResPerScHeight,View.NO_ID)


        val llsPaddingTopPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingTopPerScHeight,0f)
        val llsPaddingLeftPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingLeftPerScHeight,0f)
        val llsPaddingRightPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingRightPerScHeight,0f)
        val llsPaddingBottomPercentScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingBottomPerScHeight,0f)

        val llsPaddingTopPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingTopPerScWidth,0f)
        val llsPaddingLeftPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingLeftPerScWidth,0f)
        val llsPaddingRightPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingRightPerScWidth,0f)
        val llsPaddingBottomPercentScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingBottomPerScWidth,0f)

        val llsPaddingTopResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingTopResponsive,View.NO_ID)
        val llsPaddingLeftResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingLeftResponsive,View.NO_ID)
        val llsPaddingRightResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingRightResponsive,View.NO_ID)
        val llsPaddingBottomResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingBottomResponsive,View.NO_ID)

        val llsPaddingTopResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingTopResPerScWidth,View.NO_ID)
        val llsPaddingLeftResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingLeftResPerScWidth,View.NO_ID)
        val llsPaddingRightResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingRightResPerScWidth,View.NO_ID)
        val llsPaddingBottomResponsivePercentScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingBottomResPerScWidth,View.NO_ID)

        val llsPaddingTopResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingTopResPerScHeight,View.NO_ID)
        val llsPaddingLeftResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingLeftResPerScHeight,View.NO_ID)
        val llsPaddingRightResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingRightResPerScHeight,View.NO_ID)
        val llsPaddingBottomResponsivePercentScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingBottomResPerScHeight,View.NO_ID)

        val llsPaddingPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingPercentScHeight,0f)
        val llsPaddingPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingPercentScWidth,0f)
        val llsPaddingResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingResponsive,View.NO_ID)
        val llsPaddingResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingResPerScHeight,View.NO_ID)
        val llsPaddingResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingResPerScWidth,View.NO_ID)

        val llsPaddingVerticalPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingVerticalPerScHeight,0f)
        val llsPaddingVerticalPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingVerticalPerScWidth,0f)
        val llsPaddingVerticalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingVerticalResponsive,View.NO_ID)
        val llsPaddingVerticalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingVerticalResPerScWidth,View.NO_ID)
        val llsPaddingVerticalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingVerticalResPerScHeight,View.NO_ID)

        val llsPaddingHorizontalPerScHeight = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingHorizontalPerScHeight,0f)
        val llsPaddingHorizontalPerScWidth = a.getFloat(R.styleable.JJRippleWrapper_lplPaddingHorizontalPerScWidth,0f)
        val llsPaddingHorizontalResponsive = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingHorizontalResponsive,View.NO_ID)
        val llsPaddingHorizontalResPerScWidth = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingHorizontalResPerScWidth,View.NO_ID)
        val llsPaddingHorizontalResPerScHeight = a.getResourceId(R.styleable.JJRippleWrapper_lplPaddingHorizontalResPerScHeight,View.NO_ID)

        //endregion

        a.recycle()

        //region standard portrait

        //region margin

        if(lMarginPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(lMarginPerScHeight)
            mlpMargins = JJMargin(mar,mar,mar,mar)
        }
        if(lMarginPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(lMarginPerScWidth)
            mlpMargins = JJMargin(mar,mar,mar,mar)
        }

        if(lMarginResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lMarginResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlpMargins = JJMargin(mar,mar,mar,mar)
        }

        if(lMarginResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lMarginResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpMargins = JJMargin(mar,mar,mar,mar)
        }

        if(lMarginResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lMarginResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpMargins = JJMargin(mar,mar,mar,mar)
        }


        //endregion

        //region margin vertical
        if(lMarginVerticalPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(lMarginVerticalPerScHeight)
            mlpMargins.top = mar ; mlpMargins.bottom = mar
        }
        if(lMarginVerticalPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(lMarginVerticalPerScWidth)
            mlpMargins.top = mar ; mlpMargins.bottom = mar
        }

        if(lMarginVerticalResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lMarginVerticalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlpMargins.top = mar ; mlpMargins.bottom = mar
        }

        if(lMarginVerticalResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lMarginVerticalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpMargins.top = mar ; mlpMargins.bottom = mar
        }

        if(lMarginVerticalResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lMarginVerticalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpMargins.top = mar ; mlpMargins.bottom = mar
        }
        //endregion

        //region Horizontal margin

        if(lMarginHorizontalPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(lMarginHorizontalPerScHeight)
            mlpMargins.left = mar ; mlpMargins.right = mar
        }
        if(lMarginHorizontalPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(lMarginHorizontalPerScWidth)
            mlpMargins.left = mar ; mlpMargins.right = mar
        }

        if(lMarginHorizontalResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lMarginHorizontalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlpMargins.left = mar ; mlpMargins.right = mar
        }

        if(lMarginHorizontalResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lMarginHorizontalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpMargins.left = mar ; mlpMargins.right = mar
        }

        if(lMarginHorizontalResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lMarginHorizontalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpMargins.left = mar ; mlpMargins.right = mar
        }

        //endregion

        //region margin start end top bottom

        if(lMarginTopPercentScHeight > 0f)  mlpMargins.top = JJScreen.percentHeight(lMarginTopPercentScHeight)
        if(lMarginLeftPercentScHeight > 0f)  mlpMargins.left = JJScreen.percentHeight(lMarginLeftPercentScHeight)
        if(lMarginRightPercentScHeight > 0f) mlpMargins.right = JJScreen.percentHeight(lMarginRightPercentScHeight)
        if(lMarginBottomPercentScHeight > 0f) mlpMargins.bottom = JJScreen.percentHeight(lMarginBottomPercentScHeight)

        if(lMarginTopPercentScWidth > 0f)  mlpMargins.top = JJScreen.percentWidth(lMarginTopPercentScWidth)
        if(lMarginLeftPercentScWidth > 0f)  mlpMargins.left = JJScreen.percentWidth(lMarginLeftPercentScWidth)
        if(lMarginRightPercentScWidth > 0f) mlpMargins.right = JJScreen.percentWidth(lMarginRightPercentScWidth)
        if(lMarginBottomPercentScWidth > 0f) mlpMargins.bottom = JJScreen.percentWidth(lMarginBottomPercentScWidth)


        if(lMarginTopResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginTopResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlpMargins.top = v
        }

        if(lMarginLeftResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginLeftResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlpMargins.left = v
        }

        if(lMarginRightResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginRightResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlpMargins.right = v
        }

        if(lMarginBottomResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginBottomResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlpMargins.bottom = v
        }


        if(lMarginTopResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginTopResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpMargins.top = v
        }

        if(lMarginLeftResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginLeftResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpMargins.left = v
        }

        if(lMarginRightResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginRightResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpMargins.right = v
        }

        if(lMarginBottomResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginBottomResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpMargins.bottom = v
        }

        if(lMarginTopResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginTopResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpMargins.top = v
        }

        if(lMarginLeftResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginLeftResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpMargins.left = v
        }

        if(lMarginRightResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginRightResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpMargins.right = v
        }

        if(lMarginBottomResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lMarginBottomResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpMargins.bottom = v
        }

        //endregion

        //region padding

        if(lPaddingPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(lPaddingPerScHeight)
            mlpPadding = JJPadding(mar,mar,mar,mar)
        }
        if(lPaddingPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(lPaddingPerScWidth)
            mlpPadding = JJPadding(mar,mar,mar,mar)
        }

        if(lPaddingResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lPaddingResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlpPadding = JJPadding(mar,mar,mar,mar)
        }

        if(lPaddingResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lPaddingResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpPadding = JJPadding(mar,mar,mar,mar)
        }

        if(lPaddingResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lPaddingResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpPadding = JJPadding(mar,mar,mar,mar)
        }


        //endregion

        //region padding vertical

        if(lPaddingVerticalPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(lPaddingVerticalPerScHeight)
            mlpPadding.top = mar ; mlpPadding.bottom = mar
        }
        if(lPaddingVerticalPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(lMarginVerticalPerScWidth)
            mlpPadding.top = mar ; mlpPadding.bottom = mar
        }

        if(lPaddingVerticalResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lPaddingVerticalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlpPadding.top = mar ; mlpPadding.bottom = mar
        }

        if(lPaddingVerticalResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lPaddingVerticalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpPadding.top = mar ; mlpPadding.bottom = mar
        }

        if(lPaddingVerticalResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lPaddingVerticalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpPadding.top = mar ; mlpPadding.bottom = mar
        }
        //endregion

        //region Horizontal padding

        if(lPaddingHorizontalPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(lPaddingHorizontalPerScHeight)
            mlpPadding.left = mar ; mlpPadding.right = mar
        }
        if(lPaddingHorizontalPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(lPaddingHorizontalPerScWidth)
            mlpPadding.left = mar ; mlpPadding.right = mar
        }

        if(lPaddingHorizontalResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lPaddingHorizontalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlpPadding.left = mar ; mlpPadding.right = mar
        }

        if(lPaddingHorizontalResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lPaddingHorizontalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpPadding.left = mar ; mlpPadding.right = mar
        }

        if(lPaddingHorizontalResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(lPaddingHorizontalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpPadding.left = mar ; mlpPadding.right = mar
        }

        //endregion

        //region padding start end top bottom

        if(lPaddingTopPercentScHeight > 0f)  mlpPadding.top = JJScreen.percentHeight(lPaddingTopPercentScHeight)
        if(lPaddingLeftPercentScHeight > 0f)  mlpPadding.left = JJScreen.percentHeight(lPaddingLeftPercentScHeight)
        if(lPaddingRightPercentScHeight > 0f) mlpPadding.right = JJScreen.percentHeight(lPaddingRightPercentScHeight)
        if(lPaddingBottomPercentScHeight > 0f) mlpPadding.bottom = JJScreen.percentHeight(lPaddingBottomPercentScHeight)

        if(lPaddingTopPercentScWidth > 0f)  mlpPadding.top = JJScreen.percentWidth(lPaddingTopPercentScWidth)
        if(lPaddingLeftPercentScWidth > 0f)  mlpPadding.left = JJScreen.percentWidth(lPaddingLeftPercentScWidth)
        if(lPaddingRightPercentScWidth > 0f) mlpPadding.right = JJScreen.percentWidth(lPaddingRightPercentScWidth)
        if(lPaddingBottomPercentScWidth > 0f) mlpPadding.bottom = JJScreen.percentWidth(lPaddingBottomPercentScWidth)


        if(lPaddingTopResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingTopResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlpPadding.top = v
        }

        if(lPaddingLeftResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingLeftResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlpPadding.left = v
        }

        if(lPaddingRightResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingRightResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlpPadding.right = v
        }

        if(lPaddingBottomResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingBottomResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlpPadding.bottom = v
        }


        if(lPaddingTopResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingTopResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpPadding.top = v
        }

        if(lPaddingLeftResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingLeftResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpPadding.left = v
        }

        if(lPaddingRightResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingRightResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpPadding.right = v
        }

        if(lPaddingBottomResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingBottomResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpPadding.bottom = v
        }

        if(lPaddingTopResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingTopResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpPadding.top = v
        }

        if(lPaddingLeftResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingLeftResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpPadding.left = v
        }

        if(lPaddingRightResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingRightResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpPadding.right = v
        }

        if(lPaddingBottomResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lPaddingBottomResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpPadding.bottom = v
        }

        //endregion

        //region params layout height width

        mlpHeight = attrHeight
        mlpWidth = attrWidth

        if(lHeightPercentScreenWidth > 0f)  mlpHeight = JJScreen.percentWidth(lHeightPercentScreenWidth)
        if(lHeightPercentScreenHeight > 0f)  mlpHeight = JJScreen.percentHeight(lHeightPercentScreenHeight)
        if(lWidthPercentScreenWidth > 0f) mlpWidth = JJScreen.percentWidth(lWidthPercentScreenWidth)
        if(lWidthPercentScreenHeight > 0f) mlpWidth = JJScreen.percentHeight(lWidthPercentScreenHeight)

        if(lHeightResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lHeightResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val h = JJScreen.responsiveSize(first,second,three,four)
            mlpHeight = h
        }

        if(lWidthResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lWidthResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val w = JJScreen.responsiveSize(first,second,three,four)
            mlpWidth = w
        }


        if(lHeightResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lHeightResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val h = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlpHeight = h
        }

        if(lWidthResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lWidthResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val w = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)

            mlpWidth = w
        }

        if(lHeightResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lHeightResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val h = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlpHeight = h
        }

        if(lWidthResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(lWidthResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val w = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)

            mlpWidth = w
        }

        //endregion

        //endregion

        //region constraint Layout attr

        //region margin
        var margins = JJMargin()

        if(aMargin > 0f) margins = JJMargin(aMargin.toInt(),aMargin.toInt(),aMargin.toInt(),aMargin.toInt())

        if(aMarginPercentHeight > 0f) {
            val mar = JJScreen.percentHeight(aMarginPercentHeight)
            margins = JJMargin(mar,mar,mar,mar)
        }

        if(aMarginPercentWidth > 0f) {
            val mar = JJScreen.percentWidth(aMarginPercentWidth)
            margins = JJMargin(mar,mar,mar,mar)
        }

        if(aMarginResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            margins = JJMargin(v,v,v,v)
        }

        if(aMarginResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            margins = JJMargin(v,v,v,v)
        }

        if(aMarginResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            margins = JJMargin(v,v,v,v)
        }

        //endregion

        //region margin Vertical

        if(aMarginVertical > 0f) { margins.top = aMarginVertical.toInt() ; margins.bottom = aMarginVertical.toInt() }

        if(aMarginVerticalPercentHeight > 0f) {
            val mar = JJScreen.percentHeight(aMarginVerticalPercentHeight)
            margins.top = mar ; margins.bottom = mar
        }

        if(aMarginVerticalPercentWidth > 0f) {
            val mar = JJScreen.percentWidth(aMarginVerticalPercentWidth)
            margins.top = mar ; margins.bottom = mar
        }

        if(aMarginVerticalResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginVerticalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            margins.top = mar ; margins.bottom = mar
        }

        if(aMarginVerticalResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginVerticalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            margins.top = mar ; margins.bottom = mar
        }

        if(aMarginVerticalResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginVerticalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            margins.top = mar ; margins.bottom = mar
        }

        //endregion

        // region margin Horizontal

        if(aMarginHorizontal > 0f) { margins.left = aMarginHorizontal.toInt() ; margins.right = aMarginHorizontal.toInt() }

        if(aMarginHorizontalPercentHeight > 0f) {
            val mar = JJScreen.percentHeight(aMarginHorizontalPercentHeight)
            margins.left = mar ; margins.right = mar
        }

        if(aMarginHorizontalPercentWidth > 0f) {
            val mar = JJScreen.percentWidth(aMarginHorizontalPercentWidth)
            margins.left = mar ; margins.right = mar
        }

        if(aMarginHorizontalResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginHorizontalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            margins.left = mar ; margins.right = mar
        }

        if(aMarginHorizontalResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginHorizontalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            margins.left = mar ; margins.right = mar
        }

        if(aMarginHorizontalResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginHorizontalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            margins.left = mar ; margins.right = mar
        }

        //endregion

        //region margin start end top bottom

        if(aMarginStart > 0f)   margins.left = aMarginStart.toInt()
        if(aMarginEnd > 0f)   margins.right = aMarginEnd.toInt()
        if(aMarginTop > 0f)   margins.top = aMarginTop.toInt()
        if(aMarginBottom > 0f)   margins.bottom = aMarginBottom.toInt()

        if(aMarginStartPercent > 0f) margins.left = JJScreen.percentHeight(aMarginStartPercent)
        if(aMarginTopPercent > 0f)  margins.top = JJScreen.percentHeight(aMarginTopPercent)
        if(aMarginEndPercent > 0f)  margins.right = JJScreen.percentHeight(aMarginEndPercent)
        if(aMarginBottomPercent > 0f)  margins.bottom = JJScreen.percentHeight(aMarginBottomPercent)

        if(aMarginStartPercentWidth > 0f)  margins.left = JJScreen.percentWidth(aMarginStartPercentWidth)
        if(aMarginTopPercentWidth > 0f) margins.top = JJScreen.percentWidth(aMarginTopPercentWidth)
        if(aMarginEndPercentWidth > 0f) margins.right = JJScreen.percentWidth(aMarginEndPercentWidth)
        if(aMarginBottomPercentWidth > 0f) margins.bottom = JJScreen.percentWidth(aMarginBottomPercentWidth)

        if(aMarginStartResponsive > 0f)  {
            val arrayDimen = resources.obtainTypedArray(aMarginStartResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            margins.left = mar
        }
        if(aMarginEndResponsive > 0f)   {
            val arrayDimen = resources.obtainTypedArray(aMarginEndResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            margins.right = mar
        }
        if(aMarginTopResponsive > 0f)  {
            val arrayDimen = resources.obtainTypedArray(aMarginTopResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            margins.top = mar
        }
        if(aMarginBottomResponsive > 0f)  {
            val arrayDimen = resources.obtainTypedArray(aMarginBottomResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            margins.bottom = mar
        }

        if(aMarginStartResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginStartResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            margins.left = mar
        }

        if(aMarginEndResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginEndResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            margins.right = mar
        }

        if(aMarginTopResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginTopResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            margins.top = mar
        }

        if(aMarginBottomResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginBottomResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            margins.bottom = mar
        }

        if(aMarginStartResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginStartResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            margins.left = mar
        }

        if(aMarginEndResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginEndResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            margins.right = mar
        }

        if(aMarginTopResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginTopResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            margins.top = mar
        }

        if(aMarginBottomResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aMarginBottomResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            margins.bottom = mar
        }


        //endregion

        //region width height

        if(attrWidth > 0 || attrWidth == -2) clWidth(attrWidth)
        if(attrHeight > 0 || attrHeight == -2) clHeight(attrHeight)

        if(aHeightPercent > 0f) clPercentHeight(aHeightPercent)

        if(aHeightPercentScreenWidth > 0f) clHeight(JJScreen.percentWidth(aHeightPercentScreenWidth))
        if(aHeightPercentScreenHeight > 0f) clHeight(JJScreen.percentHeight(aHeightPercentScreenHeight))

        if(aWidthPercent > 0f) clPercentWidth(aWidthPercent)
        if(aWidthPercentScreenWidth > 0f) clWidth(JJScreen.percentWidth(aWidthPercentScreenWidth))
        if(aWidthPercentScreenHeight > 0f) clWidth(JJScreen.percentHeight(aWidthPercentScreenHeight))

        if(aHeightResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aHeightResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val h = JJScreen.responsiveSize(first,second,three,four)
            clHeight(h)
        }

        if(aWidthResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aWidthResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val w = JJScreen.responsiveSize(first,second,three,four)

            clWidth(w)
        }

        if(aHeightResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aHeightResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val h = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            clHeight(h)
        }

        if(aWidthResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aWidthResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val w = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)

            clWidth(w)
        }

        if(aHeightResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aHeightResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val h = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            clHeight(h)
        }

        if(aWidthResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(aWidthResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val w = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)

            clWidth(w)
        }

        //endregion

        //region anchors

        if(aStartToStartOf != View.NO_ID) clStartToStart(aStartToStartOf)
        if(aStartToEndOf != View.NO_ID) clStartToEnd(aStartToEndOf)
        if(aEndToEndOf != View.NO_ID) clEndToEnd(aEndToEndOf)
        if(aEndToStartOf != View.NO_ID) clEndToStart(aEndToStartOf)
        if(aTopToTopOf != View.NO_ID) clTopToTop(aTopToTopOf)
        if(aTopToBottomOf != View.NO_ID) clTopToBottom(aTopToBottomOf)
        if(aBottomToBottomOf != View.NO_ID) clBottomToBottom(aBottomToBottomOf)
        if(aBottomToTopOf != View.NO_ID) clBottomToTop(aBottomToTopOf)

        if(aStartToStartParent) clStartToStartParent()
        if(aStartToEndParent) clStartToEndParent()
        if(aEndToEndParent) clEndToEndParent()
        if(aEndToStartParent) clEndToStartParent()
        if(aTopToTopParent) clTopToTopParent()
        if(aTopToBottomParent) clTopToBottomParent()
        if(aBottomToBottomParent) clBottomToBottomParent()
        if(aBottomToTopParent) clBottomToTopParent()


        if(aCenterInParentTopVertical) clCenterInParentTopVertically()
        if(aCenterInParentBottomVertical) clCenterInParentBottomVertically()
        if(aCenterInParentStartHorizontal) clCenterInParentStartHorizontally()
        if(aCenterInParentEndHorizontal) clCenterInParentEndHorizontally()

        if(aCenterInTopVerticalOf != View.NO_ID) clCenterInTopVertically(aCenterInTopVerticalOf)
        if(aCenterInBottomVerticalOf != View.NO_ID) clCenterInBottomVertically(aCenterInBottomVerticalOf)
        if(aCenterInStartHorizontalOf != View.NO_ID) clCenterInStartHorizontally(aCenterInStartHorizontalOf)
        if(aCenterInEndHorizontalOf != View.NO_ID) clCenterInEndHorizontally(aCenterInEndHorizontalOf)

        if(aCenterVerticalOf != View.NO_ID) clCenterVerticallyOf(aCenterVerticalOf)
        if(aCenterHorizontalOf != View.NO_ID) clCenterHorizontallyOf(aCenterHorizontalOf)

        if(aCenterInParentHorizontal) clCenterInParentHorizontally()
        if(aCenterInParentVertical)  clCenterInParentVertically()

        if(aFillParentHorizontal) clFillParentHorizontally()
        if(aFillParentVertical) clFillParentVertically()

        if(aCenterInParent) clCenterInParent()
        if(aFillParent) clFillParent()

        if(aVerticalBias > 0f)  clVerticalBias(aVerticalBias)
        if(aHorizontalBias > 0f)  clHorizontalBias(aHorizontalBias)

        clMargins(margins)

        //endregion

        //endregion

        //region standard landscape

        //region margin


        if(llsMargin > 0f) {
            mlsMargins.top =  llsMargin.toInt()
            mlsMargins.left =  llsMargin.toInt()
            mlsMargins.right =  llsMargin.toInt()
            mlsMargins.bottom =  llsMargin.toInt()
        }

        if(llsMarginPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(llsMarginPerScHeight)
            mlsMargins = JJMargin(mar,mar,mar,mar)
        }
        if(llsMarginPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(llsMarginPerScWidth)
            mlsMargins = JJMargin(mar,mar,mar,mar)
        }

        if(llsMarginResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsMarginResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlsMargins = JJMargin(mar,mar,mar,mar)
        }

        if(llsMarginResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsMarginResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsMargins = JJMargin(mar,mar,mar,mar)
        }

        if(llsMarginResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsMarginResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsMargins = JJMargin(mar,mar,mar,mar)
        }


//endregion

        //region margin vertical

        if(llsMarginVertical > 0f){
            mlsMargins.top = llsMarginVertical.toInt()
            mlsMargins.bottom = llsMarginVertical.toInt()
        }

        if(llsMarginVerticalPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(llsMarginVerticalPerScHeight)
            mlsMargins.top = mar ; mlsMargins.bottom = mar
        }
        if(llsMarginVerticalPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(llsMarginVerticalPerScWidth)
            mlsMargins.top = mar ; mlsMargins.bottom = mar
        }

        if(llsMarginVerticalResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsMarginVerticalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlsMargins.top = mar ; mlsMargins.bottom = mar
        }

        if(llsMarginVerticalResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsMarginVerticalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsMargins.top = mar ; mlsMargins.bottom = mar
        }

        if(llsMarginVerticalResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsMarginVerticalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsMargins.top = mar ; mlsMargins.bottom = mar
        }
//endregion

        //region Horizontal margin

        if(llsMarginHorizontal > 0f){
            mlsMargins.left = llsMarginHorizontal.toInt()
            mlsMargins.right = llsMarginHorizontal.toInt()
        }

        if(llsMarginHorizontalPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(llsMarginHorizontalPerScHeight)
            mlsMargins.left = mar ; mlsMargins.right = mar
        }
        if(llsMarginHorizontalPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(llsMarginHorizontalPerScWidth)
            mlsMargins.left = mar ; mlsMargins.right = mar
        }

        if(llsMarginHorizontalResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsMarginHorizontalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlsMargins.left = mar ; mlsMargins.right = mar
        }

        if(llsMarginHorizontalResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsMarginHorizontalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsMargins.left = mar ; mlsMargins.right = mar
        }

        if(llsMarginHorizontalResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsMarginHorizontalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsMargins.left = mar ; mlsMargins.right = mar
        }

//endregion

        //region margin start end top bottom

        if(llsMarginTop > 0f) mlsMargins.top = llsMarginTop.toInt()
        if(llsMarginBottom > 0f) mlsMargins.bottom = llsMarginBottom.toInt()
        if(llsMarginEnd > 0f) mlsMargins.right = llsMarginEnd.toInt()
        if(llsMarginStart > 0f) mlsMargins.left = llsMarginStart.toInt()

        if(llsMarginTopPercentScHeight > 0f)  mlsMargins.top = JJScreen.percentHeight(llsMarginTopPercentScHeight)
        if(llsMarginLeftPercentScHeight > 0f)  mlsMargins.left = JJScreen.percentHeight(llsMarginLeftPercentScHeight)
        if(llsMarginRightPercentScHeight > 0f) mlsMargins.right = JJScreen.percentHeight(llsMarginRightPercentScHeight)
        if(llsMarginBottomPercentScHeight > 0f) mlsMargins.bottom = JJScreen.percentHeight(llsMarginBottomPercentScHeight)

        if(llsMarginTopPercentScWidth > 0f)  mlsMargins.top = JJScreen.percentWidth(llsMarginTopPercentScWidth)
        if(llsMarginLeftPercentScWidth > 0f)  mlsMargins.left = JJScreen.percentWidth(llsMarginLeftPercentScWidth)
        if(llsMarginRightPercentScWidth > 0f) mlsMargins.right = JJScreen.percentWidth(llsMarginRightPercentScWidth)
        if(llsMarginBottomPercentScWidth > 0f) mlsMargins.bottom = JJScreen.percentWidth(llsMarginBottomPercentScWidth)


        if(llsMarginTopResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginTopResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlsMargins.top = v
        }

        if(llsMarginLeftResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginLeftResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlsMargins.left = v
        }

        if(llsMarginRightResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginRightResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlsMargins.right = v
        }

        if(llsMarginBottomResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginBottomResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlsMargins.bottom = v
        }


        if(llsMarginTopResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginTopResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsMargins.top = v
        }

        if(llsMarginLeftResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginLeftResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsMargins.left = v
        }

        if(llsMarginRightResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginRightResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsMargins.right = v
        }

        if(llsMarginBottomResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginBottomResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsMargins.bottom = v
        }

        if(llsMarginTopResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginTopResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsMargins.top = v
        }

        if(llsMarginLeftResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginLeftResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsMargins.left = v
        }

        if(llsMarginRightResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginRightResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsMargins.right = v
        }

        if(llsMarginBottomResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsMarginBottomResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsMargins.bottom = v
        }

//endregion

        //region padding

        if(llsPadding > 0f) {
            mlsPadding.top = llsPadding.toInt()
            mlsPadding.left = llsPadding.toInt()
            mlsPadding.right = llsPadding.toInt()
            mlsPadding.bottom = llsPadding.toInt()
        }


        if(llsPaddingPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(llsPaddingPerScHeight)
            mlsPadding = JJPadding(mar,mar,mar,mar)
        }
        if(llsPaddingPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(llsPaddingPerScWidth)
            mlsPadding = JJPadding(mar,mar,mar,mar)
        }

        if(llsPaddingResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsPaddingResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlsPadding = JJPadding(mar,mar,mar,mar)
        }

        if(llsPaddingResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsPaddingResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsPadding = JJPadding(mar,mar,mar,mar)
        }

        if(llsPaddingResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsPaddingResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsPadding = JJPadding(mar,mar,mar,mar)
        }


//endregion

        //region padding vertical

        if(llsPaddingVertical > 0f){
            mlsPadding.top = llsPaddingVertical.toInt()
            mlsPadding.bottom = llsPaddingVertical.toInt()
        }

        if(llsPaddingVerticalPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(llsPaddingVerticalPerScHeight)
            mlsPadding.top = mar ; mlsPadding.bottom = mar
        }
        if(llsPaddingVerticalPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(lMarginVerticalPerScWidth)
            mlsPadding.top = mar ; mlsPadding.bottom = mar
        }

        if(llsPaddingVerticalResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsPaddingVerticalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlsPadding.top = mar ; mlsPadding.bottom = mar
        }

        if(llsPaddingVerticalResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsPaddingVerticalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsPadding.top = mar ; mlsPadding.bottom = mar
        }

        if(llsPaddingVerticalResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsPaddingVerticalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsPadding.top = mar ; mlsPadding.bottom = mar
        }
//endregion

        //region Horizontal padding

        if(llsPaddingHorizontal > 0f){
            mlsPadding.left = llsPaddingHorizontal.toInt()
            mlsPadding.right = llsPaddingHorizontal.toInt()
        }

        if(llsPaddingHorizontalPerScHeight > 0f) {
            val mar = JJScreen.percentHeight(llsPaddingHorizontalPerScHeight)
            mlsPadding.left = mar ; mlsPadding.right = mar
        }
        if(llsPaddingHorizontalPerScWidth > 0f) {
            val mar = JJScreen.percentWidth(llsPaddingHorizontalPerScWidth)
            mlsPadding.left = mar ; mlsPadding.right = mar
        }

        if(llsPaddingHorizontalResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsPaddingHorizontalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            mlsPadding.left = mar ; mlsPadding.right = mar
        }

        if(llsPaddingHorizontalResPerScHeight != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsPaddingHorizontalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsPadding.left = mar ; mlsPadding.right = mar
        }

        if(llsPaddingHorizontalResPerScWidth != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(llsPaddingHorizontalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsPadding.left = mar ; mlsPadding.right = mar
        }

//endregion

        //region padding start end top bottom

        if(llsPaddingStart > 0f) mlsPadding.left = llsPaddingStart.toInt()
        if(llsPaddingTop > 0f) mlsPadding.top = llsPaddingTop.toInt()
        if(llsPaddingEnd > 0f) mlsPadding.right = llsPaddingEnd.toInt()
        if(llsPaddingBottom > 0f) mlsPadding.bottom = llsPaddingBottom.toInt()

        if(llsPaddingTopPercentScHeight > 0f)  mlsPadding.top = JJScreen.percentHeight(llsPaddingTopPercentScHeight)
        if(llsPaddingLeftPercentScHeight > 0f)  mlsPadding.left = JJScreen.percentHeight(llsPaddingLeftPercentScHeight)
        if(llsPaddingRightPercentScHeight > 0f) mlsPadding.right = JJScreen.percentHeight(llsPaddingRightPercentScHeight)
        if(llsPaddingBottomPercentScHeight > 0f) mlsPadding.bottom = JJScreen.percentHeight(llsPaddingBottomPercentScHeight)

        if(llsPaddingTopPercentScWidth > 0f)  mlsPadding.top = JJScreen.percentWidth(llsPaddingTopPercentScWidth)
        if(llsPaddingLeftPercentScWidth > 0f)  mlsPadding.left = JJScreen.percentWidth(llsPaddingLeftPercentScWidth)
        if(llsPaddingRightPercentScWidth > 0f) mlsPadding.right = JJScreen.percentWidth(llsPaddingRightPercentScWidth)
        if(llsPaddingBottomPercentScWidth > 0f) mlsPadding.bottom = JJScreen.percentWidth(llsPaddingBottomPercentScWidth)


        if(llsPaddingTopResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingTopResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlsPadding.top = v
        }

        if(llsPaddingLeftResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingLeftResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlsPadding.left = v
        }

        if(llsPaddingRightResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingRightResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlsPadding.right = v
        }

        if(llsPaddingBottomResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingBottomResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            mlsPadding.bottom = v
        }


        if(llsPaddingTopResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingTopResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsPadding.top = v
        }

        if(llsPaddingLeftResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingLeftResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsPadding.left = v
        }

        if(llsPaddingRightResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingRightResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsPadding.right = v
        }

        if(llsPaddingBottomResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingBottomResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsPadding.bottom = v
        }

        if(llsPaddingTopResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingTopResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsPadding.top = v
        }

        if(llsPaddingLeftResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingLeftResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsPadding.left = v
        }

        if(llsPaddingRightResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingRightResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsPadding.right = v
        }

        if(llsPaddingBottomResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsPaddingBottomResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsPadding.bottom = v
        }

//endregion

        //region params layout height width

        mlsHeight = attrHeightLs
        mlsWidth = attrWidthLs

        if(llsHeightPercentScreenWidth > 0f)  mlsHeight = JJScreen.percentWidth(llsHeightPercentScreenWidth)
        if(llsHeightPercentScreenHeight > 0f)  mlsHeight = JJScreen.percentHeight(llsHeightPercentScreenHeight)
        if(llsWidthPercentScreenWidth > 0f) mlsWidth = JJScreen.percentWidth(llsWidthPercentScreenWidth)
        if(llsWidthPercentScreenHeight > 0f) mlsWidth = JJScreen.percentHeight(llsWidthPercentScreenHeight)

        if(llsHeightResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsHeightResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val h = JJScreen.responsiveSize(first,second,three,four)
            mlsHeight = h
        }

        if(llsWidthResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsWidthResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val w = JJScreen.responsiveSize(first,second,three,four)
            mlsWidth = w
        }


        if(llsHeightResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsHeightResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val h = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            mlsHeight = h
        }

        if(llsWidthResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsWidthResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val w = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)

            mlsWidth = w
        }

        if(llsHeightResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsHeightResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val h = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            mlsHeight = h
        }

        if(llsWidthResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(llsWidthResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val w = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)

            mlsWidth = w
        }

        //endregion

        //endregion

        //region constraint Layout landscape

        //region margin
        var lsMargins = JJMargin()

        if(alsMargin > 0f) lsMargins = JJMargin(alsMargin.toInt(),alsMargin.toInt(),alsMargin.toInt(),alsMargin.toInt())

        if(alsMarginPercentHeight > 0f) {
            val mar = JJScreen.percentHeight(alsMarginPercentHeight)
            lsMargins = JJMargin(mar,mar,mar,mar)
        }

        if(alsMarginPercentWidth > 0f) {
            val mar = JJScreen.percentWidth(alsMarginPercentWidth)
            lsMargins = JJMargin(mar,mar,mar,mar)
        }

        if(alsMarginResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val v = JJScreen.responsiveSize(first,second,three,four)
            lsMargins = JJMargin(v,v,v,v)
        }

        if(alsMarginResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            lsMargins = JJMargin(v,v,v,v)
        }

        if(alsMarginResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val v = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            lsMargins = JJMargin(v,v,v,v)
        }

        //endregion

        //region margin Vertical

        if(alsMarginVertical > 0f) { lsMargins.top = alsMarginVertical.toInt() ; lsMargins.bottom = alsMarginVertical.toInt() }

        if(alsMarginVerticalPercentHeight > 0f) {
            val mar = JJScreen.percentHeight(alsMarginVerticalPercentHeight)
            lsMargins.top = mar ; lsMargins.bottom = mar
        }

        if(alsMarginVerticalPercentWidth > 0f) {
            val mar = JJScreen.percentWidth(alsMarginVerticalPercentWidth)
            lsMargins.top = mar ; lsMargins.bottom = mar
        }

        if(alsMarginVerticalResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginVerticalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            lsMargins.top = mar ; lsMargins.bottom = mar
        }

        if(alsMarginVerticalResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginVerticalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            lsMargins.top = mar ; lsMargins.bottom = mar
        }

        if(alsMarginVerticalResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginVerticalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            lsMargins.top = mar ; lsMargins.bottom = mar
        }

        //endregion

        // region margin Horizontal

        if(alsMarginHorizontal > 0f) { lsMargins.left = alsMarginHorizontal.toInt() ; lsMargins.right = alsMarginHorizontal.toInt() }

        if(alsMarginHorizontalPercentHeight > 0f) {
            val mar = JJScreen.percentHeight(alsMarginHorizontalPercentHeight)
            lsMargins.left = mar ; lsMargins.right = mar
        }

        if(alsMarginHorizontalPercentWidth > 0f) {
            val mar = JJScreen.percentWidth(alsMarginHorizontalPercentWidth)
            lsMargins.left = mar ; lsMargins.right = mar
        }

        if(alsMarginHorizontalResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginHorizontalResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            lsMargins.left = mar ; lsMargins.right = mar
        }

        if(alsMarginHorizontalResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginHorizontalResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            lsMargins.left = mar ; lsMargins.right = mar
        }

        if(alsMarginHorizontalResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginHorizontalResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            lsMargins.left = mar ; lsMargins.right = mar
        }

        //endregion

        //region margin start end top bottom

        if(alsMarginStart > 0f)   lsMargins.left = alsMarginStart.toInt()
        if(alsMarginEnd > 0f)   lsMargins.right = alsMarginEnd.toInt()
        if(alsMarginTop > 0f)   lsMargins.top = alsMarginTop.toInt()
        if(alsMarginBottom > 0f)   lsMargins.bottom = alsMarginBottom.toInt()

        if(alsMarginStartPercent > 0f) lsMargins.left = JJScreen.percentHeight(alsMarginStartPercent)
        if(alsMarginTopPercent > 0f)  lsMargins.top = JJScreen.percentHeight(alsMarginTopPercent)
        if(alsMarginEndPercent > 0f)  lsMargins.right = JJScreen.percentHeight(alsMarginEndPercent)
        if(alsMarginBottomPercent > 0f)  lsMargins.bottom = JJScreen.percentHeight(alsMarginBottomPercent)

        if(alsMarginStartPercentWidth > 0f)  lsMargins.left = JJScreen.percentWidth(alsMarginStartPercentWidth)
        if(alsMarginTopPercentWidth > 0f) lsMargins.top = JJScreen.percentWidth(alsMarginTopPercentWidth)
        if(alsMarginEndPercentWidth > 0f) lsMargins.right = JJScreen.percentWidth(alsMarginEndPercentWidth)
        if(alsMarginBottomPercentWidth > 0f) lsMargins.bottom = JJScreen.percentWidth(alsMarginBottomPercentWidth)

        if(alsMarginStartResponsive > 0f)  {
            val arrayDimen = resources.obtainTypedArray(alsMarginStartResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            lsMargins.left = mar
        }
        if(alsMarginEndResponsive > 0f)   {
            val arrayDimen = resources.obtainTypedArray(alsMarginEndResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            lsMargins.right = mar
        }
        if(alsMarginTopResponsive > 0f)  {
            val arrayDimen = resources.obtainTypedArray(alsMarginTopResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            lsMargins.top = mar
        }
        if(alsMarginBottomResponsive > 0f)  {
            val arrayDimen = resources.obtainTypedArray(alsMarginBottomResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSize(first,second,three,four)
            lsMargins.bottom = mar
        }

        if(alsMarginStartResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginStartResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            lsMargins.left = mar
        }

        if(alsMarginEndResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginEndResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            lsMargins.right = mar
        }

        if(alsMarginTopResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginTopResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            lsMargins.top = mar
        }

        if(alsMarginBottomResPerScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginBottomResPerScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            lsMargins.bottom = mar
        }

        if(alsMarginStartResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginStartResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            lsMargins.left = mar
        }

        if(alsMarginEndResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginEndResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            lsMargins.right = mar
        }

        if(alsMarginTopResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginTopResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            lsMargins.top = mar
        }

        if(alsMarginBottomResPerScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsMarginBottomResPerScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val mar = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            lsMargins.bottom = mar
        }


        //endregion

        //region width height

        if(attrWidthLs > 0 || attrWidthLs == -2 ) cllWidth(attrWidthLs)
        if(attrHeightLs > 0 || attrHeightLs == -2 ) cllHeight(attrHeightLs)

        if( alsHeightPercent > 0f) cllPercentHeight( alsHeightPercent)

        if( alsHeightPercentScreenWidth > 0f) cllHeight(JJScreen.percentWidth( alsHeightPercentScreenWidth))
        if( alsHeightPercentScreenHeight > 0f) cllHeight(JJScreen.percentHeight( alsHeightPercentScreenHeight))

        if(alsWidthPercent > 0f) cllPercentWidth(alsWidthPercent)
        if(alsWidthPercentScreenWidth > 0f) cllWidth(JJScreen.percentWidth(alsWidthPercentScreenWidth))
        if(alsWidthPercentScreenHeight > 0f) cllWidth(JJScreen.percentHeight(alsWidthPercentScreenHeight))

        if( alsHeightResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray( alsHeightResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val h = JJScreen.responsiveSize(first,second,three,four)
            cllHeight(h)
        }

        if(alsWidthResponsive != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsWidthResponsive)
            val first = arrayDimen.getDimension(0, 0f).toInt()
            val second = arrayDimen.getDimension(1, 0f).toInt()
            val three = arrayDimen.getDimension(2, 0f).toInt()
            val four = arrayDimen.getDimension(3, 0f).toInt()
            arrayDimen.recycle()

            val w = JJScreen.responsiveSize(first,second,three,four)

            cllWidth(w)
        }

        if( alsHeightResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray( alsHeightResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val h = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)
            cllHeight(h)
        }

        if(alsWidthResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsWidthResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val w = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)

            cllWidth(w)
        }

        if( alsHeightResponsivePercentScWidth != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray( alsHeightResponsivePercentScWidth)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val h = JJScreen.responsiveSizePercentScreenWidth(first,second,three,four)
            cllHeight(h)
        }

        if(alsWidthResponsivePercentScHeight != View.NO_ID){
            val arrayDimen = resources.obtainTypedArray(alsWidthResponsivePercentScHeight)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

            val w = JJScreen.responsiveSizePercentScreenHeight(first,second,three,four)

            cllWidth(w)
        }

        //endregion

        //region anchors

        if(alsStartToStartOf != View.NO_ID) cllStartToStart(alsStartToStartOf)
        if(alsStartToEndOf != View.NO_ID) cllStartToEnd(alsStartToEndOf)
        if(alsEndToEndOf != View.NO_ID) cllEndToEnd(alsEndToEndOf)
        if(alsEndToStartOf != View.NO_ID) cllEndToStart(alsEndToStartOf)
        if(alsTopToTopOf != View.NO_ID) cllTopToTop(alsTopToTopOf)
        if(alsTopToBottomOf != View.NO_ID) cllTopToBottom(alsTopToBottomOf)
        if(alsBottomToBottomOf != View.NO_ID) cllBottomToBottom(alsBottomToBottomOf)
        if(alsBottomToTopOf != View.NO_ID) cllBottomToTop(alsBottomToTopOf)

        if(alsStartToStartParent) cllStartToStartParent()
        if(alsStartToEndParent) cllStartToEndParent()
        if(alsEndToEndParent) cllEndToEndParent()
        if(alsEndToStartParent) cllEndToStartParent()
        if(alsTopToTopParent) cllTopToTopParent()
        if(alsTopToBottomParent) cllTopToBottomParent()
        if(alsBottomToBottomParent) cllBottomToBottomParent()
        if(alsBottomToTopParent) cllBottomToTopParent()


        if(alsCenterInParentTopVertical) cllCenterInParentTopVertically()
        if(alsCenterInParentBottomVertical) cllCenterInParentBottomVertically()
        if(alsCenterInParentStartHorizontal) cllCenterInParentStartHorizontally()
        if(alsCenterInParentEndHorizontal) cllCenterInParentEndHorizontally()

        if(alsCenterInTopVerticalOf != View.NO_ID) cllCenterInTopVertically(alsCenterInTopVerticalOf)
        if(alsCenterInBottomVerticalOf != View.NO_ID) cllCenterInBottomVertically(alsCenterInBottomVerticalOf)
        if(alsCenterInStartHorizontalOf != View.NO_ID) cllCenterInStartHorizontally(alsCenterInStartHorizontalOf)
        if(alsCenterInEndHorizontalOf != View.NO_ID) cllCenterInEndHorizontally(alsCenterInEndHorizontalOf)

        if(alsCenterVerticalOf != View.NO_ID) cllCenterVerticallyOf(alsCenterVerticalOf)
        if(alsCenterHorizontalOf != View.NO_ID) cllCenterHorizontallyOf(alsCenterHorizontalOf)

        if(alsCenterInParentHorizontal) cllCenterInParentHorizontally()
        if(alsCenterInParentVertical)  cllCenterInParentVertically()

        if(alsFillParentHorizontal) cllFillParentHorizontally()
        if(alsFillParentVertical) cllFillParentVertically()

        if(alsCenterInParent) cllCenterInParent()
        if(alsFillParent) cllFillParent()

        if(alsVerticalBias > 0f)  cllVerticalBias(alsVerticalBias)
        if(alsHorizontalBias > 0f)  cllHorizontalBias(alsHorizontalBias)

        cllMargins(lsMargins)

        //endregion

        //endregion
    }

    private var mInit = true
    private var mlsHeight = 0
    private var mlsWidth = 0
    private var mlsMargins = JJMargin()
    private var mlsPadding = JJPadding()
    private var mConfigurationChanged = false
    private var mlpHeight = 0
    private var mlpWidth = 0
    private var mlpMargins = JJMargin()
    private var mlpPadding = JJPadding()
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val isLandScale = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if(mInit){
            if(isLandScale && mSupportLandScape) applyLayoutParamsLandScape() else applyLayoutParamsPortrait()
            mInit = false
        }
    }
    private fun applyLayoutParamsPortrait(){
        val csParent = parent as? ConstraintLayout
        val mlParent = parent as? MotionLayout
        when {
            mlParent != null -> Log.e("JJKIT","PARENT MOTION LAYOUT")
            csParent != null -> {
                if(!mIgnoreCl){
                    clApply()
                }
            }
            else -> {
                layoutParams.height = mlpHeight
                layoutParams.width = mlpWidth
                val margin = layoutParams as? MarginLayoutParams
                margin?.topMargin = mlpMargins.top
                margin?.marginStart =  mlpMargins.left
                margin?.marginEnd =  mlpMargins.right
                margin?.bottomMargin =  mlpMargins.bottom

            }
        }

        var pl = paddingLeft
        var pr = paddingRight
        if (paddingStart > 0) pl = paddingStart
        if (paddingEnd > 0) pr = paddingEnd

        if(mlpPadding.top <= 0 && paddingTop > 0) mlpPadding.top = paddingTop
        if(mlpPadding.bottom <= 0 && paddingBottom > 0) mlpPadding.bottom = paddingBottom
        if(mlpPadding.left <= 0 && pl > 0) mlpPadding.left = pl
        if(mlpPadding.right <= 0 && pr > 0) mlpPadding.right = pr

        setPaddingRelative(mlpPadding.left,mlpPadding.top,mlpPadding.right,mlpPadding.bottom)
    }
    private fun applyLayoutParamsLandScape(){
        val csParent = parent as? ConstraintLayout
        val mlParent = parent as? MotionLayout
        when {
            mlParent != null -> Log.e("JJKIT", "PARENT MOTION LAYOUT")
            csParent != null -> {
                if (!mIgnoreCl) {
                    cllApply()
                }
            }
            else -> {
                layoutParams.height = mlsHeight
                layoutParams.width = mlsWidth
                val margin = layoutParams as? MarginLayoutParams
                margin?.topMargin = mlsMargins.top
                margin?.marginStart = mlsMargins.left
                margin?.marginEnd = mlsMargins.right
                margin?.bottomMargin = mlsMargins.bottom
            }
        }
        setPaddingRelative(mlsPadding.left,mlsPadding.top,mlsPadding.right,mlsPadding.bottom)
    }
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if(mConfigurationChanged){
            if (newConfig?.orientation == Configuration.ORIENTATION_LANDSCAPE && mSupportLandScape) {
                applyLayoutParamsLandScape()
            } else  {
                applyLayoutParamsPortrait()
            }
        }
    }

    //endregion

   
    @Suppress("DEPRECATION")
    private fun init(){
        mPaint.style = Paint.Style.FILL
        mPaint.color = mRippleColor
        mPaint.alpha = mRippleAlpha
        this.setWillNotDraw(false)
        isDrawingCacheEnabled = true
        isClickable = true
    }


    @Suppress("DEPRECATION")
    @SuppressLint("CustomViewStyleable", "ResourceType")
    private fun initAttrs(context: Context, attrs: AttributeSet) {
        if (isInEditMode) return
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.JJRippleWrapper)

        mRippleColor = typedArray.getColor(R.styleable.JJRippleWrapper_rw_color, Color.WHITE)
        mRippleType = typedArray.getInt(R.styleable.JJRippleWrapper_rw_type, 0)
        mHasToZoom = typedArray.getBoolean(R.styleable.JJRippleWrapper_rw_zoom, false)
        mIsCentered = typedArray.getBoolean(R.styleable.JJRippleWrapper_rw_centered, false)
        mRadiusMax = typedArray.getDimension(R.styleable.JJRippleWrapper_rw_maxRadius,0f)
        val radiusResponsive = typedArray.getResourceId(R.styleable.JJRippleWrapper_rw_maxRadiusResponsive,View.NO_ID)
        val radiusPerScHeight = typedArray.getFloat(R.styleable.JJRippleWrapper_rw_maxRadiusPercentScHeight,0f)
       val radiusPerScWidth  = typedArray.getFloat(R.styleable.JJRippleWrapper_rw_maxRadiusPercentScWidth,0f)
        mRippleDuration =
            typedArray.getInteger(R.styleable.JJRippleWrapper_rw_rippleDuration, mRippleDuration)
        mFrameRate = typedArray.getInteger(R.styleable.JJRippleWrapper_rw_frameRate, mFrameRate)
        mRippleAlpha = typedArray.getInteger(R.styleable.JJRippleWrapper_rw_alpha, mRippleAlpha)
        mZoomScale = typedArray.getFloat(R.styleable.JJRippleWrapper_rw_zoomScale, 1.03f)
        mZoomDuration = typedArray.getInt(R.styleable.JJRippleWrapper_rw_zoomDuration, 200)

        typedArray.recycle()

        mPaint.style = Paint.Style.FILL
        mPaint.color = mRippleColor
        mPaint.alpha = mRippleAlpha
        this.setWillNotDraw(false)

        if(radiusPerScHeight > 0f) {
            mRadiusMax = JJScreen.percentHeightFloat(radiusPerScHeight)
        }

        if(radiusPerScWidth > 0f) {
            mRadiusMax = JJScreen.percentWidthFloat(radiusPerScWidth)
        }

        if (radiusResponsive != View.NO_ID) {
            val arrayDimen = resources.obtainTypedArray(radiusResponsive)
            val first = arrayDimen.getFloat(0, 0f)
            val second = arrayDimen.getFloat(1, 0f)
            val three = arrayDimen.getFloat(2, 0f)
            val four = arrayDimen.getFloat(3, 0f)
            arrayDimen.recycle()

             mRadiusMax = JJScreen.responsiveSize(first, second, three, four)
        }

        isDrawingCacheEnabled = true
        isClickable = true
    }


    //region override
    private var mIsClipChildrenEnabled = false
    private var mPathClipChildren = Path()
    private var mIsClipInPathChildren = false
    private var mIsClipOutPathChildren = false
    private var mIsPathClosureClipChildren = false
    private var mClosurePathClipChildren : ((RectF, Path)->Unit)? = null
    private var mRectClip = RectF()
    @Suppress("DEPRECATION")
    override fun onDraw(canvas: Canvas?) {

        if(mIsClipChildrenEnabled) {

            mRectClip.setEmpty()
            mRectClip.right = width.toFloat()
            mRectClip.bottom = height.toFloat()
            mRectClip.left += mlpPadding.left
            mRectClip.top   += mlpPadding.top
            mRectClip.right -= mlpPadding.right
            mRectClip.bottom -= mlpPadding.bottom
            canvas?.save()
            if (mIsClipInPathChildren) {
                if (mIsPathClosureClipChildren) {
                    mPathClipChildren.reset()
                    mClosurePathClipChildren?.invoke(mRectClip, mPathClipChildren)
                }
                canvas?.clipPath(mPathClipChildren)
            }
            if (mIsClipOutPathChildren) {
                canvas?.restore()
                canvas?.save()
                if (mIsPathClosureClipChildren) {
                    mPathClipChildren.reset()
                    mClosurePathClipChildren?.invoke(mRectClip, mPathClipChildren)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) canvas?.clipOutPath(mPathClipChildren)
                else canvas?.clipPath(mPathClipChildren, Region.Op.DIFFERENCE)
            }

        }
        super.onDraw(canvas)

    }

    private var mIsClipAllEnabled = false
    private var mPathClipAll = Path()
    private var mIsClipInPathAll = false
    private var mIsClipOutPathAll = false
    private var mIsPathClosureClipAll = false
    private var mClosurePathClipAll : ((RectF, Path, JJPadding)->Unit)? = null
    @Suppress("DEPRECATION")
    override fun draw(canvas: Canvas) {
        //clip
        if(mIsClipAllEnabled) {

            mRectClip.setEmpty()

            mRectClip.right = width.toFloat()
            mRectClip.bottom = height.toFloat()

            canvas.save()
            if (mIsClipInPathAll) {
                if (mIsPathClosureClipAll) {
                    mPathClipAll.reset()
                    mClosurePathClipAll?.invoke(mRectClip, mPathClipAll,mlpPadding)
                }
                canvas.clipPath(mPathClipAll)
            }
            if (mIsClipOutPathAll) {
                canvas.restore()
                canvas.save()
                if (mIsPathClosureClipAll) {
                    mPathClipAll.reset()
                    mClosurePathClipAll?.invoke(mRectClip, mPathClipAll,mlpPadding)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) canvas.clipOutPath(mPathClipAll)
                else canvas.clipPath(mPathClipAll, Region.Op.DIFFERENCE)
            }

        }

        //all and riple
        super.draw(canvas)
        if (mAnimationRunning) {
            canvas.save()
            if (mRippleDuration <= mTimer * mFrameRate) {
                mAnimationRunning = false
                mTimer = 0
                durationEmpty = -1
                mTimerEmpty = 0
                // There is problem on Android M where canvas.restore() seems to be called automatically
                // For now, don't call canvas.restore() manually on Android M (API 23)
                if (Build.VERSION.SDK_INT != 23) {
                    canvas.restore()
                }
                invalidate()
                if (onCompletionListener != null) onCompletionListener!!.invoke(this)
                return
            } else mCanvasHandler.postDelayed(runnable, mFrameRate.toLong())
            if (mTimer == 0) canvas.save()
            canvas.drawCircle(
                mX,
                mY,
                mRadiusMax * (mTimer.toFloat() * mFrameRate / mRippleDuration),
                mPaint
            )
            mPaint.color = Color.parseColor("#ffff4444")
            if (mRippleType == 1 && mOriginBitmap != null && mTimer.toFloat() * mFrameRate / mRippleDuration > 0.4f) {
                if (durationEmpty == -1) durationEmpty = mRippleDuration - mTimer * mFrameRate
                mTimerEmpty++
                val tmpBitmap: Bitmap? =
                    getCircleBitmap((mRadiusMax * (mTimerEmpty.toFloat() * mFrameRate / durationEmpty)).toInt())
                if(tmpBitmap != null)  canvas.drawBitmap(tmpBitmap, 0f, 0f, mPaint)

                tmpBitmap?.recycle()
            }
            mPaint.color = mRippleColor
            if (mRippleType == 1) {
                if (mTimer.toFloat() * mFrameRate / mRippleDuration > 0.6f) mPaint.alpha =
                    (mRippleAlpha - mRippleAlpha * (mTimerEmpty.toFloat() * mFrameRate / durationEmpty)).toInt() else mPaint.alpha =
                    mRippleAlpha
            } else mPaint.alpha =
                (mRippleAlpha - mRippleAlpha * (mTimer.toFloat() * mFrameRate / mRippleDuration)).toInt()
            mTimer++
        }


    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            animateRipple(event)
        }
        return super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            animateRipple(event)
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWIDTH = w
        mHEIGHT = h
        mScaleAnimation = ScaleAnimation(
            1.0f, mZoomScale, 1.0f, mZoomScale,
            (w / 2).toFloat(),
            (h / 2).toFloat()
        )
        mScaleAnimation.duration = mZoomDuration.toLong()
        mScaleAnimation.repeatMode = Animation.REVERSE
        mScaleAnimation.repeatCount = 1

    }

    //region private
    @Suppress("DEPRECATION")
    private fun getSnapShootView(): Bitmap {
        val location = IntArray(2)
        var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try{
                getLocationInWindow(location)
                PixelCopy.request(
                    (context as Activity).window,
                    Rect(location[0], location[1], location[0] + width, location[1] + height),
                    bitmap,
                    { copyResult ->
                        if(copyResult== PixelCopy.SUCCESS) Log.v("JJRippleWrapper","SUCCESS Pixel copy")
                    },
                    Handler())
            } catch (e: Exception) {
                // PixelCopy may throw IllegalArgumentException, make sure to handle it
                e.printStackTrace()
            }
            return bitmap
        }
        bitmap = getDrawingCache(true)
        return bitmap
    }


    /**
     * Launch Ripple animation for the current view with a MotionEvent
     *
     * @param event MotionEvent registered by the Ripple gesture listener
     */
    private fun animateRipple(event: MotionEvent) {
        createAnimation(event.x, event.y)
    }

    /**
     * Launch Ripple animation for the current view centered at x and y position
     *
     * @param x Horizontal position of the ripple center
     * @param y Vertical position of the ripple center
     */
    private fun animateRipple(x: Float, y: Float) {
        createAnimation(x, y)
    }

    /**
     * Create Ripple animation centered at x, y
     *
     * @param x Horizontal position of the ripple center
     * @param y Vertical position of the ripple center
     */
    private  fun createAnimation(x: Float, y: Float) {
        val savedRadius = mRadiusMax
        if (isEnabled && !mAnimationRunning) {
            if (mHasToZoom) this.startAnimation(mScaleAnimation)

            if (mIsCentered || mRippleType == 1) {
                mX = measuredWidth / 2.toFloat()
                mY = measuredHeight / 2.toFloat()
            } else {
                mX = x
                mY = y
            }

            mRadiusMax = if(savedRadius > 0f) savedRadius else min(mWIDTH, mHEIGHT).toFloat()

            mAnimationRunning = true
            if (mRippleType == 1 && mOriginBitmap == null) mOriginBitmap = getSnapShootView()


            invalidate()
        }
    }

    private fun getCircleBitmap(radius: Int): Bitmap? {
        if(mOriginBitmap != null) {
            val output = Bitmap.createBitmap(
                mOriginBitmap!!.width,
                mOriginBitmap!!.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(output)
            val paint = Paint()
            val rect = Rect(
                (mX - radius).toInt(),
                (mY - radius).toInt(),
                (mX + radius).toInt(),
                (mY + radius).toInt()
            )
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawCircle(mX, mY, radius.toFloat(), paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(mOriginBitmap!!, rect, rect, paint)
            return output
        }
        return null
    }


    //endregion

    //endregion

    
    //region set and get

    fun setClipChildrenToPath(path: Path): JJRippleWrapper {
        mPathClipChildren = path
        mIsPathClosureClipChildren = false
        mIsClipInPathChildren = true
        mIsClipChildrenEnabled = true
        mIsClipOutPathChildren = false
        return this
    }

    fun setClipAllToPath(path: Path): JJRippleWrapper {
        mPathClipAll = path
        mIsPathClosureClipAll = false
        mIsClipInPathAll = true
        mIsClipAllEnabled = true
        mIsClipOutPathAll = false
        return this
    }


    fun setClipOutChildrenToPath(path: Path): JJRippleWrapper {
        mPathClipChildren = path
        mIsPathClosureClipChildren = false
        mIsClipOutPathChildren = true
        mIsClipChildrenEnabled = true
        mIsClipInPathChildren = false
        return this
    }


    fun setClipOutAllToPath(path: Path): JJRippleWrapper {
        mPathClipAll = path
        mIsPathClosureClipAll = false
        mIsClipOutPathAll = true
        mIsClipAllEnabled = true
        mIsClipInPathAll = false
        return this
    }

    fun setClipChildrenToPath(closure:(RectF, Path)->Unit): JJRippleWrapper {
        mIsClipInPathChildren = true
        mIsPathClosureClipChildren = true
        mIsClipOutPathChildren = false
        mIsClipChildrenEnabled = true
        mClosurePathClipChildren = closure
        return this
    }

    fun setClipAllToPath(closure:(RectF, Path, JJPadding)->Unit): JJRippleWrapper {
        mIsClipInPathAll = true
        mIsPathClosureClipAll = true
        mIsClipOutPathAll = false
        mIsClipAllEnabled = true
        mClosurePathClipAll = closure
        return this
    }

    fun setClipOutChildrenToPath(closure:(RectF, Path)->Unit): JJRippleWrapper {
        mIsClipInPathChildren = false
        mIsPathClosureClipChildren = true
        mIsClipOutPathChildren = true
        mIsClipChildrenEnabled = true
        mClosurePathClipChildren = closure
        return this
    }

    fun setClipOutAllToPath(closure:(RectF, Path, JJPadding)->Unit): JJRippleWrapper {
        mIsClipInPathAll = false
        mIsPathClosureClipAll = true
        mIsClipOutPathAll = true
        mIsClipAllEnabled = true
        mClosurePathClipAll = closure
        return this
    }

    fun disposeClipPathChildren(): JJRippleWrapper {
        mIsClipOutPathChildren = false
        mIsPathClosureClipChildren = false
        mIsClipChildrenEnabled = false
        mIsClipInPathChildren = false
        mClosurePathClipChildren = null
        return  this
    }
    fun disposeClipPathAll(): JJRippleWrapper {
        mIsClipOutPathAll = false
        mIsPathClosureClipAll = false
        mIsClipAllEnabled = false
        mIsClipInPathAll = false
        mClosurePathClipAll = null
        return  this
    }

    fun getPadding() : JJPadding {
        return JJPadding(mlpPadding.left,mlpPadding.top,mlpPadding.right,mlpPadding.bottom)
    }

    fun setPadding(padding: JJPadding): JJRippleWrapper {
        mlpPadding = padding
        setPaddingRelative(padding.left,padding.top,padding.right,padding.bottom)
        return this
    }

    fun setRippleColorRes(resId: Int): JJRippleWrapper {
        mRippleColor = ContextCompat.getColor(context,resId)
        return this
    }
    fun setRippleColor(color: Int): JJRippleWrapper {
        mRippleColor = color
        return this
    }

    fun getRippleColor(): Int {
        return mRippleColor
    }

    fun getRippleType(): Int {
        return mRippleType
    }

    fun setRippleType(rippleType: Int) : JJRippleWrapper {
        this.mRippleType = rippleType
        return this
    }

    /**
     * Make the animation to be center in bounds.
     *
     * @param isCentered boolean true or false
     */
    fun setCentered(isCentered: Boolean) : JJRippleWrapper {
        mIsCentered = isCentered
        return this
    }

    fun isCentered(): Boolean {
        return mIsCentered
    }


    /**
     * At the end of Ripple effect, the child views has to zoom
     *
     * @param hasToZoom Do the child views have to zoom ? default is False
     */
    fun setZooming(hasToZoom: Boolean): JJRippleWrapper {
        mHasToZoom = hasToZoom
        return this
    }

    fun isZooming(): Boolean {
        return mHasToZoom
    }

    /**
     * Scale of the end animation
     *
     * @param zoomScale Value of scale animation, default is 1.03f
     */
    fun setZoomScale(scale: Float): JJRippleWrapper {
        mZoomScale = scale
        return this
    }

    fun getZoomScale(): Float {
        return mZoomScale
    }

    /**
     * Zoom
     * Duration of the ending animation in ms
     *
     * @param duration default is 200ms
     */
    fun setZoomDuration(duration: Int) : JJRippleWrapper {
        mZoomDuration = duration
        return this
    }

    fun getZoomDuration(): Int {
        return mZoomDuration
    }

    /**
     * Duration of the Ripple animation in ms
     *
     * @param duration Duration, default is 400ms
     */
    fun setRippleDuration(duration: Int): JJRippleWrapper {
        mRippleDuration = duration
        return this
    }

    fun getRippleDuration(): Int {
        return mRippleDuration
    }


    /**
     * Set framerate for Ripple animation
     *
     * @param rate New framerate value, default is 10
     */
    fun setFrameRate(rate: Int) : JJRippleWrapper {
        mFrameRate = rate
        return this
    }

    fun getFrameRate(): Int {
        return mFrameRate
    }


    /**
     * Set alpha for ripple effect color
     *
     * @param alpha Alpha value between 0 and 255, default is 90
     */
    fun setRippleAlpha(alpha: Int) : JJRippleWrapper {
        mRippleAlpha = alpha
        return this
    }

    fun getRippleAlpha(): Int {
        return mRippleAlpha
    }

    fun setRippleRadius(radius: Float): JJRippleWrapper {
        mRadiusMax = radius
        return this
    }

    fun getRippleRadius(): Float {
        return mRadiusMax
    }

    fun setOnRippleCompleteListener(listener: (JJRippleWrapper)-> Unit) : JJRippleWrapper {
        onCompletionListener = listener
        return this
    }

    fun diposeOnRippleCompleteListener() : JJRippleWrapper {
        onCompletionListener = null
        return this
    }




    //endregion



    //region RelativeLayout Params

    private var mRlp: RelativeLayout.LayoutParams? = null

    private fun setupRlp(){
        if(mRlp == null) {
            mRlp = layoutParams as? RelativeLayout.LayoutParams
            layoutParams = mRlp
        }
    }

    fun rlWidth(width: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.width = width
        return this
    }

    fun rlHeight(height: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.height = height
        return this
    }

    fun rlAbove(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.ABOVE,viewId)
        return this
    }

    fun rlBelow(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.BELOW,viewId)
        return this
    }

    fun rlAlignParentBottom(value : Boolean = true): JJRippleWrapper {
        setupRlp()
        val data = if(value) 1 else 0
        mRlp!!.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,data)
        return this
    }

    fun rlAlignParentTop(value : Boolean = true): JJRippleWrapper {
        setupRlp()
        val data = if(value) 1 else 0
        mRlp!!.addRule(RelativeLayout.ALIGN_PARENT_TOP,data)
        return this
    }

    fun rlAlignParentStart(value : Boolean = true): JJRippleWrapper {
        setupRlp()
        val data = if(value) 1 else 0
        mRlp!!.addRule(RelativeLayout.ALIGN_PARENT_START,data)
        return this
    }

    fun rlAlignParentEnd(value : Boolean = true): JJRippleWrapper {
        setupRlp()
        val data = if(value) 1 else 0
        mRlp!!.addRule(RelativeLayout.ALIGN_PARENT_END,data)
        return this
    }

    fun rlAlignParentLeft(value : Boolean = true): JJRippleWrapper {
        setupRlp()
        val data = if(value) 1 else 0
        mRlp!!.addRule(RelativeLayout.ALIGN_PARENT_LEFT,data)
        return this
    }

    fun rlAlignParentRight(value : Boolean = true): JJRippleWrapper {
        setupRlp()
        val data = if(value) 1 else 0
        mRlp!!.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,data)
        return this
    }

    fun rlAlignEnd(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.ALIGN_END,viewId)
        return this
    }

    fun rlAlignStart(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.ALIGN_START,viewId)
        return this
    }

    fun rlAlignTop(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.ALIGN_TOP,viewId)
        return this
    }

    fun rlAlignBottom(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.ALIGN_BOTTOM,viewId)
        return this
    }


    fun rlAlignLeft(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.ALIGN_LEFT,viewId)
        return this
    }

    fun rlAlignRight(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.ALIGN_RIGHT,viewId)
        return this
    }

    fun rlRightToLeft(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.LEFT_OF,viewId)
        return this
    }

    fun rlLeftToRight(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.RIGHT_OF,viewId)
        return this
    }

    fun rlStartToEnd(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.END_OF,viewId)
        return this
    }

    fun rlEndToStart(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.START_OF,viewId)
        return this
    }

    fun rlCenterInParent(value:Boolean = true): JJRippleWrapper {
        setupRlp()
        val data = if(value) 1 else 0
        mRlp!!.addRule(RelativeLayout.CENTER_IN_PARENT,data)
        return this
    }

    fun rlCenterInParentVertically(value:Boolean = true): JJRippleWrapper {
        setupRlp()
        val data = if(value) 1 else 0
        mRlp!!.addRule(RelativeLayout.CENTER_VERTICAL,data)
        return this
    }

    fun rlCenterInParentHorizontally(value:Boolean = true): JJRippleWrapper {
        setupRlp()
        val data = if(value) 1 else 0
        mRlp!!.addRule(RelativeLayout.CENTER_HORIZONTAL,data)
        return this
    }

    fun rlAlignBaseline(viewId: Int): JJRippleWrapper {
        setupRlp()
        mRlp!!.addRule(RelativeLayout.ALIGN_BASELINE,viewId)
        return this
    }

    fun rlMargins(margins: JJMargin): JJRippleWrapper {
        setupRlp()
        mRlp!!.setMargins(margins.left,margins.top,margins.right,margins.bottom)
        return this
    }

    //endregion

    //region MotionLayout Params

    private var mMotionConstraintSet: ConstraintSet? = null


    fun mlVisibilityMode(visibility: Int): JJRippleWrapper {
        mMotionConstraintSet?.setVisibilityMode(id, visibility)
        return this
    }

    fun mlVerticalBias(float: Float): JJRippleWrapper {
        mMotionConstraintSet?.setVerticalBias(id,float)
        return this
    }
    fun mlHorizontalBias(float: Float): JJRippleWrapper {
        mMotionConstraintSet?.setHorizontalBias(id,float)
        return this
    }

    fun mlCenterHorizontallyOf(viewId: Int, marginStart: Int = 0, marginEnd: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.START, viewId, ConstraintSet.START, marginStart)
        mMotionConstraintSet?.connect(this.id, ConstraintSet.END, viewId, ConstraintSet.END, marginEnd)
        mMotionConstraintSet?.setHorizontalBias(viewId,0.5f)
        return this
    }
    fun mlCenterVerticallyOf(viewId: Int,marginTop: Int = 0, marginBottom: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.TOP, viewId, ConstraintSet.TOP, marginTop)
        mMotionConstraintSet?.connect(this.id, ConstraintSet.BOTTOM, viewId, ConstraintSet.BOTTOM, marginBottom)
        mMotionConstraintSet?.setVerticalBias(viewId,0.5f)
        return this
    }

    fun mlMargins(margins: JJMargin) : JJRippleWrapper {
        mMotionConstraintSet?.setMargin(id,ConstraintSet.TOP,margins.top)
        mMotionConstraintSet?.setMargin(id,ConstraintSet.BOTTOM,margins.bottom)
        mMotionConstraintSet?.setMargin(id,ConstraintSet.END,margins.right)
        mMotionConstraintSet?.setMargin(id,ConstraintSet.START,margins.left)
        return this
    }


    fun mlFloatCustomAttribute(attrName: String, value: Float): JJRippleWrapper {
        mMotionConstraintSet?.setFloatValue(id,attrName,value)
        return this
    }

    fun mlIntCustomAttribute(attrName: String, value: Int): JJRippleWrapper {
        mMotionConstraintSet?.setIntValue(id,attrName,value)
        return this
    }

    fun mlColorCustomAttribute(attrName: String, value: Int): JJRippleWrapper {
        mMotionConstraintSet?.setColorValue(id,attrName,value)
        return this
    }

    fun mlStringCustomAttribute(attrName: String, value: String): JJRippleWrapper {
        mMotionConstraintSet?.setStringValue(id,attrName,value)
        return this
    }

    fun mlRotation(float: Float): JJRippleWrapper {
        mMotionConstraintSet?.setRotation(id,float)
        return this
    }

    fun mlRotationX(float: Float): JJRippleWrapper {
        mMotionConstraintSet?.setRotationX(id,float)
        return this
    }

    fun mlRotationY(float: Float): JJRippleWrapper {
        mMotionConstraintSet?.setRotationY(id,float)
        return this
    }

    fun mlTranslation(x: Float,y: Float): JJRippleWrapper {
        mMotionConstraintSet?.setTranslation(id,x,y)
        return this
    }
    fun mlTranslationX(x: Float): JJRippleWrapper {
        mMotionConstraintSet?.setTranslationX(id,x)
        return this
    }

    fun mlTranslationY(y: Float): JJRippleWrapper {
        mMotionConstraintSet?.setTranslationY(id,y)
        return this
    }

    fun mlTranslationZ(z: Float): JJRippleWrapper {
        mMotionConstraintSet?.setTranslationZ(id,z)
        return this
    }

    fun mlTransformPivot(x: Float, y: Float): JJRippleWrapper {
        mMotionConstraintSet?.setTransformPivot(id,x,y)
        return this
    }

    fun mlTransformPivotX(x: Float): JJRippleWrapper {
        mMotionConstraintSet?.setTransformPivotX(id,x)
        return this
    }

    fun mlTransformPivotY(y: Float): JJRippleWrapper {
        mMotionConstraintSet?.setTransformPivotY(id,y)
        return this
    }

    fun mlScaleX(x: Float): JJRippleWrapper {
        mMotionConstraintSet?.setScaleX(id,x)
        return this
    }

    fun mlScaleY(y: Float): JJRippleWrapper {
        mMotionConstraintSet?.setScaleY(id,y)
        return this
    }

    fun mlDimensionRatio(ratio: String): JJRippleWrapper {
        mMotionConstraintSet?.setDimensionRatio(id,ratio)
        return this
    }

    fun mlAlpha(alpha: Float): JJRippleWrapper {
        mMotionConstraintSet?.setAlpha(id,alpha)
        return this
    }



    fun mlTopToTop(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.TOP, viewId, ConstraintSet.TOP, margin)
        return this
    }

    fun mlTopToTopParent(margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin)
        return this
    }


    fun mlTopToBottomOf(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.TOP, viewId, ConstraintSet.BOTTOM, margin)
        return this
    }

    fun mlTopToBottomParent(margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin)
        return this
    }

    fun mlBottomToTopOf(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.BOTTOM, viewId, ConstraintSet.TOP, margin)

        return this
    }

    fun mlBottomToTopParent(margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin)

        return this
    }

    fun mlBottomToBottomOf(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.BOTTOM, viewId, ConstraintSet.BOTTOM, margin)

        return this
    }

    fun mlBottomToBottomParent(margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin)

        return this
    }

    fun mlStartToStartOf(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.START, viewId, ConstraintSet.START, margin)

        return this
    }

    fun mlStartToStartParent(margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin)

        return this
    }

    fun mlStartToEndOf(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.START, viewId, ConstraintSet.END, margin)

        return this
    }

    fun mlStartToEndParent(margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END, margin)

        return this
    }

    fun mlEndToEndOf(viewId: Int, margin: Int= 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.END, viewId, ConstraintSet.END, margin)

        return this
    }

    fun mlEndToEndParent(margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, margin)

        return this
    }


    fun mlEndToStartOf(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.END, viewId, ConstraintSet.START, margin)
        return this
    }

    fun mlEndToStartParent(margin: Int = 0): JJRippleWrapper {
        mMotionConstraintSet?.connect(this.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.START, margin)
        return this
    }


    fun mlWidth(width: Int): JJRippleWrapper {
        mMotionConstraintSet?.constrainWidth(id, width)
        return this
    }

    fun mlHeight(height: Int): JJRippleWrapper {
        mMotionConstraintSet?.constrainHeight(id, height)
        return this
    }

    fun mlPercentWidth(width: Float): JJRippleWrapper {
        mMotionConstraintSet?.constrainPercentWidth(id, width)
        return this
    }

    fun mlPercentHeight(height: Float): JJRippleWrapper {
        mMotionConstraintSet?.constrainPercentHeight(id, height)
        return this
    }

    fun mlCenterInParent(): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mMotionConstraintSet?.setVerticalBias(id, 0.5f)
        mMotionConstraintSet?.setHorizontalBias(id, 0.5f)

        return this
    }

    fun mlCenterInParent(verticalBias: Float, horizontalBias: Float, margin: JJMargin): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin.left)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, margin.right)
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin.top)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin.bottom)
        mMotionConstraintSet?.setVerticalBias(id, verticalBias)
        mMotionConstraintSet?.setHorizontalBias(id, horizontalBias)
        return this
    }

    fun mlCenterInParentVertically(): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mMotionConstraintSet?.setVerticalBias(id, 0.5f)

        return this
    }

    fun mlCenterInParentHorizontally(): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mMotionConstraintSet?.setHorizontalBias(id, 0.5f)
        return this
    }

    fun mlCenterInParentVertically(bias: Float, topMargin: Int, bottomMargin: Int): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMargin)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMargin)
        mMotionConstraintSet?.setVerticalBias(id, bias)
        return this
    }

    fun mlCenterInParentHorizontally(bias: Float, startMargin: Int, endtMargin: Int): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, startMargin)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, endtMargin)
        mMotionConstraintSet?.setHorizontalBias(id, bias)
        return this
    }


    fun mlCenterInParentTopVertically(): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mMotionConstraintSet?.setVerticalBias(id, 0.5f)
        return this
    }


    fun mlCenterInParentBottomVertically(): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mMotionConstraintSet?.setVerticalBias(id, 0.5f)
        return this
    }

    fun mlCenterInParentStartHorizontally(): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mMotionConstraintSet?.setHorizontalBias(id, 0.5f)
        return this
    }

    fun mlCenterInParentEndHorizontally(): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mMotionConstraintSet?.setHorizontalBias(id, 0.5f)
        return this
    }

    fun mlCenterInTopVerticallyOf(viewId: Int): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, viewId, ConstraintSet.TOP, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, viewId, ConstraintSet.TOP, 0)
        mMotionConstraintSet?.setVerticalBias(id, 0.5f)
        return this
    }


    fun mlCenterInBottomVerticallyOf(viewId: Int): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, viewId, ConstraintSet.BOTTOM, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, viewId, ConstraintSet.BOTTOM, 0)
        mMotionConstraintSet?.setVerticalBias(id, 0.5f)
        return this
    }

    fun mlCenterInStartHorizontallyOf(viewId: Int): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.START, viewId, ConstraintSet.START, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, viewId, ConstraintSet.START, 0)
        mMotionConstraintSet?.setHorizontalBias(id, 0.5f)
        return this
    }

    fun mlCenterInEndHorizontallyOf(viewId: Int): JJRippleWrapper {
        mMotionConstraintSet?.connect(id, ConstraintSet.START, viewId, ConstraintSet.END, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, viewId, ConstraintSet.END, 0)
        mMotionConstraintSet?.setHorizontalBias(id, 0.5f)
        return this
    }

    fun mlCenterVertically(topId: Int, topSide: Int, topMargin: Int, bottomId: Int, bottomSide: Int, bottomMargin: Int, bias: Float): JJRippleWrapper {
        mMotionConstraintSet?.centerVertically(id, topId, topSide, topMargin, bottomId, bottomSide, bottomMargin, bias)
        return this
    }

    fun mlCenterHorizontally(startId: Int, startSide: Int, startMargin: Int, endId: Int, endSide: Int, endMargin: Int, bias: Float): JJRippleWrapper {
        mMotionConstraintSet?.centerHorizontally(id, startId, startSide, startMargin, endId, endSide, endMargin, bias)
        return this
    }


    fun mlFillParent(): JJRippleWrapper {
        mMotionConstraintSet?.constrainWidth(id,0)
        mMotionConstraintSet?.constrainHeight(id,0)
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        return this
    }

    fun mlFillParent(margin: JJMargin): JJRippleWrapper {
        mMotionConstraintSet?.constrainWidth(id,0)
        mMotionConstraintSet?.constrainHeight(id,0)
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin.top)
        mMotionConstraintSet?.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin.left)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, margin.right)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin.bottom)
        return this
    }

    fun mlFillParentHorizontally(): JJRippleWrapper {
        mMotionConstraintSet?.constrainWidth(id,0)
        mMotionConstraintSet?.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        return this
    }

    fun mlFillParentVertically(): JJRippleWrapper {
        mMotionConstraintSet?.constrainHeight(id,0)
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        return this
    }

    fun mlFillParentHorizontally(startMargin: Int, endMargin: Int): JJRippleWrapper {
        mMotionConstraintSet?.constrainWidth(id,0)
        mMotionConstraintSet?.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, startMargin)
        mMotionConstraintSet?.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, endMargin)
        return this
    }

    fun mlFillParentVertically(topMargin: Int, bottomMargin: Int): JJRippleWrapper {
        mMotionConstraintSet?.constrainHeight(id,0)
        mMotionConstraintSet?.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMargin)
        mMotionConstraintSet?.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMargin)
        return this
    }

    fun mlVisibility(visibility: Int): JJRippleWrapper {
        mMotionConstraintSet?.setVisibility(id, visibility)
        return this
    }

    fun mlElevation(elevation: Float): JJRippleWrapper {
        mMotionConstraintSet?.setElevation(id, elevation)
        return this
    }

    fun mlApply(): JJRippleWrapper {
        mMotionConstraintSet?.applyTo(parent as ConstraintLayout)
        return this
    }

    fun mlSetConstraint(cs : ConstraintSet?): JJRippleWrapper {
        mMotionConstraintSet = cs
        return this
    }

    fun mlDisposeConstraint(): JJRippleWrapper {
        mMotionConstraintSet = null
        return this
    }

    //endregion

    //region ConstraintLayout Params
    private val mConstraintSet = ConstraintSet()

    fun clApply(): JJRippleWrapper {
        mConstraintSet.applyTo(parent as ConstraintLayout)
        return this
    }

    fun clVisibilityMode(visibility: Int): JJRippleWrapper {
        mConstraintSet.setVisibilityMode(id, visibility)
        return this
    }

    fun clVerticalBias(float: Float): JJRippleWrapper {
        mConstraintSet.setVerticalBias(id,float)
        return this
    }
    fun clHorizontalBias(float: Float): JJRippleWrapper {
        mConstraintSet.setHorizontalBias(id,float)
        return this
    }

    fun clCenterHorizontallyOf(viewId: Int, marginStart: Int = 0, marginEnd: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.START, viewId, ConstraintSet.START, marginStart)
        mConstraintSet.connect(this.id, ConstraintSet.END, viewId, ConstraintSet.END, marginEnd)
        mConstraintSet.setHorizontalBias(id,0.5f)
        return this
    }
    fun clCenterVerticallyOf(viewId: Int,marginTop: Int = 0, marginBottom: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.TOP, viewId, ConstraintSet.TOP, marginTop)
        mConstraintSet.connect(this.id, ConstraintSet.BOTTOM, viewId, ConstraintSet.BOTTOM, marginBottom)
        mConstraintSet.setVerticalBias(id,0.5f)
        return this
    }

    fun clMargins(margins: JJMargin) : JJRippleWrapper {
        mConstraintSet.setMargin(id,ConstraintSet.TOP,margins.top)
        mConstraintSet.setMargin(id,ConstraintSet.BOTTOM,margins.bottom)
        mConstraintSet.setMargin(id,ConstraintSet.END,margins.right)
        mConstraintSet.setMargin(id,ConstraintSet.START,margins.left)
        return this
    }


    fun clTopToTop(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.TOP, viewId, ConstraintSet.TOP, margin)
        return this
    }

    fun clTopToTopParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin)
        return this
    }


    fun clTopToBottom(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.TOP, viewId, ConstraintSet.BOTTOM, margin)
        return this
    }

    fun clTopToBottomParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin)
        return this
    }

    fun clBottomToTop(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.BOTTOM, viewId, ConstraintSet.TOP, margin)
        return this
    }

    fun clBottomToTopParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin)
        return this
    }

    fun clBottomToBottom(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.BOTTOM, viewId, ConstraintSet.BOTTOM, margin)
        return this
    }

    fun clBottomToBottomParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin)
        return this
    }

    fun clStartToStart(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.START, viewId, ConstraintSet.START, margin)
        return this
    }

    fun clStartToStartParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin)
        return this
    }

    fun clStartToEnd(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.START, viewId, ConstraintSet.END, margin)
        return this
    }

    fun clStartToEndParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END, margin)
        return this
    }

    fun clEndToEnd(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.END, viewId, ConstraintSet.END, margin)
        return this
    }

    fun clEndToEndParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, margin)
        return this
    }


    fun clEndToStart(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.END, viewId, ConstraintSet.START, margin)
        return this
    }

    fun clEndToStartParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSet.connect(this.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.START, margin)
        return this
    }


    fun clWidth(width: Int): JJRippleWrapper {
        mConstraintSet.constrainWidth(id, width)
        return this
    }

    fun clHeight(height: Int): JJRippleWrapper {
        mConstraintSet.constrainHeight(id, height)
        return this
    }

    fun clPercentWidth(width: Float): JJRippleWrapper {
        mConstraintSet.constrainPercentWidth(id, width)
        return this
    }

    fun clPercentHeight(height: Float): JJRippleWrapper {
        mConstraintSet.constrainPercentHeight(id, height)
        return this
    }

    fun clCenterInParent(): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSet.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mConstraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mConstraintSet.setVerticalBias(id, 0.5f)
        mConstraintSet.setHorizontalBias(id, 0.5f)
        return this
    }

    fun clCenterInParent(verticalBias: Float, horizontalBias: Float, margin: JJMargin): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin.left)
        mConstraintSet.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, margin.right)
        mConstraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin.top)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin.bottom)
        mConstraintSet.setVerticalBias(id, verticalBias)
        mConstraintSet.setHorizontalBias(id, horizontalBias)
        return this
    }

    fun clCenterInParentVertically(): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mConstraintSet.setVerticalBias(id, 0.5f)
        return this
    }

    fun clCenterInParentHorizontally(): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSet.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mConstraintSet.setHorizontalBias(id, 0.5f)
        return this
    }

    fun clCenterInParentVertically(bias: Float, topMargin: Int, bottomMargin: Int): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMargin)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMargin)
        mConstraintSet.setVerticalBias(id, bias)
        return this
    }

    fun clCenterInParentHorizontally(bias: Float, startMargin: Int, endtMargin: Int): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, startMargin)
        mConstraintSet.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, endtMargin)
        mConstraintSet.setHorizontalBias(id, bias)
        return this
    }


    fun clCenterInParentTopVertically(): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSet.setVerticalBias(id, 0.5f)
        return this
    }


    fun clCenterInParentBottomVertically(): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mConstraintSet.setVerticalBias(id, 0.5f)
        return this
    }

    fun clCenterInParentStartHorizontally(): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSet.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSet.setHorizontalBias(id, 0.5f)
        return this
    }

    fun clCenterInParentEndHorizontally(): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mConstraintSet.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mConstraintSet.setHorizontalBias(id, 0.5f)
        return this
    }

    fun clCenterInTopVertically(topId: Int): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.TOP, topId, ConstraintSet.TOP, 0)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, topId, ConstraintSet.TOP, 0)
        mConstraintSet.setVerticalBias(id, 0.5f)
        return this
    }


    fun clCenterInBottomVertically(bottomId: Int): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.TOP, bottomId, ConstraintSet.BOTTOM, 0)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, bottomId, ConstraintSet.BOTTOM, 0)
        mConstraintSet.setVerticalBias(id, 0.5f)
        return this
    }

    fun clCenterInStartHorizontally(startId: Int): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.START, startId, ConstraintSet.START, 0)
        mConstraintSet.connect(id, ConstraintSet.END, startId, ConstraintSet.START, 0)
        mConstraintSet.setHorizontalBias(id, 0.5f)
        return this
    }

    fun clCenterInEndHorizontally(endId: Int): JJRippleWrapper {
        mConstraintSet.connect(id, ConstraintSet.START, endId, ConstraintSet.END, 0)
        mConstraintSet.connect(id, ConstraintSet.END, endId, ConstraintSet.END, 0)
        mConstraintSet.setHorizontalBias(id, 0.5f)
        return this
    }

    fun clCenterVertically(topId: Int, topSide: Int, topMargin: Int, bottomId: Int, bottomSide: Int, bottomMargin: Int, bias: Float): JJRippleWrapper {
        mConstraintSet.centerVertically(id, topId, topSide, topMargin, bottomId, bottomSide, bottomMargin, bias)
        return this
    }

    fun clCenterHorizontally(startId: Int, startSide: Int, startMargin: Int, endId: Int, endSide: Int, endMargin: Int, bias: Float): JJRippleWrapper {
        mConstraintSet.centerHorizontally(id, startId, startSide, startMargin, endId, endSide, endMargin, bias)
        return this
    }


    fun clFillParent(): JJRippleWrapper {
        mConstraintSet.constrainWidth(id,0)
        mConstraintSet.constrainHeight(id,0)
        mConstraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSet.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        return this
    }

    fun clFillParent(margin: JJMargin): JJRippleWrapper {
        mConstraintSet.constrainWidth(id,0)
        mConstraintSet.constrainHeight(id,0)
        mConstraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin.top)
        mConstraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin.left)
        mConstraintSet.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, margin.right)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin.bottom)
        return this
    }

    fun clFillParentHorizontally(): JJRippleWrapper {
        mConstraintSet.constrainWidth(id,0)
        mConstraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSet.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        return this
    }

    fun clFillParentVertically(): JJRippleWrapper {
        mConstraintSet.constrainHeight(id,0)
        mConstraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        return this
    }

    fun clFillParentHorizontally(startMargin: Int, endMargin: Int): JJRippleWrapper {
        mConstraintSet.constrainWidth(id,0)
        mConstraintSet.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, startMargin)
        mConstraintSet.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, endMargin)
        return this
    }

    fun clFillParentVertically(topMargin: Int, bottomMargin: Int): JJRippleWrapper {
        mConstraintSet.constrainHeight(id,0)
        mConstraintSet.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMargin)
        mConstraintSet.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMargin)
        return this
    }

    fun clVisibility(visibility: Int): JJRippleWrapper {
        mConstraintSet.setVisibility(id, visibility)
        return this
    }



    fun clElevation(elevation: Float): JJRippleWrapper {
        mConstraintSet.setElevation(id, elevation)

        return this
    }

    fun clConstraintSet() : ConstraintSet {
        return mConstraintSet
    }

    fun clMinWidth(w:Int): JJRippleWrapper {
        mConstraintSet.constrainMinWidth(id,w)
        return this
    }

    fun clMinHeight(h:Int): JJRippleWrapper {
        mConstraintSet.constrainMinHeight(id,h)
        return this
    }

    fun clMaxWidth(w:Int): JJRippleWrapper {
        mConstraintSet.constrainMaxWidth(id,w)
        return this
    }

    fun clMaxHeight(h:Int): JJRippleWrapper {
        mConstraintSet.constrainMaxHeight(id,h)
        return this
    }






    //endregion

    //region ConstraintLayout LandScape Params
    private val mConstraintSetLandScape = ConstraintSet()

    fun cllApply(): JJRippleWrapper {
        mConstraintSetLandScape.applyTo(parent as ConstraintLayout)
        return this
    }

    fun cllVisibilityMode(visibility: Int): JJRippleWrapper {
        mConstraintSetLandScape.setVisibilityMode(id, visibility)
        return this
    }

    fun cllVerticalBias(float: Float): JJRippleWrapper {
        mConstraintSetLandScape.setVerticalBias(id,float)
        return this
    }
    fun cllHorizontalBias(float: Float): JJRippleWrapper {
        mConstraintSetLandScape.setHorizontalBias(id,float)
        return this
    }

    fun cllCenterHorizontallyOf(viewId: Int, marginStart: Int = 0, marginEnd: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.START, viewId, ConstraintSet.START, marginStart)
        mConstraintSetLandScape.connect(this.id, ConstraintSet.END, viewId, ConstraintSet.END, marginEnd)
        mConstraintSetLandScape.setHorizontalBias(id,0.5f)
        return this
    }
    fun cllCenterVerticallyOf(viewId: Int,marginTop: Int = 0, marginBottom: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.TOP, viewId, ConstraintSet.TOP, marginTop)
        mConstraintSetLandScape.connect(this.id, ConstraintSet.BOTTOM, viewId, ConstraintSet.BOTTOM, marginBottom)
        mConstraintSetLandScape.setVerticalBias(id,0.5f)
        return this
    }

    fun cllMargins(margins: JJMargin) : JJRippleWrapper {
        mConstraintSetLandScape.setMargin(id,ConstraintSet.TOP,margins.top)
        mConstraintSetLandScape.setMargin(id,ConstraintSet.BOTTOM,margins.bottom)
        mConstraintSetLandScape.setMargin(id,ConstraintSet.END,margins.right)
        mConstraintSetLandScape.setMargin(id,ConstraintSet.START,margins.left)
        return this
    }


    fun cllTopToTop(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.TOP, viewId, ConstraintSet.TOP, margin)
        return this
    }

    fun cllTopToTopParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin)
        return this
    }


    fun cllTopToBottom(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.TOP, viewId, ConstraintSet.BOTTOM, margin)
        return this
    }

    fun cllTopToBottomParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin)
        return this
    }

    fun cllBottomToTop(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.BOTTOM, viewId, ConstraintSet.TOP, margin)
        return this
    }

    fun cllBottomToTopParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin)
        return this
    }

    fun cllBottomToBottom(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.BOTTOM, viewId, ConstraintSet.BOTTOM, margin)
        return this
    }

    fun cllBottomToBottomParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin)
        return this
    }

    fun cllStartToStart(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.START, viewId, ConstraintSet.START, margin)
        return this
    }

    fun cllStartToStartParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin)
        return this
    }

    fun cllStartToEnd(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.START, viewId, ConstraintSet.END, margin)
        return this
    }

    fun cllStartToEndParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END, margin)
        return this
    }

    fun cllEndToEnd(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.END, viewId, ConstraintSet.END, margin)
        return this
    }

    fun cllEndToEndParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, margin)
        return this
    }


    fun cllEndToStart(viewId: Int, margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.END, viewId, ConstraintSet.START, margin)
        return this
    }

    fun cllEndToStartParent(margin: Int = 0): JJRippleWrapper {
        mConstraintSetLandScape.connect(this.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.START, margin)
        return this
    }


    fun cllWidth(width: Int): JJRippleWrapper {
        mConstraintSetLandScape.constrainWidth(id, width)
        return this
    }

    fun cllHeight(height: Int): JJRippleWrapper {
        mConstraintSetLandScape.constrainHeight(id, height)
        return this
    }

    fun cllPercentWidth(width: Float): JJRippleWrapper {
        mConstraintSetLandScape.constrainPercentWidth(id, width)
        return this
    }

    fun cllPercentHeight(height: Float): JJRippleWrapper {
        mConstraintSetLandScape.constrainPercentHeight(id, height)
        return this
    }

    fun cllCenterInParent(): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mConstraintSetLandScape.setVerticalBias(id, 0.5f)
        mConstraintSetLandScape.setHorizontalBias(id, 0.5f)
        return this
    }

    fun cllCenterInParent(verticalBias: Float, horizontalBias: Float, margin: JJMargin): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin.left)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, margin.right)
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin.top)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin.bottom)
        mConstraintSetLandScape.setVerticalBias(id, verticalBias)
        mConstraintSetLandScape.setHorizontalBias(id, horizontalBias)
        return this
    }

    fun cllCenterInParentVertically(): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mConstraintSetLandScape.setVerticalBias(id, 0.5f)
        return this
    }

    fun cllCenterInParentHorizontally(): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mConstraintSetLandScape.setHorizontalBias(id, 0.5f)
        return this
    }

    fun cllCenterInParentVertically(bias: Float, topMargin: Int, bottomMargin: Int): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMargin)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMargin)
        mConstraintSetLandScape.setVerticalBias(id, bias)
        return this
    }

    fun cllCenterInParentHorizontally(bias: Float, startMargin: Int, endtMargin: Int): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, startMargin)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, endtMargin)
        mConstraintSetLandScape.setHorizontalBias(id, bias)
        return this
    }


    fun cllCenterInParentTopVertically(): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSetLandScape.setVerticalBias(id, 0.5f)
        return this
    }


    fun cllCenterInParentBottomVertically(): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        mConstraintSetLandScape.setVerticalBias(id, 0.5f)
        return this
    }

    fun cllCenterInParentStartHorizontally(): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSetLandScape.setHorizontalBias(id, 0.5f)
        return this
    }

    fun cllCenterInParentEndHorizontally(): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mConstraintSetLandScape.setHorizontalBias(id, 0.5f)
        return this
    }

    fun cllCenterInTopVertically(topId: Int): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, topId, ConstraintSet.TOP, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, topId, ConstraintSet.TOP, 0)
        mConstraintSetLandScape.setVerticalBias(id, 0.5f)
        return this
    }


    fun cllCenterInBottomVertically(bottomId: Int): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, bottomId, ConstraintSet.BOTTOM, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, bottomId, ConstraintSet.BOTTOM, 0)
        mConstraintSetLandScape.setVerticalBias(id, 0.5f)
        return this
    }

    fun cllCenterInStartHorizontally(startId: Int): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.START, startId, ConstraintSet.START, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, startId, ConstraintSet.START, 0)
        mConstraintSetLandScape.setHorizontalBias(id, 0.5f)
        return this
    }

    fun cllCenterInEndHorizontally(endId: Int): JJRippleWrapper {
        mConstraintSetLandScape.connect(id, ConstraintSet.START, endId, ConstraintSet.END, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, endId, ConstraintSet.END, 0)
        mConstraintSetLandScape.setHorizontalBias(id, 0.5f)
        return this
    }

    fun cllCenterVertically(topId: Int, topSide: Int, topMargin: Int, bottomId: Int, bottomSide: Int, bottomMargin: Int, bias: Float): JJRippleWrapper {
        mConstraintSetLandScape.centerVertically(id, topId, topSide, topMargin, bottomId, bottomSide, bottomMargin, bias)
        return this
    }

    fun cllCenterHorizontally(startId: Int, startSide: Int, startMargin: Int, endId: Int, endSide: Int, endMargin: Int, bias: Float): JJRippleWrapper {
        mConstraintSetLandScape.centerHorizontally(id, startId, startSide, startMargin, endId, endSide, endMargin, bias)
        return this
    }


    fun cllFillParent(): JJRippleWrapper {
        mConstraintSetLandScape.constrainWidth(id,0)
        mConstraintSetLandScape.constrainHeight(id,0)
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        return this
    }

    fun cllFillParent(margin: JJMargin): JJRippleWrapper {
        mConstraintSetLandScape.constrainWidth(id,0)
        mConstraintSetLandScape.constrainHeight(id,0)
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin.top)
        mConstraintSetLandScape.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin.left)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, margin.right)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin.bottom)
        return this
    }

    fun cllFillParentHorizontally(): JJRippleWrapper {
        mConstraintSetLandScape.constrainWidth(id,0)
        mConstraintSetLandScape.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0)
        return this
    }

    fun cllFillParentVertically(): JJRippleWrapper {
        mConstraintSetLandScape.constrainHeight(id,0)
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        return this
    }

    fun cllFillParentHorizontally(startMargin: Int, endMargin: Int): JJRippleWrapper {
        mConstraintSetLandScape.constrainWidth(id,0)
        mConstraintSetLandScape.connect(id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, startMargin)
        mConstraintSetLandScape.connect(id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, endMargin)
        return this
    }

    fun cllFillParentVertically(topMargin: Int, bottomMargin: Int): JJRippleWrapper {
        mConstraintSetLandScape.constrainHeight(id,0)
        mConstraintSetLandScape.connect(id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMargin)
        mConstraintSetLandScape.connect(id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMargin)
        return this
    }

    fun cllVisibility(visibility: Int): JJRippleWrapper {
        mConstraintSetLandScape.setVisibility(id, visibility)
        return this
    }



    fun cllElevation(elevation: Float): JJRippleWrapper {
        mConstraintSetLandScape.setElevation(id, elevation)

        return this
    }

    fun cllConstraintSet() : ConstraintSet {
        return mConstraintSetLandScape
    }

    fun cllMinWidth(w:Int): JJRippleWrapper {
        mConstraintSetLandScape.constrainMinWidth(id,w)
        return this
    }

    fun cllMinHeight(h:Int): JJRippleWrapper {
        mConstraintSetLandScape.constrainMinHeight(id,h)
        return this
    }

    fun cllMaxWidth(w:Int): JJRippleWrapper {
        mConstraintSetLandScape.constrainMaxWidth(id,w)
        return this
    }

    fun cllMaxHeight(h:Int): JJRippleWrapper {
        mConstraintSetLandScape.constrainMaxHeight(id,h)
        return this
    }






//endregion

    //region LinearLayout Params

    private var mLlp: LinearLayout.LayoutParams? = null
    private fun setupLlp() {
        if (mLlp == null) {
            mLlp = layoutParams as? LinearLayout.LayoutParams
            layoutParams = mLlp
        }
    }

    fun llWidth(width: Int): JJRippleWrapper {
        setupLlp()
        mLlp!!.width = width
        return this
    }

    fun llHeight(height: Int): JJRippleWrapper {
        setupLlp()
        mLlp!!.height = height
        return this
    }

    fun llWeight(weigth: Float): JJRippleWrapper {
        setupLlp()
        mLlp!!.weight = weigth
        return this
    }

    fun llGravity(gravity: Int): JJRippleWrapper {
        setupLlp()
        mLlp!!.gravity = gravity
        return this
    }

    fun llTopMargin(m : Int): JJRippleWrapper {
        setupLlp()
        mLlp!!.topMargin = m
        return this
    }

    fun llBottomMargin(m : Int): JJRippleWrapper {
        setupLlp()
        mLlp!!.bottomMargin = m
        return this
    }

    fun llStartMargin(m : Int): JJRippleWrapper {
        setupLlp()
        mLlp!!.marginStart = m
        return this
    }

    fun llEndMargin(m : Int): JJRippleWrapper {
        setupLlp()
        mLlp!!.marginEnd = m
        return this
    }

    fun llLeftMargin(m : Int): JJRippleWrapper {
        setupLlp()
        mLlp!!.leftMargin = m
        return this
    }

    fun llRightMargin(m : Int): JJRippleWrapper {
        setupLlp()
        mLlp!!.rightMargin = m
        return this
    }


    fun llMargins( margins : JJMargin): JJRippleWrapper {
        setupLlp()
        mLlp!!.setMargins(margins.left,margins.top,margins.right,margins.bottom)
        return this
    }




    //endregion

    //region ScrollView Params

    private var mSvp: FrameLayout.LayoutParams? = null

    private fun setupSvp() {
        if (mSvp == null) {
            mSvp = layoutParams as? FrameLayout.LayoutParams
            layoutParams = mSvp
        }
    }

    fun svWidth(width: Int): JJRippleWrapper {
        setupSvp()
        mSvp!!.width = width
        return this
    }

    fun svHeight(height: Int): JJRippleWrapper {
        setupSvp()
        mSvp!!.height = height
        return this
    }


    fun svMargins( margins : JJMargin): JJRippleWrapper {
        setupSvp()
        mSvp!!.setMargins(margins.left,margins.top,margins.right,margins.bottom)
        return this
    }
    //endregion


}
