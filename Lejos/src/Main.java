

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
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.utility.Delay;

public class Main {
	
	private static Brick brick;
	
	private static byte[] messageByte = new byte[1000];
	private static Socket send_Socket;
	private static final int SEND_PORT = 5678;
	private static final String IP = "10.0.1.10"; // PAN address, use OS monitor on Android to find it out
	
	private static String receiveMessage, sendMessage;
	private static OutputStream ostream ; 
	private static PrintWriter pwrite;
	
	       // receiving from server ( receiveRead  object)
	private static InputStream istream;
	private static BufferedReader receiveRead;
	
	private static EV3LargeRegulatedMotor motorA = new EV3LargeRegulatedMotor(MotorPort.A);
	
	public static void main(String[] args) {
		
		brick = BrickFinder.getDefault();
		int a = 0;
		//setupSendSocket();
		
		Sound.systemSound(true, Sound.DESCENDING);
		
		setupSendSocket();
		
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
			
			sendReceiveMessage("" +a);
			
			a ++;
		}
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
	
	static void sendReceiveMessage(String message)
	{
		sendMessage = "client " + message;  // keyboard reading
		pwrite.println(sendMessage);       // sending to server
		pwrite.flush();                    // flush the data
		
		try {
			if((receiveMessage = receiveRead.readLine()) != null) //receive from server
			{
				System.out.println(receiveMessage); // displaying at DOS prompt
				
				motorA.rotateTo(Integer.parseInt(receiveMessage), true);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
                
	}
	
}
