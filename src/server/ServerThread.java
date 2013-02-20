package server;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.Timer;

public class ServerThread extends Thread implements ActionListener {

	private static final long PING_TIMEOUT = 30000;
	
	private static final ConcurrentMap<String,ServerThread> connectedIds = new ConcurrentHashMap<String,ServerThread>(); 
	
	private final String id;
	private final Timer t;
	private final Socket clientSocket;
	private final Scanner fromClient;
	private final PrintStream toClient;
	
	private long lastPing = System.currentTimeMillis();
	private String pingValue;

	public ServerThread(Socket accept) throws IOException {
		System.out.println("Got new connection from " + accept);
		clientSocket = accept;
		fromClient = new Scanner(clientSocket.getInputStream());
		toClient = new PrintStream(clientSocket.getOutputStream());
		id = generateId();
		connectedIds.put(id, this);
		broadcastAll("User " + id + " has connected. :)");
		t = new Timer((int) (PING_TIMEOUT/3), this);
		t.start();
	}
	
	private void broadcastAll(String string) {
		System.out.println(string);
		for(ServerThread user : connectedIds.values()){
			user.broadcastAttr(string);
		}
	}

	private static String generateId(){
		final int INT_MIN = 1000, INT_MAX = 9999;
		
		String res;
		while(connectedIds.containsKey(res = new Integer((int) (Math.random()*(INT_MAX - INT_MIN) + INT_MIN)).toString()));
		return res;
	}

	@Override
	public void run() {
		super.run();
		while(fromClient.hasNextLine()){
			String line = fromClient.nextLine();
			if(line.equals("PONG " + pingValue)){
				lastPing = System.currentTimeMillis();
				continue;
			}
			String[] lines = line.split("\\s+");
			if(line.startsWith("/")){
				processCommands(lines);
				continue;
			}
			System.out.println(Arrays.toString(lines));
			String putStr;
			{
				StringBuilder str = new StringBuilder();
				for(int i=1;i<lines.length;i++){
					str.append(lines[i]);
					str.append(' ');
				}
				putStr = str.toString();
			}
			if(putStr != null && !putStr.isEmpty()){
				try{
					String str = id + "->" + lines[0] + ": " + putStr;
					if(!lines[0].equals(id)){
						System.out.println("time to get");
						connectedIds.get(lines[0]).
						broadcast(str);
					}
					broadcast(str);
				}catch(NullPointerException e){
					processCommands(new String[]{"/users"});
					e.printStackTrace();
					broadcastAttr("That client id is not currently connected");
				}
			}else{
				broadcastAttr("Command not valid");
			}
		}
		toClient.close();
		fromClient.close();
	}
	
	private void processCommands(String[] lines) {
		if(lines[0].equals("/users")){
			StringBuilder str = new StringBuilder("Users currently online: ");
			Iterator<String> iter = connectedIds.keySet().iterator();
			while(true){
				String value = iter.next();
				str.append(value);
				if(value.equals(id)){
					str.append(" (you)");
				}
				if(iter.hasNext()){
					str.append(", ");
				}else{
					str.append('.');
					break;
				}
			}
			broadcast(str.toString());
		}else{
			broadcast("Command " + lines[0] + " not regocnised. :/");
		}
	}

	private void broadcast(String string){
		try{
			toClient.println(string);
		}catch(IllegalStateException e){
			removeUser();
		}
	}
	
	private void broadcastAttr(String string){
		broadcast("Attention: " + string);
	}
	
	private void ping(){
		pingValue = new Integer((int) (Math.random()*100000)).toString();
		broadcast("PING " + pingValue);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		long lastPinged = System.currentTimeMillis() - lastPing; 
		System.out.println("checking if user " + id + " is connected (" + lastPinged + ").");
		if(lastPinged > PING_TIMEOUT){
			removeUser();
		}else{
			ping();
		}
	}
	
	private void removeUser(){
		t.stop();
		connectedIds.remove(id);
		fromClient.close();
		toClient.close();
		try {
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		broadcastAll("User " + id + " disconnected D:");
	}
}
