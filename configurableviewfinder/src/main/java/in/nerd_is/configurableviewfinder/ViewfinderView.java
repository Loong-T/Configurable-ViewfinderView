package in.nerd_is.configurableviewfinder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.CameraPreview;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Xuqiang ZHENG on 17/7/13.
 */
@SuppressWarnings("unused")
public class ViewfinderView extends View {

    public static final int GRAVITY_NONE = -1;
    public static final int GRAVITY_CENTER = 0;
    public static final int GRAVITY_CENTER_HORIZONTAL = 1;
    public static final int GRAVITY_CENTER_VERTICAL = 2;

    protected static final long ANIMATION_DELAY = 80L;
    protected static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    protected static final int CURRENT_POINT_OPACITY = 0xA0;
    protected static final int MAX_RESULT_POINTS = 20;
    protected static final int POINT_SIZE = 6;

    private int maskColor;
    private int laserColor;
    private int pointColor;
    private int resultColor;
    @FrameGravity
    private int gravity = GRAVITY_NONE;

    private RectF frameRect = new RectF();
    private Paint paint = new Paint();
    private Path framePath = new Path();

    @Nullable
    private Bitmap resultBitmap;
    private int alphaIndex = 4;
    protected List<ResultPoint> possibleResultPoints = new ArrayList<>(MAX_RESULT_POINTS);
    protected List<ResultPoint> lastPossibleResultPoints = new ArrayList<>(MAX_RESULT_POINTS);

    private CameraPreview cameraPreview;

    public ViewfinderView(Context context) {
        super(context);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView);

        //noinspection WrongConstant
        gravity = a.getInt(R.styleable.ViewfinderView_vfv_frameGravity, GRAVITY_NONE);
        maskColor = a.getColor(R.styleable.ViewfinderView_vfv_maskColor,
                ContextCompat.getColor(context, R.color.vfv_viewfinder_mask));
        laserColor = a.getColor(R.styleable.ViewfinderView_vfv_laserColor,
                ContextCompat.getColor(context, R.color.vfv_viewfinder_laser));
        pointColor = a.getColor(R.styleable.ViewfinderView_vfv_possibleResultPointsColor,
                ContextCompat.getColor(context, R.color.vfv_possible_result_points));
        resultColor = a.getColor(R.styleable.ViewfinderView_vfv_resultViewColor,
                ContextCompat.getColor(context, R.color.vfv_result_view_color));

