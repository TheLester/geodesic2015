package com.dogar.geodesic.activity;

import com.dogar.geodesic.R;
import com.dogar.geodesic.enums.GeodesicProblemType;
import com.dogar.geodesic.utils.ToastUtils;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.EditText;
import android.widget.TextView;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import static com.dogar.geodesic.utils.Constants.*;

public class GeodesicProblemActivity extends AppCompatActivity {
    @InjectView(R.id.main_toolbar)           Toolbar  toolbar;
    @InjectView(R.id.input_lat_one)          EditText inputLatOne;
    @InjectView(R.id.input_lon_one)          EditText inputLonOne;
    @InjectView(R.id.tv_first_parameter)     TextView firstParamName;
    @InjectView(R.id.input_first_parameter)  EditText firstParamInput;
    @InjectView(R.id.tv_second_parameter)    TextView secondParamName;
    @InjectView(R.id.input_second_parameter) EditText secondParamInput;
    //resul
    @InjectView(R.id.tv_name_answer_first)   TextView answerNameFirst;
    @InjectView(R.id.tv_name_answer_second)  TextView answerNameSecond;
    @InjectView(R.id.tv_name_answer_third)   TextView answerNameThird;
    @InjectView(R.id.tv_answer_first)        TextView answerFirst;
    @InjectView(R.id.tv_answer_second)       TextView answerSecond;
    @InjectView(R.id.tv_answer_third)        TextView answerThird;

    private GeodesicProblemType currentProblemType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geodesic_problem);
        ButterKnife.inject(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        currentProblemType = (GeodesicProblemType) intent.getExtras().getSerializable(GEODESIC_PROBLEM);
        switch (currentProblemType) {
            case DIRECT:
                getSupportActionBar().setTitle(R.string.direct_geodesic);
                firstParamName.setText(R.string.azi);
                secondParamName.setText(R.string.dist);

                answerNameFirst.setText(R.string.lat2);
                answerNameSecond.setText(R.string.lon2);
                answerNameThird.setText(R.string.azi2);
                break;
            case INDIRECT:
                getSupportActionBar().setTitle(R.string.indirect_geodesic);
                firstParamName.setText(R.string.lat2);
                secondParamName.setText(R.string.lon2);

                answerNameFirst.setText(R.string.dist);
                answerNameSecond.setText(R.string.azi);
                answerNameThird.setText(R.string.azi2);
                break;
            default:
                break;
        }
    }

    @OnClick(R.id.button_clear)
    void clearClicked() {
        inputLatOne.setText("");
        inputLonOne.setText("");
        firstParamInput.setText("");
        secondParamInput.setText("");
        answerFirst.setText("");
        answerSecond.setText("");
        answerThird.setText("");
    }

    @OnClick(R.id.button_solve)
    void solveClicked() {
        solveProblem();
    }

    private void solveProblem() {
        try {
            double latitude1 = Double
                    .parseDouble(inputLatOne.getText().toString());
            double longitude1 = Double.parseDouble(inputLonOne.getText()
                    .toString());
            double firstAdditionalParameter = Double.parseDouble(firstParamInput.getText().toString());
            double secondAdditionalParameter = Double.parseDouble(secondParamInput.getText().toString());

            GeodesicData result = null;
            switch (currentProblemType) {
                case INDIRECT:
                    result = Geodesic.WGS84.Inverse(latitude1, longitude1,
                            firstAdditionalParameter, secondAdditionalParameter);

                    answerFirst.setText(String.valueOf(result.s12));
                    answerSecond.setText(String.valueOf(result.azi1 + " °"));
                    answerThird.setText(String.valueOf(result.azi2 + " °"));
                    break;
                case DIRECT:
                    result = Geodesic.WGS84.Direct(latitude1, longitude1,
                            firstAdditionalParameter, secondAdditionalParameter);

                    answerFirst.setText(String.valueOf(result.lat2));
                    answerSecond.setText(String.valueOf(result.lon2));
                    answerThird.setText(String.valueOf(result.azi2 + " °"));
                    break;
                default:
                    break;

            }
        } catch (NumberFormatException ex) {
            ToastUtils.show(this, "Wrong input! Make sure you set all values.");
        }
    }
}
