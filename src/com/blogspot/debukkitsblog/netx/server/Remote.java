package com.blogspot.debukkitsblog.netx.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.blogspot.debukkitsblog.netx.Datapackage;

public class Remote {
	
	private String id;
	private String group;
	
	private Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	
	public Remote(Datapackage loginPackage, Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
		
		if(loginPackage.id().equalsIgnoreCase("__INTERNAL_LOGIN__")) {
			this.id = (String) loginPackage.get(1);
			this.group = (String) loginPackage.get(2);
		}
		
		this.socket = socket;
		this.socket = socket;
		this.ois = ois;
		this.oos = oos;
	}

	public String getId() {
		return id;
	}

	public String getGroup() {
		return group;
	}

	public Socket getSocket() {
		return socket;
	}

	public ObjectOutputStream getOos() {
		return oos;
	}

	public ObjectInputStream getOis() {
		return ois;
	}
	
	public synchronized void sendMessage(String id, Object... o) {
		try {
			Datapackage pack = new Datapackage(id, o);
			oos.writeObject(pack);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Remote) {
			Remote other = (Remote) obj;
			return other.getId().equals(this.getId());
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "[" + id + " (Gr: " + group + ") @ " + socket.getRemoteSocketAddress() + "]";
	}

}