        a.recycle();
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ViewfinderView(Context context, @Nullable AttributeSet attrs,
                          int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setCameraPreview(CameraPreview view) {
        cameraPreview = view;
        view.addStateListener(new CameraPreview.StateListener() {
            @Override
            public void previewSized() {
                invalidate();
            }

            @Override
            public void previewStarted() {

            }

            @Override
            public void previewStopped() {

            }

            @Override
            public void cameraError(Exception error) {

            }

            @Override
            public void cameraClosed() {

            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (cameraPreview == null) {
            return;
        }
        Rect previewFramingRect = cameraPreview.getPreviewFramingRect();
        if (cameraPreview.getFramingRect() == null || previewFramingRect == null) {
            return;
        }

        final float width = canvas.getWidth();
        final float height = canvas.getHeight();

        refreshFrameSize(width, height);

        // draw the exterior
        paint.setColor(resultBitmap == null ? maskColor : resultColor);
        framePath.addRect(0f, 0f, width, height, Path.Direction.CW);
        framePath.addRect(frameRect, Path.Direction.CCW);
        canvas.drawPath(framePath, paint);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frameRect, paint);
        } else {
            // Draw a red "laser scanner" line through the middle to show decoding is active
            paint.setColor(laserColor);
            paint.setAlpha(SCANNER_ALPHA[alphaIndex]);
            alphaIndex = (alphaIndex + 1) % SCANNER_ALPHA.length;
            final float middle = frameRect.top + frameRect.height() / 2;
            canvas.drawRect(frameRect.left + 1, middle - 1, frameRect.right - 1, middle + 2, paint);

            final float scaleX = frameRect.width() / previewFramingRect.width();
            final float scaleY = frameRect.height() / previewFramingRect.height();

            // draw the last possible result points
            if (!lastPossibleResultPoints.isEmpty()) {
                paint.setAlpha(CURRENT_POINT_OPACITY / 2);
                paint.setColor(pointColor);
                float radius = POINT_SIZE / 2.0f;
                for (final ResultPoint point : lastPossibleResultPoints) {
                    canvas.drawCircle(
                            frameRect.left + point.getX() * scaleX,
                            frameRect.top + point.getY() * scaleY,
                            radius, paint
                    );
                }
                lastPossibleResultPoints.clear();
            }

            // draw current possible result points
            if (!possibleResultPoints.isEmpty()) {
                paint.setAlpha(CURRENT_POINT_OPACITY);
                paint.setColor(pointColor);
                for (final ResultPoint point : possibleResultPoints) {
                    canvas.drawCircle(frameRect.left + point.getX(),
                            frameRect.top + point.getY(), POINT_SIZE, paint);
                }

                // swap and clear buffers
                final List<ResultPoint> temp = possibleResultPoints;
                possibleResultPoints = lastPossibleResultPoints;
                lastPossibleResultPoints = temp;
                possibleResultPoints.clear();
            }
        }

        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(ANIMATION_DELAY,
                (int) frameRect.left - POINT_SIZE,
                (int) frameRect.top - POINT_SIZE,
                (int) frameRect.right + POINT_SIZE,
                (int) frameRect.bottom + POINT_SIZE);
    }

    public void swapPossibleResultPoints(List<ResultPoint> points) {
        List<ResultPoint> pointList;
        if (points.size() > MAX_RESULT_POINTS) {
            pointList = points.subList(0, MAX_RESULT_POINTS);
        } else {
            pointList = points;
        }
        possibleResultPoints = new ArrayList<>(pointList);
    }

    public void drawViewfinder() {
        if (resultBitmap == null) {
            return;
        }

        Bitmap tempBitmap = resultBitmap;
        resultBitmap = null;
        tempBitmap.recycle();
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param result An image of the result.
     */
    public void drawResultBitmap(Bitmap result) {
        resultBitmap = result;
        invalidate();
    }


    public int getMaskColor() {
        return maskColor;
    }

    public void setMaskColor(int maskColor) {
        this.maskColor = maskColor;
    }

    public int getLaserColor() {
        return laserColor;
    }

    public void setLaserColor(int laserColor) {
        this.laserColor = laserColor;
    }

    public int getPointColor() {
        return pointColor;
    }

    public void setPointColor(int pointColor) {
        this.pointColor = pointColor;
    }

    public int getResultColor() {
        return resultColor;
    }

    public void setResultColor(int resultColor) {
        this.resultColor = resultColor;
    }

    @FrameGravity
    public int getGravity() {
        return gravity;
    }

    public void setGravity(@FrameGravity int gravity) {
        this.gravity = gravity;
    }

    private void refreshFrameSize(float width, float height) {
        Rect framingRect = cameraPreview.getFramingRect();
        switch (gravity) {
            case GRAVITY_CENTER:
                frameRect.left = (width - framingRect.width()) / 2;
                frameRect.top = (height - framingRect.height()) / 2;
                break;
            case GRAVITY_CENTER_HORIZONTAL:
                frameRect.left = (width - framingRect.width()) / 2;
                frameRect.top = getPaddingTop();
                break;
            case GRAVITY_CENTER_VERTICAL:
                frameRect.left = getPaddingLeft();
                frameRect.top = (height - framingRect.height()) / 2;
            case GRAVITY_NONE:
                frameRect.left = getPaddingLeft();
                frameRect.top = getPaddingTop();
                break;
            default:
                frameRect.left = getPaddingLeft();
                frameRect.top = getPaddingTop();
                break;
        }
        frameRect.right = frameRect.left + framingRect.width();
        frameRect.bottom = frameRect.top + framingRect.height();
    }
}
