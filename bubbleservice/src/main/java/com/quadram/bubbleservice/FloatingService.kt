package com.quadram.bubbleservice

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.view.View.inflate
import android.widget.FrameLayout
import android.widget.ImageSwitcher
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.view.contains
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.Exception
import kotlin.math.exp


open abstract class FloatingService: Service() {
    private var mWindowManager: WindowManager? = null
    private var mFloatingWidgetView: View? = null
    private var collapsedView: View? = null
    private var expandedView: View? = null
    private var remove_image_view: ImageView? = null
    private val szWindow: Point = Point()
    private var removeFloatingWidgetView: View? = null
    private var x_init_cord: Int = 0
    private var y_init_cord: Int = 0
    private var x_init_margin: Int = 0
    private var y_init_margin: Int = 0
    private var lastCoordinate: Int = 0
    private var linearLayoutParent: LinearLayout? = null
    private var lastState: FloatingStates = FloatingStates.DEFAULT
    private var collapsedItem: ImageSwitcher? = null
    private var changeImageRunnable: Runnable? = null
    private var changeToWaitingRunnable: Runnable? = null
    private var handler: Handler? = null

    //Variable to check if the Floating widget view is on left side or in right side
    // initially we are displaying Floating widget view to Left side so set it to true
    private var isLeft: Boolean = true

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    abstract val removeViewDrawable: Drawable?
    abstract val drawableStates: List<DrawableState>
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

        //init WindowManager
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManagerDefaultDisplay

