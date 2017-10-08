package com.blogspot.debukkitsblog.net;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A Datapackage contains the data transmitted and received by
 * SimpleServerClient Clients and Servers
 * 
 * @author Leonard Bienbeck
 * @version 2.3.0
 */
public class Datapackage extends ArrayList<Object> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7086684098959626540L;

	/**
	 * Constructs a Datapackage consisting of an ID used by the remote application
	 * to identify the package and payload data
	 * 
	 * @param id
	 *            The ID used by the remote application to identify the package
	 * @param o
	 *            The payload
	 */
	public Datapackage(String id, Object... o) {
		this.add(0, id);
		for (Object current : o) {
			this.add(current);
		}
	}

	/**
	 * Returns the ID of the package. The Datapackage can be identified with this.
	 * 
	 * @return The ID of the package
	 */
	public String id() {
		if (!(this.get(0) instanceof String)) {
			throw new IllegalArgumentException("Identifier of Datapackage is not a String");
		}
		return (String) this.get(0);
	}

	/**
	 * Returns the Datapackage as ArrayList, containing the Datapackage's ID at
	 * index 0 and the payload from index 1 to the end. This method is redundant
	 * since Datapackage extends ArrayList.
	 * 
	 * @return The Datapackage itself.
	 */
	@Deprecated
	public ArrayList<Object> open() {
		return this;
	}

}