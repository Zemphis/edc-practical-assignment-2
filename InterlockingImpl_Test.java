import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

public class InterlockingImpl_Test {
    private Interlocking il;

    @Before
    public void setUp() {
        il = new InterlockingImpl();
    }

    @Test
    public void testAdd_southbound_passenger_line1_to_8() {
        il.addTrain("P1", 1, 8);
        assertEquals(1, il.getTrain("P1"));
        assertEquals("P1", il.getSection(1));
    }

    @Test
    public void testAdd_southbound_passenger_line1_to_9() {
        il.addTrain("P1", 1, 9);
        assertEquals(1, il.getTrain("P1"));
    }

    @Test
    public void testAdd_southbound_passenger_line1_to_siding() {
        il.addTrain("P1", 1, 4);
        assertEquals(1, il.getTrain("P1"));
    }

    @Test
    public void testAdd_southbound_freight_3_to_11() {
        il.addTrain("F1", 3, 11);
        assertEquals(3, il.getTrain("F1"));
        assertEquals("F1", il.getSection(3));
    }

    @Test
    public void testAdd_northbound_from_siding_to_2() {
        il.addTrain("N1", 4, 2);
        assertEquals(4, il.getTrain("N1"));
    }

    @Test
    public void testAdd_northbound_from_9_to_2() {
        il.addTrain("N1", 9, 2);
        assertEquals(9, il.getTrain("N1"));
    }

    @Test
    public void testAdd_northbound_from_10_to_2() {
        il.addTrain("N1", 10, 2);
        assertEquals(10, il.getTrain("N1"));
    }

    @Test
    public void testAdd_northbound_freight_11_to_3() {
        il.addTrain("F1", 11, 3);
        assertEquals(11, il.getTrain("F1"));
    }