        //Init LayoutInflater
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addRemoveView(inflater)
        addFloatingWidgetView(inflater)
        implementTouchListenerToFloatingWidgetView()
        collapsedItem?.setFactory { ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        } }
        modifyState(FloatingStates.CLOSE)
        setItems(items, callback)
    }

    private fun getWindowViewType(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
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
        changeImageRunnable = Runnable {
            if (isLeft) {
                modifyState(FloatingStates.IDLE_LEFT)
            } else {
                modifyState(FloatingStates.IDLE_RIGHT)
            }
        }
        changeToWaitingRunnable = Runnable {
            modifyState(FloatingStates.WAITING)
        }
        handler = Handler()
    }

    private fun modifyState(state: FloatingStates) {
        this.lastState = state
        when (state) {
            FloatingStates.WAITING -> handler?.postDelayed(changeImageRunnable!!, timeToSetTransparent)
            FloatingStates.OPEN -> {
                handler?.removeCallbacks(changeImageRunnable!!)
                handler?.removeCallbacks(changeToWaitingRunnable!!)
            }
            FloatingStates.IDLE_LEFT -> {
                handler?.removeCallbacks(changeImageRunnable!!)
                handler?.removeCallbacks(changeToWaitingRunnable!!)
            }
            FloatingStates.IDLE_RIGHT -> {
                handler?.removeCallbacks(changeImageRunnable!!)
                handler?.removeCallbacks(changeToWaitingRunnable!!)
            }
            FloatingStates.CLOSE -> handler?.postDelayed(changeToWaitingRunnable!!, timeToWaiting)
            FloatingStates.MOVING -> {
                handler?.removeCallbacks(changeImageRunnable!!)
                handler?.removeCallbacks(changeToWaitingRunnable!!)
                this.lastState = FloatingStates.CLOSE
            }
            FloatingStates.DEFAULT -> {}
        }
        val drawable = drawableStates.firstOrNull { it.state == this.lastState }
        drawable?.let {
            collapsedItem?.setImageDrawable(it.drawable)
        } ?: run {
            collapsedItem?.setImageDrawable(drawableStates.firstOrNull { it.state == FloatingStates.DEFAULT }?.drawable ?:
            throw IllegalStateException("You need to provide a default state with FloatingStates.DEFAULT options"))
        }
    }

    private val windowManagerDefaultDisplay: Unit
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) mWindowManager!!.defaultDisplay
                .getSize(szWindow) else {
                val w: Int = mWindowManager!!.defaultDisplay.width
                val h: Int = mWindowManager!!.defaultDisplay.height
                szWindow.set(w, h)
            }
        }

    /*  Implement Touch Listener to Floating Widget Root View  */
    private fun implementTouchListenerToFloatingWidgetView() {
        //Drag and move floating view using user's touch action.
        mFloatingWidgetView!!.findViewById<View>(R.id.root_container)
            .setOnTouchListener(object : OnTouchListener {
                var time_start: Long = 0
                var time_end: Long = 0
                var isLongClick: Boolean = false //variable to judge if user click long press
                var inBounded: Boolean =
                    false //variable to judge if floating view is bounded to remove view
                var remove_img_width: Int = 0
                var remove_img_height: Int = 0
                var handler_longClick: Handler = Handler()
                var runnable_longClick: Runnable = Runnable {
                    isLongClick = true
                    removeFloatingWidgetView!!.visibility = View.VISIBLE
                    onFloatingWidgetLongClick()
                }

                override fun onTouch(
                    v: View,
                    event: MotionEvent
                ): Boolean {

                    //Get Floating widget view params
                    val layoutParams: WindowManager.LayoutParams =
                        mFloatingWidgetView!!.layoutParams as WindowManager.LayoutParams

                    //get the touch location coordinates
                    val x_cord: Int = event.rawX.toInt()
                    val y_cord: Int = event.rawY.toInt()
                    val x_cord_Destination: Int
                    var y_cord_Destination: Int
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            time_start = System.currentTimeMillis()
                            handler_longClick.postDelayed(runnable_longClick, 600)
                            remove_img_width = remove_image_view!!.layoutParams.width
                            remove_img_height = remove_image_view!!.layoutParams.height
                            x_init_cord = x_cord
                            y_init_cord = y_cord

                            //remember the initial position.
                            x_init_margin = layoutParams.x
                            y_init_margin = layoutParams.y
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            isLongClick = false
                            removeFloatingWidgetView!!.visibility = View.GONE
                            remove_image_view!!.layoutParams.height = remove_img_height
                            remove_image_view!!.layoutParams.width = remove_img_width
                            handler_longClick.removeCallbacks(runnable_longClick)

                            //If user drag and drop the floating widget view into remove view then stop the service
                            if (inBounded) {
                                stopSelf()
                                inBounded = false
                                return false
                            }


                            //Get the difference between initial coordinate and current coordinate
                            val x_diff: Int = x_cord - x_init_cord
                            val y_diff: Int = y_cord - y_init_cord

                            //The check for x_diff <5 && y_diff< 5 because sometime elements moves a little while clicking.
                            //So that is click event.
                            if (Math.abs(x_diff) < 5 && Math.abs(y_diff) < 5) {
                                time_end = System.currentTimeMillis()

                                //Also check the difference between start time and end time should be less than 300ms
                                if ((time_end - time_start) < 300)
                                    toggle()
                            }
                            y_cord_Destination = y_init_margin + y_diff
                            val barHeight: Int = statusBarHeight
                            if (y_cord_Destination < 0) {
                                y_cord_Destination = 0
                            } else if (y_cord_Destination + (mFloatingWidgetView!!.height + barHeight) > szWindow.y) {
                                y_cord_Destination =
                                    szWindow.y - (mFloatingWidgetView!!.height + barHeight)
                            }
                            layoutParams.y = y_cord_Destination
                            inBounded = false

                            //reset position if user drags the floating view
                            lastCoordinate = x_cord
                            resetPosition(x_cord)
                            if (lastState != FloatingStates.OPEN) {
                                modifyState(lastState)
                            }
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (!isViewCollapsed) {
                                closeView()
                            }
                            val x_diff_move: Int = x_cord - x_init_cord
                            val y_diff_move: Int = y_cord - y_init_cord
                            x_cord_Destination = x_init_margin + x_diff_move
                            y_cord_Destination = y_init_margin + y_diff_move

                            //If user long click the floating view, update remove view
                            if (isLongClick) {
                                val x_bound_left: Int =
                                    szWindow.x / 2 - (remove_img_width * 1.5).toInt()
                                val x_bound_right: Int =
                                    szWindow.x / 2 + (remove_img_width * 1.5).toInt()
                                val y_bound_top: Int =
                                    szWindow.y - (remove_img_height * 1.5).toInt()

                                //If Floating view comes under Remove View update Window Manager
                                if ((x_cord in x_bound_left..x_bound_right) && y_cord >= y_bound_top) {
                                    inBounded = true
                                    val x_cord_remove: Int =
                                        ((szWindow.x - (remove_img_height * 1.5)) / 2).toInt()
                                    val y_cord_remove: Int =
                                        (szWindow.y - ((remove_img_width * 1.5) + statusBarHeight)).toInt()
                                    if (remove_image_view!!.layoutParams.height == remove_img_height) {
                                        remove_image_view!!.layoutParams.height =
                                            (remove_img_height * 1.5).toInt()
                                        remove_image_view!!.layoutParams.width =
                                            (remove_img_width * 1.5).toInt()
                                        val param_remove: WindowManager.LayoutParams =
                                            removeFloatingWidgetView!!.layoutParams as WindowManager.LayoutParams
                                        param_remove.x = x_cord_remove
                                        param_remove.y = y_cord_remove
                                        mWindowManager!!.updateViewLayout(
                                            removeFloatingWidgetView,
                                            param_remove
                                        )
                                    }
                                    layoutParams.x =
                                        x_cord_remove + (Math.abs(removeFloatingWidgetView!!.width - mFloatingWidgetView!!.width)) / 2
                                    layoutParams.y =
                                        y_cord_remove + (Math.abs(removeFloatingWidgetView!!.height - mFloatingWidgetView!!.height)) / 2

                                    //Update the layout with new X & Y coordinate
                                    mWindowManager!!.updateViewLayout(
                                        mFloatingWidgetView,
                                        layoutParams
                                    )
                                    return false
                                } else {
                                    //If Floating window gets out of the Remove view update Remove view again
                                    inBounded = false
                                    remove_image_view!!.layoutParams.height = remove_img_height
                                    remove_image_view!!.layoutParams.width = remove_img_width
                                    //onFloatingWidgetClick()
                                }
                            }
                            layoutParams.x = x_cord_Destination
                            layoutParams.y = y_cord_Destination
                            modifyState(FloatingStates.MOVING)
                            //Update the layout with new X & Y coordinate
                            mWindowManager!!.updateViewLayout(mFloatingWidgetView, layoutParams)
                            return true
                        }
                    }
                    return false
                }
            })
    }


    /*  on Floating Widget Long Click, increase the size of remove view as it look like taking focus */
    private fun onFloatingWidgetLongClick() {
        //Get remove Floating view params
        val removeParams: WindowManager.LayoutParams =
            removeFloatingWidgetView!!.layoutParams as WindowManager.LayoutParams

        //get x and y coordinates of remove view
        val x_cord: Int = (szWindow.x - removeFloatingWidgetView!!.width) / 2
        val y_cord: Int = szWindow.y - (removeFloatingWidgetView!!.height + statusBarHeight)
        removeParams.x = x_cord
        removeParams.y = y_cord

        //Update Remove view params
        mWindowManager!!.updateViewLayout(removeFloatingWidgetView, removeParams)
    }

    /*  Reset position of Floating Widget view on dragging  */
    private fun resetPosition(x_cord_now: Int) {
        Log.e("JFEM", "$x_cord_now posicion reseteada")
        if (x_cord_now <= szWindow.x / 2) {
            isLeft = true
            moveToLeft(x_cord_now)
        } else {
            isLeft = false
            moveToRight(x_cord_now)
        }
    }

    /*  Method to move the Floating widget view to Left  */
    private fun moveToLeft(current_x_cord: Int) {
        val mParams: WindowManager.LayoutParams =
            mFloatingWidgetView!!.layoutParams as WindowManager.LayoutParams
        mParams.x = 0

        //Update window manager for Floating Widget
        mWindowManager!!.updateViewLayout(mFloatingWidgetView, mParams)
    }

    /*  Method to move the Floating widget view to Right  */
    private fun moveToRight(current_x_cord: Int) {
        val mParams: WindowManager.LayoutParams =
            mFloatingWidgetView!!.layoutParams as WindowManager.LayoutParams
        mParams.x = szWindow.x + current_x_cord - mFloatingWidgetView!!.width

        //Update window manager for Floating Widget
        mWindowManager!!.updateViewLayout(mFloatingWidgetView, mParams)
    }

    /*  Get Bounce value if you want to make bounce effect to your Floating Widget */
    private fun bounceValue(step: Long, scale: Long): Double {
        val value: Double =
            scale * Math.exp(-0.055 * step) * Math.cos(0.08 * step)
        return value
    }

    /*  Detect if the floating view is collapsed or expanded */
    private val isViewCollapsed: Boolean
        get() = expandedView?.parent == null

    /*  return status bar height on basis of device display metrics  */
    private val statusBarHeight: Int
        get() {
            return Math.ceil(
                25 * applicationContext.resources.displayMetrics.density.toDouble()
            ).toInt()
        }

    /*  Update Floating Widget view coordinates on Configuration change  */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        windowManagerDefaultDisplay
        val layoutParams: WindowManager.LayoutParams =
            mFloatingWidgetView!!.layoutParams as WindowManager.LayoutParams
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (layoutParams.y + (mFloatingWidgetView!!.height + statusBarHeight) > szWindow.y) {
                layoutParams.y =
                    szWindow.y - (mFloatingWidgetView!!.height + statusBarHeight)
                mWindowManager!!.updateViewLayout(mFloatingWidgetView, layoutParams)
            }
            if (layoutParams.x != 0 && layoutParams.x < szWindow.x) {
                resetPosition(szWindow.x)
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (layoutParams.x > szWindow.x) {
                resetPosition(szWindow.x)
            }
        }
    }

    /*  on Floating widget click show expanded view  */
    private fun onFloatingWidgetClick() {
        if (isViewCollapsed) {
            if (expandedView?.parent != null ) {
                (expandedView?.parent as ViewGroup).removeView(expandedView)
            }
            linearLayoutParent?.addView(expandedView!!)
        }
    }

    fun toggle() {
        if (isViewCollapsed) {
            modifyState(FloatingStates.OPEN)
            onFloatingWidgetClick()
        } else {
            modifyState(FloatingStates.CLOSE)
            closeView()
        }
    }

    private fun closeView() {
        resetPosition(lastCoordinate)
        linearLayoutParent?.removeView(expandedView)
    }

    override fun onDestroy() {
        super.onDestroy()

        /*  on destroy remove both view from window manager */if (mFloatingWidgetView != null) mWindowManager!!.removeView(
            mFloatingWidgetView
        )
        if (removeFloatingWidgetView != null) mWindowManager!!.removeView(removeFloatingWidgetView)
    }
}