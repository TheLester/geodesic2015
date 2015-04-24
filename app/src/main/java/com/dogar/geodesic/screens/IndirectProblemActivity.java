package com.dogar.geodesic.screens;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;

import com.dogar.geodesic.R;

import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class IndirectProblemActivity extends Activity {
	private EditText lat1Edit;
	private EditText lat2Edit;
	private EditText lon1Edit;
	private EditText lon2Edit;
	private TextView resultText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.indirect_geodesic_problem);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		findEditTexts();
		addListenerOnButton();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// Respond to the action bar's Up/Home button
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void addListenerOnButton() {

		Button button = (Button) findViewById(R.id.solvebuttonUn);

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				solveProblem();
			}

		});
	}

	private void findEditTexts() {
		lat1Edit = (EditText) findViewById(R.id.lat1EditUnd);
		lat2Edit = (EditText) findViewById(R.id.lat2EditUnd);
		lon1Edit = (EditText) findViewById(R.id.lon1EditUnd);
		lon2Edit = (EditText) findViewById(R.id.lon2EditUnd);
		resultText = (TextView) findViewById(R.id.resultviewUn);
	}

	private void solveProblem() {
		try {
			double latitude1 = Double
					.parseDouble(lat1Edit.getText().toString());
			double latitude2 = Double
					.parseDouble(lat2Edit.getText().toString());
			double longitude1 = Double.parseDouble(lon1Edit.getText()
					.toString());
			double longitude2 = Double.parseDouble(lon2Edit.getText()
					.toString());
			GeodesicData result = Geodesic.WGS84.Inverse(latitude1, longitude1,
					latitude2, longitude2);
			resultText.setText("Distance between points: " + result.s12
					+ " meters, Azimuth at 1 point: " + result.azi1
					+ " °, Azimuth at 2 point: " + result.azi2 + " °.");
		} catch (NumberFormatException ex) {
			Toast t = Toast.makeText(this, "Wrong input! Make sure you set all values.",
					Toast.LENGTH_LONG);
			t.show();
		}
	}
}
