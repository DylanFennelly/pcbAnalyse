package pcb.pcb;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class StartController {
    //todo: remove noise from blackWhite image
    //todo: array of only valid roots
    private int[] pixelSet;
    private ArrayList<Integer> roots, validRoots, totalRoots = new ArrayList<>();   //totalRoots: ArrayList of roots across multiple
    private Image ogImg;
    private final DecimalFormat df = new DecimalFormat("#.##");   //https://stackoverflow.com/questions/153724/how-to-round-a-number-to-n-decimal-places-in-java
    private int hueTolerance, minSetSize, icbCount, resistorCount, solderCount, miscCount;
    private double satTolerance, briTolerance;

    @FXML
    private MenuBar menuBar;

    @FXML
    private ImageView ogImageView, newImageView;

    @FXML
    private Pane ogImageViewPane;

    @FXML
    private Label satRangeLabel, briRangeLabel, totalComponentsLabel, totalICBsLabel, totalResistorsLabel, totalMiscLabel, totalSoldersLabel;

    @FXML
    private Slider satRangeSlider, briRangeSlider;

    @FXML
    private Spinner<Integer> hueRangeSpinner, minPixelSizeSpinner;

    @FXML
    private TextArea componentsTextArea;

    @FXML
    private void initialize(){
        df.setRoundingMode(RoundingMode.CEILING);
        //updating slider labels upon moving sliders    |   https://stackoverflow.com/questions/40593284/how-can-i-define-a-method-for-slider-change-in-controller-of-a-javafx-program
        briRangeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            String twodp = df.format( newValue.doubleValue());
            briRangeLabel.setText(twodp);
        });
        satRangeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            String twodp = df.format( newValue.doubleValue());
            satRangeLabel.setText(twodp);
        });

        ogImageView.setOnMouseClicked(e -> {
            PixelReader pr = ogImageView.getImage().getPixelReader();
            Color col = pr.getColor( (int)e.getX(), (int)e.getY());
            double hue = col.getHue();
            double sat = col.getSaturation();
            double bri = col.getBrightness();
            roots = new ArrayList<>(); validRoots = new ArrayList<>(); //creating flexible arrayList to keep track of roots and validRoots (roots over minimum set size limit)

            System.out.println("\nHue: " + hue + "\nSaturation: " + sat + "\nBrightness: " + bri);
            setRangeValues();
            isolateSelectedColour(col, pr, hue, sat, bri);
            processImgToDisjoint(newImageView.getImage(), pixelSet);
            identifyRoots(pixelSet, roots);
            identifyValidRoots(roots,validRoots,pixelSet,minSetSize);
            validRoots.sort((Integer root1, Integer root2) -> Integer.compare(sizeOfSet(root2, pixelSet), sizeOfSet(root1, pixelSet)));  //sorts validRoots ArrayList in descending order by size of set  |  IntelliJ improvements from https://stackoverflow.com/questions/16751540/sorting-an-object-arraylist-by-an-attribute-value-in-java#comment62928013_16751550
            identifyComponentType(hue,sat,bri,validRoots);
            drawRectangles(pixelSet, validRoots, newImageView.getImage(), totalRoots);
            totalRoots.addAll(validRoots);

            totalICBsLabel.setText("ICBs: " + icbCount);
            totalResistorsLabel.setText("Resistors: " + resistorCount);
            totalSoldersLabel.setText("Solder Points: " + solderCount);
            totalMiscLabel.setText("Misc: " + miscCount);
        });
    }

    private void setRangeValues(){  //run when apply button is pressed or when image is clicked
        hueTolerance = hueRangeSpinner.getValue();
        satTolerance = satRangeSlider.getValue();
        briTolerance = briRangeSlider.getValue();
        minSetSize = minPixelSizeSpinner.getValue();
    }

    @FXML
    private void resetRangeValues(){    //resets values back to defaults
        hueRangeSpinner.getValueFactory().setValue(20);
        satRangeSlider.setValue(0.32);
        satRangeLabel.setText("0.32");
        briRangeSlider.setValue(0.25);
        briRangeLabel.setText("0.25");
        minPixelSizeSpinner.getValueFactory().setValue(55);
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
            ogImageViewPane.setPrefWidth(image.getWidth()); ogImageViewPane.setPrefHeight(image.getHeight());   //resizes pane to image resolution (for drawing rectangle)
            ogImg = image;  //saving copy of original image;
            if (ogImageViewPane.getChildren().size() == 2)  //removing existing rectangles from image if they exist rectangle group is second child of ogImageViewPane)
                ogImageViewPane.getChildren().remove(1);
            pixelSet = new int[(int) (image.getWidth() * image.getHeight())];
            totalRoots.clear();     //empty total roots
            componentsTextArea.clear();
            removeRectangles();

            icbCount = 0; resistorCount = 0; miscCount = 0; solderCount = 0;
            totalComponentsLabel.setText("Total Components: 0");
            totalICBsLabel.setText("ICBs: 0");
            totalResistorsLabel.setText("Resistors: 0");
            totalSoldersLabel.setText("Solder Points: 0");
            totalMiscLabel.setText("Misc: 0");
            ogImageView.setImage(image);
        }
    }

    private void isolateSelectedColour(Color selectedCol,PixelReader pixelReader, double hue, double sat, double bri){
        //todo: modulo for overflow
        boolean hueRange = false, satRange = false, briRange = false, rollOver = false, rollUnder = false;
        int width = (int) ogImageView.getImage().getWidth();
        int height = (int) ogImageView.getImage().getHeight();
        double reducedHue = hue - hueTolerance, increasedHue = hue + hueTolerance, reducedSat = sat - satTolerance, increasedSat = sat + satTolerance, reducedBri = bri - briTolerance, increasedBri = bri + briTolerance;
        //ensuring that the hue values correctly roll over
        if (reducedHue < 0) {
            reducedHue = 360 + (hue - hueTolerance);
            rollUnder = true;
        }

        if (increasedHue > 360) {
            increasedHue = (hue + hueTolerance) - 360;
            rollOver = true;
        }
        WritableImage blackWhite = new WritableImage(width, height);
        System.out.println("hue: " + hue + " | reduced hue: " + reducedHue + " | increased hue: " + increasedHue);

        for (int y=0; y<height; y++){
            for (int x=0; x<width; x++){
                if (pixelReader.getColor(x, y).getHue() >= (360 - hueTolerance)) {    //for hue values on the upper border
                    if (rollUnder) {     //if reduced hue value rolled under to 350's
                        hueRange = (pixelReader.getColor(x, y).getHue() >= reducedHue); //check only if hue is greater than rolled-under reduced value.
                    }else if(rollOver){
                        hueRange = (pixelReader.getColor(x, y).getHue() <= 360);
                    } else {      //if no roll under occurred
                        hueRange = (pixelReader.getColor(x, y).getHue() >= reducedHue) && (pixelReader.getColor(x, y).getHue() <= increasedHue);    //check as normal
                    }
                }else if(pixelReader.getColor(x, y).getHue() <= hueTolerance){ //for hue values on the lower border
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
    }

    private void processImgToDisjoint(Image blackWhite, int[] pixelSet){    //processing the b&w image to a disjoint set
        int width = (int) blackWhite.getWidth();
        int height = (int) blackWhite.getHeight();

        //initialising array and array values

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
        //todo: fix issue of sets of right of screen connecting to sets on left of screen
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

    private void identifyRoots(int[] pixelSet, ArrayList<Integer> roots){
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
    }

    private void identifyValidRoots(ArrayList<Integer> roots, ArrayList<Integer> validRoots, int[] pixelSet, int minSetSize){
        for (Integer root : roots){
            if (sizeOfSet(root, pixelSet) >= minSetSize)
                validRoots.add(root);
        }
    }

    private int sizeOfSet(int root, int[] pixelSet){
            int noNodes = 0;
            for (int elementID=0;elementID< pixelSet.length;elementID++) {
                if (find(pixelSet, elementID) == root)
                    noNodes++;
            }
            return noNodes;
    }

    private void identifyComponentType(double hue, double sat, double bri, ArrayList<Integer> validRoots) {
        //we assume that all components detected in click are similar and of the same type
        //resistor check, samples taken from pcb images in CA information PDF
        if((hue >= 20 && hue <= 40) && (sat >= 0.25 && sat <= 0.75) && (bri >= 0.65 && bri <= 0.9)) {
            resistorCount += validRoots.size();
        //solder point check
        }else if((sat >= 0.02 && sat <= 0.1) && (bri >= 0.8 && bri <= 1) ){
            solderCount += validRoots.size();
        //icb check
        }else if ((sat >= 0.025 && sat <= 0.45) && (bri >=0.1 && bri <= 0.35)){
            icbCount += validRoots.size();
        //if component is not resistor, solder, or icb, mark as misc
        }else{
            miscCount += validRoots.size();
        }
        //todo: resistor
        System.out.println("No of ICBs: " + icbCount);
        System.out.println("No of Resistors: " + resistorCount);
        System.out.println("No of Solder Points: " + solderCount);
        System.out.println("No of Misc: " + miscCount);
    }

    private void drawRectangles(int[] pixelSet, ArrayList<Integer> validRoots, Image blackWhite, ArrayList<Integer> totalRoots){
        //https://stackoverflow.com/questions/43260526/how-to-add-a-group-to-the-scene-in-javafx
        //https://stackoverflow.com/questions/34160639/add-shapes-to-javafx-pane-with-cartesian-coordinates
        //https://stackoverflow.com/questions/40729967/drawing-shapes-on-javafx-canvas

        Group root = new Group();   //creating group of nodes to add to pane

        int width = (int) blackWhite.getWidth(), componentNo = totalRoots.size() + 1;   //increase for each component scanned

        for (Integer currentRoot : validRoots) {  //for each root in array
                boolean topLeft = false;    //for one time condition to obtain top left of each disjoint set
                double x = 0, y = 0, l = 0, w = 0; //values for drawing rectangles
                for (int elementID = 0; elementID < pixelSet.length; elementID++) {  //iterating through each pixel
                    if (find(pixelSet, elementID) != -1 && find(pixelSet, elementID) == currentRoot) {  //if value of pixel is not -1
                        if (!topLeft) {   //if topLeft pixel has not been defined
                            x = elementID % width;      // remainder of current index divided by width
                            y = Math.floor(elementID / width);  //always rounds down (https://docs.oracle.com/javase/7/docs/api/java/lang/Math.html#floor%28double%29)
                            //System.out.println("Top Left of disjoint: " + x + " , " + y);
                            topLeft = true;

                        } else {
                            if (elementID % width < x) { //if x value of pixel is less than top left (i.e further left)
                                x = elementID % width;  //update x value to move further left
                            }
                            //length
                            if (elementID % width > x) {  //if x value of pixel is greater than top left (i.e further right)
                                if (elementID % width - x > l)  //if the difference between the two values is greater than current difference (length)
                                    l = elementID % width - x;  //length is equal to the difference between element ID and x
                            }
                            //width
                            if (Math.floor(elementID / width) > y) { //if y value is greater than top left (i.e further down)
                                if (Math.floor(elementID / width) - y > w)  //if difference between two values is greater than current difference (width)
                                    w = Math.floor(elementID / width) - y;    //width is equal to difference between elementID and y
                            }
                        }
                    }
                }
                //todo: move stuff to method(s)
                Rectangle rect = new Rectangle(x, y, l, w);
                rect.setFill(Color.TRANSPARENT);
                rect.setStroke(Color.RED);
                rect.setStrokeWidth(2.0);
                Text number = new Text(x+2,y+8,""+componentNo);//draws a label with the componentNo in the top left of each rectangle
                number.setFont(Font.font("Arial", FontWeight.NORMAL, FontPosture.REGULAR,10));  //https://www.tutorialspoint.com/how-to-add-stroke-and-color-to-text-in-javafx
                number.setFill(Color.YELLOW);
                Tooltip tooltip = new Tooltip("Component number: " + componentNo + "\nEstimated size (pixel units): " + sizeOfSet(currentRoot, pixelSet));
                Tooltip.install(rect, tooltip);     ////https://openjfx.io/javadoc/13/javafx.controls/javafx/scene/control/Tooltip.html
                root.getChildren().addAll(rect,number);
                componentNo++;
                componentsTextArea.appendText(tooltip.getText() + "\n\n");
            }
        ogImageViewPane.getChildren().add(root);
        totalComponentsLabel.setText("Total Components: " + (componentNo-1)); //componentNo iterates 1 above actual no of components
        //System.out.println(ogImageViewPane.getChildren());
    }

    @FXML
    private void removeRectangles(){
        if (ogImageViewPane.getChildren().size() > 1) {  //removing existing rectangles from image if they exist
            for (int i = ogImageViewPane.getChildren().size() - 1; i > 0; i--) {
                ogImageViewPane.getChildren().remove(i);
            }
            totalRoots.clear();
            componentsTextArea.clear();
            icbCount = 0;
            resistorCount = 0;
            solderCount = 0;
            miscCount = 0;
            totalComponentsLabel.setText("Total Components: 0");
            totalICBsLabel.setText("ICBs: 0");
            totalResistorsLabel.setText("Resistors: 0");
            totalSoldersLabel.setText("Solder Points: 0");
            totalMiscLabel.setText("Misc: 0");
        }
    }
}
