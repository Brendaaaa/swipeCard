package com.lorentzos.flingswipe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Created by dionysis_lorentzos on 5/8/14
 * for package com.lorentzos.swipecards
 * and project Swipe cards.
 * Use with caution dinausaurs might appear!
 */


public class FlingCardListener implements View.OnTouchListener {

    private static final String TAG = FlingCardListener.class.getSimpleName();
    private static final int INVALID_POINTER_ID = -1;
    private static final int NO_SCORE = 0;
    private static final int SCORE_1 = 1;
    private static final int SCORE_2 = 2;
    private static final int SCORE_3 = 3;
    private static final int SCORE_4 = 4;

    private final float objectX;
    private final float objectY;
    private final int objectH;
    private final int objectW;
    private final int parentWidth;
    private final int parentHeight;
    private final FlingListener mFlingListener;
    private final Object dataObject;
    private final float halfWidth;
    private final float halfHeight;
    private float BASE_ROTATION_DEGREES;

    private float aPosX;
    private float aPosY;
    private float aDownTouchX;
    private float aDownTouchY;

    // The active pointer is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;
    private View frame = null;


    private final int TOUCH_ABOVE = 0;
    private final int TOUCH_BELOW = 1;
    private int touchPosition;
    private final Object obj = new Object();
    private boolean isAnimationRunning = false;
    private float MAX_COS = (float) Math.cos(Math.toRadians(45));

    public FlingCardListener(View frame, Object itemAtPosition, FlingListener flingListener) {
        this(frame, itemAtPosition, 15f, flingListener);
    }

    public FlingCardListener(View frame, Object itemAtPosition, float rotation_degrees, FlingListener flingListener) {
        super();
        this.frame = frame;
        this.objectX = frame.getX();
        this.objectY = frame.getY();
        this.objectH = frame.getHeight();
        this.objectW = frame.getWidth();
        this.halfWidth = objectW / 2f;
        this.halfHeight = objectH / 2f;
        this.dataObject = itemAtPosition;
        this.parentWidth = ((ViewGroup) frame.getParent()).getWidth();
        this.parentHeight = ((ViewGroup) frame.getParent()).getHeight();
        this.BASE_ROTATION_DEGREES = rotation_degrees;
        this.mFlingListener = flingListener;
    }

    public boolean onTouch(View view, MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                // from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Save the ID of this pointer

                mActivePointerId = event.getPointerId(0);
                float x = 0;
                float y = 0;
                boolean success = false;
                try {
                    x = event.getX(mActivePointerId);
                    y = event.getY(mActivePointerId);
                    success = true;
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Exception in onTouch(view, event) : " + mActivePointerId, e);
                }
                if (success) {
                    // Remember where we started
                    aDownTouchX = x;
                    aDownTouchY = y;
                    //to prevent an initial jump of the magnifier, aposX and aPosY must
                    //have the values from the magnifier frame
                    if (aPosX == 0) {
                        aPosX = frame.getX();
                    }
                    if (aPosY == 0) {
                        aPosY = frame.getY();
                    }

                    if (y < objectH / 2) {
                        touchPosition = TOUCH_ABOVE;
                    } else {
                        touchPosition = TOUCH_BELOW;
                    }
                }

                view.getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER_ID;
                resetCardViewOnStack();
                view.getParent().requestDisallowInterceptTouchEvent(false);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // Extract the index of the pointer that left the touch sensor
                final int pointerIndex = (event.getAction() &
                        MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            case MotionEvent.ACTION_MOVE:

                // Find the index of the active pointer and fetch its position
                final int pointerIndexMove = event.findPointerIndex(mActivePointerId);
                final float xMove = event.getX(pointerIndexMove);
                final float yMove = event.getY(pointerIndexMove);

                //from http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Calculate the distance moved
                final float dx = xMove - aDownTouchX;
                final float dy = yMove - aDownTouchY;

                // Move the frame
                aPosX += dx;
                aPosY += dy;

                // calculate the rotation degrees
                float distobjectX = aPosX - objectX;
                float rotation = BASE_ROTATION_DEGREES * 2.f * distobjectX / parentWidth;
                if (touchPosition == TOUCH_BELOW) {
                    rotation = -rotation;
                }

                //in this area would be code for doing something with the view as the frame moves.
                frame.setX(aPosX);
                frame.setY(aPosY);
                frame.setRotation(rotation);
                mFlingListener.onScroll(getScrollProgressPercent());
                break;

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                view.getParent().requestDisallowInterceptTouchEvent(false);
                break;
            }
        }

