package cc.cloudist.app.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import cc.cloudist.app.R;

public class LocationDotView extends View {

    private static final String CIRCLE_COLOR = "#29B6F6";
    private static final int MAX_FADING_CIRCLE_ALPHA = 100;
    private static final int TOTAL_ANIMATION_TIME = 450;
    private float cXStationary;
    private float cYStationary;

    private float rStationary;
    private float rStationaryGF;
    private float rOrbiting;

    private float orbitPathDistanceFromCenter;

    private final Paint mPaintStationaryCircle = new Paint();
    private final Paint mPaintGrowingFadingCircle = new Paint();

    private int stationaryCircleColor;
    private int fadingCircleColor;
    private int rotationDirection;

    // Animation calculation fields
    private float currentAnimationTime;
    private float delta;

    private int fadingCircleAlpha = 255;


    private RefreshViewRunnable refreshViewRunnable;

    public LocationDotView(Context context) {
        super(context);
        init();
    }

    public LocationDotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public LocationDotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {

        final TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attributeSet,
                R.styleable.LocationDotView,
                0, 0
        );

        try {

            stationaryCircleColor = typedArray.getColor(R.styleable.LocationDotView_stationary_circle_color, Color.parseColor(CIRCLE_COLOR));
            fadingCircleColor = typedArray.getColor(R.styleable.LocationDotView_fading_circle_color, Color.parseColor(CIRCLE_COLOR));
            rotationDirection = typedArray.getInt(R.styleable.LocationDotView_direction, 0);
            rStationary = typedArray.getDimension(R.styleable.LocationDotView_stationary_circle_radius, 12f);
            float orbitingCircleRadius = typedArray.getDimension(R.styleable.LocationDotView_orbiting_circle_radius, 6f);
            // In order to make sure the orbiting circles are at least 75% the
            // size of the stationary circle this check is in place.
            if (orbitingCircleRadius > (rStationary / 3)) {
                rOrbiting = rStationary / 2;
            } else {
                rOrbiting = orbitingCircleRadius;
            }
        } finally {
            typedArray.recycle();
        }

        setupColorPallets();

        setupInitialValuesForAnimation();
    }

    private void init() {

        stationaryCircleColor = Color.parseColor(CIRCLE_COLOR);
        fadingCircleColor = Color.parseColor(CIRCLE_COLOR);
        rStationary = 12f;
        rOrbiting = rStationary / 2;
        setupColorPallets();

        setupInitialValuesForAnimation();
    }

    private void setupInitialValuesForAnimation() {
        orbitPathDistanceFromCenter = 4 * rStationary;
    }

    private void setupColorPallets() {
        mPaintGrowingFadingCircle.setColor(fadingCircleColor);
        mPaintGrowingFadingCircle.setAntiAlias(true);
        mPaintStationaryCircle.setColor(stationaryCircleColor);
        mPaintStationaryCircle.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cXStationary = w / 2;
        cYStationary = h / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // This draws the stationary circle in the center of the view.
        canvas.drawCircle(cXStationary, cYStationary, rStationary, mPaintStationaryCircle);

        // This is the lighter circle that grows bigger in size over time and fades away.
        canvas.drawCircle(cXStationary, cYStationary, rStationaryGF, mPaintGrowingFadingCircle);

        mPaintGrowingFadingCircle.setAlpha(fadingCircleAlpha);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (View.GONE == visibility) {
            removeCallbacks(refreshViewRunnable);
        } else {
            removeCallbacks(refreshViewRunnable);
            refreshViewRunnable = new RefreshViewRunnable();
            post(refreshViewRunnable);
        }
    }

    private void drawCircle(Canvas canvas, float theta, Paint paint) {

        double thetaInRadians = Math.toRadians(theta);
        float oribitingCX, oribitingCY;

        if (rotationDirection == 0) {
            oribitingCX = cXStationary + (orbitPathDistanceFromCenter * (float) Math.cos(thetaInRadians));
            oribitingCY = cYStationary + (orbitPathDistanceFromCenter * (float) Math.sin(thetaInRadians));
        } else {
            oribitingCX = cXStationary + (orbitPathDistanceFromCenter * (float) Math.sin(thetaInRadians));
            oribitingCY = cYStationary + (orbitPathDistanceFromCenter * (float) Math.cos(thetaInRadians));
        }

        canvas.drawCircle(oribitingCX, oribitingCY, rOrbiting, paint);

    }

    private class RefreshViewRunnable implements Runnable {

        @Override
        public void run() {

            synchronized (LocationDotView.this) {

                if (currentAnimationTime >= 0) {
                    currentAnimationTime += 5;
                    delta = currentAnimationTime / TOTAL_ANIMATION_TIME;
                    rStationaryGF = rStationary * 4 * delta;
                    if (delta >= 1.0) {
                        currentAnimationTime = 0;
                        rStationaryGF = 0f;
                    }
                    fadingCircleAlpha = MAX_FADING_CIRCLE_ALPHA - (int) (delta * MAX_FADING_CIRCLE_ALPHA);
                    invalidate();
                    postDelayed(this, 16);
                }

            }

        }
    }
}
