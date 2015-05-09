package com.dogar.geodesic.activity;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

import com.dogar.geodesic.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class DirectProblemActivity extends AppCompatActivity {
    @InjectView(R.id.main_toolbar) Toolbar toolbar;
    @InjectView(R.id.lat1EditDi)   EditText lat1Edit;
    @InjectView(R.id.lon1EditDi)   EditText lon1Edit;
    @InjectView(R.id.azimuthDi)    EditText aziEdit;
    @InjectView(R.id.distanceDi)   EditText distEdit;
    @InjectView(R.id.resultviewDi) TextView resultText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.direct_geodesic_problem);
        ButterKnife.inject(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

    @OnClick(R.id.solvebuttonDi)
    void solveClicked() {
        solveProblem();
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