        return true;
    }

    private int getScrollProgressPercent() {
        if (movedBeyondLeftBorder() && movedBeyondTopBorder()) { //score1
            System.out.println("score1");
            return 1;
        } else if (movedBeyondLeftBorder() && movedBeyondBottomBorder()) { //score2
            System.out.println("score2");
            return 2;
        } else if (movedBeyondRightBorder() && movedBeyondBottomBorder()) { //score3
            System.out.println("score3");
            return 3;
        } else if (movedBeyondRightBorder() && movedBeyondTopBorder()) { //score4
            System.out.println("score4");
            return 4;
        } else if (!movedBeyondRightBorder() && !movedBeyondLeftBorder() && movedBeyondTopBorder2()) { //no score
            System.out.println("no score");
            return 0;
        } else {
            return -1;
        }

//            float zeroToOneValue = (aPosX + halfWidth - leftBorder()) / (rightBorder() - leftBorder());
//            return zeroToOneValue * 2f - 1f;
    }

    private boolean resetCardViewOnStack() {
        int score = getScrollProgressPercent();
        if (score == SCORE_1 || score == SCORE_2) { //left
            // Left Swipe
            onSelected(score, getExitPoint(-objectW), 100);
            mFlingListener.onScroll(-1.0f);
        } else if (score == SCORE_4 || score == SCORE_3) {
            // Right Swipe
            onSelected(score, getExitPoint(parentWidth), 100);
            mFlingListener.onScroll(1.0f);
        } else if (score == NO_SCORE){
            // Top Swipe
            onSelected(score, getExitPoint(0), 100);
            mFlingListener.onScroll(-2.0f);
        } else {
            float abslMoveDistance = Math.abs(aPosX - objectX);
            aPosX = 0;
            aPosY = 0;
            aDownTouchX = 0;
            aDownTouchY = 0;
            frame.animate()
                    .setDuration(200)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .x(objectX)
                    .y(objectY)
                    .rotation(0);
            mFlingListener.onScroll(0.0f);
            if (abslMoveDistance < 4.0) {
                System.out.println("click");
//                mFlingListener.onClick(dataObject);
            }
        }
        return false;
    }

    private boolean movedBeyondLeftBorder() {
        return aPosX + halfWidth < leftBorder();
    }

    private boolean movedBeyondRightBorder() {
        return aPosX + halfWidth > rightBorder();
    }

    private boolean movedBeyondTopBorder() {
        return aPosY + halfHeight < topBorder();
    }

    private boolean movedBeyondTopBorder2() {
        return aPosY + halfHeight < topBorder2();
    }

    private boolean movedBeyondBottomBorder() {
        return aPosY + halfHeight > bottomBorder();
    }

    public float topBorder() {
        return parentHeight / 3.f;
    }

    public float topBorder2() {
        return parentHeight / 4.f;
    }

    public float leftBorder() {
        return parentWidth / 4.f;
    }

    public float rightBorder() {
        return 3 * parentWidth / 4.f;
    }

    public float bottomBorder() {
        return 2 * parentHeight / 3.f;
    }


    public void onSelected(final int score, float exitY, long duration) {
        boolean isLeft = false;

        isAnimationRunning = true;
        float exitX;
        if (score == SCORE_1 || score == SCORE_2) { //esquerda
            isLeft = true;
            exitX = -objectW - getRotationWidthOffset();
            System.out.println("exitX " + exitX + "objectX " + objectX + " objectY " + objectY + " aPosX " + aPosX);
        } else if (score == NO_SCORE){
                System.out.println("objectX " + objectX + " objectY " + objectY + " aPosX " + aPosX);
                exitX = 0;
                exitY = -200;
        } else { //direita
                exitX = parentWidth + getRotationWidthOffset();
                System.out.println("exitX " + exitX + " objectX " + objectX + " objectY " + objectY + " aPosX " + aPosX);
        }

        this.frame.animate()
                .setDuration(duration)
                .setInterpolator(new AccelerateInterpolator())
                .x(exitX)
                .y(exitY)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mFlingListener.onCardExited();
                        if (score == NO_SCORE) {
                            mFlingListener.noScoreExit(dataObject);
                        } else if (score == SCORE_1) {
                            mFlingListener.score1Exit(dataObject);
                        } else if (score == SCORE_2) {
                            mFlingListener.score2Exit(dataObject);
                        } else if (score == SCORE_3) {
                            mFlingListener.score3Exit(dataObject);
                        } else if (score == SCORE_4) {
                            mFlingListener.score4Exit(dataObject);
                        }
                        isAnimationRunning = false;
                    }
                })
                .rotation(getExitRotation(isLeft));
    }


    /**
     * Starts a default left exit animation.
     */
    public void selectLeft() {
        if (!isAnimationRunning)
            onSelected(SCORE_1, objectY, 200);
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectRight() {
        if (!isAnimationRunning)
            onSelected(SCORE_4, objectY, 200);
    }

    public void selectTop() {
        if (!isAnimationRunning)
            onSelected(NO_SCORE, objectY, 200);
    }

    private float getExitPoint(int exitXPoint) {
        float[] x = new float[2];
        x[0] = objectX;
        x[1] = aPosX;

        float[] y = new float[2];
        y[0] = objectY;
        y[1] = aPosY;

        LinearRegression regression = new LinearRegression(x, y);

        //Your typical y = ax+b linear regression
        return (float) regression.slope() * exitXPoint + (float) regression.intercept();
    }

    private float getExitRotation(boolean isLeft) {
        float rotation = BASE_ROTATION_DEGREES * 2.f * (parentWidth - objectX) / parentWidth;
        if (touchPosition == TOUCH_BELOW) {
            rotation = -rotation;
        }
        if (isLeft) {
            rotation = -rotation;
        }
        return rotation;
    }


    /**
     * When the object rotates it's width becomes bigger.
     * The maximum width is at 45 degrees.
     * <p/>
     * The below method calculates the width offset of the rotation.
     */
    private float getRotationWidthOffset() {
        return objectW / MAX_COS - objectW;
    }


    public void setRotationDegrees(float degrees) {
        this.BASE_ROTATION_DEGREES = degrees;
    }

    public boolean isTouching() {
        return this.mActivePointerId != INVALID_POINTER_ID;
    }

    public PointF getLastPoint() {
        return new PointF(this.aPosX, this.aPosY);
    }

    protected interface FlingListener {
        void onCardExited();

        void score1Exit(Object dataObject);

        void score2Exit(Object dataObject);

        void score3Exit(Object dataObject);

        void score4Exit(Object dataObject);

        void noScoreExit(Object dataObject);

        void onClick(Object dataObject);

        void onScroll(float scrollProgressPercent);
    }

}





