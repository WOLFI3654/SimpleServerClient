package com.blogspot.debukkitsblog.netx;

import com.blogspot.debukkitsblog.netx.server.RemoteMessageHandler;
import com.blogspot.debukkitsblog.netx.server.ServerExecutable;
import com.blogspot.debukkitsblog.netx.server.Remote;
import com.blogspot.debukkitsblog.netx.server.Server;

public class StartBiDiServer {

	public static void main(String[] args) {
		
		System.out.println("=== BiDi Test-Server ===");
		
		RemoteMessageHandler rmh = new RemoteMessageHandler();
		rmh.registerExecutable("PING", new ServerExecutable() {			
			@Override
			public void execute(Remote remote, Datapackage message) {
				remote.sendMessage("PONG", message.get(1));
			}
		});
		
		Server server = new Server(8612, rmh);
		
		System.out.println("Ready.");
	}

}