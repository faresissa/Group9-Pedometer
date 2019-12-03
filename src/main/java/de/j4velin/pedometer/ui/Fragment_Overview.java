/*
 * Copyright 2014 Thomas Hoffmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.j4velin.pedometer.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Entity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.BarModel;
import org.eazegraph.lib.models.PieModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.time.LocalDate; //
import java.text.DateFormatSymbols; //
import java.util.Calendar;

import de.j4velin.pedometer.BuildConfig;
import de.j4velin.pedometer.Database;
import de.j4velin.pedometer.R;
import de.j4velin.pedometer.SensorListener;
import de.j4velin.pedometer.util.API26Wrapper;
import de.j4velin.pedometer.util.Logger;
import de.j4velin.pedometer.util.Util;
import de.j4velin.pedometer.MonthlyAverage;

public class Fragment_Overview extends Fragment implements SensorEventListener {

    private TextView stepsView, totalView, averageView;
    private PieModel sliceGoal, sliceCurrent;
    private PieChart pg;

    private Button button;

    private Button graph_button;

    private int todayOffset, total_start, goal, since_boot, total_days;
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
    private boolean showSteps = true;

    private boolean monthlyAverage = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (Build.VERSION.SDK_INT >= 26) {
            API26Wrapper.startForegroundService(getActivity(),
                    new Intent(getActivity(), SensorListener.class));
        } else {
            getActivity().startService(new Intent(getActivity(), SensorListener.class));
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_overview, null);
        stepsView = v.findViewById(R.id.steps);
        totalView = v.findViewById(R.id.total);
        averageView = v.findViewById(R.id.average);

        button = v.findViewById(R.id.reset_steps_button);


        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                Database db = Database.getInstance(getActivity());

                Integer steps = db.getSteps(Util.getToday());
                Logger.log("Steps in database before reset: " + steps.toString());

                db.saveCurrentSteps(0);
                steps = db.getSteps(Util.getToday());
                Logger.log("Steps in database after reset: " + steps.toString());
                db.close();
                since_boot = 0;
            }
        });

        graph_button = v.findViewById(R.id.toggle_graph_button);

        graph_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                monthlyAverage = !monthlyAverage;
                updateBars();
            }
        });

        pg = v.findViewById(R.id.graph);

        // slice for the steps taken today
        sliceCurrent = new PieModel("", 0, Color.parseColor("#99CC00"));
        pg.addPieSlice(sliceCurrent);

        // slice for the "missing" steps until reaching the goal
        sliceGoal = new PieModel("", Fragment_Settings.DEFAULT_GOAL, Color.parseColor("#CC0000"));
        pg.addPieSlice(sliceGoal);

        pg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                showSteps = !showSteps;
                stepsDistanceChanged();
            }
        });

        pg.setDrawValueInPie(false);
        pg.setUsePieRotation(true);
        pg.startAnimation();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);

        Database db = Database.getInstance(getActivity());

        if (BuildConfig.DEBUG) db.logState();
        // read todays offset
        todayOffset = db.getSteps(Util.getToday());

        SharedPreferences prefs =
                getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        goal = prefs.getInt("goal", Fragment_Settings.DEFAULT_GOAL);
        since_boot = db.getCurrentSteps();
        int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

        // register a sensorlistener to live update the UI if a step is taken
        SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor == null) {
            new AlertDialog.Builder(getActivity()).setTitle(R.string.no_sensor)
                    .setMessage(R.string.no_sensor_explain)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(final DialogInterface dialogInterface) {
                            getActivity().finish();
                        }
                    }).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();
        } else {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
        }

        since_boot -= pauseDifference;

        total_start = db.getTotalWithoutToday();
        total_days = db.getDays();

        db.close();

        stepsDistanceChanged();
    }

    /**
     * Call this method if the Fragment should update the "steps"/"km" text in
     * the pie graph as well as the pie and the bars graphs.
     */
    private void stepsDistanceChanged() {
        if (showSteps) {
            ((TextView) getView().findViewById(R.id.unit)).setText(getString(R.string.steps));
        } else {
            String unit = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                    .getString("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT);
            if (unit.equals("cm")) {
                unit = "km";
            } else {
                unit = "mi";
            }
            ((TextView) getView().findViewById(R.id.unit)).setText(unit);
        }

        updatePie();
        updateBars();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            SensorManager sm =
                    (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Logger.log(e);
        }
        Database db = Database.getInstance(getActivity());
        db.saveCurrentSteps(since_boot);
        db.close();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_split_count:
                Dialog_Split.getDialog(getActivity(),
                        total_start + Math.max(todayOffset + since_boot, 0)).show();
                return true;
            default:
                return ((Activity_Main) getActivity()).optionsItemSelected(item);
        }
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {
        // won't happen
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        double total = (x * x + y * y + z * z)/(SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

        if (BuildConfig.DEBUG) Logger.log(
                "UI - sensorChanged | todayOffset: " + todayOffset + " since boot: " +
                        total);
        if (total > Integer.MAX_VALUE || total == 0) {
            return;
        }
        if (todayOffset == Integer.MIN_VALUE) {
            // no values for today
            // we dont know when the reboot was, so set todays steps to 0 by
            // initializing them with -STEPS_SINCE_BOOT
            todayOffset = -(int) total;
            Database db = Database.getInstance(getActivity());
            db.insertNewDay(Util.getToday(), (int) total);
            db.close();
        }

        if ((int) total >  1) {
            since_boot = (int) total + since_boot;
        }
        updatePie();
    }

    /**
     * Updates the pie graph to show todays steps/distance as well as the
     * yesterday and total values. Should be called when switching from step
     * count to distance.
     */
    private void updatePie() {
        if (BuildConfig.DEBUG) Logger.log("UI - update steps: " + since_boot);
        // todayOffset might still be Integer.MIN_VALUE on first start
        int steps_today = Math.max(todayOffset + since_boot, 0);
        sliceCurrent.setValue(steps_today);
        if (goal - steps_today > 0) {
            // goal not reached yet
            if (pg.getData().size() == 1) {
                // can happen if the goal value was changed: old goal value was
                // reached but now there are some steps missing for the new goal
                pg.addPieSlice(sliceGoal);
            }
            sliceGoal.setValue(goal - steps_today);
        } else {
            // goal reached
            pg.clearChart();
            pg.addPieSlice(sliceCurrent);
        }
        pg.update();
        if (showSteps) {
            stepsView.setText(formatter.format(steps_today));
            totalView.setText(formatter.format(total_start + steps_today));
            if(total_days == 1) {
                averageView.setText(formatter.format(0));
            }
            else {
                averageView.setText(formatter.format((total_start) / (total_days - 1) ));
            }

        } else {
            // update only every 10 steps when displaying distance
            SharedPreferences prefs =
                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            float stepsize = prefs.getFloat("stepsize_value", Fragment_Settings.DEFAULT_STEP_SIZE);
            float distance_today = steps_today * stepsize;
            float distance_total = (total_start + steps_today) * stepsize;
            if (prefs.getString("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT)
                    .equals("cm")) {
                distance_today /= 100000;
                distance_total /= 100000;
            } else {
                distance_today /= 5280;
                distance_total /= 5280;
            }
            stepsView.setText(formatter.format(distance_today));
            totalView.setText(formatter.format(distance_total));
            if(total_days == 1) {
                averageView.setText(formatter.format(0));
            }
            else {
                averageView.setText(formatter.format((total_start) / (total_days - 1) ));
            }

        }
    }

    /**
     * Updates the bar graph to show the steps/distance of the last week. Should
     * be called when switching from step count to distance.
     */
    private void updateBars() {
        if (!monthlyAverage) {
            SimpleDateFormat df = new SimpleDateFormat("E", Locale.getDefault());
            BarChart barChart = getView().findViewById(R.id.bargraph);
            if (barChart.getData().size() > 0) barChart.clearChart();
            int steps;
            float distance, stepsize = Fragment_Settings.DEFAULT_STEP_SIZE;
            boolean stepsize_cm = true;
            if (!showSteps) {
                // load some more settings if distance is needed
                SharedPreferences prefs =
                        getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
                stepsize = prefs.getFloat("stepsize_value", Fragment_Settings.DEFAULT_STEP_SIZE);
                stepsize_cm = prefs.getString("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT)
                        .equals("cm");
            }
            barChart.setShowDecimal(!showSteps); // show decimal in distance view only
            BarModel bm;
            Database db = Database.getInstance(getActivity());
            List<Pair<Long, Integer>> last = db.getLastEntries(8);
            db.close();
            for (int i = last.size() - 1; i > 0; i--) {
                Pair<Long, Integer> current = last.get(i);
                steps = current.second;
                if (steps > 0) {
                    bm = new BarModel(df.format(new Date(current.first)), 0,
                            steps > goal ? Color.parseColor("#99CC00") : Color.parseColor("#0099cc"));
                    if (showSteps) {
                        bm.setValue(steps);
                    } else {
                        distance = steps * stepsize;
                        if (stepsize_cm) {
                            distance /= 100000;
                        } else {
                            distance /= 5280;
                        }
                        distance = Math.round(distance * 1000) / 1000f; // 3 decimals
                        bm.setValue(distance);
                    }
                    barChart.addBar(bm);
                }
            }
            db.close();
            if (barChart.getData().size() > 0) {
                barChart.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        Dialog_Statistics.getDialog(getActivity(), since_boot).show();
                    }
                });
                barChart.startAnimation();
            } else {
                barChart.setVisibility(View.GONE);
            }
        } else {
            MonthlyAverage mA = new MonthlyAverage();
            BarChart barChart = getView().findViewById(R.id.bargraph);
            if (barChart.getData().size() > 0) barChart.clearChart();
            int steps;
            float distance, stepsize = Fragment_Settings.DEFAULT_STEP_SIZE;
            boolean stepsize_cm = true;

            int totalEntries = mA.getCurrentDay();
            Database db = Database.getInstance(getActivity());
            List<Pair<Long, Integer>> last;

            int monthStepSum = 0;
            int monthAvg;
            int previousSums;

            if (!showSteps) {
                // load some more settings if distance is needed
                SharedPreferences prefs =
                        getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
                stepsize = prefs.getFloat("stepsize_value", Fragment_Settings.DEFAULT_STEP_SIZE);
                stepsize_cm = prefs.getString("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT)
                        .equals("cm");
            }
            barChart.setShowDecimal(!showSteps); // show decimal in distance view only

            last = db.getLastEntries(mA.getCurrentDay() + 1);
            if (last.size() > 0) {
                last.remove(0);

                monthStepSum = mA.stepSum(last);

                Logger.log("monthStepSum: " + monthStepSum);

                Logger.log("Entries_per_month: " + mA.getCurrentDay() );

                monthAvg = mA.calculateAvg(monthStepSum, mA.getCurrentDay() );

                mA.addBar(barChart, mA.getDate(), monthAvg);
            }

            mA.calculateEntries();

            for (int i = 0; i < 6; i++) {
                previousSums = mA.getCurrentDay();

                totalEntries += mA.getOneEntry(i);

                Logger.log("i: " + i);

                //Logger.log("totalEntries: " + totalEntries);

                last = db.getLastEntries(totalEntries + 1);

                //Logger.log("i: "+i+" Last array list: " + Arrays.toString(last.toArray()));

                if (last.size() > 0) {
                    last.remove(0);

                    for (int j = 0; j < i; j++) {
                        previousSums += mA.getOneEntry(j);
                    }

                    mA.removeInitialEntries(previousSums,last);

                    //Logger.log("i: "+i+" Modified Last array list: " + Arrays.toString(last.toArray()));

                    monthStepSum = mA.stepSum(last);

                    Logger.log("monthStepSum: " + monthStepSum);

                    Logger.log("Entries_per_month: " + mA.getOneEntry(i));

                    monthAvg = mA.calculateAvg(monthStepSum, mA.getOneEntry(i));

                    mA.addBar(barChart, mA.getDate().minusMonths(i + 1), monthAvg);
                }
            }
            db.close();
            if (barChart.getData().size() > 0) {
                barChart.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        Dialog_Statistics.getDialog(getActivity(), since_boot).show();
                    }
                });
                barChart.startAnimation();
            } else {
                barChart.setVisibility(View.GONE);
            }
        }
    }
}
