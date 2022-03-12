package pcb.pcb;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class StartController {
    private int[] pixelSet;
    private ArrayList<Integer> roots;
    private Image ogImg;
    @FXML
    private MenuBar menuBar;

    @FXML
    private ImageView ogImageView, newImageView;

    @FXML
    private void initialize(){
        ogImageView.setOnMouseClicked(e -> {
            PixelReader pr = ogImageView.getImage().getPixelReader();
            Color col = pr.getColor( (int)e.getX(), (int)e.getY());
            double hue = col.getHue();
            double sat = col.getSaturation();
            double bri = col.getBrightness();

            System.out.println("\nHue: " + hue + "\nSaturation: " + sat + "\nBrightness: " + bri);
            isolateSelectedColour(col, pr, hue, sat, bri);
        });
    }

    @FXML
    private void openImage(){
        FileChooser fileChooser = new FileChooser();    //https://docs.oracle.com/javafx/2/ui_controls/file-chooser.htm
        fileChooser.setTitle("Open Image");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home") + "\\Desktop"));   //setting initial directory to desktop
        FileChooser.ExtensionFilter imgFilter = new FileChooser.ExtensionFilter("Image files (*.png, *.jpg, *.jpeg)", "*.png","*.jpg","*.jpeg");    //extension filter: only image format files will be shown   //https://stackoverflow.com/questions/36960844/how-to-open-a-pdf-file-javafx
        fileChooser.getExtensionFilters().add(imgFilter);
        File imageFile = fileChooser.showOpenDialog(menuBar.getScene().getWindow()); //https://stackoverflow.com/questions/25491732/how-do-i-open-the-javafx-filechooser-from-a-controller-class

        System.out.println(imageFile);
        if (imageFile != null) {
            Image image = new Image(imageFile.toURI().toString(), ogImageView.getFitWidth(), ogImageView.getFitHeight(), true, true);
            ogImg = image;  //saving copy of original image;
            ogImageView.setImage(image);
        }
    }

    private void isolateSelectedColour(Color selectedCol,PixelReader pixelReader, double hue, double sat, double bri){
        boolean hueRange = false, satRange = false, briRange = false, rollOver = false, rollUnder = false;
        int width = (int) ogImageView.getImage().getWidth();
        int height = (int) ogImageView.getImage().getHeight();
        double reducedHue = hue - 30, increasedHue = hue + 30, reducedSat = sat - 0.25, increasedSat = sat + 0.25, reducedBri = bri - 0.25, increasedBri = bri + 0.25;
        //ensuring that the hue values correctly roll over
        if (reducedHue < 0) {
            reducedHue = 360 + (hue - 30);
            rollUnder = true;
        }

        if (increasedHue > 360) {
            increasedHue = (hue + 30) - 360;
            rollOver = true;
        }
        WritableImage blackWhite = new WritableImage(width, height);
        System.out.println("hue: " + hue + " | reduced hue: " + reducedHue + " | increased hue: " + increasedHue);

        for (int y=0; y<height; y++){
            for (int x=0; x<width; x++){
                if (pixelReader.getColor(x, y).getHue() >= 330) {    //for hue values on the upper border
                    if (rollUnder) {     //if reduced hue value rolled under to 350's
                        hueRange = (pixelReader.getColor(x, y).getHue() >= reducedHue); //check only if hue is greater than rolled-under reduced value.
                    }else if(rollOver){
                        hueRange = (pixelReader.getColor(x, y).getHue() <= 360);
                    } else {      //if no roll under occurred
                        hueRange = (pixelReader.getColor(x, y).getHue() >= reducedHue) && (pixelReader.getColor(x, y).getHue() <= increasedHue);    //check as normal
                    }
                }else if(pixelReader.getColor(x, y).getHue() <= 30){ //for hue values on the lower border
                    if (rollOver) { //if increase hue valued rolled over to single digits
                        hueRange = (pixelReader.getColor(x, y).getHue() <= increasedHue); //check only if hue is less than rolled-over increased value.
                    }else if(rollUnder){
                        hueRange = (pixelReader.getColor(x, y).getHue() >= 0);
                    }else {      //if no roll over occurred
                        hueRange = (pixelReader.getColor(x, y).getHue() >= reducedHue) && (pixelReader.getColor(x, y).getHue() <= increasedHue);    //check as normal
                    }
                }else { //for values not on either border (the nice values)
                    hueRange = (pixelReader.getColor(x, y).getHue() >= reducedHue) && (pixelReader.getColor(x, y).getHue() <= increasedHue);
                }
                satRange = ((pixelReader.getColor(x,y).getSaturation() >= reducedSat) && (pixelReader.getColor(x,y).getSaturation() <= increasedSat));
                briRange = ((pixelReader.getColor(x,y).getBrightness() >= reducedBri) && (pixelReader.getColor(x,y).getBrightness() <= increasedBri));

                if (hueRange && satRange && briRange){
                    //if within range, set to black
                    blackWhite.getPixelWriter().setColor(x,y,new Color(0,0,0,1));
                }else{
                    //else, set to white
                    blackWhite.getPixelWriter().setColor(x,y, new Color(1,1,1,1));
                }
            }
        }
        newImageView.setImage(blackWhite);
        //todo: testing, remove
        processImgToDisjoint(blackWhite, pixelSet);
    }

    private void processImgToDisjoint(WritableImage blackWhite, int[] pixelSet){    //processing the b&w image to a disjoint set
        int width = (int) blackWhite.getWidth();
        int height = (int) blackWhite.getHeight();

        //initialising array and array values
        pixelSet = new int[(width * height)];
        for (int i = 0;i < pixelSet.length; i++){
            pixelSet[i] = i;
        }
        //index used for iterating through array
        int arrayIndex = 0;

        //creating disjoint sets from array based on colour
        for (int y=0; y<height; y++) {
            for (int x = 0; x < width; x++) {
                if (blackWhite.getPixelReader().getArgb(x,y) == 0xffffffff) {
                    pixelSet[arrayIndex] = -1;
                }else {
                    union(arrayIndex,pixelSet,blackWhite.getWidth());
                }

                arrayIndex++;
            }
        }
//        for (int rootId=0;rootId< pixelSet.length;rootId++){
//            for (int elementID=0;elementID< pixelSet.length;elementID++){
//                if (find(pixelSet,elementID) == rootId){
//                    System.out.println("The root of " + elementID + " is " + find(pixelSet, elementID));
//                }
//            }
//        }

        System.out.println("Number of disjoint sets: " + numOfRoots(pixelSet));
        for (Integer i : roots){
            System.out.println("Set of root " + i + " has " + sizeOfSet(i, pixelSet) + " element(s)");
        }


    }

    //Iterative version of find
    public static int find(int[] a, int id) {
        if (a[id] != -1) {
            while (a[id] != id)
                id = a[id];
            return id;
        }else
            return -1;

    }

    public static void union(int index, int[] pixelSet, double width){
        //checking pixel to right
        if (!(index + 1 > pixelSet.length -1)) {
            if (pixelSet[index + 1] != -1) {
                pixelSet[find(pixelSet,index + 1)] = find(pixelSet,index);
            }
        }
        if (!(index + width > pixelSet.length -1)) {
            //checking pixel below
            if (pixelSet[(int) (index + width)] != -1) {
                // pixelSet[(int) (index + width)] = index;
                pixelSet[find(pixelSet, (int) (index + width))] = find(pixelSet,index);

            }
        }

    }
    private int numOfRoots(int[] pixelSet){
        roots = new ArrayList<>();   //creating flexible arrayList to keep track of roots
        for (int elementID=0;elementID< pixelSet.length;elementID++){   //navigating through each element (pixel) in the pixelSet array
            boolean matchingRoot = false;
            if (roots.size() == 0 && find(pixelSet,elementID) != -1){   //if no elements have been added yet and pixel is not equal to -1
                roots.add(find(pixelSet,elementID));    //add element as root and move on
            }else {     //else if roots set is not empty
                if (find(pixelSet,elementID) != -1){
                    for (Integer root : roots) {    //iterating through roots arrayList
                        if (find(pixelSet, elementID) == root) {    //if root is already recorded in roots arrayList
                            matchingRoot = true;
                            break;
                        }
                    }
                } else {    //else, if element is equal to -1
                    matchingRoot = true;
                }
                if (!matchingRoot){     //if root has yet to be recorded and element is not -1
                    roots.add(find(pixelSet,elementID));
                }
            }
        }
        return roots.size();
    }

    private int sizeOfSet(int root, int[] pixelSet){
            int noNodes = 0;
            for (int elementID=0;elementID< pixelSet.length;elementID++) {
                if (find(pixelSet, elementID) == root)
                    noNodes++;
            }
            return noNodes;
    }
}