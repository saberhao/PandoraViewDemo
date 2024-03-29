package com.saberhao.pandoraviewdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.Nullable;

public class PandoraView extends View implements GestureDetector.OnGestureListener, View.OnTouchListener {
    private GestureDetector mGestureDetector;
    private BitmapFactory.Options mOptions;
    private Rect mRect;
    private Scroller mScroller;
    private int mImageWidth;
    private int mImageHeight;
    private int mViewWidth;
    private int mViewHeight;
    private float mScale;
    private BitmapRegionDecoder mDecoder;
    private Bitmap mBitmap;
    private boolean isLongImage;  //true: 长图  false: 宽图
    private Matrix matrix = new Matrix();

    public PandoraView(Context context) {
        this(context, null, 0);
    }

    public PandoraView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PandoraView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //第一步：设置PandoraView的成员变量
        mRect = new Rect();
        //内存复用
        mOptions = new BitmapFactory.Options();
        //手势识别
        mGestureDetector = new GestureDetector(context, this);
        //滚动类
        mScroller = new Scroller(context);

        setOnTouchListener(this);
    }

    // 2.设置图片，得到图片的信息
    public void setImage(InputStream is) {
        // 获取图片的宽和高，注意：不能将图片整个加载进内存
        mOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeStream(is, null, mOptions);
        mImageWidth = mOptions.outWidth;
        mImageHeight = mOptions.outHeight;

        // 开启复用
        mOptions.inMutable = true;
        // 设置格式为RGB565 所占内存比ARGB_8888小
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        mOptions.inJustDecodeBounds = false;

        //判断是长图还是宽图
        isLongImage = mImageHeight > mImageWidth;

        // 区域解码器
        try {
            mDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestLayout();

    }

    // 3.开始测量，得到View的宽和高，测量加载的图片到底要缩放成什么样子
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 得到View的宽和高
        mViewWidth = getMeasuredWidth();
        mViewHeight = getMeasuredHeight();

        // 确定要加载图片的区域
        mRect.left = 0;
        mRect.top = 0;

        if (isLongImage) {
            mRect.right = mImageWidth;
            // 计算缩放因子
            mScale = mViewWidth / (float) mImageWidth;
            mRect.bottom = (int) (mViewHeight / mScale);
        } else {
            mRect.bottom = mImageHeight;
            // 计算缩放因子
            mScale = mViewHeight / (float) mImageHeight;
            mRect.right = (int) (mViewWidth / mScale);
        }
        // 得到一个矩阵进行缩放，相当于得到View的大小
        matrix.setScale(mScale, mScale);
    }

    // 4.画出具体的内容
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 判断解码器是不是为null，如果解码器没有拿到，表示没有设置过图片
        if (mDecoder == null) {
            return;
        }
        // 真正的内存复用  注意：复用的bitmap必须和即将解码的bitmap尺寸一样
        mOptions.inBitmap = mBitmap;
        // 指定解码区域
        mBitmap = mDecoder.decodeRegion(mRect, mOptions);

        canvas.drawBitmap(mBitmap, matrix, null);

    }

    // 5.处理点击事件
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // 直接将事件交给手势事件处理
        return mGestureDetector.onTouchEvent(event);
    }

    // 6.手按下去
    @Override
    public boolean onDown(MotionEvent e) {
        // 如果移动没有停止，强制停止
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        // 继续接受后续事件
        return true;
    }

    // 7.处理滑动事件
    /**
     * @param e1        开始事件，手指按下去，开始获取坐标
     * @param e2        获取当前事件坐标
     * @param distanceX x轴移动的距离
     * @param distanceY y轴移动的距离
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

        if (isLongImage){
            // 上下滑动时，mRect需要改变显示的区域
            mRect.offset(0, Math.round(distanceY));
            // 移动时，处理到达底部和顶部的情况
            if (mRect.bottom > mImageHeight) {
                mRect.bottom = mImageHeight;
                mRect.top = mImageHeight - (int) (mViewHeight / mScale);
            }
            if (mRect.top < 0) {
                mRect.top = 0;
                mRect.bottom = (int) (mViewHeight / mScale);
            }
        } else {
            // 左右滑动时，mRect需要改变显示的区域
            mRect.offset(Math.round(distanceX), 0);
            // 移动时，处理到达左边和右边·的情况
            if (mRect.right > mImageWidth) {
                mRect.right = mImageWidth;
                mRect.left = mImageWidth - (int) (mViewWidth / mScale);
            }
            if (mRect.left < 0) {
                mRect.left = 0;
                mRect.right = (int) (mViewWidth / mScale);
            }
        }
        invalidate();
        return false;
    }

    // 8.为Scroller添加滑动惯性
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (isLongImage) {
            mScroller.fling(0, mRect.top, 0, (int) Math.abs(velocityY), 0, 0, 0,
                    mImageHeight - (int) (mViewHeight / mScale));
        } else {
            mScroller.fling(mRect.left, 0, (int) Math.abs(velocityX), 0, 0, mImageWidth
             - (int) (mViewWidth / mScale), 0, 0);
        }
        return false;
    }

    // 9.计算由于Scroller惯性而移位的Rect
    @Override
    public void computeScroll() {
        if (mScroller.isFinished()) {
            return;
        }
        if (mScroller.computeScrollOffset()) {
            if (isLongImage) {
                mRect.top = mScroller.getCurrY();
                mRect.bottom = mRect.top + (int) (mViewHeight / mScale);
            } else {
                mRect.left = mScroller.getCurrX();
                mRect.right = mRect.left + (int) (mViewWidth / mScale);
            }
            invalidate();
        }
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

}
