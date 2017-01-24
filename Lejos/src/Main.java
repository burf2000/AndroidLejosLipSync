

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.device.NXTCam;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.Color;
import lejos.robotics.SampleProvider;
import lejos.robotics.geometry.Rectangle2D;
import lejos.utility.Delay;

public class Main {
	
	private static Brick brick;
	
	private static byte[] messageByte = new byte[1000];
	private static Socket send_Socket;
	private static final int SEND_PORT = 5678;
	private static final String IP = "10.0.1.13"; // PAN address, use OS monitor on Android to find it out
	
	private static String receiveMessage, sendMessage;
	private static OutputStream ostream ; 
	private static PrintWriter pwrite;
	
	       // receiving from server ( receiveRead  object)
	private static InputStream istream;
	private static BufferedReader receiveRead;
	
	private static EV3MediumRegulatedMotor motorA = new EV3MediumRegulatedMotor(MotorPort.C);
	private static EV3ColorSensor colourSensor;
	private static NXTCam camera;
	private static EV3MediumRegulatedMotor upMotor;
	private static EV3LargeRegulatedMotor turningMotor;
	private static EV3UltrasonicSensor sonicSensor;
	
	//final static int INTERVAL = 500; // milliseconds
	static String objects = "Objects: ";
	static int numObjects;
	
	static boolean found = false;
	
	private static int lastColour = 0;
	private static int currentColour = 0;
	private static int TURNING_LIMIT = 330;
	private static String textToSay = "";
	
