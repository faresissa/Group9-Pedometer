import android.util.Pair;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.j4velin.pedometer.MonthlyAverage;

import static org.junit.Assert.*;

public class MonthlyAverageTest {
    private MonthlyAverage testObj;
    private List<Pair<Long, Integer>> listOne;
    private List<Pair<Long, Integer>> listTwo;

    @Before
    public void setUp() {
        testObj = new MonthlyAverage();
        listOne = new ArrayList<>();
        listTwo = new ArrayList<>();
    }

    @After
    public void tearDown() {
        testObj = null;
        listOne = null;
        listTwo = null;
    }

    @Test
    public void testGetters() {
        //December 2019, modify as needed
        assertTrue(testObj.getCurrentYear() == 2019);
        assertTrue(testObj.getCurrentMonth() == 11);
        for(int i = 0; i < 7; i++) {
            assertTrue(testObj.getOneEntry(i) == 0);
        }
    }

    @Test
    public void testCalculateEntries() {

        for(int i = 0; i < 6; i++) {
            System.out.println(testObj.getOneEntry(i));
        }
    }

    @Test
    public void testStepSum(){
        int result;

        for(int i = 0; i < 100; i++) {
            listOne.add(new Pair(new Long(i), new Integer(i)));
            listTwo.add(new Pair(0L, i));
        }


        result = testObj.stepSum(listOne);
        assertTrue(result == 5050);

        result = testObj.stepSum(listTwo);
        assertFalse(result == 5050);
        assertTrue(result == 0);

    }

    @Test
    public void testCalculateAverage() {
        int result = 0;
        MonthlyAverage testObj = new MonthlyAverage();
        result = testObj.calculateAvg(1000, 2);
        assertTrue(result == 500);
    }
}