package com.example.feihualinggame.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.example.feihualinggame.R;

/**
 * 自定义圆形裁剪视图
 * 支持拖动和双指缩放，提供直观的圆形裁剪体验
 */
public class CircleCropView extends View {

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;

    private Bitmap mBitmap;
    private Matrix mMatrix = new Matrix();
    private Matrix mSavedMatrix = new Matrix();
    
    private Paint mBorderPaint;
    private Paint mOverlayPaint;
    private Paint mGridPaint;
    
    private float[] mLastEvent = new float[4];
    private float mMidX, mMidY;
    private float mLastTouchX, mLastTouchY;
    private int mMode = NONE;
    
    private float mScaleFactor = 1.0f;
    private float mMaxScale = 5.0f;
    private float mMinScale = 0.5f;
    
    private ScaleGestureDetector mScaleDetector;
    
    // 裁剪区域
    private RectF mCropRect = new RectF();
    private float mCropRadius;
    
    public CircleCropView(Context context) {
        super(context);
        init();
    }

    public CircleCropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setColor(0xFFFFFFFF);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(3f);
        
        mOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOverlayPaint.setColor(0xB3000000);
        
        mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGridPaint.setColor(0x4DFFFFFF);
        mGridPaint.setStyle(Paint.Style.STROKE);
        mGridPaint.setStrokeWidth(1f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // 计算裁剪圆形区域（屏幕宽高的较小值的80%）
        int size = Math.min(w, h);
        mCropRadius = size * 0.35f;
        
        float centerX = w / 2f;
        float centerY = h / 2f;
        
        mCropRect.set(
            centerX - mCropRadius,
            centerY - mCropRadius,
            centerX + mCropRadius,
            centerY + mCropRadius
        );
    }

