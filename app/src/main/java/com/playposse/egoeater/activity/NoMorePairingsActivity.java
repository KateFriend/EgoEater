package com.playposse.egoeater.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.playposse.egoeater.R;
import com.playposse.egoeater.contentprovider.MainDatabaseHelper;
import com.playposse.egoeater.services.PopulatePipelineService;
import com.playposse.egoeater.util.DatabaseDumper;

import java.util.List;

/**
 * An {@link android.app.Activity} that informs the user that no more pairings are available.
 */
public class NoMorePairingsActivity extends ParentWithLocationCheckActivity {

    private static final String LOG_TAG = NoMorePairingsActivity.class.getSimpleName();

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_no_more_pairings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Let's kick the service to build the pipeline. Maybe, another pairing can be found.
        startService(new Intent(this, PopulatePipelineService.class));

        DatabaseDumper.dumpTables(new MainDatabaseHelper(this));
    }
}