	private static SampleProvider distance;
	private static float[] sample;
	
	
	public static void main(String[] args) {
		
		brick = BrickFinder.getDefault();
		int a = 0;
		
		Sound.systemSound(true, Sound.DESCENDING);
		
		setupSendSocket();
		
		camera = new NXTCam(brick.getPort("S3"));
		
		camera.sendCommand('A'); // sort objects by size
		camera.sendCommand('E'); // start tracking
		
		sonicSensor = new EV3UltrasonicSensor(brick.getPort("S1"));
	    distance = sonicSensor.getDistanceMode();
		// initialise an array of floats for fetching samples
		sample = new float[distance.sampleSize()];

		
		upMotor =  new EV3MediumRegulatedMotor(brick.getPort("B"));
		upMotor.flt();
		upMotor.resetTachoCount();
		upMotor.setSpeed(7200);
		
		motorA.setSpeed(7200);
		motorA.flt();
		upMotor.resetTachoCount();
		
		turningMotor =  new EV3LargeRegulatedMotor(brick.getPort("A"));
		turningMotor.flt();
		turningMotor.resetTachoCount();
		turningMotor.setSpeed(500); //7200
		
		colourSensor = new EV3ColorSensor(brick.getPort("S2"));
		colourSensor.setFloodlight(Color.RED);
		
		while (true)
		{
			Delay.msDelay(50);			
			
			if (Button.DOWN.isDown() )
			{
				Sound.systemSound(true, Sound.BEEP);
			}
			
			if (Button.ESCAPE.isDown() )
			{
				try {
					send_Socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
			
			// fetch a sample
			distance.fetchSample(sample, 0);
			
			LCD.drawString("s" +sample[0], 0, 0);
			if (sample[0] < 0.2)
			{
				sayText("Hello and welcome to the Lab area. We love to discuss technology!");
				//Delay.msDelay(1000);	
			}
			
			showObjects();
			sendReceiveMessage("" +a);
			
			a ++;
		}
	}
	
	static void showObjects()
	{
//		LCD.clear();
//		LCD.drawString(camera.getVendorID(), 0, 0);
//		LCD.drawString(camera.getProductID(), 0, 1);
//		LCD.drawString(camera.getVersion(), 9, 1);
//		LCD.drawString(objects, 0, 2);
//		LCD.drawInt(numObjects = camera.getNumberOfObjects(),1,9,2);
		
		numObjects = camera.getNumberOfObjects();
		
		if (numObjects >= 1 && numObjects <= 8) {
			for (int i=0;i<numObjects;i++) {
				Rectangle2D r = camera.getRectangle(i);
				if (r.getHeight() > 5 && r.getWidth() > 5) {
					
					//LCD.drawInt(camera.getObjectColor(i), 3, 0, 3+i);
					//LCD.drawInt((int) r.getWidth(), 3, 4, 3+i);
					//LCD.drawInt((int) r.getHeight(), 3, 8, 3+i);
					
					//System.out.println("C" + r.getCenterX() + " " + r.getCenterY() + " " + upMotor.getTachoCount());
					//Log.info("Simon");
					
					if (camera.getObjectColor(0) == 1)
					{
						colourSensor.setFloodlight(Color.RED);
						currentColour = 1;
						
					}
					else if (camera.getObjectColor(0) == 2)
					{
						colourSensor.setFloodlight(Color.BLUE);
						currentColour = 2;
					}
					
					if ( r.getCenterY() < 40 ) //upMotor.getTachoCount() > -7200  &&
					{
						upMotor.rotate(-360, true);
						//upMotor.backward();
						//System.out.println("Backward");
						found = false;
					}
					else if (r.getCenterY() > 60) //upMotor.getTachoCount() < 0 &&
					{
						//upMotor.forward();
						upMotor.rotate(360 , true);
						//System.out.println("Forward");
						found = false;
					}
					else
					{
						upMotor.stop();
						found = true;
					}
					
					if ( r.getCenterX() < 60 && turningMotor.getTachoCount() > -TURNING_LIMIT) //upMotor.getTachoCount() > -7200  &&
					{
						turningMotor.rotate(-36, true);
						//upMotor.backward();
						//System.out.println("right");
						found = false;
					}
					else if (r.getCenterX() > 95  && turningMotor.getTachoCount() < TURNING_LIMIT) //upMotor.getTachoCount() < 0 &&
					{
						//upMotor.forward();
						turningMotor.rotate(36 , true);
						//System.out.println("left");
						found = false;
					}
					else
					{
						turningMotor.stop();
					}
					
					if (found == true)
					{
						
						
						if (lastColour != 1 && currentColour == 1)
						{
							lastColour = 1;
							sayText("This is a red ball");

							
						}
						else if (lastColour != 2 && currentColour == 2)
						{
							lastColour = 2;
							sayText("This is a blue ball");
						}
					}
					
				}
				else if (i == 0)
				{
					colourSensor.setFloodlight(Color.WHITE);
					
					upMotor.stop();
					currentColour = 0;
					lastColour = 0;
				}

			}
		}
		else
		{
			colourSensor.setFloodlight(Color.WHITE);
			currentColour = 0;
			lastColour = 0;
		}
		
		LCD.refresh();
//		try {
//			Thread.sleep(INTERVAL);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	static void setupSendSocket()
	{
		try {
			send_Socket = new Socket(IP, SEND_PORT);

			// sending to client (pwrite object)
			ostream = send_Socket.getOutputStream(); 
			pwrite = new PrintWriter(ostream, true);
			
			       // receiving from server ( receiveRead  object)
			istream = send_Socket.getInputStream();
			receiveRead = new BufferedReader(new InputStreamReader(istream));
			
			System.out.println("Starting Up");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static void sayText(String text)
	{
		textToSay = text;
	}
	
	static void sendReceiveMessage(String message)
	{
		sendMessage = textToSay;  // keyboard reading
		pwrite.println(sendMessage);       // sending to server
		pwrite.flush();                    // flush the data
		textToSay = "";
		try {
			if((receiveMessage = receiveRead.readLine()) != null) //receive from server
			{
				int base = (Integer.parseInt(receiveMessage) - 75) ;
				
				//System.out.println(base); // displaying at DOS prompt
				motorA.rotateTo(base , true);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			setupSendSocket();
		}
                
	}
	
}
