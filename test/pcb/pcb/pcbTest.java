package pcb.pcb;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class pcbTest {
    private int[] pixelSet, pixelSet2;
    private double width;
    private StartController startController;
    private ArrayList<Integer> roots;
    private HashMap<Integer, String> existingComponentTypes;

    @BeforeEach
    void setUp() {
        //array representation of testBlackWhite.png
        pixelSet = new int[16];
        for (int i=0; i < 16; i++){
            pixelSet[i] = i;
        }
        pixelSet[1] = -1;
        pixelSet[5] = -1;
        pixelSet[6] = -1;
        pixelSet[8] = -1;
        pixelSet[11] = -1;
        pixelSet[12] = -1;

        pixelSet2 = new int[16];
        for (int i=0; i < 16; i++){
            pixelSet2[i] = -1;
        }

        width=4;

        roots = new ArrayList<>();
        existingComponentTypes = new HashMap<>();

        startController = new StartController();
    }

    @AfterEach
    void tearDown() {
        pixelSet = null;
        width = 0;
        startController = null;
    }

    @Test
    void unionFindTest(){
        //testing downward union with -1 sideways
        StartController.union(0,pixelSet,width);
        assertEquals(0,StartController.find(pixelSet,4));
        assertEquals(-1,StartController.find(pixelSet,1));

        //testing sideways union with -1 downwards
        StartController.union(2,pixelSet,width);
        assertEquals(2,StartController.find(pixelSet,3));
        assertEquals(-1,StartController.find(pixelSet,6));

        //testing right-left boundary union    |   root of 3 is 2 because of above
        StartController.union(3,pixelSet,width);
        assertEquals(0,StartController.find(pixelSet,4));
        assertEquals(2,StartController.find(pixelSet,7));

        //testing down and left boundary union
        StartController.union(9,pixelSet,width);
        assertEquals(9,StartController.find(pixelSet,10));
        assertEquals(9,StartController.find(pixelSet,13));

        //testing last pixel
        StartController.union(15,pixelSet,width);
    }

    @Test
    void identifyRootsTest(){
        //non-unioned pixel set
        startController.identifyRoots(pixelSet,roots);
        assertEquals(10,roots.size());

        roots.clear();

        //unioned pixel set
        for (int i=0; i < pixelSet.length; i++){
            if (pixelSet[i] != -1)  //checking pixel is not white (-1)
                StartController.union(i,pixelSet,width);
        }
        startController.identifyRoots(pixelSet,roots);
        assertEquals(3,roots.size());

        roots.clear();

        //blacked out pixel set
        startController.identifyRoots(pixelSet2,roots);
        assertEquals(0, roots.size());
    }

    @Test
    void cleanUpExistingComponentsHashMapTest(){
        //non-unioned pixel set
        startController.identifyRoots(pixelSet,roots);
        assertEquals(10,roots.size());
        for (int i=0; i < roots.size(); i++){
            existingComponentTypes.put(i,"Test");
        }
        assertEquals(10,existingComponentTypes.size());

        //union the pixel set
        for (int i=0; i < pixelSet.length; i++){
            if (pixelSet[i] != -1)  //checking pixel is not white (-1)
                StartController.union(i,pixelSet,width);
        }
        roots.clear();
        startController.identifyRoots(pixelSet,roots);
        startController.cleanUpExistingComponentsHashMap(existingComponentTypes,roots);
        assertEquals(3,existingComponentTypes.size());
    }

    @Test

}