package com.liang.anim;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.ArrayRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * 序列帧动画显示控件
 */
public class SequenceFrameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, Animatable {
    private static final String TAG = "SequenceFrameView";
    private final SparseArray<String> mAssetsPath = new SparseArray<>();
    @SuppressLint("UseSparseArrays")
    private final SparseArray<Integer> mArrayResIds = new SparseArray<>();
    private Rect drawRect = new Rect();
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private boolean mIsRunning;
    private boolean mIsInitialized;
    private long mDuration;
    private boolean mIsLoop;
    private int mIndex = 0;
    private AssetManager mAssetsManager;
    private AnimationListener mAnimationListener;
    private Drawable mBackgroundDrawable;

    public SequenceFrameView(Context context) {
        this(context, null);
    }

    public SequenceFrameView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SequenceFrameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SequenceFrameView,
                defStyleAttr, 0);
        mIsLoop = typedArray.getBoolean(R.styleable.SequenceFrameView_loop, false);
        mDuration = typedArray.getInt(R.styleable.SequenceFrameView_duration, 100);
        String typedArrayPath = typedArray.getString(R.styleable.SequenceFrameView_animAssets);
        int arrayResourceId = typedArray.getResourceId(R.styleable.SequenceFrameView_animResource, 0);
        typedArray.recycle();
        if (!TextUtils.isEmpty(typedArrayPath)) {
            initAnimAssets(typedArrayPath);
        }
        if (arrayResourceId > 0) {
            initAnimResource(arrayResourceId);
        }
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    /**
     * 设置动画监听器
     *
     * @param animationListener 监听器
     */
    public void setAnimationListener(AnimationListener animationListener) {
        mAnimationListener = animationListener;
    }

    @Override
    protected void onDetachedFromWindow() {
        synchronized (this) {
            stop();
            mAssetsPath.clear();
            mArrayResIds.clear();
        }
        super.onDetachedFromWindow();
    }

    /**
     * 设置Assets动画资源
     *
     * @param assetsFolderPath Assets资源文件夹路径
     */
    public void setAnimAssets(final String assetsFolderPath) {
        if (TextUtils.isEmpty(assetsFolderPath)) {
            Log.w(TAG, "setAnimAssets: The path is not found.");
            return;
        }
        initAnimAssets(assetsFolderPath);
    }

    private void initAnimAssets(final String assetsFolderPath) {
        doBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (SequenceFrameView.this) {
                        String[] paths = getAssetManager().list(assetsFolderPath);
                        if (paths == null) {
                            Log.w(TAG, "initAnimAssets: paths is null.");
                            return;
                        }
                        mAssetsPath.clear();
                        mArrayResIds.clear();
                        for (int index = 0; index < paths.length; index++) {
                            mAssetsPath.put(index, assetsFolderPath + "/" + paths[index]);
                        }
                        if (mIsInitialized) {
                            drawFrame(mIndex);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "initAnimAssets: failed", e);
                }
            }
        });
    }

    private AssetManager getAssetManager() {
        if (mAssetsManager == null) {
            mAssetsManager = getContext().getAssets();
        }
        return mAssetsManager;
    }

    /**
     * 设置Res动画资源
     *
     * @param arrayRes 资源列表集合，一般在arrays.xml里边定义
     */
    @SuppressLint("Recycle")
    public void setAnimResource(@ArrayRes int arrayRes) {
        if (arrayRes == 0) {
            Log.w(TAG, "setAnimResource: The integer-array resources is not found.");
            return;
        }
        initAnimResource(arrayRes);
    }

    private void initAnimResource(@ArrayRes final int arrayRes) {
        doBackground(new Runnable() {
            @Override
            public void run() {
                synchronized (SequenceFrameView.this) {
                    mAssetsPath.clear();
                    mArrayResIds.clear();
                    TypedArray typedArray = getResources().obtainTypedArray(arrayRes);
                    for (int index = 0; index < typedArray.length(); index++) {
                        mArrayResIds.put(index, typedArray.getResourceId(index, 0));
                    }
                    typedArray.recycle();
                    if (mIsInitialized) {
                        drawFrame(mIndex);
                    }
                }
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        synchronized (this) {
            drawRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
            drawBackground(holder);
        }
    }

    private void drawBackground(SurfaceHolder holder) {
        if (mBackgroundDrawable != null && holder != null) {
            Log.d(TAG, "drawBackground");
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                try {
                    mBackgroundDrawable.setBounds(drawRect);
                    mBackgroundDrawable.draw(canvas);
                } catch (Exception e) {
                    Log.e(TAG, "drawBackground: failed", e);
                } finally {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        synchronized (this) {
            mBackgroundDrawable = background;
            if (mIsInitialized) {
                drawFrame(mIndex);
            } else {
                drawBackground(getHolder());
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        synchronized (this) {
            drawRect.set(0, 0, width, height);
            mBackgroundDrawable.setBounds(drawRect);
            mIsInitialized = true;
            startHandlerThread();
            if (mIsRunning) {
                startDrawFrame();
            } else {
                if (mHandler != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            drawFrame(mIndex);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        synchronized (this) {
            mIsInitialized = false;
            stopHandlerThread();
        }
    }

    @Override
    public void run() {
        if (mHandler == null) {
            Log.w(TAG, "run: failed, This thread is died");
            return;
        }
        synchronized (this) {
            doDrawing();
        }
    }

    @Override
    public void start() {
        if (mIsRunning) {
            return;
        }
        mIsRunning = true;
        synchronized (this) {
            if (mIsInitialized) {
                startDrawFrame();
            }
        }
    }

    /**
     * 重新启动动画，动画从头开始播放
     */
    public void reStart() {
        synchronized (this) {
            mIndex = 0;
        }
        if (mAnimationListener != null) {
            mAnimationListener.onAnimRepeat();
        }
        start();
    }

    private void startDrawFrame() {
        if (mHandler == null) {
            Log.w(TAG, "start: failed, This thread is died");
            return;
        }
        if (mAnimationListener != null) {
            mAnimationListener.onAnimStart();
        }
        mHandler.post(this);
    }

    @Override
    public void stop() {
        mIsRunning = false;
        if (mHandler == null) {
            Log.w(TAG, "stop: The anim thread is died");
            return;
        }
        mHandler.removeCallbacks(this);
        if (mAnimationListener != null) {
            mAnimationListener.onAnimEnd();
        }
        refreshIndex();
    }

    private void refreshIndex() {
        synchronized (this) {
            boolean isFinish = mIndex >= mAssetsPath.size() && mIndex >= mArrayResIds.size();
            if (isFinish) {
                mIndex = 0;
            }
            mIndex = Math.max(0, mIndex - 1);
        }
    }

    /**
     * 按整体进度绘制动画
     *
     * @param progress 动画进度 0.0 - 1.0
     */
    public void drawProgress(float progress) {
        if (mIsRunning) {
            return;
        }
        float offset = progress;
        if (progress < 0) {
            offset = .0f;
        }
        if (progress > 1) {
            offset = 1.0f;
        }
        synchronized (this) {
            if (mArrayResIds.size() > 0) {
                mIndex = Math.round((mArrayResIds.size() - 1) * offset);
            } else if (mAssetsPath.size() > 0) {
                mIndex = Math.round((mAssetsPath.size() - 1) * offset);
            }
            if (mHandler != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        drawFrame(mIndex);
                    }
                });
            }
        }
    }

    /**
     * 绘制动画某一帧
     *
     * @param index 帧下标
     */
    private void drawFrame(int index) {
        drawBitmap(mArrayResIds.size() > 0 ? getBitmap(mArrayResIds.get(index, 0)) : getBitmap(mAssetsPath.get(index)));
    }

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    private void startHandlerThread() {
        if (mHandlerThread != null && mHandlerThread.isAlive()) {
            return;
        }
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        Looper looper = mHandlerThread.getLooper();
        if (looper != null) {
            mHandler = new Handler(looper);
        }
    }

    private void stopHandlerThread() {
        if (mHandler == null) {
            Log.w(TAG, "stopHandlerThread: The anim thread is died");
            return;
        }
        mHandlerThread.quit();
        mHandler = null;
        if (mAnimationListener != null) {
            mAnimationListener.onAnimEnd();
        }
    }

    private void doBackground(Runnable runnable) {
        if (runnable == null) {
            Log.w(TAG, "doBackground failed: runnable is null");
            return;
        }
        startHandlerThread();
        if (mHandler == null) {
            Log.w(TAG, "doBackground failed: The anim thread is died");
            return;
        }
        mHandler.post(runnable);
    }

    private void doDrawing() {
        boolean isFinish = mIndex >= mAssetsPath.size() && mIndex >= mArrayResIds.size();
        if (isFinish) {
            if (mIsLoop) {
                mIndex = 0;
            } else {
                mIsRunning = false;
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (mAnimationListener != null) {
                            mAnimationListener.onAnimEnd();
                        }
                    }
                });
                return;
            }
        }
        if (mHandler == null) {
            Log.w(TAG, "doDrawing: The anim thread is died");
            return;
        }
        mHandler.postDelayed(this, mDuration);
        drawFrame(mIndex);
        mIndex++;
    }

    private void drawBitmap(Bitmap bitmap) {
        synchronized (this) {
            if (!mIsInitialized) {
                return;
            }
            SurfaceHolder surfaceHolder = getHolder();
            if (surfaceHolder == null || bitmap == null) {
                return;
            }
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    mBackgroundDrawable.draw(canvas);
                    if (!bitmap.isRecycled()) {
                        canvas.drawBitmap(bitmap, null, drawRect, null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "drawBitmap: failed", e);
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                    bitmap.recycle();
                }
            }
        }
    }

    private Bitmap getBitmap(String assetsPath) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getAssetManager().open(assetsPath));
        } catch (IOException e) {
            Log.e(TAG, "getBitmap: failed", e);
        }
        return bitmap;
    }

    private Bitmap getBitmap(int resId) {
        return BitmapFactory.decodeResource(getResources(), resId);
    }

    /**
     * 动画监听器
     */
    public interface AnimationListener {
        void onAnimStart();

        void onAnimEnd();

        void onAnimRepeat();
    }
}
