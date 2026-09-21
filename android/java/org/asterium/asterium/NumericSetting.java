package org.asterium.asterium;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

final class NumericSetting extends LinearLayout
{
	private final String label;
	private final PropertySheet.Scale scale;
	private final Double defaultValue;
	private final PropertySheet.OnSlide commit;
	private final TextView readout;
	private double value;
	private Dialog editor;

	NumericSetting(Context context, String label, PropertySheet.Scale scale,
	               double value, Double defaultValue, PropertySheet.OnSlide commit)
	{
		super(context);
		this.label = label;
		this.scale = scale;
		this.defaultValue = defaultValue;
		this.commit = commit;
		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER_VERTICAL);
		setMinimumHeight(Theme.dp(Theme.ROW));
		Theme.padding(this, 16, 10, 16, 10);
		setBackground(Theme.pressable(Theme.box(Color.TRANSPARENT, 0), 0));
		addView(Theme.text(context, label, 14, Theme.TEXT_CHIP, false),
				new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
		readout = Theme.text(context, "", 13, Theme.ACCENT, true);
		readout.setGravity(Gravity.END);
		final LayoutParams readoutParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
		readoutParams.leftMargin = Theme.dp(12);
		addView(readout, readoutParams);
		final TextView chevron = Theme.text(context, "›", 20, Theme.ACCENT_LINK, false);
		Theme.padding(chevron, 10, 0, 0, 0);
		addView(chevron);
		setValue(value);
		setFocusable(true);
		setOnClickListener(v -> edit());
	}

	void setValue(double value)
	{
		this.value = value;
		readout.setText(T.t(scale.readout(value)));
		setContentDescription(T.t(label) + ": " + T.t(scale.readout(value)));
	}

