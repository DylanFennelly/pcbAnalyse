package pcb.pcb;


import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class pcbTest {
    private int[] pixelSet, pixelSet2;
    private double width;
    private PCBAnalyseController pcbAnalyseController;
    private ArrayList<Integer> roots;
    private HashMap<Integer, String> existingComponentTypes;

    @BeforeEach
    void setUp() {
        //array representation of testBlackWhite.png
        pixelSet = new int[16];
        for (int i = 0; i < 16; i++) {
            pixelSet[i] = i;
        }
        pixelSet[1] = -1;
        pixelSet[5] = -1;
        pixelSet[6] = -1;
        pixelSet[8] = -1;
        pixelSet[11] = -1;
        pixelSet[12] = -1;

        pixelSet2 = new int[16];
        for (int i = 0; i < 16; i++) {
            pixelSet2[i] = -1;
        }

        width = 4;

        roots = new ArrayList<>();
        existingComponentTypes = new HashMap<>();

        pcbAnalyseController = new PCBAnalyseController();
    }

    @AfterEach
    void tearDown() {
        pixelSet = null;
        width = 0;
        pcbAnalyseController = null;
    }

    @Test
    void unionFindTest() {
        //testing downward union with -1 sideways
        PCBAnalyseController.union(0, pixelSet, width);
        assertEquals(0, PCBAnalyseController.find(pixelSet, 4));
        assertEquals(-1, PCBAnalyseController.find(pixelSet, 1));

        //testing sideways union with -1 downwards
        PCBAnalyseController.union(2, pixelSet, width);
        assertEquals(2, PCBAnalyseController.find(pixelSet, 3));
        assertEquals(-1, PCBAnalyseController.find(pixelSet, 6));

        //testing right-left boundary union    |   root of 3 is 2 because of above
        PCBAnalyseController.union(3, pixelSet, width);
        assertEquals(0, PCBAnalyseController.find(pixelSet, 4));
        assertEquals(2, PCBAnalyseController.find(pixelSet, 7));

        //testing down and left boundary union
        PCBAnalyseController.union(9, pixelSet, width);
        assertEquals(9, PCBAnalyseController.find(pixelSet, 10));
        assertEquals(9, PCBAnalyseController.find(pixelSet, 13));

        //testing last pixel
        PCBAnalyseController.union(15, pixelSet, width);
    }

    @Test
    void identifyRootsTest() {
        //non-unioned pixel set
        pcbAnalyseController.identifyRoots(pixelSet, roots);
        assertEquals(10, roots.size());

        roots.clear();

        //unioned pixel set
        for (int i = 0; i < pixelSet.length; i++) {
            if (pixelSet[i] != -1)  //checking pixel is not white (-1)
                PCBAnalyseController.union(i, pixelSet, width);
        }
        pcbAnalyseController.identifyRoots(pixelSet, roots);
        assertEquals(3, roots.size());

        roots.clear();

        //blacked out pixel set
        pcbAnalyseController.identifyRoots(pixelSet2, roots);
        assertEquals(0, roots.size());
    }

    @Test
    void cleanUpExistingComponentsHashMapTest() {
        //non-unioned pixel set
        pcbAnalyseController.identifyRoots(pixelSet, roots);
        assertEquals(10, roots.size());
        for (int i = 0; i < roots.size(); i++) {
            existingComponentTypes.put(i, "Test");
        }
        assertEquals(10, existingComponentTypes.size());

        //union the pixel set
        for (int i = 0; i < pixelSet.length; i++) {
            if (pixelSet[i] != -1)  //checking pixel is not white (-1)
                PCBAnalyseController.union(i, pixelSet, width);
        }
        roots.clear();
        pcbAnalyseController.identifyRoots(pixelSet, roots);
        pcbAnalyseController.cleanUpExistingComponentsHashMap(existingComponentTypes, roots);
        assertEquals(3, existingComponentTypes.size());
    }

    //extracted version of colourRangeTrials method with only hue check and pixelReader removed, as I could not get JavaFX tests working
    private boolean HueRangeTrailsAlt(Color color, int hueTolerance, boolean rollUnder, boolean rollOver, double increasedHue, double reducedHue) {
        boolean hueRange;
        if (color.getHue() >= (360 - hueTolerance)) {    //for hue values on the upper border
            if (rollUnder) {     //if reduced hue value rolled under to 350's
                hueRange = (color.getHue() >= reducedHue); //check only if hue is greater than rolled-under reduced value.
            } else if (rollOver) {
                hueRange = (color.getHue() <= 360);
            } else {      //if no roll under occurred
                hueRange = (color.getHue() >= reducedHue) && (color.getHue() <= increasedHue);    //check as normal
            }
        } else if (color.getHue() <= hueTolerance) { //for hue values on the lower border
            if (rollOver) { //if increase hue valued rolled over to single digits
                hueRange = (color.getHue() <= increasedHue); //check only if hue is less than rolled-over increased value.
            } else if (rollUnder) {
                hueRange = (color.getHue() >= 0);
            } else {      //if no roll over occurred
                hueRange = (color.getHue() >= reducedHue) && (color.getHue() <= increasedHue);    //check as normal
            }
        } else { //for values not on either border (the nice values)
            hueRange = (color.getHue() >= reducedHue) && (color.getHue() <= increasedHue);
        }
        return (hueRange);
    }

    @Test
    void colourRangeTrialsTest() {
        //GREEN Hue = 120 (Normal); RED hue = 0 (low hue); CRIMSON = 348 (high hue); BLUE = 240

        //Normal colour, inside valid range
        assertTrue(HueRangeTrailsAlt(Color.GREEN, 20, false, false, 140, 100));
        //Normal colour, outside valid range
        assertFalse(HueRangeTrailsAlt(Color.GREEN, 20, false, false, 200, 160));

        //High hue, roll under
        assertTrue(HueRangeTrailsAlt(Color.CRIMSON, 40, true, false, 20, 300));
        //High hue, roll over
        assertTrue(HueRangeTrailsAlt(Color.CRIMSON, 40, false, true, 0, 320));

        //Low hue, roll under
        assertTrue(HueRangeTrailsAlt(Color.RED, 40, true, false, 60, 340));
        //Low hue, roll over
        assertTrue(HueRangeTrailsAlt(Color.RED, 60, false, true, 20, 280));

        //outside valid range, roll under
        assertFalse(HueRangeTrailsAlt(Color.BLUE, 40, true, false, 60, 340));
        //outside valid range, roll over
        assertFalse(HueRangeTrailsAlt(Color.BLUE, 60, false, true, 20, 280));
    }
}