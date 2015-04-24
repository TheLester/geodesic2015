package com.dogar.geodesic.screens;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

import com.dogar.geodesic.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class DirectProblemActivity extends Activity {
	private EditText lat1Edit;
	private EditText lon1Edit;
	private EditText aziEdit;
	private EditText distEdit;
	private TextView resultText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.direct_geodesic_problem);
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

		Button button = (Button) findViewById(R.id.solvebuttonDi);

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				solveProblem();
			}

		});
	}

	private void findEditTexts() {
		lat1Edit = (EditText) findViewById(R.id.lat1EditDi);
		lon1Edit = (EditText) findViewById(R.id.lon1EditDi);
		aziEdit = (EditText) findViewById(R.id.azimuthDi);
		distEdit = (EditText) findViewById(R.id.distanceDi);
		resultText = (TextView) findViewById(R.id.resultviewDi);
	}

	private void solveProblem() {
		try {
			double latitude1 = Double
					.parseDouble(lat1Edit.getText().toString());
			double longitude1 = Double.parseDouble(lon1Edit.getText()
					.toString());
			double azimuth = Double.parseDouble(aziEdit.getText().toString());

			double distance = Double.parseDouble(distEdit.getText().toString());
			GeodesicData result = Geodesic.WGS84.Direct(latitude1, longitude1,
					azimuth, distance);
			resultText.setText("Latitude of 2 point: " + result.lat2
					+ " , Longitude at 2 point: " + result.lon2
					+ " , Azimuth at 2 point: " + result.azi2 + " °.");
		} catch (NumberFormatException ex) {
			Toast t = Toast.makeText(this,
					"Wrong input! Make sure you set all values.",
					Toast.LENGTH_LONG);
			t.show();
		}
	}
}
