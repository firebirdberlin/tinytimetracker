package com.firebirdberlin.tinytimetracker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.firebirdberlin.tinytimetracker.models.TrackerEntry;

public class AddTrackerActivity extends AppCompatActivity {
    private static String TAG = "AddTrackerActivity";
    private TrackerEntry tracker = null;
    private EditText edit_tracker_verbose_name = null;
    private EditText edit_tracker_working_hours = null;

    private final int RED = Color.parseColor("#f44336");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_tracker_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.action_edit));

        setSupportActionBar(toolbar);

        // Enable the Up button
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        long tracker_id = intent.getLongExtra("tracker_id", -1L);

        LogDataSource datasource = new LogDataSource(this);
        if (tracker_id > -1L) {
            tracker = datasource.getTracker(tracker_id);
        }

        init();
        datasource.close();
    }

    private void init() {
        edit_tracker_verbose_name = findViewById(R.id.edit_tracker_verbose_name);
        edit_tracker_working_hours = findViewById(R.id.edit_tracker_working_hours);
        if (tracker != null) {
            edit_tracker_verbose_name.setText("");
            edit_tracker_verbose_name.append(tracker.verbose_name);
            edit_tracker_working_hours.setText("");
            edit_tracker_working_hours.append(String.valueOf(tracker.working_hours));
        } else {
            edit_tracker_working_hours.setText("");
            edit_tracker_working_hours.append("8");
        }
        edit_tracker_verbose_name.requestFocus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_tracker_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_ok) {
            onClickOk(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClickOk(View v) {
        String verbose_name = edit_tracker_verbose_name.getText().toString();
        String working_hours = edit_tracker_working_hours.getText().toString();

        if (!validateInputs(verbose_name)) {
            return;
        }

        if (tracker == null) {
            tracker = new TrackerEntry(verbose_name, "Manual");
        }

        tracker.verbose_name = verbose_name.trim();
        tracker.working_hours = Float.parseFloat(working_hours);

        LogDataSource datasource = new LogDataSource(this);
        datasource.save(tracker);
        datasource.close();

        this.finish();
    }

    private boolean validateInputs(String verbose_name) {
        if (verbose_name.isEmpty()) {
            edit_tracker_verbose_name.setBackgroundColor(RED);
            edit_tracker_verbose_name.requestFocus();
            edit_tracker_verbose_name.invalidate();
            return false;
        }

        String working_hours = edit_tracker_working_hours.getText().toString();
        if (working_hours.isEmpty() ||
             Float.parseFloat(working_hours) < 0.f || Float.parseFloat(working_hours) > 24.f) {
            edit_tracker_working_hours.setBackgroundColor(RED);
            edit_tracker_working_hours.requestFocus();
            edit_tracker_working_hours.invalidate();
            return false;
        }

        LogDataSource datasource = new LogDataSource(this);
        TrackerEntry other = datasource.getTracker(verbose_name);
        datasource.close();
        if (tracker == null && other != null) { // a tracker with this name already exists
            edit_tracker_verbose_name.setBackgroundColor(RED);
            edit_tracker_verbose_name.invalidate();
            return false;
        }

        if (tracker != null && other != null && other.id != tracker.id) {
            edit_tracker_verbose_name.setBackgroundColor(RED);
            edit_tracker_verbose_name.invalidate();
            return false;
        }

        return true;
    }

    public static void open(Context context) {
        Intent myIntent = new Intent(context, AddTrackerActivity.class);
        context.startActivity(myIntent);
    }

    public static void open(Context context, long tracker_id) {
        Intent myIntent = new Intent(context, AddTrackerActivity.class);
        myIntent.putExtra("tracker_id", tracker_id);
        context.startActivity(myIntent);
    }
}
