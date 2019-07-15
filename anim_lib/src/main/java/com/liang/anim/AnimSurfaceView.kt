package com.liang.anim

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.graphics.*
import android.support.annotation.ArrayRes
import android.util.Log
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt


class AnimSurfaceView : SurfaceView, SurfaceHolder.Callback, Runnable {

    private val executors = Executors.newSingleThreadExecutor()
    private var assetsFolder = ""
    private var strings: Array<String>? = null
    private var typedArray: TypedArray? = null// 用于播放动画的图片资源id数组
    private var isStart = false
    private var isPause = false
    private var isAutoStart = false
    private var isRunning = false
    private var isInitialized = false
    var duration = 200L
    var isLoop = false
    private var srcRect: Rect? = null
    private var destRect: Rect? = null
    private var canvas: Canvas? = null
    private val paint = Paint()
    private var index = 0

    var animationListener: AnimationListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AnimSurfaceView,
                defStyle, 0)
        isLoop = typedArray.getBoolean(R.styleable.AnimSurfaceView_loop, false)
        duration = typedArray.getInt(R.styleable.AnimSurfaceView_duration, 100).toLong()
        isAutoStart = typedArray.getBoolean(R.styleable.AnimSurfaceView_autoStart, false)
        setAnimAssets(typedArray.getString(R.styleable.AnimSurfaceView_animAssets))
        setAnimResource(typedArray.getResourceId(R.styleable.AnimSurfaceView_animResource, 0))
        typedArray.recycle()
    }

    private var mSurfaceHolder: SurfaceHolder? = null

    private val assetsManager: AssetManager by lazy {
        context.assets
    }

    init {
        mSurfaceHolder = holder
        mSurfaceHolder?.addCallback(this)
        mSurfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE)
        setZOrderOnTop(true)
        mSurfaceHolder?.setFormat(PixelFormat.TRANSLUCENT)
        paint.isAntiAlias = true;
    }


    @SuppressLint("Recycle")
    fun setAnimResource(@ArrayRes arrayRes: Int) {
        if (arrayRes == 0) {
            return
        }
        strings = null
        typedArray = resources.obtainTypedArray(arrayRes)
    }

    fun setAnimAssets(assetsFolder: String?) {
        if (assetsFolder.isNullOrEmpty()) {
            return
        }
        typedArray?.recycle()
        typedArray = null
        this.assetsFolder = assetsFolder
        strings = assetsManager.list(assetsFolder)
    }

    fun start() {
        isStart = true
        if (isRunning) {
            return
        }

        if (!isInitialized) {
            return
        }

        executors.execute(this)
//        Thread(this).start()
    }

    fun stop() {
        isStart = false
        isAutoStart = false
        isPause = false
    }

    fun pause() {
        isPause = isRunning
        isStart = false
        isAutoStart = false
    }

    fun reStart() {
        index = 0
        start()
    }

    fun setProgress(progress: Float) {

        if (isRunning) {
            return
        }

        val offset = when {
            progress < 0 -> 0f
            progress > 1 -> 1f
            else -> progress
        }

        index = when {
            typedArray != null && typedArray?.length()!! > 0 -> ((typedArray?.length()!! - 1) * offset).roundToInt()
            strings != null && strings?.isNotEmpty()!! -> ((strings?.size!! - 1) * offset).roundToInt()
            else -> 0
        }

        drawFame(index)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        typedArray?.recycle()
        executors.shutdown()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        destRect = Rect(0, 0, width, height)
        isInitialized = true

        drawFame(index)

        if (isAutoStart || isPause || isStart) {
            start()
        }
    }

    fun drawFame(index: Int) {
        if (typedArray != null && typedArray?.length()!! > 0 && index < typedArray?.length()!!) {
            drawRes(typedArray?.getResourceId(index, 0)!!)
        } else if (strings != null && strings?.isNotEmpty()!! && index < strings?.size!!) {
            drawAssets("$assetsFolder/${strings?.get(index)}")
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        pause()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
    }

    override fun run() {
        if (typedArray != null) {
            runRes()
        }

        if (strings != null) {
            runAssets()
        }
    }

    private fun runRes() {
        if (typedArray != null && typedArray?.length()!! > 0) {
            isRunning = true
            post {
                animationListener?.onAnimationStart()
            }
            do {
                drawRes(typedArray?.getResourceId(index, 0)!!)
                index++
                if (isLoop && index == typedArray?.length()!!) {
                    index = 0
                    post {
                        animationListener?.onAnimationRepeat()
                    }
                }
                Thread.sleep(duration)
            } while (isStart && index < typedArray?.length()!!)

            refreshIndex(typedArray?.length()!!)

            isRunning = false
            post {
                animationListener?.onAnimationEnd()
            }
        }
    }

    private fun runAssets() {
        if (strings != null && strings?.isNotEmpty()!!) {
            isRunning = true
            post {
                animationListener?.onAnimationStart()
            }
            do {
                Log.e("runAssets", "index: $index")
                drawAssets("$assetsFolder/${strings?.get(index)}")
                index++
                if (isLoop && index == strings?.size) {
                    index = 0
                    post {
                        animationListener?.onAnimationRepeat()
                    }
                }
                Thread.sleep(duration)
            } while (isStart && index < strings?.size!!)

            refreshIndex(strings?.size!!)

            isRunning = false
            post {
                animationListener?.onAnimationEnd()
            }
        }
    }

    private fun refreshIndex(count: Int) {
        index = when {
            index >= count -> 0
            else -> index
        }
        index = max(0, index - 1)
    }

    private fun drawRes(resId: Int) {
        drawBitmap(BitmapFactory.decodeResource(resources, resId))
    }

    private fun drawAssets(name: String) {
        drawBitmap(BitmapFactory.decodeStream(assetsManager.open(name)))
    }

    private fun drawBitmap(bitmap: Bitmap?) {
        if (mSurfaceHolder == null || bitmap == null) {
            return
        }
        synchronized(mSurfaceHolder!!) {
            canvas = mSurfaceHolder?.lockCanvas()
            if (canvas != null) {
                try {
                    canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    if (!bitmap.isRecycled) {
                        srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                        canvas?.drawBitmap(bitmap, srcRect, destRect, paint)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (canvas != null) {
                        mSurfaceHolder?.unlockCanvasAndPost(canvas)
                    }
                    bitmap.recycle()
                }
            }
        }
    }

    interface AnimationListener {
        fun onAnimationStart()
        fun onAnimationEnd()
        fun onAnimationRepeat()
    }
}