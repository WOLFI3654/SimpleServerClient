package com.blogspot.debukkitsblog.netx.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.blogspot.debukkitsblog.netx.Datapackage;

public class Server {
	
	private int port;
	private RemoteMessageHandler handler;
	
	
	public Server(int port, RemoteMessageHandler handler) {
		this.port = port;
		this.handler = handler;
		
		start();
	}
	
	public void broadcast(String id, Object... o) {
		handler.broadcast(id, o);
	}
	
	private void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				while(true) {
					
					ServerSocket server = null;
					
					try {
						
						server = new ServerSocket(port);
						
						while(true) {
							
							try {
								// Wait for new client
								Socket tempRemoteSocket = server.accept();
								
								// Build connection channels to and from client
								ObjectInputStream ois = new ObjectInputStream(tempRemoteSocket.getInputStream());
								ObjectOutputStream oos = new ObjectOutputStream(tempRemoteSocket.getOutputStream());
								Object rawMessage = ois.readObject();
								
								// Register client
								if (rawMessage instanceof Datapackage) {
									Datapackage message = (Datapackage) rawMessage;
									handler.addClient(new Remote(message, tempRemoteSocket, ois, oos));
								}

							} catch (Exception e) {
								e.printStackTrace();
							}
							
						}
						
					} catch(Exception e) {
						if(server != null) {
							try {
								server.close();
							} catch(IOException e2) {
								e.printStackTrace();
							}
						}
						e.printStackTrace();
					}
					
				}
				
			}			
		}).start();
	}

}