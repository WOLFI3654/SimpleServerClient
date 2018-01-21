package com.blogspot.debukkitsblog.netx.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.UUID;

import com.blogspot.debukkitsblog.netx.Datapackage;

public class Client {

	private String address;
	private int port;
	
	private Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;

	private String id;
	private String group;
	
	private HashMap<String, ClientExecutable> executables;

	public Client(String address, int port) {
		this.address = address;
		this.port = port;
		this.id = UUID.randomUUID().toString();
		this.group = "__INTERNAL_GROUP_DEFAULT__";
		this.executables = new HashMap<>();
	}

	public Client(String address, int port, String id) {
		this(address, port);
		this.id = id;
	}

	public Client(String address, int port, String id, String group) {
		this(address, port, id);
		this.group = group;
	}

	public void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				while (true) {

					System.out.println("Starting CLH routine...");
					
					try {

						connect();
						login();
						handleConnection();

					} catch(ConnectException e) {
						System.err.println("[Client] Server unreachable @ " + address + ":" + port);
					} catch(SocketException e) {
						System.err.println("[Client] Connection lost.");
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						System.out.println("CLH Routine over.");
					}
					
					System.err.println("[Client] Reconnecting in 5 seconds...");
					try { Thread.sleep(5000); } catch (InterruptedException e) {}

				}

			}
		}).start();
	}
	
	private void connect() throws IOException {
		System.out.println("Connecting...");
		socket = new Socket();
		socket.connect(new InetSocketAddress(address, port), 10*1000);
		System.out.println("Connected.");
	}
	
	private void login() throws IOException {
		System.out.println("Logging in...");
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
		oos.writeObject(new Datapackage("__INTERNAL_LOGIN__", id, group));
		System.out.println("Logged in.");
	}
	
	private void handleConnection() throws IOException, ClassNotFoundException {
		
		System.out.println("Handling connection...");
		while(true) {
			System.out.println("Waiting for incoming object...");
			Object rawMessage = ois.readObject();
			System.out.println("Got a message!");
			if(rawMessage instanceof Datapackage) {
				System.out.println("It's a datapackage!");
				Datapackage msg = (Datapackage) rawMessage;
				System.out.println("Executing method for id '" + msg.id() + "'");
				executables.get(msg.id()).execute(msg);
			}			
		}
		
	}
	
	public void sendMessage(String id, Object... o) {
		try {
			System.out.println("Sending message...");
			Datapackage pack = new Datapackage(id, o);
			oos.writeObject(pack);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void registerExecutable(String id, ClientExecutable exe) {
		executables.put(id, exe);
	}
	
	public void unregisterExecutable(String id) {
		executables.remove(id);
	}

}