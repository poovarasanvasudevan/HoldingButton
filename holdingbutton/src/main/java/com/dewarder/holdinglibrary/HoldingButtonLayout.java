/*
 * Copyright (C) 2017 Artem Glugovsky
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dewarder.holdinglibrary;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class HoldingButtonLayout extends FrameLayout {

    private static final float DEFAULT_CANCEL_OFFSET = 0.3f;
    private static final float COLLAPSING_SCALE_Y_VALUE_START = 0.8f;
    private static final float COLLAPSING_SCALE_Y_VALUE_END = 1f;
    private static final float COLLAPSING_ALPHA_VALUE_START = 0f;
    private static final float COLLAPSING_ALPHA_VALUE_END = 1f;

    private int mHoldingViewId = NO_ID;
    private View mHoldingView;
    private Rect mHoldingViewRect = new Rect();
    private float mCancelOffset = DEFAULT_CANCEL_OFFSET;
    private float mDeltaX;

    private View mHoldingCircle;
    private HoldingDrawable mHoldingDrawable;
    private int[] mOffset = new int[2];
    private int[] mViewLocation = new int[2];
    private int[] mHoldingViewLocation = new int[2];

    private LayoutDirection mLayoutDirection = LayoutDirection.LTR;
    private Direction mDirection;
    private boolean mAnimateHoldingView = true;
    private boolean mButtonEnabled = true;
    private boolean mIsCancel = false;
    private boolean mIsExpanded = false;

    private int mCollapsingAnimationDuration;

    private HoldingButtonTouchListener mTouchListener = new SimpleHoldingButtonTouchListener();
    private final DrawableListener mDrawableListener = new DrawableListener();
    private final List<HoldingButtonLayoutListener> mListeners = new ArrayList<>();

    public HoldingButtonLayout(@NonNull Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public HoldingButtonLayout(@NonNull Context context,
                               @Nullable AttributeSet attrs) {

        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public HoldingButtonLayout(@NonNull Context context,
                               @Nullable AttributeSet attrs,
                               @AttrRes int defStyleAttr) {

        super(context, attrs, defStyleAttr);
        init(context, attrs, 0, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HoldingButtonLayout(@NonNull Context context,
                               @Nullable AttributeSet attrs,
                               @AttrRes int defStyleAttr,
                               @StyleRes int defStyleRes) {

        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        mCollapsingAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mHoldingDrawable = new HoldingDrawable();
        mHoldingDrawable.setListener(mDrawableListener);
        mLayoutDirection = DirectionHelper.resolveLayoutDirection(this);

        if (attrs != null) {
            TypedArray array = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.HoldingButtonLayout,
                    defStyleAttr,
                    defStyleRes);

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_enabled)) {
                setButtonEnabled(array.getBoolean(R.styleable.HoldingButtonLayout_hbl_enabled, true));
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_radius)) {
                mHoldingDrawable.setRadius(array.getDimensionPixelSize(R.styleable.HoldingButtonLayout_hbl_radius, 280));
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_icon)) {
                setIcon(array.getResourceId(R.styleable.HoldingButtonLayout_hbl_icon, 0));
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_cancel_icon)) {
                setCancelIcon(array.getResourceId(R.styleable.HoldingButtonLayout_hbl_cancel_icon, 0));
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_offset_x)) {
                mOffset[0] = array.getDimensionPixelSize(R.styleable.HoldingButtonLayout_hbl_offset_x, 0);
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_offset_y)) {
                mOffset[1] = array.getDimensionPixelSize(R.styleable.HoldingButtonLayout_hbl_offset_y, 0);
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_holding_view)) {
                mHoldingViewId = array.getResourceId(R.styleable.HoldingButtonLayout_hbl_holding_view, NO_ID);
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_animate_holding_view)) {
                mAnimateHoldingView = array.getBoolean(R.styleable.HoldingButtonLayout_hbl_animate_holding_view, true);
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_color)) {
                mHoldingDrawable.setColor(array.getColor(R.styleable.HoldingButtonLayout_hbl_color, 0));
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_cancel_color)) {
                mHoldingDrawable.setCancelColor(array.getColor(R.styleable.HoldingButtonLayout_hbl_cancel_color, 0));
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_second_radius)) {
                mHoldingDrawable.setSecondRadius(array.getDimension(R.styleable.HoldingButtonLayout_hbl_second_radius, 0));
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_second_alpha)) {
                float alphaMultiplier = array.getFloat(R.styleable.HoldingButtonLayout_hbl_second_alpha, 1);
                if (alphaMultiplier < 0 && alphaMultiplier > 1) {
                    throw new IllegalStateException("Second alpha value must be between 0 and 1");
                }
                mHoldingDrawable.setSecondAlpha((int) (255 * alphaMultiplier));
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_cancel_offset)) {
                float cancelOffset = array.getFloat(R.styleable.HoldingButtonLayout_hbl_cancel_offset, 1);
                if (cancelOffset < 0 && cancelOffset > 1) {
                    throw new IllegalStateException("Cancel offset must be between 0 and 1");
                }
                mCancelOffset = cancelOffset;
            }

            if (array.hasValue(R.styleable.HoldingButtonLayout_hbl_direction)) {
                int directionFlag = array.getInt(R.styleable.HoldingButtonLayout_hbl_direction, -1);
                if (directionFlag != -1) {
                    mDirection = DirectionHelper.adaptSlidingDirection(this, Direction.fromFlag(directionFlag));
                }
            }

            array.recycle();
        }

        if (mDirection == null) {
            mDirection = DirectionHelper.resolveDefaultSlidingDirection(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                return isButtonEnabled() && isViewTouched(mHoldingView, ev);
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if (isButtonEnabled() && isViewTouched(mHoldingView, event) && shouldInterceptAnimation()) {
                    mHoldingView.getLocationInWindow(mHoldingViewLocation);
                    getLocationInWindow(mViewLocation);

                    int layoutWidth = getWidth();
                    int viewCenterX = mHoldingViewLocation[0] + mHoldingView.getWidth() / 2;
                    int circleCenterX = mHoldingCircle.getWidth() / 2;
                    int offsetX = mDirection.getOffsetX(mOffset[0]);
                    float translationX = mLayoutDirection.calculateTranslationX(
                            layoutWidth, viewCenterX, circleCenterX, offsetX);

                    int centerY = mHoldingViewLocation[1] + mHoldingView.getHeight() / 2;
                    float translationY = centerY - mHoldingCircle.getHeight() / 2f + mOffset[1];

                    mHoldingCircle.setTranslationX(translationX);
                    mHoldingCircle.setTranslationY(translationY);

                    mDeltaX = event.getRawX() - viewCenterX - offsetX;

                    mHoldingDrawable.expand();
                    mIsCancel = false;
                    mIsExpanded = true;
                    return true;
                }
            }

            case MotionEvent.ACTION_MOVE: {
                if (mIsExpanded) {
                    float circleCenterX = mHoldingCircle.getWidth() / 2;
                    float x = event.getRawX() - mDeltaX - circleCenterX;
                    float slideOffset = mDirection.getSlideOffset(x, circleCenterX, mViewLocation, getWidth(), mHoldingViewLocation, mHoldingView.getWidth(), mOffset);

                    if (slideOffset >= 0 && slideOffset <= 1) {
                        mHoldingCircle.setX(x);
                        mIsCancel = slideOffset >= mCancelOffset;
                        mHoldingDrawable.setCancel(mIsCancel);
                        notifyOnOffsetChanged(slideOffset, mIsCancel);
                    }
                    return true;
                }
            }

            case MotionEvent.ACTION_UP: {
                if (mIsExpanded) {
                    submit();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mHoldingCircle.getParent() != null) {
            ((ViewGroup) mHoldingCircle.getParent()).removeView(mHoldingCircle);
        }
        getDecorView().addView(mHoldingCircle, mHoldingDrawable.getIntrinsicWidth(), mHoldingDrawable.getIntrinsicHeight());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mHoldingView == null && mHoldingViewId != NO_ID) {
            mHoldingView = findViewById(mHoldingViewId);
        }

        if (mHoldingView == null) {
            throw new IllegalStateException("Holding view doesn't set. Call setHoldingView before inflate");
        }

        mHoldingCircle = new View(getContext());
        mHoldingCircle.setVisibility(INVISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mHoldingCircle.setBackground(mHoldingDrawable);
        } else {
            mHoldingCircle.setBackgroundDrawable(mHoldingDrawable);
        }
    }

    protected ViewGroup getDecorView() {
        //Try to fetch DecorView from context
        if (getContext() instanceof Activity) {
            View decor = ((Activity) getContext()).getWindow().getDecorView();

            if (decor instanceof ViewGroup) {
                return (ViewGroup) decor;
            }
        }

        //Try to fetch DecorView from parents
        ViewGroup view = this;
        while (view.getParent() != null && view.getParent() instanceof ViewGroup) {
            view = (ViewGroup) view.getParent();
        }
        return view;
    }

    public void setTouchListener(@NonNull HoldingButtonTouchListener listener) {
        mTouchListener = listener;
    }

    public void addListener(HoldingButtonLayoutListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(HoldingButtonLayoutListener listener) {
        mListeners.remove(listener);
    }

    private boolean shouldInterceptAnimation() {
        return mTouchListener.onHoldingViewTouched();
    }

    private boolean isViewTouched(View view, MotionEvent event) {
        view.getDrawingRect(mHoldingViewRect);
        view.getLocationOnScreen(mHoldingViewLocation);
        mHoldingViewRect.offset(mHoldingViewLocation[0], mHoldingViewLocation[1]);
        return mHoldingViewRect.contains((int) event.getRawX(), (int) event.getRawY());
    }

    public void cancel() {
        if (mIsExpanded) {
            mIsCancel = true;
            submit();
        }
    }

    public void submit() {
        if (mIsExpanded) {
            mHoldingDrawable.collapse();
            mIsExpanded = false;
        }
    }

    public boolean isButtonEnabled() {
        return mButtonEnabled;
    }

    public void setButtonEnabled(boolean enabled) {
        mButtonEnabled = enabled;
    }

    @ColorInt
    public int getColor() {
        return mHoldingDrawable.getColor();
    }

    public void setColor(@ColorInt int color) {
        mHoldingDrawable.setColor(color);
    }

    @ColorInt
    public int getCancelColor() {
        return mHoldingDrawable.getCancelColor();
    }

    public void setCancelColor(@ColorInt int color) {
        mHoldingDrawable.setCancelColor(color);
    }

    @FloatRange(from = 0, to = 1)
    public float getCancelOffset() {
        return mCancelOffset;
    }

    public void setCancelOffset(@FloatRange(from = 0, to = 1) float offset) {
        mCancelOffset = offset;
    }

    public float getRadius() {
        return mHoldingDrawable.getRadius();
    }

    public void setRadius(float radius) {
        mHoldingDrawable.setRadius(radius);
    }

    public float getSecondRadius() {
        return mHoldingDrawable.getSecondRadius();
    }

    public void setSecondRadius(float radius) {
        mHoldingDrawable.setSecondRadius(radius);
    }

    public void setIcon(@DrawableRes int drawableRes) {
        setIcon(DrawableHelper.getBitmap(getContext(), drawableRes));
    }

    public void setIcon(Drawable drawable) {
        setIcon(DrawableHelper.getBitmap(drawable));
    }

    public void setIcon(Bitmap bitmap) {
        mHoldingDrawable.setIcon(bitmap);
    }

    public void setCancelIcon(@DrawableRes int drawableRes) {
        setCancelIcon(DrawableHelper.getBitmap(getContext(), drawableRes));
    }

    public void setCancelIcon(Drawable drawable) {
        setCancelIcon(DrawableHelper.getBitmap(drawable));
    }

    public void setCancelIcon(Bitmap bitmap) {
        mHoldingDrawable.setCancelIcon(bitmap);
    }

    public View getHoldingView() {
        return mHoldingView;
    }

    public void setHoldingView(View holdingView) {
        mHoldingView = holdingView;
    }

    public Direction getDirection() {
        return mDirection;
    }

    public void setDirection(Direction direction) {
        mDirection = DirectionHelper.adaptSlidingDirection(this, direction);
    }

    public void setAnimateHoldingView(boolean animate) {
        mAnimateHoldingView = animate;
    }

    public boolean isAnimateHoldingView() {
        return mAnimateHoldingView;
    }

    public int getOffsetX() {
        return mOffset[0];
    }

    public void setOffsetX(int offsetX) {
        mOffset[0] = offsetX;
    }

    public int getOffsetY() {
        return mOffset[1];
    }

    public void setOffsetY(int offsetY) {
        mOffset[1] = offsetY;
    }

    private void notifyOnBeforeExpand() {
        for (HoldingButtonLayoutListener listener : mListeners) {
            listener.onBeforeExpand();
        }
    }

    private void notifyOnBeforeCollapse() {
        for (HoldingButtonLayoutListener listener : mListeners) {
            listener.onBeforeCollapse();
        }
    }

    private void notifyOnExpand() {
        for (HoldingButtonLayoutListener listener : mListeners) {
            listener.onExpand();
        }
    }

    private void notifyOnCollapse(boolean isCancel) {
        for (HoldingButtonLayoutListener listener : mListeners) {
            listener.onCollapse(isCancel);
        }
    }

    private void notifyOnOffsetChanged(float offset, boolean isCancel) {
        for (HoldingButtonLayoutListener listener : mListeners) {
            listener.onOffsetChanged(offset, isCancel);
        }
    }

    private class SimpleHoldingButtonTouchListener implements HoldingButtonTouchListener {

        @Override
        public boolean onHoldingViewTouched() {
            return true;
        }
    }

    private class DrawableListener implements HoldingDrawableListener {

        @Override
        public void onBeforeExpand() {
            notifyOnBeforeExpand();
            mHoldingDrawable.reset();
            mHoldingCircle.setVisibility(VISIBLE);

            if (mAnimateHoldingView) {
                mHoldingView.setVisibility(INVISIBLE);
            }
        }

        @Override
        public void onBeforeCollapse() {
            notifyOnBeforeCollapse();
        }

        @Override
        public void onExpand() {
            notifyOnExpand();
        }

        @Override
        public void onCollapse() {
            notifyOnCollapse(mIsCancel);
            mHoldingCircle.setVisibility(GONE);

            if (mAnimateHoldingView) {
                mHoldingView.setAlpha(COLLAPSING_ALPHA_VALUE_START);
                mHoldingView.setScaleY(COLLAPSING_SCALE_Y_VALUE_START);
                mHoldingView.setVisibility(VISIBLE);
                mHoldingView.animate()
                        .alpha(COLLAPSING_ALPHA_VALUE_END)
                        .scaleY(COLLAPSING_SCALE_Y_VALUE_END)
                        .setDuration(mCollapsingAnimationDuration)
                        .start();
            }
        }
    }

    enum LayoutDirection {
        LTR {
            @Override
            public float calculateTranslationX(int layoutWidth, int viewCenterX, int circleCenterX, int offsetX) {
                return viewCenterX - circleCenterX + offsetX;
            }
        },
        RTL {
            @Override
            public float calculateTranslationX(int layoutWidth, int viewCenterX, int circleCenterX, int offsetX) {
                return -layoutWidth + viewCenterX + circleCenterX + offsetX;
            }
        };

        public abstract float calculateTranslationX(
                int layoutWidth, int viewCenterX, int circleCenterX, int offsetX);
    }

    public enum Direction {
        START(0) {
            @Override
            int getOffsetX(int offsetX) {
                return LEFT.getOffsetX(offsetX);
            }

            @Override
            float getSlideOffset(float x, float circleCenterX, int[] viewLocation, int viewWidth, int[] holdingViewLocation, int holdingViewWidth, int[] offset) {
                return LEFT.getSlideOffset(x, circleCenterX, viewLocation, viewWidth, holdingViewLocation, holdingViewWidth, offset);
            }

            @Override
            Direction toRtl() {
                return END;
            }
        },

        END(1) {
            @Override
            int getOffsetX(int offsetX) {
                return RIGHT.getOffsetX(offsetX);
            }

            @Override
            float getSlideOffset(float x, float circleCenterX, int[] viewLocation, int viewWidth, int[] holdingViewLocation, int holdingViewWidth, int[] offset) {
                return RIGHT.getSlideOffset(x, circleCenterX, viewLocation, viewWidth, holdingViewLocation, holdingViewWidth, offset);
            }

            @Override
            Direction toRtl() {
                return START;
            }
        },

        LEFT(2) {
            @Override
            int getOffsetX(int offsetX) {
                return offsetX;
            }

            @Override
            float getSlideOffset(float x, float circleCenterX, int[] viewLocation, int viewWidth, int[] holdingViewLocation, int holdingViewWidth, int[] offset) {
                float holdingViewCenterX = holdingViewLocation[0] + holdingViewWidth / 2;
                float minX = viewLocation[0] + circleCenterX;
                return (x + circleCenterX - holdingViewCenterX - offset[0]) / (minX - holdingViewCenterX);
            }

            @Override
            Direction toRtl() {
                return LEFT;
            }
        },

        RIGHT(3) {
            @Override
            int getOffsetX(int offsetX) {
                return -offsetX;
            }

            @Override
            float getSlideOffset(float x, float circleCenterX, int[] viewLocation, int viewWidth, int[] holdingViewLocation, int holdingViewWidth, int[] offset) {
                float holdingViewCenterX = holdingViewLocation[0] + holdingViewWidth / 2;
                float maxX = viewLocation[0] + viewWidth - circleCenterX;
                return (x + circleCenterX - holdingViewCenterX + offset[0]) / (maxX - holdingViewCenterX);
            }

            @Override
            Direction toRtl() {
                return RIGHT;
            }
        };

        private final int mFlag;

        Direction(int flag) {
            mFlag = flag;
        }

        abstract int getOffsetX(int offsetX);

        abstract float getSlideOffset(float x, float circleCenterX, int[] viewLocation, int viewWidth, int[] holdingViewLocation, int holdingViewWidth, int[] offset);

        abstract Direction toRtl();

        static Direction fromFlag(int flag) {
            for (Direction direction : values()) {
                if (direction.mFlag == flag) {
                    return direction;
                }
            }
            throw new IllegalStateException("There is no direction with flag " + flag);
        }

    }
}