	private void edit()
	{
		if (editor != null)
			return;
		final Context context = getContext();
		final Dialog dialog = new Dialog(context);
		editor = dialog;
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setOnDismissListener(d -> editor = null);

		final LinearLayout column = new LinearLayout(context);
		column.setOrientation(VERTICAL);
		column.setBackground(Theme.box(Theme.SHEET, 16, Theme.PANEL_EDGE));
		column.setFocusableInTouchMode(true);
		Theme.padding(column, 20, 18, 20, 18);
		column.addView(Theme.text(context, label, 16, Theme.TEXT, false));

		final EditText input = Widgets.field(context, label, "", true);
		input.setInputType(InputType.TYPE_CLASS_NUMBER
				| (scale.inputMin() < 0. ? InputType.TYPE_NUMBER_FLAG_SIGNED : 0)
				| (scale.inputDecimals() > 0 ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
		input.setText(T.t(scale.input(value)));
		input.setTextColor(Theme.ACCENT);
		input.setSelectAllOnFocus(true);
		final LayoutParams inputParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		inputParams.topMargin = Theme.dp(16);
		column.addView(input, inputParams);

		final String range = PropertySheet.format(scale.inputMin(), scale.inputDecimals())
				+ " – " + PropertySheet.format(scale.inputMax(), scale.inputDecimals())
				+ (scale.inputUnit().isEmpty() ? "" : " " + T.t(scale.inputUnit()));
		final TextView rangeLabel = Theme.text(context, range, 12, Theme.TEXT_DIM, false);
		Theme.padding(rangeLabel, 0, 8, 0, 4);
		column.addView(rangeLabel);

		final int steps = scale.decimals == 0
				? Math.max(1, Math.min(1000, (int) Math.round(scale.max - scale.min))) : 1000;
		final SeekBar slider = Widgets.slider(context, steps,
				(int) Math.round(scale.position(value) * steps));
		slider.setContentDescription(T.t(label));
		column.addView(slider);
		final TextView preview = Theme.text(context, scale.readout(value), 13, Theme.ACCENT, false);
		column.addView(preview);

		final boolean[] changing = { false };
		final Double[] exactValue = { value };
		input.addTextChangedListener(new TextWatcher()
		{
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void afterTextChanged(Editable text)
			{
				if (changing[0])
					return;
				exactValue[0] = null;
				input.setError(null);
				final double typed = Widgets.number(input);
				if (!valid(typed))
					return;
				final double nativeValue = scale.fromInput(typed);
				slider.setProgress((int) Math.round(scale.position(nativeValue) * steps));
				preview.setText(T.t(scale.readout(nativeValue)));
			}
		});
		slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			public void onProgressChanged(SeekBar bar, int progress, boolean fromUser)
			{
				if (!fromUser)
					return;
				changing[0] = true;
				exactValue[0] = null;
				final double next = scale.value(progress / (double) steps);
				input.setText(T.t(scale.input(next)));
				input.setSelection(input.length());
				input.setError(null);
				preview.setText(T.t(scale.readout(scale.fromInput(Widgets.number(input)))));
				changing[0] = false;
			}
			public void onStartTrackingTouch(SeekBar bar) {}
			public void onStopTrackingTouch(SeekBar bar) {}
		});
		if (defaultValue != null)
		{
			final LayoutParams restoreParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			restoreParams.topMargin = Theme.dp(16);
			column.addView(Widgets.secondaryButton(context, "Restore default", v ->
			{
				changing[0] = true;
				exactValue[0] = defaultValue;
				input.setText(T.t(scale.input(defaultValue)));
				input.setSelection(input.length());
				input.setError(null);
				slider.setProgress((int) Math.round(scale.position(defaultValue) * steps));
				preview.setText(T.t(scale.readout(defaultValue)));
				changing[0] = false;
			}), restoreParams);
		}

		final Runnable apply = () ->
		{
			final double typed = Widgets.number(input);
			if (!valid(typed))
			{
				input.setError(T.t(scale.inputDecimals() == 0 && Double.isFinite(typed)
						&& typed != Math.rint(typed) ? "Enter a whole number."
						: "Enter a number within this range.") + " " + range);
				input.requestFocus();
				return;
			}
			final double rounded = Double.parseDouble(PropertySheet.format(typed, scale.inputDecimals()));
			final double next = exactValue[0] != null ? exactValue[0] : scale.fromInput(rounded);
			setValue(next);
			dialog.dismiss();
			commit.slid(next);
		};
		input.setOnEditorActionListener((view, action, event) ->
		{
			if (action == EditorInfo.IME_ACTION_DONE
					|| (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
						&& event.getAction() == KeyEvent.ACTION_UP))
			{
				apply.run();
				return true;
			}
			return false;
		});
		final LinearLayout actions = new LinearLayout(context);
		actions.setOrientation(HORIZONTAL);
		final LayoutParams actionsParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		actionsParams.topMargin = Theme.dp(18);
		column.addView(actions, actionsParams);
		final LayoutParams cancelParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
		cancelParams.rightMargin = Theme.dp(10);
		actions.addView(Widgets.secondaryButton(context, "Cancel", v -> dialog.dismiss()), cancelParams);
		actions.addView(Widgets.primaryButton(context, "Apply", v -> apply.run()),
				new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

		final ScrollView scroll = new ScrollView(context);
		scroll.setFillViewport(true);
		scroll.addView(column);
		dialog.setContentView(scroll);
		column.requestFocus();
		final Window window = dialog.getWindow();
		if (window != null)
		{
			window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
					| WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		}
		dialog.show();
		if (window != null)
		{
			final int screen = context.getResources().getDisplayMetrics().widthPixels;
			window.setLayout(Math.min(Theme.dp(360), screen - Theme.dp(32)),
					WindowManager.LayoutParams.WRAP_CONTENT);
		}
	}

	private boolean valid(double typed)
	{
		return Double.isFinite(typed) && typed >= scale.inputMin() && typed <= scale.inputMax()
				&& (scale.inputDecimals() != 0 || typed == Math.rint(typed));
	}

	@Override
	protected void onDetachedFromWindow()
	{
		if (editor != null)
			editor.dismiss();
		super.onDetachedFromWindow();
	}
}
