package server;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {

	private final ServerSocket serverSocket;
	private boolean running = true;
	
	Server() throws IOException{
		serverSocket = new ServerSocket(10894);
		System.out.println("Started server.");
		System.out.println(serverSocket);
		while(running){
			try{
				new ServerThread(serverSocket.accept()).start();
			}catch(IOException e){
				System.out.println("AN I/O EXCEPTION OCCURED: " + e.getMessage());
			}
		}
	}
	
	public static void main(String[] args){
		try {
			new Server();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
