package com.quadram.bubbleservice

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.*
import android.view.MotionEvent.INVALID_POINTER_ID
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationSet
import android.widget.FrameLayout
import android.widget.ImageSwitcher
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


open abstract class FloatingService: Service() {

    private val TAG = "FLOATING_LIBRARY"
    private var mActivePointerId = INVALID_POINTER_ID

    private val clickThreshold = 15
    private var startPositionX = 0f
    private  var startPositionY: Float = 0f
    private var currentPositionX = 0f
    private  var currentPositionY: Float = 0f
    private var aLastTouchX = 0f
    private  var aLastTouchY: Float = 0f
    private var screenWidth = 0f
    private  var screenHeight: Float = 0f
    private var viewWidth = 0f
    private  var viewHeight: Float = 0f
    private val transparancyHandler: Handler = Handler()
    private var moveListener: MoveListener? = null
    private var isMenuOpened: Boolean = false
    private val transparentAfterMilliseconds = 2000
    private var pointOnScreen = Point(0, 0)

    private var mWindowManager: WindowManager? = null
    private var mFloatingWidgetView: View? = null
    private var collapsedView: View? = null
    private var expandedView: View? = null
    private var remove_image_view: ImageView? = null
    private val szWindow: Point = Point()
    private var removeFloatingWidgetView: View? = null
    private var linearLayoutParent: LinearLayout? = null

    open val movementStyle: FloatingStyle = FloatingStyle.FREE
    open val iconOpen: Drawable? = null
    open val iconClose: Drawable? = null
    open val iconHideLeft: Drawable? = null
    open val iconHideRight: Drawable? = null


    private var collapsedItem: ImageSwitcher? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    abstract val removeViewDrawable: Drawable?
    abstract val callback: OnFloatingClickListener
    abstract val items: List<FloatingItem>
    open val timeToSetTransparent: Long = 500
    open val timeToWaiting: Long = 500

    fun setItems(items: List<FloatingItem>, callback: OnFloatingClickListener) {
        (expandedView as RecyclerView).layoutManager = LinearLayoutManager(this)
        (expandedView as RecyclerView).adapter = FloatingAdapter(items.filter { it.isVisible }.toMutableList(), callback)
    }

    fun updateItems(items: List<FloatingItem>) {
        ((expandedView as RecyclerView).adapter as FloatingAdapter).updateElements(items.filter { it.isVisible }.toMutableList())
    }

