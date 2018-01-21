package com.blogspot.debukkitsblog.netx.server;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.blogspot.debukkitsblog.netx.Datapackage;

public class RemoteMessageHandler {

	private List<Remote> clients;
	private HashMap<String, ServerExecutable> executables;
	
	public RemoteMessageHandler() {
		this.clients = new ArrayList<>();
		this.executables = new HashMap<>();
	}
	
	public void registerExecutable(String id, ServerExecutable exe) {
		executables.put(id, exe);
	}
	
	public void unregisterExecutable(String id) {
		executables.remove(id);
	}
	
	public synchronized void broadcast(String id, Object... o) {
		List<Remote> toBeDeleted = new ArrayList<>();
		
		// Send message to all clients
		for(Remote r : clients) {
			try {
				r.sendMessage(id, o);
			} catch(Exception e) {
				toBeDeleted.add(r);
			}
		}
		
		// Remove clients which caused errors while broadcasting
		for(Remote deleteMe : toBeDeleted) {
			clients.remove(deleteMe);
		}
	}
	
	public synchronized void removeClient(Remote r) {
		clients.remove(r);
	}
	
	public synchronized void addClient(Remote r) {
		
		clients.add(r);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				while(!Thread.interrupted()) {
					
					try {
						Object rawMessage = r.getOis().readObject();
						if(rawMessage instanceof Datapackage) {
							Datapackage msg = (Datapackage) rawMessage;
							executables.get(msg.id()).execute(r, msg);
						}
					
					} catch(SocketException e) {
						// Client disconnected
						System.err.println("[ClientHandler] Client '" + r.getId() + "': Connection reset");
						removeClient(r);
						break;
					} catch(Exception e) {
						e.printStackTrace();
						removeClient(r);
						break;
					}
					
				}
				
			}			
		}).start();
		
	}
	
}