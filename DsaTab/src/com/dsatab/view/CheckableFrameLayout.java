package com.dsatab.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.gandulf.guilib.listener.CheckableListenable;
import com.gandulf.guilib.listener.OnCheckedChangeListener;

public class CheckableFrameLayout extends FrameLayout implements CheckableListenable {
	boolean mChecked = false;

	private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

	private OnCheckedChangeListener mOnCheckedChangeListener;

	public CheckableFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CheckableFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CheckableFrameLayout(Context context) {
		super(context);
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		if (mChecked != checked) {
			mChecked = checked;
			refreshDrawableState();

			if (mOnCheckedChangeListener != null) {
				mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
			}
		}
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}

	public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
		this.mOnCheckedChangeListener = onCheckedChangeListener;
	}

}