    override fun onCreate() {
        super.onCreate()
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManagerDefaultDisplay
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addRemoveView(inflater)
        addFloatingWidgetView(inflater)
        implementTouchListenerToFloatingWidgetView()
        collapsedItem?.setFactory { ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        } }
        setDefaultImage()
        setItems(items, callback)
    }

    @SuppressLint("InlinedApi")
    private fun getWindowViewType(): Int {
        return when {
            //The app is running in the problematic smartphones and with the android 7.1 version ------> WindowManager.LayoutParams.TYPE_PHONE
            Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1 -> WindowManager.LayoutParams.TYPE_PHONE
            //The app is running in a no problematic device and with android 7.1 version
            //!Build.MANUFACTURER.exceptionNougatManufacture() && Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1 -> WindowManager.LayoutParams.TYPE_PHONE
            //The app is running in a version lower than android 8 ----------> WindowManager.LayoutParams.TYPE_TOAST
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> WindowManager.LayoutParams.TYPE_TOAST
            //The app is running in a version equal or higher than android 8 ----------> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
    }

    /*  Add Remove View to Window Manager  */
    private fun addRemoveView(inflater: LayoutInflater): View? {
        //Inflate the removing view layout we created
        removeFloatingWidgetView = inflater.inflate(R.layout.remove_floating_widget_layout, null)

        //Add the view to the window.
        val paramRemove: WindowManager.LayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getWindowViewType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        //Specify the view position
        paramRemove.gravity = Gravity.TOP or Gravity.START

        //Initially the Removing widget view is not visible, so set visibility to GONE
        removeFloatingWidgetView!!.visibility = View.GONE
        remove_image_view = removeFloatingWidgetView!!.findViewById<View>(R.id.remove_img) as ImageView
        removeViewDrawable?.let {
            remove_image_view!!.setImageDrawable(it)
        }
        //Add the view to the window
        mWindowManager!!.addView(removeFloatingWidgetView, paramRemove)
        return remove_image_view
    }

    /*  Add Floating Widget View to Window Manager  */
    private fun addFloatingWidgetView(inflater: LayoutInflater) {
        //Inflate the floating view layout we created
        mFloatingWidgetView = inflater.inflate(R.layout.floating_widget_layout, null)
        linearLayoutParent = mFloatingWidgetView!!.findViewById<LinearLayout>(R.id.root_container)
        //Add the view to the window.
        val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getWindowViewType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        //Specify the view position
        params.gravity = Gravity.TOP or Gravity.START

        //Initially view will be added to top-left corner, you change x-y coordinates according to your need
        params.x = 0
        params.y = 100

        //Add the view to the window
        mWindowManager!!.addView(mFloatingWidgetView, params)

        //find id of collapsed view layout
        collapsedView = mFloatingWidgetView!!.findViewById(R.id.collapse_view)

        collapsedItem = collapsedView!!.findViewById(R.id.collapsed_iv)
        //find id of the expanded view layout
        expandedView = RecyclerView(this)
        val screen: Point? = getDisplayDimensions(this)
        screenWidth = screen?.x?.toFloat() ?: 0f
        screenHeight = screen?.y?.toFloat() ?: 0f
        Log.e(TAG, "Tamaño de la pantalla: $screen")
        mFloatingWidgetView?.viewTreeObserver?.addOnGlobalLayoutListener {
            viewWidth = mFloatingWidgetView?.measuredWidth?.toFloat() ?: 0f
            viewHeight = mFloatingWidgetView?.measuredHeight?.toFloat()?: 0f
        }
    }

    private val windowManagerDefaultDisplay: Unit
        get() {
            mWindowManager!!.defaultDisplay
                .getSize(szWindow)
        }

    /*  Implement Touch Listener to Floating Widget Root View  */
    private fun implementTouchListenerToFloatingWidgetView() {
        //Drag and move floating view using user's touch action.
        mFloatingWidgetView!!.findViewById<View>(R.id.root_container)
            .setOnTouchListener { v, event ->
                restoreTransparency()
                when(event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mActivePointerId = event.getPointerId(0)
                        aLastTouchX = event.getX(mActivePointerId)
                        aLastTouchY = event.getY(mActivePointerId)
                        val point = IntArray(2)
                        v.getLocationOnScreen(point)

                        currentPositionX = point[0].toFloat()
                        currentPositionY = point[1].toFloat()

                        startPositionX = aLastTouchX
                        startPositionY = aLastTouchY

                        moveListener?.onDown()
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isAClick(
                                clickThreshold,
                                startPositionX,
                                event.x,
                                startPositionY,
                                event.y
                            )
                        ) {
                            // If the state is Open, the it will close after this click
                            if (isMenuOpen()) beginGoTransparentProcess()
                            toggleMenu()
                            Log.e(TAG, "Has hecho click")
                        } else {
                            if (!isMenuOpen()) beginGoTransparentProcess()
                            if (movementStyle === FloatingStyle.STICKED_TO_SIDES) {
                                val padding = 10
                                val boundaries: BooleanArray =
                                    isGlobalViewOutsideBoundaries(padding)
                                val top = boundaries[1]
                                val bottom = boundaries[3]
                                if (top) {
                                    currentPositionY = 100f // top
                                } else if (bottom) {
                                    currentPositionY = screenHeight - (100f + viewHeight)
                                }

                                Log.e(TAG, "Current position and screen size: $currentPositionX , $screenWidth")

                                // Force the button to stick either to right or left of the screen
                                currentPositionX = if (currentPositionX >= screenWidth / 2) {
                                    screenWidth - viewWidth
                                } else {
                                    0f
                                }

                                val layoutParams: WindowManager.LayoutParams =
                                    mFloatingWidgetView!!.layoutParams as WindowManager.LayoutParams
                                layoutParams.x = currentPositionX.toInt()
                                layoutParams.y = currentPositionY.toInt()
                                mWindowManager!!.updateViewLayout(mFloatingWidgetView, layoutParams)
                            }
                            if (isMenuOpened) {
                                val p = Point()
                                p.x += (viewWidth / 2 + currentPositionX.toInt()).toInt()
                                p.y += (viewHeight / 2 + currentPositionY.toInt()).toInt()
                                reOpenMenu()
                            }
                        }
                        moveListener?.onUp()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (movementStyle !== FloatingStyle.ANCHORED) {
                            val pointerIndexMove = event.findPointerIndex(mActivePointerId)
                            val point = IntArray(2)
                            v.getLocationOnScreen(point)
                            pointOnScreen = Point(point[0], point[1])
                            // get the old coordinates
                            val oldPositionX: Float = point[0].toFloat()
                            val oldPositionY: Float = point[1].toFloat()
                            // calculate the new coordinates based on the finger's movement
                            currentPositionX = oldPositionX + event.getRawX(pointerIndexMove) - aLastTouchX
                            currentPositionY = oldPositionY + event.getRawY(pointerIndexMove) - aLastTouchY
                            val layoutParams: WindowManager.LayoutParams =
                                mFloatingWidgetView!!.layoutParams as WindowManager.LayoutParams
                            layoutParams.x = currentPositionX.toInt()
                            layoutParams.y = currentPositionY.toInt()
                            mWindowManager!!.updateViewLayout(mFloatingWidgetView, layoutParams)
                            // check if the difference between old and new coordinates violates the boundaries

                            moveListener?.move(currentPositionX, currentPositionY)
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        mActivePointerId = INVALID_POINTER_ID
                        true
                    }
                    else -> false
                }
            }
    }


    private fun beginGoTransparentProcess() {
        transparancyHandler.removeCallbacks(transparencyRunnable)
        if (transparentAfterMilliseconds >= 0) {
            transparancyHandler.postDelayed(
                transparencyRunnable,
                transparentAfterMilliseconds.toLong()
            )
        }
    }

    private fun restoreTransparency() {
        if (iconClose != null) {
            changeImage(iconClose!!)
        } else {
            mFloatingWidgetView?.alpha = 1f
        }
        transparancyHandler.removeCallbacks(transparencyRunnable)
    }

    open fun openMenu() {
        transparancyHandler.removeCallbacks(transparencyRunnable)
        if (iconOpen != null) changeImage(iconOpen!!)
        isMenuOpened = true
        expandedView?.parent?.let {
            (it as ViewGroup).removeView(expandedView)
        }
        linearLayoutParent?.addView(expandedView!!)
    }

    private fun changeImage(
        image: Drawable
    ) {
        collapsedItem!!.setImageDrawable(image)
    }

    open fun closeMenu() {
        if (iconClose != null) {
            changeImage(iconClose!!)
        }
        isMenuOpened = false
        expandedView?.parent?.let {
            (it as ViewGroup).removeView(expandedView)
        }
    }

    private fun reOpenMenu() {
        isMenuOpened = false
    }

    open fun toggleMenu() {
        if (isMenuOpened) {
            closeMenu()
        } else {
            openMenu()
        }
    }

    open fun isMenuOpen(): Boolean {
        return isMenuOpened
    }

    private fun setDefaultImage() {
        if (iconClose != null) {
            changeImage(iconClose!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        /*  on destroy remove both view from window manager */if (mFloatingWidgetView != null) mWindowManager!!.removeView(
            mFloatingWidgetView
        )
        if (removeFloatingWidgetView != null) mWindowManager!!.removeView(removeFloatingWidgetView)
    }


    private fun isGlobalViewOutsideBoundaries(padding: Int): BooleanArray {
        val results = BooleanArray(4)
        val realRadius: Float = getRealRadius(padding)

        // get the center
        val center: Point = getActionViewCenter()
        results[0] = center.x - realRadius <= 0 // left
        results[2] = center.x + realRadius >= screenWidth // right
        results[1] = center.y - realRadius <= 0 // top
        results[3] = center.y + realRadius >= screenHeight // bottom
        return results
    }

    private fun getRealRadius(padding: Int): Float {
        // 1 - get the largest width/height of the children
        val largestWidth = pointOnScreen.x
        val largestHeight = pointOnScreen.y
        return padding + 100f + (if (largestWidth >= largestHeight) largestWidth else largestHeight) / 2f
    }

    private fun getActionViewCenter(): Point {
        val point: Point = getActionViewCoordinates()
        point.x += (pointOnScreen.x / 2).toInt()
        point.y += (pointOnScreen.y / 2).toInt()
        return point
    }


    private fun getActionViewCoordinates(): Point {
        val coordinates = IntArray(2)
        // This method returns a x and y values that can be larger than the dimensions of the device screen.
        mFloatingWidgetView?.getLocationOnScreen(coordinates)
        // We then need to deduce the offsets
        val activityFrame = Rect()
        if (mFloatingWidgetView != null) {
            mFloatingWidgetView?.getWindowVisibleDisplayFrame(activityFrame)
            coordinates[0] -= getScreenSize(this)!!.x - pointOnScreen.x.toInt()
            coordinates[1] -= activityFrame.height() + activityFrame.top - pointOnScreen.y.toInt()
        }
        return Point(coordinates[0], coordinates[1])
    }

    private fun isCentralViewOutsideBoundaries(
        oldPositionX: Float,
        oldPositionY: Float,
        newPositionX: Float,
        newPositionY: Float
    ): BooleanArray {
        val results = BooleanArray(4)

        results[0] = newPositionX <= 0 && newPositionX <= oldPositionX // left
        results[2] =
            newPositionX + pointOnScreen.x >= screenWidth && newPositionX >= oldPositionX // right
        results[1] = newPositionY <= 0 && newPositionY <= oldPositionY // top
        results[3] =
            newPositionY + pointOnScreen.y >= screenHeight && newPositionY >= oldPositionY // bottom
        return results
    }

    private val transparencyRunnable = Runnable {
        if (iconHideLeft == null || iconHideRight == null) {
            val fadeOut: Animation = AlphaAnimation(1f, 0.6f)
            fadeOut.interpolator = AccelerateInterpolator() //and this
            fadeOut.duration = 300
            fadeOut.setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    mFloatingWidgetView?.alpha = 0.6f
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
            val animation = AnimationSet(false) //change to false
            animation.addAnimation(fadeOut)
        } else {
            if (pointOnScreen.x < screenWidth / 2) {
                if (iconHideLeft != null) changeImage(
                    iconHideLeft!!
                ) else changeImage(iconHideRight!!)
            } else {
                if (iconHideRight != null) changeImage(
                    iconHideRight!!
                ) else changeImage(iconHideLeft!!)
            }
        }
    }
}