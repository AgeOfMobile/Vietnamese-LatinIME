/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.inputmethod.latin.suggestions;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import com.android.inputmethod.keyboard.KeyDetector;
import com.android.inputmethod.keyboard.Keyboard;
import com.android.inputmethod.keyboard.KeyboardActionListener;
import com.android.inputmethod.keyboard.KeyboardView;
import com.android.inputmethod.keyboard.MoreKeysDetector;
import com.android.inputmethod.keyboard.MoreKeysPanel;
import com.android.inputmethod.keyboard.PointerTracker;
import com.android.inputmethod.keyboard.PointerTracker.DrawingProxy;
import com.android.inputmethod.keyboard.PointerTracker.KeyEventHandler;
import com.android.inputmethod.keyboard.PointerTracker.TimerProxy;
import vietnamese.com.android.inputmethod.latin.R;

/**
 * A view that renders a virtual {@link MoreSuggestions}. It handles rendering of keys and detecting
 * key presses and touch movements.
 */
public class MoreSuggestionsView extends KeyboardView implements MoreKeysPanel {
    private final int[] mCoordinates = new int[2];

    final KeyDetector mModalPanelKeyDetector;
    private final KeyDetector mSlidingPanelKeyDetector;

    private Controller mController;
    KeyboardActionListener mListener;
    private int mOriginX;
    private int mOriginY;

    static final TimerProxy EMPTY_TIMER_PROXY = new TimerProxy.Adapter();

    final KeyboardActionListener mSuggestionsPaneListener =
            new KeyboardActionListener.Adapter() {
        @Override
        public void onPressKey(int primaryCode) {
            mListener.onPressKey(primaryCode);
        }

        @Override
        public void onReleaseKey(int primaryCode, boolean withSliding) {
            mListener.onReleaseKey(primaryCode, withSliding);
        }

        @Override
        public void onCodeInput(int primaryCode, int x, int y) {
            final int index = primaryCode - MoreSuggestions.SUGGESTION_CODE_BASE;
            if (index >= 0 && index < SuggestionsView.MAX_SUGGESTIONS) {
                mListener.onCustomRequest(index);
            }
        }

        @Override
        public void onCancelInput() {
            mListener.onCancelInput();
        }
    };

    public MoreSuggestionsView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.moreSuggestionsViewStyle);
    }

    public MoreSuggestionsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources res = context.getResources();
        mModalPanelKeyDetector = new KeyDetector(/* keyHysteresisDistance */ 0);
        mSlidingPanelKeyDetector = new MoreKeysDetector(
                res.getDimension(R.dimen.more_suggestions_slide_allowance));
        setKeyPreviewPopupEnabled(false, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final Keyboard keyboard = getKeyboard();
        if (keyboard != null) {
            final int width = keyboard.mOccupiedWidth + getPaddingLeft() + getPaddingRight();
            final int height = keyboard.mOccupiedHeight + getPaddingTop() + getPaddingBottom();
            setMeasuredDimension(width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public void setKeyboard(Keyboard keyboard) {
        super.setKeyboard(keyboard);
        mModalPanelKeyDetector.setKeyboard(keyboard, -getPaddingLeft(), -getPaddingTop());
        mSlidingPanelKeyDetector.setKeyboard(keyboard, -getPaddingLeft(),
                -getPaddingTop() + mVerticalCorrection);
    }

    @Override
    public KeyDetector getKeyDetector() {
        return mSlidingPanelKeyDetector;
    }

    @Override
    public KeyboardActionListener getKeyboardActionListener() {
        return mSuggestionsPaneListener;
    }

    @Override
    public DrawingProxy getDrawingProxy() {
        return this;
    }

    @Override
    public TimerProxy getTimerProxy() {
        return EMPTY_TIMER_PROXY;
    }

    @Override
    public void setKeyPreviewPopupEnabled(boolean previewEnabled, int delay) {
        // Suggestions pane needs no pop-up key preview displayed, so we pass always false with a
        // delay of 0. The delay does not matter actually since the popup is not shown anyway.
        super.setKeyPreviewPopupEnabled(false, 0);
    }

    @Override
    public void showMoreKeysPanel(View parentView, Controller controller, int pointX, int pointY,
            PopupWindow window, KeyboardActionListener listener) {
        mController = controller;
        mListener = listener;
        final View container = (View)getParent();
        final MoreSuggestions pane = (MoreSuggestions)getKeyboard();
        final int defaultCoordX = pane.mOccupiedWidth / 2;
        // The coordinates of panel's left-top corner in parentView's coordinate system.
        final int x = pointX - defaultCoordX - container.getPaddingLeft();
        final int y = pointY - container.getMeasuredHeight() + container.getPaddingBottom();

        window.setContentView(container);
        window.setWidth(container.getMeasuredWidth());
        window.setHeight(container.getMeasuredHeight());
        parentView.getLocationInWindow(mCoordinates);
        window.showAtLocation(parentView, Gravity.NO_GRAVITY,
                x + mCoordinates[0], y + mCoordinates[1]);

        mOriginX = x + container.getPaddingLeft();
        mOriginY = y + container.getPaddingTop();
    }

    private boolean mIsDismissing;

    @Override
    public boolean dismissMoreKeysPanel() {
        if (mIsDismissing || mController == null) return false;
        mIsDismissing = true;
        final boolean dismissed = mController.dismissMoreKeysPanel();
        mIsDismissing = false;
        return dismissed;
    }

    @Override
    public int translateX(int x) {
        return x - mOriginX;
    }

    @Override
    public int translateY(int y) {
        return y - mOriginY;
    }

    private final KeyEventHandler mModalPanelKeyEventHandler = new KeyEventHandler() {
        @Override
        public KeyDetector getKeyDetector() {
            return mModalPanelKeyDetector;
        }

        @Override
        public KeyboardActionListener getKeyboardActionListener() {
            return mSuggestionsPaneListener;
        }

        @Override
        public DrawingProxy getDrawingProxy() {
            return MoreSuggestionsView.this;
        }

        @Override
        public TimerProxy getTimerProxy() {
            return EMPTY_TIMER_PROXY;
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        final int action = me.getAction();
        final long eventTime = me.getEventTime();
        final int index = me.getActionIndex();
        final int id = me.getPointerId(index);
        final PointerTracker tracker = PointerTracker.getPointerTracker(id, this);
        final int x = (int)me.getX(index);
        final int y = (int)me.getY(index);
        tracker.processMotionEvent(action, x, y, eventTime, mModalPanelKeyEventHandler);
        return true;
    }
}
