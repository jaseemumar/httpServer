import java.util.StringTokenizer;
import java.net.*;
import java.io.*;

public class httpConnection implements Runnable{
	Socket clientSocket;
	boolean open;
	BufferedReader in;
	DataOutputStream out;

	public httpConnection(Socket _clientSocket){
		clientSocket=_clientSocket;

		open=true;
		try{
			in=	new BufferedReader(new InputStreamReader (clientSocket.getInputStream()));
			out = new DataOutputStream(clientSocket.getOutputStream());
		}
		catch (IOException e) {
			System.err.println("Exception caught while getting/creating Streams");
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
	public void run(){
		System.out.println("Thread started running");
		while(open){
			receiveRequest();
			System.out.println("Request dealt with\n\n");
		}
		if(!clientSocket.isClosed()) closeConnection();
	}
	public void closeConnection(){
		try{
			System.out.println("Closing Connection");
			open=false;
			clientSocket.close();
			//in.close();
			//out.close();}
		}
		catch (IOException e){
			System.err.println("Couldn't get I/O for the connection while closing");
			System.exit(1);
		}
	}

	public void receiveRequest(){
		String requestLine="";
		String header="a";
		String host="";
		try {
			while(!in.ready());
			requestLine= in.readLine();
			System.out.println("RequestLine: "+requestLine);
			while(true){
				header=in.readLine();
				System.out.println(header);
				if(header.isEmpty()) break;
				String fieldAndValue[] = header.split(":");
				if ( fieldAndValue[0].trim().equals("Connection")) {
					//System.out.println("Connection field");
					String value=fieldAndValue[1].trim();
					if(value.equals("close")) open=false;
					//System.out.println("Value: "+value);
				}
				if ( fieldAndValue[0].trim().equals("Host")) {
					//System.out.println("Connection field");
					 host=fieldAndValue[1].trim();
					//System.out.println("Value: "+value);
				}

			}

		}
		catch (IOException e) {
			System.err.println("Error while receiving request. Requestline:\n");
			e.printStackTrace();
		}
		System.out.println("Got message:\n" + requestLine);
		sendResponse(requestLine, host);
		System.out.println("Response Send");
	}

	public void sendResponse(String requestLine, String host){
		String requestPath, fileName, userName, home="", filePath;
		boolean trailSlash=true;
		//StringTokenizer tokenizedLine =
				new StringTokenizer(requestLine);
		String tokens[]=requestLine.split(" ");
		try{
			if ( tokens[0].equals("GET")) {
				fileName = tokens[1];
				requestPath=fileName;
				//if(!tokens[2].equals("HTTP/1.1")) open=false;
				//System.out.println("HTTP version:"+ tokens[2]);
				if(fileName.endsWith("/")==true) 
					fileName=fileName.substring(0,fileName.length()-1);
				else trailSlash=false;
				if(fileName.startsWith("/") == true )
					fileName = fileName.substring(1);
				
				if(fileName.startsWith("~")==true){ 
					int pathStart=fileName.indexOf("/");
					if(pathStart!=-1){
						userName=fileName.substring(0,pathStart );
						fileName=fileName.substring(pathStart);
					}
					else {
						userName=fileName;
						fileName="";
					}

					home=new BufferedReader
							(new InputStreamReader(Runtime.getRuntime().exec
									( new String[]{"sh", "-c", "echo " + userName}).getInputStream())).readLine();
					System.out.println("home: "+home);
					
				}
				filePath=home+"/public_html/"+fileName;

				File file = new File(filePath);
				System.out.println("trailSlash:"+trailSlash);
				if(file.isDirectory()==true && trailSlash==false) redirSlash(requestPath, host);
				else {
					if(file.isDirectory()==true) {
						System.out.println("Accessing a directory");
						filePath=filePath+"index.html";
						file=new File(filePath);
					}

					if(file.exists()==false) send404(filePath);
					else sendFile(filePath,(int) file.length());
				}
			}
		}
		catch(IOException e){

			System.err.println("Error while processing request");
			e.printStackTrace();
			sendServerError();
		}
	}

	public void sendFile(String filePath,int size){
		try {
			FileInputStream fileIn = new FileInputStream(filePath);
			byte[] fileInBytes = new byte[size];

			fileIn.read(fileInBytes);
			fileIn.close();
			out.writeBytes
			("HTTP/1.1 200 OK\r\n");
			if(filePath.endsWith(".jpg"))
				out.writeBytes
				("Content-Type: image/jpeg\r\n");

			if(filePath.endsWith(".gif"))
				out.writeBytes
				("Content-Type: image/gif\r\n");
			out.writeBytes("Content-Length: " +
					size + "\r\n");
			out.writeBytes("\r\n");
			out.write(fileInBytes, 0, size);
		} 
		catch (IOException e) {
			System.out.println("Error while sending file");
			e.printStackTrace();
			if(!clientSocket.isClosed()) sendServerError();
		}
	}
	public void redirSlash(String filePath, String host){
		System.out.println("Sending 301 because of lack of trailing slash of "+filePath );
		try {
			out.writeBytes
			("HTTP/1.1 301 Moved Permanently\r\n");
		
		out.writeBytes
		("Location:"+filePath+"/\r\n");
		out.writeBytes("\r\n");
		out.writeBytes("Content-Length: " +
					0 + "\r\n");
		//out.writeBytes("Error 404");
		} catch (IOException e) {
			System.out.println("Error while sending 301");
			e.printStackTrace();
		}
	}
	public void send404(String filePath){
		System.out.println("Sending Error 404 for file: "+filePath );
		try{
			out.writeBytes
			("HTTP/1.1 404 File Not Found\r\n");
			out.writeBytes
			("Content-Type: text/plain\r\n");
			out.writeBytes("Content-Length: " +
					9 + "\r\n");
			out.writeBytes("\r\n");
			out.writeBytes("Error 404");
		}
		catch (IOException e) {
			System.out.println("Error while sending 404");
			e.printStackTrace();
		}
	}
	public void sendServerError() {
		try{
			out.writeBytes
			("HTTP/1.1 500 Server Error\r\n");
			out.writeBytes
			("Content-Type: text/plain\r\n");
			out.writeBytes("Content-Length: " +
					9 + "\r\n");
			out.writeBytes("\r\n");
			out.writeBytes("Error 500");
		}
		catch (IOException e) {
			System.out.println("Error while sending 500 server error");
			e.printStackTrace();
			//sendServerError();
		}

	}
}