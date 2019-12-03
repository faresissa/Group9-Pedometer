package de.j4velin.pedometer;

import android.graphics.Color;
import android.support.v4.util.Pair;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.models.BarModel;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;

public class MonthlyAverage {

    private LocalDate date = LocalDate.now();
    private int currentMonth = date.getMonthValue() - 1;
    private int currentDay = date.getDayOfMonth();
    private int currentYear = date.getYear();

    private int[] Entries_per_month = new int[7];

    public LocalDate getDate() {
        return this.date;
    }

    public int getCurrentMonth() {
        return this.currentMonth;
    }

    public int getCurrentDay() {
        return this.currentDay;
    }

    public int getCurrentYear() {
        return this.currentYear;
    }

    public int getOneEntry(int i) {
        return Entries_per_month[i];
    }

    public void calculateEntries() {
        int tempMonth;
        int tempYear;
        int temp;
        Calendar cal = Calendar.getInstance();

        tempYear = getCurrentYear();

        for(int i = 0; i < 7; i++) {
            tempMonth = getCurrentMonth() - i - 1;
            if (tempMonth < 0) {
                tempMonth += 12;
                tempYear -= 1;
            }

            cal.set(tempYear, tempMonth, 1);
            temp = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            //System.out.println("Entry value is "+temp);
            Entries_per_month[i] = temp;
        }
    }

    public int stepSum(List<Pair<Long, Integer>> list) {
        Pair<Long, Integer> tempPair;
        int monthStepSum = 0;
        for (int k = list.size() - 1; k > -1; k--) {
            tempPair = list.get(k);
            monthStepSum += tempPair.second;
        }
        return monthStepSum;
    }

    public int calculateAvg(int steps, int days) {
        return steps/days;
    }

    public void addBar(BarChart barChart, LocalDate label, int barValue) {
        BarModel bm;
        bm = new BarModel(label.getMonth().toString().substring(0, 3), 0, Color.parseColor("#0099cc"));
        bm.setValue(barValue);
        barChart.addBar(bm);
    }

}