    public void setImageBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
        // 延迟调用 resetMatrix，确保 View 尺寸已确定
        if (getWidth() > 0 && getHeight() > 0 && mCropRadius > 0) {
            resetMatrix();
        } else {
            post(this::resetMatrix);
        }
        invalidate();
    }

    private void resetMatrix() {
        if (mBitmap == null) return;
        
        mMatrix.reset();
        
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float bitmapWidth = mBitmap.getWidth();
        float bitmapHeight = mBitmap.getHeight();
        
        // 初始缩放，让图片覆盖整个裁剪区域
        float scale = Math.max(
            (mCropRadius * 2) / bitmapWidth,
            (mCropRadius * 2) / bitmapHeight
        );
        
        mMatrix.postScale(scale, scale);
        
        // 居中
        float scaledWidth = bitmapWidth * scale;
        float scaledHeight = bitmapHeight * scale;
        float dx = (viewWidth - scaledWidth) / 2f;
        float dy = (viewHeight - scaledHeight) / 2f;
        
        mMatrix.postTranslate(dx, dy);
        mSavedMatrix.set(mMatrix);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 绘制黑色背景
        canvas.drawColor(0xFF000000);
        
        if (mBitmap == null) return;
        
        // 绘制图片
        canvas.drawBitmap(mBitmap, mMatrix, null);
        
        // 绘制遮罩（圆形外的半透明区域）
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        path.addCircle(
            mCropRect.centerX(),
            mCropRect.centerY(),
            mCropRadius,
            Path.Direction.CCW
        );
        canvas.drawPath(path, mOverlayPaint);
        
        // 绘制圆形边框
        canvas.drawCircle(
            mCropRect.centerX(),
            mCropRect.centerY(),
            mCropRadius,
            mBorderPaint
        );
        
        // 绘制辅助网格线（九宫格）
        drawGridLines(canvas);
    }

    private void drawGridLines(Canvas canvas) {
        float left = mCropRect.left;
        float top = mCropRect.top;
        float right = mCropRect.right;
        float bottom = mCropRect.bottom;
        
        // 竖线
        float x1 = left + (right - left) / 3f;
        float x2 = left + 2 * (right - left) / 3f;
        canvas.drawLine(x1, top, x1, bottom, mGridPaint);
        canvas.drawLine(x2, top, x2, bottom, mGridPaint);
        
        // 横线
        float y1 = top + (bottom - top) / 3f;
        float y2 = top + 2 * (bottom - top) / 3f;
        canvas.drawLine(left, y1, right, y1, mGridPaint);
        canvas.drawLine(left, y2, right, y2, mGridPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mSavedMatrix.set(mMatrix);
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                mMode = DRAG;
                break;
                
            case MotionEvent.ACTION_POINTER_DOWN:
                mSavedMatrix.set(mMatrix);
                midPoint(event);
                mMode = ZOOM;
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (mMode == DRAG) {
                    mMatrix.set(mSavedMatrix);
                    mMatrix.postTranslate(
                        event.getX() - mLastTouchX,
                        event.getY() - mLastTouchY
                    );
                } else if (mMode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        mMatrix.set(mSavedMatrix);
                        float scale = newDist / spacing(mLastEvent[0], mLastEvent[1], mLastEvent[2], mLastEvent[3]);
                        scale = Math.max(mMinScale, Math.min(scale, mMaxScale));
                        mMatrix.postScale(scale, scale, mMidX, mMidY);
                    }
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mMode = NONE;
                break;
        }
        
        invalidate();
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private float spacing(float x0, float y0, float x1, float y1) {
        float x = x0 - x1;
        float y = y0 - y1;
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(MotionEvent event) {
        mMidX = (event.getX(0) + event.getX(1)) / 2;
        mMidY = (event.getY(0) + event.getY(1)) / 2;
        
        mLastEvent[0] = event.getX(0);
        mLastEvent[1] = event.getY(0);
        mLastEvent[2] = event.getX(1);
        mLastEvent[3] = event.getY(1);
    }

    /**
     * 获取裁剪后的圆形Bitmap
     */
    public Bitmap getCroppedBitmap() {
        if (mBitmap == null || mCropRadius <= 0) {
            android.util.Log.e("CircleCropView", "getCroppedBitmap: mBitmap=" + (mBitmap != null) + ", mCropRadius=" + mCropRadius);
            return null;
        }

        int cropSize = (int) (mCropRadius * 2);
        android.util.Log.d("CircleCropView", "getCroppedBitmap: cropSize=" + cropSize + ", bitmap=" + mBitmap.getWidth() + "x" + mBitmap.getHeight());

        // 创建一个足够大的临时画布
        int tempSize = Math.max(getWidth(), getHeight()) * 3;
        android.util.Log.d("CircleCropView", "tempSize=" + tempSize + ", view=" + getWidth() + "x" + getHeight());

        Bitmap tempBitmap = Bitmap.createBitmap(tempSize, tempSize, Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(tempBitmap);

        // 将变换后的图片绘制到临时画布中心
        float offsetX = (tempSize - getWidth()) / 2f;
        float offsetY = (tempSize - getHeight()) / 2f;
        tempCanvas.translate(offsetX, offsetY);
        tempCanvas.drawBitmap(mBitmap, mMatrix, null);

        // 计算裁剪区域在临时画布上的坐标
        float cropCenterX = offsetX + mCropRect.centerX();
        float cropCenterY = offsetY + mCropRect.centerY();

        android.util.Log.d("CircleCropView", "cropCenter: (" + cropCenterX + ", " + cropCenterY + ")" +
                ", cropRect: " + mCropRect.toString());

        // 从临时画布截取裁剪区域
        int startX = Math.max(0, (int) (cropCenterX - mCropRadius));
        int startY = Math.max(0, (int) (cropCenterY - mCropRadius));
        int actualWidth = Math.min(cropSize, tempSize - startX);
        int actualHeight = Math.min(cropSize, tempSize - startY);

        android.util.Log.d("CircleCropView", "crop params: startX=" + startX + ", startY=" + startY +
                ", actualWidth=" + actualWidth + ", actualHeight=" + actualHeight);

        Bitmap cropResult;
        try {
            cropResult = Bitmap.createBitmap(tempBitmap, startX, startY, actualWidth, actualHeight);
        } catch (Exception e) {
            android.util.Log.e("CircleCropView", "createBitmap failed: " + e.getMessage());
            tempBitmap.recycle();
            return null;
        }
        tempBitmap.recycle();

        // 缩放为标准尺寸
        Bitmap scaledCrop = Bitmap.createScaledBitmap(cropResult, cropSize, cropSize, true);
        cropResult.recycle();

        // 应用圆形遮罩
        Bitmap output = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
        Canvas outputCanvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outputCanvas.drawCircle(cropSize / 2f, cropSize / 2f, cropSize / 2f, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        outputCanvas.drawBitmap(scaledCrop, 0, 0, paint);
        scaledCrop.recycle();

        android.util.Log.d("CircleCropView", "getCroppedBitmap success: " + output.getWidth() + "x" + output.getHeight());
        return output;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            scaleFactor = Math.max(mMinScale, Math.min(scaleFactor, mMaxScale));
            
            mMatrix.postScale(
                scaleFactor,
                scaleFactor,
                detector.getFocusX(),
                detector.getFocusY()
            );
            
            return true;
        }
    }
}
