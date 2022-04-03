package pcb.pcb;

import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class pcbTest {

    private Pane pane = new Pane();
    private int[] pixelSet, pixelSet2;
    private double width;
    private Image blackWhite;
    private StartController startController;

    @BeforeEach
    void setUp() {
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

        width=4;

        blackWhite = new Image("pcb/pcb/testBlackWhite.png");
        pixelSet2 = new int[(int) (blackWhite.getHeight() * blackWhite.getWidth())];
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
    void processImgToDisjointTest(){
            startController.processImgToDisjoint(blackWhite,pixelSet2);
     }

}