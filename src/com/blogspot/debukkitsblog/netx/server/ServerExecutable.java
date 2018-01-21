package com.blogspot.debukkitsblog.netx.server;

import com.blogspot.debukkitsblog.netx.Datapackage;

public interface ServerExecutable {

	public void execute(Remote remote, Datapackage message);
	
}
