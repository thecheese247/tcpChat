package client;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client extends Thread{
	
	private static final long PING_TIMEOUT = 30000;
	
	private final Socket server;
	private final Scanner fromServer;
	private final PrintStream toServer;
	
	private long lastPing = System.currentTimeMillis();
	
	public Client(String host, int port) throws IOException, UnknownHostException{
		server = new Socket(host,port);
		toServer = new PrintStream(server.getOutputStream());
		fromServer = new Scanner(server.getInputStream());
	}
	
	public Client() throws IOException{
		System.out.println("Please enter the server and port of the chat server (e.g. 10.08.94 25565 or localhost 10894)");
		while(true){
			String[] serverDetails;
			while(true){
				serverDetails = new Scanner(System.in).nextLine().split("\\s+");
				if(validServer(serverDetails)){
					break;
				}
				System.out.println("Invalid server selected. Try again.");
			}
			Socket serverCon;
			try{
				serverCon = new Socket(serverDetails[0], Integer.parseInt(serverDetails[1]));
			}catch(UnknownHostException e){
				System.out.println("Could not determine the host to connect to. Try again.");
				continue;
			}catch(ConnectException e){
				System.out.println("Connection was refused by the host. Try again.");
				continue;
			}
			if(serverCon.isConnected()){
				server = serverCon;
				break;
			}
			System.out.println("Determined host but failed to connect to server. Try again");
			try{
				serverCon.close();
			}catch(IOException e){}
		}
		toServer = new PrintStream(server.getOutputStream());
		fromServer = new Scanner(server.getInputStream());
	}
	
	@Override
	public void run() {
		super.run();
		new PingChecker().start();
		new ClientTextReciever().start();
		Scanner in = new Scanner(System.in);
		while(in.hasNextLine()){
			toServer.println(in.nextLine());
		}
		close();
	}
	
	private void close(){
		try {
			System.in.close();
		} catch (IOException e1) {
		}
		toServer.close();
		fromServer.close();
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Closing connection...");
		System.exit(-1);
	}
	
	private class ClientTextReciever extends Thread {
		
		@Override
		public void run() {
			super.run();
			while(fromServer.hasNextLine()){
				String line = fromServer.nextLine();
				if(line.startsWith("PING")){
					toServer.println("PONG" + line.substring(4));
					lastPing = System.currentTimeMillis();
					continue;
				}
				System.out.println(line);
			}
			System.out.println("Server closed the connection. Perhaps the server died?");
			close();
		}
	}
	
	private class PingChecker extends Thread {
		
		@Override
		public void run() {
			super.run();
			while(true){
				try {
					sleep(lastPing + PING_TIMEOUT - System.currentTimeMillis());
				} catch (InterruptedException e) {
				}
				if(System.currentTimeMillis() > lastPing + PING_TIMEOUT){
					System.out.println("Server connection timed out.");
					break;
				}
			}
			close();
		}
	}

	private boolean validServer(String[] serverDetails) {
		try{
			return serverDetails.length == 2 && new Integer(serverDetails[1]) != null;
		}catch(NumberFormatException e){
			return false;
		}
	}

	public static void main(String[] args) {
		try{
			if(args.length == 2){
				try{
					new Client(args[0],Integer.parseInt(args[1])).start();
				}catch(NumberFormatException e){
					System.out.println("The port provided was invalid.");
				}catch(UnknownHostException e){
					System.out.println("Could not determine the host to connect to. Try again.");
				}catch(ConnectException e){
					System.out.println("Connection was refused by the host. Try again.");
				}
			}else{
				new Client().start();
			}
		}catch(IOException e){
			System.out.println(e.getMessage());
		}
	}

}
