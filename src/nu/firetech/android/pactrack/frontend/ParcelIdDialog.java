/*
 * Copyright (C) 2009 Joakim Andersson
 * 
 * This file is part of PactrackDroid, an Android application to keep
 * track of parcels sent with the Swedish mail service (Posten).
 * 
 * PactrackDroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * PactrackDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package nu.firetech.android.pactrack.frontend;

import nu.firetech.android.pactrack.R;
import nu.firetech.android.pactrack.backend.ParcelDbAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.view.WindowManager.LayoutParams;

public class ParcelIdDialog extends Dialog implements
		DialogAwareListActivity.Dialog {

	private EditText mParcelText;
	private Long mRowId;
	private ParcelDbAdapter mDbAdapter;
	private boolean mCloseDbAdapter = false;
	private String mInitialText;

	private AlertDialog mErrorDialog;

	public static void show(final DialogAwareListActivity context, Long rowId,
			ParcelDbAdapter dbAdapter) {
		ParcelIdDialog d = new ParcelIdDialog(context, rowId, dbAdapter);
		d.show();
	}

	public static void show(final DialogAwareListActivity context, Bundle dialogData) {
		ParcelDbAdapter dbAdapter = new ParcelDbAdapter(context);
		dbAdapter.open();
		ParcelIdDialog d = new ParcelIdDialog(context,
				(dialogData.containsKey(ParcelDbAdapter.KEY_ROWID) ? dialogData.getLong(ParcelDbAdapter.KEY_ROWID) : null),
				dbAdapter);
		d.mInitialText = dialogData.getString(ParcelDbAdapter.KEY_PARCEL);
		d.mCloseDbAdapter = true;
		d.show();
	}

	public ParcelIdDialog(final DialogAwareListActivity context, Long rowId,
			ParcelDbAdapter dbAdapter) {
		super(context);
		if (context instanceof Activity) {
			setOwnerActivity((Activity)context);
		}
		
		mRowId = rowId;
		mDbAdapter = dbAdapter;

		mInitialText = null;

		context.addDialog(this);
		setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				context.removeDialog(ParcelIdDialog.this);
				
				if (mCloseDbAdapter) {
					mDbAdapter.close();
				}
			}
		});

		mErrorDialog = new AlertDialog.Builder(context).setTitle(
				R.string.id_error_title).setIcon(
				android.R.drawable.ic_dialog_alert).setMessage(
				R.string.id_error_message).setNeutralButton(R.string.ok,
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).create();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);

		setContentView(R.layout.parcel_id_dialog);
		setTitle(R.string.parcelid);

		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
				R.drawable.ic_dialog_menu_generic);
		LayoutParams params = w.getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		w.setAttributes(params);

		mParcelText = (EditText) findViewById(R.id.parcelid);
		mParcelText.setKeyListener(new NumberKeyListener() {
			private char[] acceptedChars = null;

			@Override
			protected char[] getAcceptedChars() {
				if (acceptedChars == null) {
					acceptedChars = new char[] { '0', '1', '2', '3', '4', '5',
							'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
							'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
							'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
				}

				return acceptedChars;
			}

			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_TEXT
						| InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
			}
		});

		Button okButton = (Button) findViewById(R.id.ok);
		okButton.setOnClickListener(new OkListener());
		Button cancelButton = (Button) findViewById(R.id.cancel);
		cancelButton.setOnClickListener(new ButtonListener());

		if (savedInstanceState != null) {
			mRowId = savedInstanceState.getLong(ParcelDbAdapter.KEY_ROWID);
		}

		if (mRowId != null && mInitialText == null) {
			if (savedInstanceState != null
					&& savedInstanceState
							.containsKey(ParcelDbAdapter.KEY_PARCEL)) {
				mInitialText = savedInstanceState
						.getString(ParcelDbAdapter.KEY_PARCEL);
			} else {
				Cursor parcel = mDbAdapter.fetchParcel(mRowId);
				mInitialText = parcel.getString(parcel
						.getColumnIndexOrThrow(ParcelDbAdapter.KEY_PARCEL));
				parcel.close();
			}
		}

		if (mInitialText == null) {
			mInitialText = "";
		}
		mParcelText.setText(mInitialText);
	}

	@Override
	public Bundle getInstanceState() {
		Bundle outState = new Bundle(2);

		if (mRowId != null) {
			outState.putLong(ParcelDbAdapter.KEY_ROWID, mRowId);
		}
		outState.putString(ParcelDbAdapter.KEY_PARCEL, mParcelText.getText()
				.toString());
		return outState;
	}

	private class ButtonListener implements android.view.View.OnClickListener {
		@Override
		public void onClick(View v) {
			ParcelIdDialog.this.dismiss();
		}
	}

	private class OkListener extends ButtonListener {
		@Override
		public void onClick(View v) {
			String parcel = mParcelText.getText().toString();
			if (parcel.length() < 9) {
				mErrorDialog.show();
				return;
			}

			boolean changed = true;
			if (mRowId == null) {
				mRowId = mDbAdapter.addParcel(parcel);
			} else {
				Cursor dbParcel = mDbAdapter.fetchParcel(mRowId);
				String oldParcel = dbParcel.getString(dbParcel
						.getColumnIndexOrThrow(ParcelDbAdapter.KEY_PARCEL));
				dbParcel.close();

				if (oldParcel.equals(parcel)) {
					changed = false;
				} else {
					mDbAdapter.changeParcelId(mRowId, parcel);
				}
			}

			if (changed) {
				if (getOwnerActivity() instanceof ParcelView) {
					((ParcelView) getOwnerActivity()).updateView(true);
				} else {
					Intent i = new Intent(getContext(), ParcelView.class)
							.putExtra(ParcelDbAdapter.KEY_ROWID, mRowId)
							.putExtra(ParcelView.FORCE_REFRESH, true);

					getContext().startActivity(i);
				}
			}

			super.onClick(v);
		}
	}

	@Override
	public void onContextChange(Context newContext) {
	}

	@Override
	public void onContextDestroy(Context oldContext) {
		dismiss();
	}

}
