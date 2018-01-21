package com.blogspot.debukkitsblog.netx;

import com.blogspot.debukkitsblog.netx.client.Client;
import com.blogspot.debukkitsblog.netx.client.ClientExecutable;

public class StartBiDiClient {
	
	public static void main(String[] args) {
		
		System.out.println("=== BiDi TestClient ===");
		
		Client client = new Client("192.168.178.38", 8612);
		client.registerExecutable("PONG", new ClientExecutable() {
			@Override
			public void execute(Datapackage message) {
				System.err.println(System.currentTimeMillis() - (Long) message.get(1));
			}			
		});
		
		client.start();
		
		System.out.println("Ready.");
		
		try { Thread.sleep(5000); } catch (InterruptedException e) {}
		
		for(int i = 0; i < 50; i++) {
			client.sendMessage("PING", (Long) System.currentTimeMillis());
		}
	}
	
}
