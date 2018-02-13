import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import fr.inria.papart.procam.*; 
import fr.inria.papart.procam.camera.*; 
import fr.inria.papart.procam.display.*; 
import org.bytedeco.javacpp.*; 
import org.reflections.*; 
import TUIO.*; 
import toxi.geom.*; 
import org.openni.*; 
import fr.inria.guimodes.Mode; 
import tech.lity.rea.scanner3d.GrayCode; 
import fr.inria.papart.calibration.*; 
import fr.inria.papart.calibration.files.*; 
import tech.lity.rea.skatolo.*; 
import tech.lity.rea.skatolo.gui.controllers.*; 
import tech.lity.rea.scanner3d.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class grayCodeConfiguration extends PApplet {













int sc = 2;

int displayTime = 200;
int captureTime = 180;
int delay = 70;
int decodeValue = 120;
int decodeType = 1;

int whiteColor = 200;
int blackColor = 0;


PImage imageOut;
GrayCode grayCode;
PImage[] grayCodesCaptures;
PImage projectorView = null;

Camera cameraTracking;
ProjectorDisplay projector;

int frameSizeX;
int frameSizeY;
int framePosX = 0;
int framePosY = 0;
int cameraX;
int cameraY;



Papart papart;
PGraphics decodedImage;

public void settings(){
    fullScreen(P3D);

}

public void setup(){

    frameSizeX = width;
    frameSizeY = height;

    initGui();

    papart = Papart.projection(this);

    papart.startCameraThread();
    
    cameraTracking = papart.getCameraTracking();
    cameraX = cameraTracking.width();
    cameraY = cameraTracking.height();

    decodedImage = createGraphics(cameraX, cameraY);

    projector = (ProjectorDisplay) papart.getDisplay();
    projector.manualMode();

    frameSizeX = projector.getWidth();
    frameSizeY = projector.getHeight();

    imageOut = createImage(cameraX, cameraY, RGB);

    frameRate(100);
}




PImage cameraImage;


private boolean getCameraImage(){
    cameraImage = cameraTracking.getPImage();
    return cameraImage != null;
}


public void draw(){
    background(0);


    if(!getCameraImage()){
	return;
    }

    // video... (to check if it is the correct camera)
    drawWait();

    // capture...
    drawCode();

    // decoded !
    drawResult();
}

public void drawWait(){
    if(Mode.is("wait")){
	image(cameraImage, 0, 0, cameraX, cameraY);
    }
}


public void drawCode(){
    if(Mode.is("code")){
	updateCodes();

	// Display the gray code to capture
	// Other possibility.
	// 	image(grayCodes[code], 0, 0, frameSizeX, frameSizeY);

	grayCode.display((PGraphicsOpenGL) this.g, code);
	tryCaptureImage();

	if(allCodesCaptured()){
	    Mode.set("result");
	    decodeBang.show();
	}

    }
}


PVector imVisu = new PVector(400, 300);

public void drawResult(){
    if(Mode.is("result")){

	if(!grayCode.isDecoded())
	    decode();

	updateCodes();

	image(decodedImage, 0, 0, cameraX / 2f, cameraY /2f);

	// Draw each captured image bone by one.
	image(grayCodesCaptures[code], cameraX, 0,
	      (int) imVisu.x, (int) imVisu.y);


	// Decode and show the decoded...
	PImage decodedIm = grayCode.getImageDecoded(code, decodeType, decodeValue);

	image(decodedIm, cameraX /2f, (int) imVisu.y,
	      (int) imVisu.x, (int) imVisu.y);

	image(projectorView,
	      0, cameraY/2,
	      frameSizeX / 3,
	      frameSizeY / 3);


    }
}

public void decode(){

    println("Decode " + decodeType);
    println("value " + decodeValue);

    grayCode.decode(decodeType,decodeValue);

    drawDecoded();
    saveScanBang.show();
    projectorView = grayCode.getProjectorImage();
    projectorView.save("ProjView.bmp");
    System.gc();
}




public void tryCaptureImage(){
    if(captureOK()){

    	if(grayCodesCaptures[codeProjected] == null){

	    if(cameraTracking.getIplImage() != null){
		
		// Create an image
		PImage im = cameraTracking.getPImageCopy();
		setNextCaptureTime();
		addCapturedImage(im);
		if(codeProjected == nbCodes){
		    grayCode.setRefImage(im);
		}
	    }
	}
    }
}

public void addCapturedImage(PImage im){
    grayCode.addCapture(im, codeProjected);
    grayCodesCaptures[codeProjected] = im;
    nbCaptured++;
    println("Captured code: " + code + ", total: " + nbCaptured);
}


public void startCapture(){
    Mode.set("code");

    grayCode = new GrayCode(this, frameSizeX, frameSizeY, sc);
    grayCode.setBlackWhiteColors(blackColor, whiteColor);

    nbCodes = grayCode.nbCodes();
    grayCodesCaptures = new PImage[nbCodes];

    code = 0;
    codeProjected = 0;
    nbCaptured = 0;
    startTime = millis();
    nextCapture = startTime + captureTime;
    decodeBang.hide();
    saveScanBang.hide();
}


boolean test = false;

public void keyPressed() {

  if(key == 't')
    test = !test;

}


public void checkStart(){
    if(Mode.is("wait") || Mode.is("result")){
	startCapture();
    }
}


// Offscreen drawing.

public void drawDecoded(){
    int[] decodedX = grayCode.decodedX();
    int[] decodedY = grayCode.decodedY();
    boolean[] mask = grayCode.mask();

    decodedImage.beginDraw();
    decodedImage.background(0);
    decodedImage.colorMode(RGB, frameSizeX, frameSizeY, 255);
    for(int y = 0 ; y < cameraY; y+= 1) {
	for(int x = 0; x < cameraX; x+= 1) {
	    int offset = x + y* cameraX;
	    if(!mask[offset]){
		decodedImage.stroke(255, 255, 255);
		decodedImage.stroke((int) random(255));
		decodedImage.point(x,y);
		continue;
	    }
	    decodedImage.stroke(decodedX[offset], decodedY[offset], 100);
	    decodedImage.point(x,y);
	}
    }
    decodedImage.endDraw();
}
int startTime;
int nextCapture;

int nbCaptured = 0;
int nbCodes;

int code;
int codeProjected;

public void updateCodes(){
    code = (currentTime() / displayTime) %  nbCodes;
    codeProjected = ((currentTime() - delay) / displayTime) %  nbCodes;
}

public boolean allCodesCaptured(){
    return nbCaptured >= nbCodes;
}

public boolean captureOK(){
    return millis() >= nextCapture;
}

public void setNextCaptureTime(){
    int elapsed = displayTime * (codeProjected+1);
    nextCapture = startTime + elapsed + captureTime;
}

public int currentTime() {
    return millis() - startTime;
}













Skatolo skatolo;

Mode waitForStartMode, startPressed, displayCodeMode, displayResult;
//   wait            , start       , code           , result

Bang saveScanBang, decodeBang;
ControlFrame cf;

PApplet mainSketch;

public void initGui(){

    Mode.init(this);
    waitForStartMode = Mode.add("wait");
    startPressed = Mode.add("start");
    displayCodeMode = Mode.add("code");
    displayResult = Mode.add("result");

    Mode.set("wait");
    mainSketch = this;
    cf = new ControlFrame();
}


// the ControlFrame class extends PApplet, so we
// are creating a new processing applet inside a
// new frame with a skatolo object loaded
public class ControlFrame extends PApplet {
    Skatolo skatolo;

    public ControlFrame() {
	super();
	PApplet.runSketch(new String[]{this.getClass().getName()}, this);
    }

    public void settings() {
	size(400, 400, P2D);
    }

    public void setup(){

        skatolo = new Skatolo(this);

        // add a horizontal sliders, the value of this slider will be linked
        // to variable 'sliderValue'
        skatolo.addSlider("displayTime")
            .setPosition( 10, 20)
            .setRange(30,1200)
            .setValue(displayTime)
            .plugTo(mainSketch, "displayTime")
            ;

        skatolo.addSlider("captureTime")
            .setPosition( 10, 40)
            .setRange(30, 1200)
            .setValue(captureTime)
            .plugTo(mainSketch, "captureTime")
            ;

        skatolo.addSlider("delay")
            .setPosition( 10, 60)
            .setRange(0, 300)
            .setValue(delay)
            .plugTo(mainSketch, "delay")
            ;


        skatolo.addSlider("sc")
            .setPosition( 10, 80)
            .setRange(1, 8)
            .setValue(sc)
            .plugTo(mainSketch, "sc")
            .setLabel("pixel scale")
            ;

        skatolo.addSlider("blackColor")
            .setPosition(10, 100)
            .setRange(0, 255)
            .setValue(blackColor)
            .plugTo(mainSketch, "blackColor")
            ;

        skatolo.addSlider("whiteColor")
            .setPosition(10, 110)
            .setRange(0, 255)
            .setValue(whiteColor)
            .plugTo(mainSketch, "whiteColor")
            ;



        skatolo.addRadioButton("decodeType")
            .setPosition( 10, 140)
            .addItem("reference", GrayCode.DECODE_REF)
            .addItem("absolute", GrayCode.DECODE_ABS)
            .activate(1)
            .setNoneSelectedAllowed(true)
            .plugTo(mainSketch, "decodeType")
            ;

        skatolo.addSlider("decodeValue")
            .setPosition( 10, 160)
            .setRange(0, 255)
            .setValue(decodeValue)
            .setLabel("Decode value")
            .plugTo(mainSketch, "decodeValue")
            ;


        skatolo.addBang("startButton")
            .setPosition( 10, 200)
            .setSize(20, 20)
            .setLabel("Start")
            .plugTo(mainSketch, "checkStart")
            ;


        skatolo.addBang("saveCalib")
            .setPosition(100, 300)
            .setSize(20, 20)
            .setLabel("Save Calibration")
            ;

        decodeBang = skatolo.addBang("decodeBang")
            .setPosition( 10, 260)
            .setSize(20, 20)
            .setLabel("Decode again")
            .plugTo(mainSketch, "decode")
            ;

        saveScanBang = skatolo.addBang("saveScan")
            .setPosition(10, 300)
            .setSize(20, 20)
            .setLabel("Save decoded")
            ;

        saveScanBang.hide();
        decodeBang.hide();
    }




    int saveID = 0;
    public void saveScan(){
        grayCode.save("scan"+saveID);
        saveID++;
        saveScanBang.hide();
    }


    public void loadCalib(){

    }

    public void saveCalib(){

        CameraProjectorSync cps = new CameraProjectorSync(displayTime, captureTime, delay);
        cps.setDecodeParameters(decodeType, decodeValue);

        cps.saveTo(this, "sync.xml");
    }


    public void draw(){
        background(0);
    }

    public Skatolo control() {
        return skatolo;
    }
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "grayCodeConfiguration" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