    @Test(expected = IllegalArgumentException.class)
    public void testAdd_duplicateName() {
        il.addTrain("X", 1, 8);
        il.addTrain("X", 3, 11);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdd_invalidEntrySection() {
        il.addTrain("X", 99, 8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdd_invalidDestSection() {
        il.addTrain("X", 1, 99);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdd_sectionNotAnEntry() {
        // Section 5 is an internal section — not a valid entry
        il.addTrain("X", 5, 8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdd_wrongExit_southboundCannotExitAt2() {
        // 2 is a northbound exit, not southbound
        il.addTrain("X", 1, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdd_wrongExit_northboundCannotExitAt8() {
        il.addTrain("X", 9, 8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdd_noPath_line1_to_11() {
        // passenger line cannot reach freight exit 11
        il.addTrain("X", 1, 11);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdd_noPath_freightTo_passExits() {
        il.addTrain("X", 3, 8);
    }


    @Test(expected = IllegalStateException.class)
    public void testAdd_entryOccupied() {
        il.addTrain("A", 1, 8);
        il.addTrain("B", 1, 9);
    }


    @Test
    public void testMove_oneStep_SB_line1() {
        il.addTrain("P", 1, 8);
        int moved = il.moveTrains(new String[]{"P"});
        assertEquals(1, moved);
        assertEquals(5, il.getTrain("P"));
        assertNull(il.getSection(1));
        assertEquals("P", il.getSection(5));
    }

    @Test
    public void testMove_fullJourney_SB_1_to_8() {
        il.addTrain("P", 1, 8);
        il.moveTrains(new String[]{"P"});
        assertEquals(5, il.getTrain("P"));
        il.moveTrains(new String[]{"P"});
        assertEquals(8, il.getTrain("P"));
        il.moveTrains(new String[]{"P"});
        assertEquals(-1, il.getTrain("P"));
        assertNull(il.getSection(8));
    }

    @Test
    public void testMove_fullJourney_SB_freight_3_to_11() {
        il.addTrain("F", 3, 11);
        il.moveTrains(new String[]{"F"});
        assertEquals(7, il.getTrain("F"));
        il.moveTrains(new String[]{"F"});
        assertEquals(11, il.getTrain("F"));
        il.moveTrains(new String[]{"F"});
        assertEquals(-1, il.getTrain("F"));
    }

    @Test
    public void testMove_fullJourney_SB_1_to_siding_4() {
        il.addTrain("P", 1, 4);
        il.moveTrains(new String[]{"P"});
        assertEquals(5, il.getTrain("P"));
        il.moveTrains(new String[]{"P"});
        assertEquals(4, il.getTrain("P"));
        il.moveTrains(new String[]{"P"});
        assertEquals(-1, il.getTrain("P"));
    }

    @Test
    public void testMove_fullJourney_NB_siding_to_2() {
        il.addTrain("N", 4, 2);
        il.moveTrains(new String[]{"N"});
        assertEquals(5, il.getTrain("N"));
        il.moveTrains(new String[]{"N"});
        assertEquals(2, il.getTrain("N"));
        il.moveTrains(new String[]{"N"});
        assertEquals(-1, il.getTrain("N"));
    }

    @Test
    public void testMove_fullJourney_NB_freight_11_to_3() {
        il.addTrain("F", 11, 3);
        il.moveTrains(new String[]{"F"});
        assertEquals(7, il.getTrain("F"));
        il.moveTrains(new String[]{"F"});
        assertEquals(3, il.getTrain("F"));
        il.moveTrains(new String[]{"F"});
        assertEquals(-1, il.getTrain("F"));
    }

    @Test
    public void testMove_fullJourney_NB_10_to_2() {
        il.addTrain("N", 10, 2);
        il.moveTrains(new String[]{"N"});
        assertEquals(6, il.getTrain("N"));
        il.moveTrains(new String[]{"N"});
        assertEquals(2, il.getTrain("N"));
        il.moveTrains(new String[]{"N"});
        assertEquals(-1, il.getTrain("N"));
    }


    @Test
    public void testMove_blocked_nextSectionOccupied() {
        il.addTrain("A", 1, 8);
        il.addTrain("B", 5, 8);
        int moved = il.moveTrains(new String[]{"A"});
        assertEquals(0, moved);
        assertEquals(1, il.getTrain("A"));
    }

    @Test
    public void testMove_blocked_returnsZero() {
        il.addTrain("A", 5, 8);
        il.addTrain("B", 1, 8);
        il.moveTrains(new String[]{"A"});
        // Now A is at destination 8. Move B: B wants 5 (now free)
        int moved = il.moveTrains(new String[]{"B"});
        assertEquals(1, moved);
        assertEquals(5, il.getTrain("B"));
    }

    @Test
    public void testMove_twoPassengerLines_parallel() {
        il.addTrain("P1", 1, 8);
        il.addTrain("P2", 9, 2);
        int moved = il.moveTrains(new String[]{"P1", "P2"});
        assertTrue("At least one train must move", moved >= 1);
    }

    @Test
    public void testMove_freightAndPassenger_independent() {
        il.addTrain("F", 3, 11);
        il.addTrain("P", 1, 8);
        int moved = il.moveTrains(new String[]{"F", "P"});
        assertEquals(2, moved);
        assertEquals(7, il.getTrain("F"));
        assertEquals(5, il.getTrain("P"));
    }

    @Test
    public void testMove_threeTrains_differentLines() {
        il.addTrain("P1", 1, 8);
        il.addTrain("P2", 9, 2);
        il.addTrain("F",  3, 11);
        int moved = il.moveTrains(new String[]{"P1", "P2", "F"});
        assertTrue(moved >= 2);
        assertEquals(7, il.getTrain("F"));
    }

    @Test
    public void testMove_conflictTwoTrainsWantSameSection() {
        il.addTrain("P1", 1, 8);
        il.addTrain("P2", 8, 4);
        int moved = il.moveTrains(new String[]{"P1", "P2"});
        assertEquals(1, moved);
        assertEquals(5, il.getTrain("P1"));
        assertEquals(8, il.getTrain("P2")); // P2 blocked
    }

    @Test
    public void testMove_headOnBlocked() {
        il.addTrain("P1", 5, 4);
        il = new InterlockingImpl();

        il.addTrain("A", 1, 8);
        il.addTrain("B", 8, 4);

        il.moveTrains(new String[]{"A"});
        int moved = il.moveTrains(new String[]{"A", "B"});
        assertEquals("Head-on: neither should move", 0, moved);
        assertEquals(5, il.getTrain("A"));
        assertEquals(8, il.getTrain("B"));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testMove_unknownTrain() {
        il.moveTrains(new String[]{"GHOST"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMove_exitedTrain() {
        il.addTrain("P", 1, 8);
        il.moveTrains(new String[]{"P"});  // →5
        il.moveTrains(new String[]{"P"});  // →8
        il.moveTrains(new String[]{"P"});  // exits
        il.moveTrains(new String[]{"P"});  // must throw
    }


    @Test
    public void testGetSection_empty() {
        for (int s : new int[]{1,2,3,4,5,6,7,8,9,10,11})
            assertNull(il.getSection(s));
    }

    @Test
    public void testGetSection_occupied() {
        il.addTrain("P", 1, 8);
        assertEquals("P", il.getSection(1));
    }

    @Test
    public void testGetSection_afterExit() {
        il.addTrain("P", 1, 8);
        il.moveTrains(new String[]{"P"});
        il.moveTrains(new String[]{"P"});
        il.moveTrains(new String[]{"P"});
        assertNull(il.getSection(8));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSection_invalid() {
        il.getSection(12);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSection_zero() {
        il.getSection(0);
    }



    @Test
    public void testGetTrain_exited() {
        il.addTrain("F", 3, 11);
        il.moveTrains(new String[]{"F"});
        il.moveTrains(new String[]{"F"});
        il.moveTrains(new String[]{"F"});
        assertEquals(-1, il.getTrain("F"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetTrain_unknown() {
        il.getTrain("NOBODY");
    }



    @Test(expected = IllegalArgumentException.class)
    public void testFreightSegregation_passengerCannotEnterFreightPath() {
        il.addTrain("P", 1, 11);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFreightSegregation_freightCannotEnterPassengerPath() {
        il.addTrain("F", 3, 8);
    }

    @Test
    public void testFreightSegregation_freightStaysOnFreightLine() {
        il.addTrain("F", 3, 11);
        il.moveTrains(new String[]{"F"});
        assertEquals(7, il.getTrain("F"));
    }


    @Test
    public void testSectionFreed_newTrainCanUseIt() {
        il.addTrain("A", 1, 8);
        il.moveTrains(new String[]{"A"});
        il.moveTrains(new String[]{"A"});
        il.moveTrains(new String[]{"A"});

        il.addTrain("B", 8, 2);
        assertEquals(8, il.getTrain("B"));
    }



    @Test
    public void testMove_duplicateNamesCountedOnce() {
        il.addTrain("P", 1, 8);
        int moved = il.moveTrains(new String[]{"P", "P", "P"});
        assertEquals(1, moved);
        assertEquals(5, il.getTrain("P"));
    }



    @Test
    public void testEndToEnd_freightAndTwoPassengers() {
        il.addTrain("F",  3, 11);
        il.addTrain("P1", 1, 8);
        il.addTrain("P2", 9, 2);

        il.moveTrains(new String[]{"F", "P1", "P2"});
        assertEquals(7, il.getTrain("F"));

        il.moveTrains(new String[]{"F", "P1", "P2"});
        assertEquals(11, il.getTrain("F"));

        il.moveTrains(new String[]{"F", "P1", "P2"});
        assertEquals(-1, il.getTrain("F"));
    }

    @Test
    public void testEndToEnd_northbound_9_to_2_fullPath() {
        il.addTrain("N", 9, 2);
        il.moveTrains(new String[]{"N"});
        int sec = il.getTrain("N");
        assertTrue("N should be at 5 or 6", sec == 5 || sec == 6);
        il.moveTrains(new String[]{"N"});
        assertEquals(2, il.getTrain("N"));
        il.moveTrains(new String[]{"N"});
        assertEquals(-1, il.getTrain("N"));
    }
}