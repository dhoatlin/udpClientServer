import java.io.*;
import java.net.*;
import java.util.*;

/*
The client side of our UDP assignment.

AUTHOR: Dave Hoatlin

DATE:10/13/2010
*/


public class UdpClient
{
	InetAddress serverAddress;
	int serverPort;
	DatagramSocket clientSocket;
	
	String clientFolder;
	
	int lossRate;
	
	Random rand;
	
	Scanner scan;
	
	public UdpClient() throws Exception
	{
		//create scanner
		scan = new Scanner(System.in);
		
		//get IP address
		System.out.print("Enter IP address:\n>");
		String ip = scan.nextLine();
		
		//get port number
		System.out.print("Enter port number:\n>");
		serverPort = scan.nextInt();
		
		//setup socket
		serverAddress = InetAddress.getByName(ip);
		clientSocket = new DatagramSocket();
		
		//get filepath
		clientFolder = System.getProperty("user.home") +"\\client";
		
		//create random number generator
		rand = new Random();
		
		//flush the scanner
		scan.nextLine();
		
		welcomeUser();
	}
	
	private void welcomeUser()
	{
		System.out.print("\n\n\n\n\n");
		System.out.println("Welcome to the client side of our UDP assignment.");
		System.out.println("\nPlease insert a command(get, list, logout)");
	}
	
	private void commandMode() throws Exception
	{
		boolean leave = false;
		while(!leave)
		{
			//signal that user can input something
			System.out.print(">");
			
			//grab input
			String commandString = scan.nextLine();
			
			//create arrays for incoming and outgoing data
			byte[] sendData = new byte[1024];
			byte[] recvData = new byte[1024];
			
			//create packet to send and recieve from server
			DatagramPacket sendPacket;
			DatagramPacket recvPacket;
			
			//checking if command is to be sent to server
			if(commandString.equals("get") || commandString.equals("list"))
			{
				//send command in packet to server
				sendData = commandString.getBytes();
				sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
				clientSocket.send(sendPacket);
				
				//need additional stuff for get command
				if(commandString.equals("get"))
				{
					//at this point the server will need a file name
					//scan for file name to send back to server
					System.out.print("Enter file name to send:\n>");
					//commandString = commandScan.nextLine();
					commandString = scan.nextLine();
					
					//reset sendData so nothing overlaps from previous
					sendData = new byte[1024];
					
					//ask user for loss probability
					System.out.print("preparing to receive file.\nSpecify loss probability (0-100):\n>");
					lossRate = Integer.parseInt(scan.nextLine());
					
					//send command to server
					sendData = commandString.getBytes();
					sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
					clientSocket.send(sendPacket);

					//at this point we have told the server which file we want
					//to get. Now move to receive mode. After done receiving we
					//will and back inside this while loop until the user logs
					receiveMode(commandString);
				}
				else
				{
					//grab and print response packet from server
					//at this point all data received will be text
					recvPacket = new DatagramPacket(recvData, recvData.length);
					clientSocket.receive(recvPacket);
					
					String response = new String(recvPacket.getData()).trim();
					System.out.println(response);
				}	
			}
			//if client wants to logout
			else if(commandString.equals("logout"))
			{
				//logout
				leave = true;
			}
			//incorrect command
			else
			{
				System.out.println("Command not recognized.\nInput a new command(get, list, logout)");
			}
		}
		logout();
	}
	
	private void receiveMode(String file) throws Exception
	{
		//fileOutputStream that creates the transfered file
		FileOutputStream fos = new FileOutputStream(clientFolder + "\\" + file, true);
		
		//counters for some interesting data
		int packetsIgnored = 0;
		int packetsWritten = 0;
		
		boolean receiving = true;
		while(receiving)
		{
			//reset arrays to avoid overlapping data
			byte[] sendData = new byte[1024];
			byte[] recvData = new byte[1024];
			
			//create receiving and sending packets
			DatagramPacket recvPacket;
			DatagramPacket ackPacket;
			
			//receive packet from server;
			recvPacket = new DatagramPacket(recvData, recvData.length);
			clientSocket.receive(recvPacket);
			
			//string to see if end of file is sent
			String endFile = new String(recvPacket.getData()).trim();
			
			//if end of file break out of loop
			if(endFile.equals("end"))
			{
				System.out.println("File transfer complete\n");
				System.out.println("Packets ignored: " + packetsIgnored);
				System.out.println("Packets written: " + packetsWritten);
				System.out.println("\nInput a new command(get, list, logout)");
				receiving = false;
			}
			//simulate packet loss
			else if(rand.nextInt(100) < lossRate)
			{
				packetsIgnored++;
				System.out.println("* LOST PACKET *");
			}
			//write to file
			else
			{
				packetsWritten++;
				fos.write(recvPacket.getData());
				System.out.println("data written");
				
				//tell the server this packet was received
				String ack = "ack";
				sendData = ack.getBytes();
				ackPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
				clientSocket.send(ackPacket);
				System.out.println("ack sent");
			}
		}
		
		//close fileOutputStream
		fos.close();
		
		//now returns to commandMode
	}
	
	private void logout()
	{
		System.out.println("logging out");
		clientSocket.close();
	}
	
	public static void main(String args[]) throws Exception
	{
		//create client
		UdpClient client = new UdpClient();
		
		//send client to command mode
		client.commandMode();
	}
	
}