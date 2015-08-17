import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class WebServer1
{
	static int portNumber = 5123;
	static boolean listening = true;
	static ServerSocket listenSocket;
	static ExecutorService threadPool =
		        Executors.newFixedThreadPool(10);
	public static void main(String argv[]) throws Exception
	{
		try	{
		listenSocket = new ServerSocket(portNumber);

			while(listening){
				System.out.println("Listening for connections");
				Socket clientSocket = listenSocket.accept();
				
				System.out.println("New Connection accepted");
				threadPool.execute(
						new httpConnection(clientSocket));
				//httpConnection newConnection=new httpConnection(clientSocket);
				//newConnection.start();
				System.out.println("New Connection thread spawned");
			}
			listenSocket.close();
		}
		catch (IOException e) {
			System.err.println("Could not listen on port ");
			System.exit(-1);
		}
	
	}

}
