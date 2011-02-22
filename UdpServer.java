import java.io.*;
import java.net.*;
import java.util.*;

/*
The client side of our UDP assignment.

AUTHOR: Dave Hoatlin

DATE:10/13/2010
*/


public class UdpServer
{
	DatagramSocket serverSocket;
	int socketPort;
	
	String serverHome;
	String[] serverFiles;
	
	InetAddress clientIP;
	int clientPort;
	
	//initialize server
	public UdpServer() throws Exception
	{
		//scanner to read int
		Scanner scan = new Scanner(System.in);
		
		//getting the port number
		System.out.print("Enter port number:\n>");
		socketPort = scan.nextInt();
		
		//creating socket
		serverSocket = new DatagramSocket(socketPort);

		//getting file path to server directory
		serverHome = System.getProperty("user.home") + "\\server";
		
		//get available files
		serverFiles = getFiles(serverHome);
		
		//welcome the user
		welcomeUser();
	}
	
	private void welcomeUser()
	{
		System.out.print("\n\n\n\n\n");
		System.out.println("Welcome to the server side of our UDP assignment.");
		System.out.println("Press ctrl-c to exit.");
	}
	
	private void getCommand() throws Exception
	{
		while(true)
		{			
			byte[] recvData = new byte[1024];
			
			//recieve initial packet from client
			DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
			serverSocket.receive(recvPacket);
			
			//getting packets ip and port
			clientIP = recvPacket.getAddress();
			clientPort = recvPacket.getPort(); 
			
			//convert packet to string to find the user's command
			String clientCommand = new String(recvPacket.getData()).trim();
			
			//process the command
			processCommand(clientCommand);
		}
	}
	
	private void processCommand(String command) throws Exception
	{
		//data arrays for packets
		byte[] recvData = new byte[1024];
		byte[] sendData = new byte[1024];
		
		if(command.equals("get"))
		{
			//recieve file name from client
			DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
			serverSocket.receive(recvPacket);
			
			//grab string from packet
			String file = new String(recvPacket.getData()).trim();
			
			//pass file number to transmit method
			transmitFile(file);
			
			//reopen the socket to reset timeout
			serverSocket = new DatagramSocket(socketPort);
		}
		else if(command.equals("list"))
		{
			//list available files to client
			String files = filesToString(serverFiles);
			sendData = files.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
			serverSocket.send(sendPacket);
		}
		else
		{
			//tell client possible data corruption
			String corrupt = "Command not recognized, possibly corrupted during transmission";
			sendData = corrupt.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
			serverSocket.send(sendPacket);
		}
	}
	
	private void transmitFile(String file) throws Exception
	{
		//data arrays
		byte[] recvData = new byte[1024];
		byte[] sendData = new byte[1024];
		
		//path to the file we want to send
		String filePath = serverHome + "\\" + file;
		
		//create bufferedInputStream that reads the file to send
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath));
	
		//set serverTimout length
		serverSocket.setSoTimeout(500);
		
		//some interesting data
		int packetsSent = 0;
		
		//while there is still data to be read
		int check;
		while((check = bis.read(sendData)) != -1)
		{
			//create and send packet
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
			serverSocket.send(sendPacket);
			packetsSent++;
			System.out.println("data sent");
			
			//create the receiving packet
			DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
			
			//wait for ack until timeout
			int rcvCheck = 0;
			while(rcvCheck == 0)
			{
				try
				{
					rcvCheck = 1;
					serverSocket.receive(recvPacket);
				}
				catch (SocketTimeoutException se)
				{
					rcvCheck = 0;
					System.out.println("Client return packet timed out.\nRetransmitting...");
					serverSocket.send(sendPacket);
					packetsSent++;
				}
			}
			
			//recreate arrays here to avoid overlap from previous packets
			recvData = new byte[1024];
			sendData = new byte[1024];
		}
		
		//at this point all the data has been sent.
		//tell the client that the transmission is done
		String end = "end";
		sendData = end.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIP, clientPort);
		serverSocket.send(sendPacket);
		packetsSent++;
		System.out.println("done sending data\n");
		System.out.println("Total packets sent: " + packetsSent);
		System.out.println("\nAwaiting next command from client.");
		
		//close BufferedInputStream
		bis.close();

		//close server socket to reset timeout
		serverSocket.close();
	}
	
	private String[] getFiles(String directory)
	{
		//get the folder we want to grab files from
		File folderToRead = new File(directory);
		
		//save all files into file array this includes subdirectories
		File[] folderFiles = folderToRead.listFiles();
		
		//create string array to hold all file names
		String[] fileNames = new String[folderFiles.length];
		
		//loop through every file
		int fileCount = 0;
		for(int i = 0; i < folderFiles.length; i++)
		{
			//if file is not a subdirectory
			if(folderFiles[i].isFile())
			{
				//add this file name to the string array
				fileNames[fileCount] = folderFiles[i].getName();
				fileCount++;
			}
		}
		
		//return the string array
		return fileNames;
	}
	
	private String filesToString(String[] files)
	{
		String filesToString = "";
		
		//loop through files and turn into one long string
		for(int i = 0; i < files.length; i++)
		{
			filesToString = filesToString + files[i] + "\n";
		}
		
		return filesToString;
	}

	public static void main(String args[]) throws Exception
	{
		//create server
		UdpServer server = new UdpServer();

		//tell server to wait for a command
		server.getCommand();
	}
}