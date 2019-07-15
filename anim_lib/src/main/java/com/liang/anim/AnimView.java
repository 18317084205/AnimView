package com.liang.anim;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.*;
import android.support.annotation.ArrayRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnimView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private ExecutorService executors = Executors.newSingleThreadExecutor();
    private String assetsFolder = "";
    private String[] strings;
    private TypedArray typedArray;// 用于播放动画的图片资源id数组
    private boolean isStart = false;
    private boolean isPause = false;
    private boolean isAutoStart;
    private boolean isRunning = false;
    private boolean isInitialized = false;
    private long duration;
    private boolean isLoop;
    private Rect srcRect;
    private Rect destRect;
    private Canvas canvas;
    private Paint paint = new Paint();
    private int index = 0;

    private SurfaceHolder mSurfaceHolder;
    private AssetManager assetsManager;

    private AnimationListener animationListener;

    public AnimView(Context context) {
        this(context, null);
    }

    public AnimView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AnimSurfaceView,
                defStyleAttr, 0);
        isLoop = typedArray.getBoolean(R.styleable.AnimSurfaceView_loop, false);
        duration = typedArray.getInt(R.styleable.AnimSurfaceView_duration, 100);
        isAutoStart = typedArray.getBoolean(R.styleable.AnimSurfaceView_autoStart, false);
        setAnimAssets(typedArray.getString(R.styleable.AnimSurfaceView_animAssets));
        setAnimResource(typedArray.getResourceId(R.styleable.AnimSurfaceView_animResource, 0));
        typedArray.recycle();
    }

    private void init() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);
        setZOrderOnTop(true);
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        paint.setAntiAlias(true);
        assetsManager = getContext().getAssets();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (typedArray != null) {
            typedArray.recycle();
        }
        executors.shutdown();
    }


    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isLoop() {
        return isLoop;
    }

    public void setLoop(boolean loop) {
        isLoop = loop;
    }

    public AnimationListener getAnimationListener() {
        return animationListener;
    }

    public void setAnimationListener(AnimationListener animationListener) {
        this.animationListener = animationListener;
    }

    @SuppressLint("Recycle")
    public void setAnimResource(@ArrayRes int arrayRes) {
        if (arrayRes == 0) {
            return;
        }
        strings = null;
        typedArray = getResources().obtainTypedArray(arrayRes);
    }

    public void setAnimAssets(String assetsFolder) {
        if (TextUtils.isEmpty(assetsFolder)) {
            return;
        }
        if (typedArray != null) {
            typedArray.recycle();
        }
        typedArray = null;
        this.assetsFolder = assetsFolder;
        try {
            strings = assetsManager.list(assetsFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        isStart = true;
        if (isRunning) {
            return;
        }

        if (!isInitialized) {
            return;
        }

        executors.execute(this);
//        Thread(this).start()
    }

    public void stop() {
        isStart = false;
        isAutoStart = false;
        isPause = false;
    }

    public void pause() {
        isPause = isRunning;
        isStart = false;
        isAutoStart = false;
    }

    public void reStart() {
        index = 0;
        start();
    }


    public void setProgress(float progress) {

        if (isRunning) {
            return;
        }

        float offset = progress;

        if (progress < 0) {
            offset = 0f;
        }

        if (progress > 1) {
            offset = 1f;
        }

        if (typedArray != null && typedArray.length() > 0) {
            index = Math.round((typedArray.length() - 1) * offset);
        } else if (strings != null && strings.length > 0) {
            index = Math.round((strings.length - 1) * offset);
        }

        drawFame(index);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        destRect = new Rect(0, 0, width, height);
        isInitialized = true;

        drawFame(index);

        if (isAutoStart || isPause || isStart) {
            start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    @Override
    public void run() {
        if (typedArray != null) {
            runRes();
        }

        if (strings != null) {
            runAssets();
        }
    }


    public void drawFame(final int index) {
        executors.execute(new Runnable() {
            @Override
            public void run() {
                if (typedArray != null && typedArray.length() > 0 && index < typedArray.length()) {
                    drawRes(typedArray.getResourceId(index, 0));
                } else if (strings != null && strings.length > 0 && index < strings.length) {
                    drawAssets(assetsFolder + "/" + strings[index]);
                }
            }
        });
    }


    private void runRes() {
        if (typedArray != null && typedArray.length() > 0) {
            isRunning = true;
            postAnimationStart();
            do {
                drawRes(typedArray.getResourceId(index, 0));
                index++;
                if (isLoop && index == typedArray.length()) {
                    index = 0;
                    postAnimationRepeat();
                }
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (isStart && index < typedArray.length());

            refreshIndex(typedArray.length());

            isRunning = false;
            postAnimationEnd();
        }
    }

    private void runAssets() {
        if (strings != null && strings.length > 0) {
            isRunning = true;
            postAnimationStart();
            do {
                drawAssets(assetsFolder + "/" + strings[index]);
                index++;
                if (isLoop && index == strings.length) {
                    index = 0;
                    postAnimationRepeat();
                }
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (isStart && index < strings.length);

            refreshIndex(strings.length);

            isRunning = false;
            postAnimationEnd();
        }
    }

    private void refreshIndex(int count) {

        if (index >= count) {
            index = 0;
        }

        index = Math.max(0, index - 1);
    }

    private void drawRes(int resId) {
        drawBitmap(BitmapFactory.decodeResource(getResources(), resId));
    }

    private void drawAssets(String name) {
        try {
            drawBitmap(BitmapFactory.decodeStream(assetsManager.open(name)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawBitmap(Bitmap bitmap) {
        if (mSurfaceHolder == null || bitmap == null) {
            return;
        }
        synchronized (mSurfaceHolder) {
            canvas = mSurfaceHolder.lockCanvas();
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    if (!bitmap.isRecycled()) {
                        srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        canvas.drawBitmap(bitmap, srcRect, destRect, paint);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                    bitmap.recycle();
                }
            }
        }
    }


    private void postAnimationStart() {
        post(new Runnable() {
            @Override
            public void run() {
                if (animationListener != null) {
                    animationListener.onAnimStart();
                }
            }
        });
    }

    private void postAnimationEnd() {
        post(new Runnable() {
            @Override
            public void run() {
                if (animationListener != null) {
                    animationListener.onAnimEnd();
                }
            }
        });
    }

    private void postAnimationRepeat() {
        post(new Runnable() {
            @Override
            public void run() {
                if (animationListener != null) {
                    animationListener.onAnimRepeat();
                }
            }
        });
    }


    public interface AnimationListener {
        void onAnimStart();

        void onAnimEnd();

        void onAnimRepeat();
    }
}
