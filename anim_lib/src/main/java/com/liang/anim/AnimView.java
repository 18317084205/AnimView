package com.liang.anim;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.*;
import android.support.annotation.ArrayRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AnimView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final ThreadPoolExecutor executors= new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    private String assetsFolder = "";
    private ArrayList<String> strings = new ArrayList<>();
    private ArrayList<Integer> resIds = new ArrayList<>();

    private boolean isStart = false;
    private boolean isPause = false;
    private boolean isAutoStart;
    private boolean isRunning = false;
    private boolean isInitialized = false;
    private long duration;
    private boolean isLoop;
    private Rect srcRect = new Rect();
    private Rect destRect;
    private Paint paint = new Paint();
    private int index = 0;

    private boolean isDestroyed = true;

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
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);
        setZOrderOnTop(true);
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        paint.setAntiAlias(true);
        assetsManager = getContext().getAssets();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
        isLoop = false;
        executors.shutdown();
        strings.clear();
        resIds.clear();
    }

    public boolean isDestroyed() {
        return isDestroyed;
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

    public String getAssetsFolder() {
        return assetsFolder;
    }

    public boolean isStart() {
        return isStart;
    }

    public boolean isPause() {
        return isPause;
    }

    public boolean isAutoStart() {
        return isAutoStart;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public ArrayList<String> getStrings() {
        return strings;
    }

    public ArrayList<Integer> getResIds() {
        return resIds;
    }

    public int getIndex() {
        return index;
    }

    public AnimationListener getAnimationListener() {
        return animationListener;
    }

    public void setAnimationListener(AnimationListener animationListener) {
        this.animationListener = animationListener;
    }

    @SuppressLint("Recycle")
    public void setAnimResource(@ArrayRes final int arrayRes) {
        if (arrayRes == 0) {
            return;
        }
        strings.clear();
        resIds.clear();
        index = 0;
        executors.execute(new Runnable() {
            @Override
            public void run() {
                TypedArray typedArray = getResources().obtainTypedArray(arrayRes);
                for (int i = 0; i < typedArray.length(); i++) {
                    resIds.add(typedArray.getResourceId(i, 0));
                }
                typedArray.recycle();
                checkStart();
            }
        });
    }

    public void setAnimAssets(final String assetsFolder) {
        if (TextUtils.isEmpty(assetsFolder)) {
            return;
        }
        resIds.clear();
        strings.clear();
        this.assetsFolder = assetsFolder;
        index = 0;
        executors.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    strings.addAll(Arrays.asList(assetsManager.list(assetsFolder)));
                    checkStart();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void checkStart() {
        if (isAutoStart || isStart) {
            start();
        }
    }

    public void start() {

        if (isDestroyed || isRunning || !isInitialized) {
            return;
        }

        isStart = true;
        executors.execute(this);
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

    void resume() {
        if (isAutoStart || isPause || isStart) {
            start();
        }
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

        if (resIds.size() > 0) {
            index = Math.round((resIds.size() - 1) * offset);
        } else if (strings != null && strings.size() > 0) {
            index = Math.round((strings.size() - 1) * offset);
        }

        drawFame(index);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isDestroyed = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        destRect = new Rect(0, 0, width, height);
        isInitialized = true;
        drawFame(index);

        resume();
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isDestroyed = true;
        pause();
    }

    @Override
    public void run() {
        if (!resIds.isEmpty()) {
            runRes();
        }

        if (!strings.isEmpty()) {
            runAssets();
        }
    }


    public void drawFame(final int index) {

        if (isDestroyed) {
            return;
        }

        executors.execute(new Runnable() {
            @Override
            public void run() {
                if (!resIds.isEmpty() && index < resIds.size()) {
                    drawRes(resIds.get(index));
                } else if (!strings.isEmpty() && index < strings.size()) {
                    drawAssets(assetsFolder + "/" + strings.get(index));
                }
            }
        });
    }


    private void runRes() {
        if (!resIds.isEmpty()) {
            isRunning = true;
            postAnimationStart();
            do {
                drawRes(resIds.get(index));
                index++;
                if (isLoop && index == resIds.size()) {
                    index = 0;
                    postAnimationRepeat();
                }
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (isStart && index < resIds.size());

            refreshIndex(resIds.size());

            isRunning = false;
            postAnimationEnd();
        }
    }

    private void runAssets() {
        if (!strings.isEmpty()) {
            isRunning = true;
            postAnimationStart();
            do {
                drawAssets(assetsFolder + "/" + strings.get(index));
                index++;
                if (isLoop && index == strings.size()) {
                    index = 0;
                    postAnimationRepeat();
                }
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (isStart && index < strings.size());

            refreshIndex(strings.size());

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
        drawBitmap(getBitmap(resId));
    }

    private void drawAssets(String name) {
        try {
            drawBitmap(BitmapFactory.decodeStream(assetsManager.open(name)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawBitmap(Bitmap bitmap) {
        synchronized (this) {

            if (isDestroyed) {
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
                    if (!bitmap.isRecycled()) {
                        srcRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        canvas.drawBitmap(bitmap, srcRect, destRect, paint);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
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

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != VISIBLE) {
            stop();
        }
    }

    private Bitmap getBitmap(int resId){
        BitmapFactory.Options options = new BitmapFactory.Options();
        TypedValue value=new TypedValue();
        getResources().openRawResource(resId, value);
        options.inTargetDensity = value.density;
        options.inScaled = false;
        return BitmapFactory.decodeResource(getResources(), resId, options);
    }

    public interface AnimationListener {
        void onAnimStart();

        void onAnimEnd();

        void onAnimRepeat();
    }
}
