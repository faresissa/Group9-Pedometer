import android.support.v4.util.Pair;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.j4velin.pedometer.MonthlyAverage;

import static org.junit.Assert.*;

public class MonthlyAverageTest {
    private MonthlyAverage testObj;
    ArrayList<Pair<Long, Integer>> testList = new ArrayList<Pair<Long, Integer>>(){{
        add(new Pair<>(0L, 0));
        add(new Pair<>(0L, 1));
        add(new Pair<>(0L, 4));
        add(new Pair<>(0L, 9));
        add(new Pair<>(0L, 16));
        add(new Pair<>(0L, 25));
    }};//Sample list of <Long,Integer> pairs
    private Map<Integer, Integer> days_in_month = new HashMap<Integer,Integer>(){{
        put(0,31); //JAN
        put(1,28); //FEB
        put(2,31); //MAR
        put(3,30); //APR
        put(4,31); //MAY
        put(5,30); //JUN
        put(6,31); //JUL
        put(7,31); //AUG
        put(8,30); //SEP
        put(9,31); //OCT
        put(10,30); //NOV
        put(11,31); //DEC
    }}; //Zero based <month,day> map

    @Before
    public void setUp() {
        testObj = new MonthlyAverage();
    }

    @After
    public void tearDown() {
        testObj = null;
    }

    @Test
    public void testGetters() {
        LocalDate date =  LocalDate.now();
        //December 03 2019
        assertTrue(testObj.getCurrentYear() == date.getYear());
        assertTrue(testObj.getCurrentMonth() == date.getMonthValue() - 1);
        assertTrue(testObj.getCurrentDay() == date.getDayOfMonth());
        assertTrue(testObj.getDate().equals(date));
        for(int i = 0; i < 7; i++) {
            assertTrue(testObj.getOneEntry(i) == 0);
        }
    }

    @Test
    public void testSetter() {
        int currentMonth = testObj.getCurrentMonth();
        if(currentMonth != 0) {
            testObj.setCurrentMonth(currentMonth - 1);
        }
        else {
            testObj.setCurrentMonth(currentMonth + 1);
        }
        assertFalse(currentMonth == testObj.getCurrentMonth());
    }

    @Test
    public void testCalculateEntries() {
        //normal case
        int currentMonth = testObj.getCurrentMonth();
        testObj.calculateEntries();

        for(int i = 0; i < 7; i++) {
            assertTrue(testObj.getOneEntry(i) == days_in_month.get(currentMonth - i - 1));
        }

        //current year to last year case
        testObj.setCurrentMonth(2);
        currentMonth = testObj.getCurrentMonth();
        testObj.calculateEntries();

        for(int i = 0; i < 7; i++) {
            if(i == 2) {
                currentMonth += 12;
            }
            assertTrue(testObj.getOneEntry(i) == days_in_month.get(currentMonth - i - 1));
        }
    }

    @Test
    public void testRemoveInitialEntries() {
        ArrayList<Pair<Long, Integer>> list = new ArrayList<Pair<Long, Integer>>();

        for(int i = 0; i < 100; i++) {
            list.add(new Pair<>(0L,i+1));
        }

        testObj.removeInitialEntries(20,list);

        assertTrue(list.size() == 80);

        for(int i = 0; i < 80; i++) {
            assertTrue(list.get(i).second == i + 21);
        }
    }

    @Test
    public void testStepSum(){
        int result;
        ArrayList<Pair<Long, Integer>> list = new ArrayList<Pair<Long, Integer>>();

        for(int i = 0; i < 100; i++) {
            list.add(new Pair<>(0L,i+1));
        }

        result = testObj.stepSum(testList);
        assertTrue(result == 55);

        result = testObj.stepSum(list);
        assertTrue(result == 5050);

    }

    @Test
    public void testCalculateAverage() {
        int result = 0;
        result = testObj.calculateAvg(1000, 2);
        assertTrue(result == 500);
    }
}