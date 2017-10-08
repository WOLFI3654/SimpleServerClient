package com.blogspot.debukkitsblog.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.AlreadyConnectedException;
import java.util.HashMap;
import java.util.UUID;

import javax.net.ssl.SSLSocketFactory;

/**
 * A very simple Client class for Java network applications<br>
 * originally created on March 9, 2016 in Horstmar, Germany
 * 
 * @author Leonard Bienbeck
 * @version 2.3.0
 */
public class Client {

	private String id;
	private String group;

	private Socket loginSocket;
	private InetSocketAddress address;
	private int timeout;

	private Thread listeningThread;
	private HashMap<String, Executable> idMethods = new HashMap<String, Executable>();

	private int errorCount;

	private boolean autoKill;
	private boolean secureMode;
	private boolean muted;

	/**
	 * Constructs a simple client with just a hostname and port to connect to
	 * 
	 * @param hostname
	 *            The hostname to connect to
	 * @param port
	 *            The port to connect to
	 */
	public Client(String hostname, int port) {
		this(hostname, port, 10000, false, false, UUID.randomUUID().toString(), "_DEFAULT_GROUP_");
	}

	/**
	 * Constructs a simple client with a hostname and port to connect to and an id
	 * the server uses to identify this client in the future (e.g. for sending
	 * messages only this client should receive)
	 * 
	 * @param hostname
	 *            The hostname to connect to
	 * @param port
	 *            The port to connect to
	 * @param id
	 *            The id the server may use to identify this client
	 */
	public Client(String hostname, int port, String id) {
		this(hostname, port, 10000, false, false, id, "_DEFAULT_GROUP_");
	}

	/**
	 * Constructs a simple client with a hostname and port to connect to, an id the
	 * server uses to identify this client in the future (e.g. for sending messages
	 * only this client should receive) and a group name the server uses to identify
	 * this and some other clients in the future (e.g. for sending messages to the
	 * members of this group, but no other clients)
	 * 
	 * @param hostname
	 *            The hostname to connect to
	 * @param port
	 *            The port to connect to
	 * @param id
	 *            The id the server may use to identify this client
	 * @param group
	 *            The group name the server may use to identify this and similar
	 *            clients
	 */
	public Client(String hostname, int port, String id, String group) {
		this(hostname, port, 10000, false, false, id, group);
	}

	/**
	 * Constructs a simple client with all possible configurations
	 * 
	 * @param hostname
	 *            The hostname to connect to
	 * @param port
	 *            The port to connect to
	 * @param timeout
	 *            The timeout after a connection attempt will be given up
	 * @param autoKill
	 *            Whether the program should exit after 30 failed connection
	 *            attempts
	 * @param useSSL
	 *            Whether a secure SSL connection should be used
	 * @param id
	 *            The id the server may use to identify this client
	 * @param group
	 *            The group name the server may use to identify this and similar
	 *            clients
	 */
	public Client(String hostname, int port, int timeout, boolean autoKill, boolean useSSL, String id, String group) {
		this.id = id;
		this.group = group;

		this.errorCount = 0;
		this.address = new InetSocketAddress(hostname, port);
		this.timeout = timeout;
		this.autoKill = autoKill;

		if (secureMode = useSSL) {
			System.setProperty("javax.net.ssl.trustStore", "ssc.store");
			System.setProperty("javax.net.ssl.keyStorePassword", "SimpleServerClient");
		}
	}

	/**
	 * Mutes the console output of this instance, errors will still be printed
	 * 
	 * @param muted
	 *            true if there should be no console output, except error messages
	 */
	public void setMuted(boolean muted) {
		this.muted = muted;
	}

	/**
	 * Starts the client. This will cause a connection attempt, a login on the
	 * server and the start of a new listening thread (both to receive messages and
	 * broadcasts from the server)
	 */
	public void start() {
		login();
		startListening();
	}

	/**
	 * Called to repair the connection if it is lost
	 */
	private void repairConnection() {
		if (!muted)
			System.out.println("[Client-Connection-Repair] Repairing connection...");
		if (loginSocket != null) {
			try {
				loginSocket.close();
			} catch (IOException e) {
			}
			loginSocket = null;
		}

		login();
		startListening();
	}

	/**
	 * Logs in to the server to receive messages and broadcasts from the server
	 * later
	 */
	private void login() {
		// Verbindung herstellen
		try {
			if (!muted)
				System.out.println("[Client] Connecting" + (secureMode ? " using SSL..." : "..."));
			if (loginSocket != null && loginSocket.isConnected()) {
				throw new AlreadyConnectedException();
			}

			if (secureMode) {
				loginSocket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(address.getAddress(),
						address.getPort());
			} else {
				loginSocket = new Socket();
				loginSocket.connect(this.address, this.timeout);
			}

			if (!muted)
				System.out.println("[Client] Connected to " + loginSocket.getRemoteSocketAddress());
		} catch (IOException ex) {
			ex.printStackTrace();
			onConnectionProblem();
		}

		// Einloggen
		try {
			if (!muted)
				System.out.println("[Client] Logging in...");
			ObjectOutputStream out = new ObjectOutputStream(loginSocket.getOutputStream());
			out.writeObject(new Datapackage("_INTERNAL_LOGIN_", id, group));
			if (!muted)
				System.out.println("[Client] Logged in.");
			onReconnect();
		} catch (IOException ex) {
			System.err.println("[Client] Login failed.");
		}
	}

	/**
	 * Starts a new thread listening for messages from the server. A message will
	 * only be processed if a handler for its identifier has been registered before
	 * using <code>registerMethod(String identifier, Executable executable)</code>
	 */
	private void startListening() {

		// Wenn der ListeningThread lebt, nicht neu starten!
		if (listeningThread != null && listeningThread.isAlive()) {
			return;
		}

		listeningThread = new Thread(new Runnable() {
			@Override
			public void run() {

				// Ständig wiederholen:
				while (true) {
					try {
						// Bei fehlerhafter Verbindung, diese reparieren
						if (loginSocket != null && !loginSocket.isConnected()) {
							while (!loginSocket.isConnected()) {
								repairConnection();
								if (loginSocket.isConnected()) {
									break;
								}

								Thread.sleep(5000);
								repairConnection();
							}
						}

						onConnectionGood();

						// Auf eingehende Nachricht warten und diese bei Eintreffen lesen
						ObjectInputStream ois = new ObjectInputStream(loginSocket.getInputStream());
						Object raw = ois.readObject();

						// Nachricht auswerten
						if (raw instanceof Datapackage) {
							final Datapackage msg = (Datapackage) raw;

							for (final String current : idMethods.keySet()) {
								if (msg.id().equalsIgnoreCase(current)) {
									if (!muted)
										System.out.println("[Client] Message received. Executing method for '"
												+ msg.id() + "'...");
									new Thread(new Runnable() {
										public void run() {
											idMethods.get(current).run(msg, loginSocket);
										}
									}).start();
									break;
								}
							}

						}

					} catch (Exception ex) {
						ex.printStackTrace();
						onConnectionProblem();
						System.err.println("Server offline?");
						if ((++errorCount > 30) && autoKill) {
							System.err.println("Server dauerhaft nicht erreichbar, beende.");
							System.exit(0);
						} else {
							repairConnection();
						}
					}

					// Bis hieher fehlerfrei? Dann errorCount auf Null setzen:
					errorCount = 0;

				} // while true

			}// run
		});

		// Thread starten
		listeningThread.start();
	}

	/**
	 * Sends a message to the server using a brand new socket and returns the
	 * server's response
	 * 
	 * @param message
	 *            The message to send to the server
	 * @param timeout
	 *            The timeout after a connection attempt will be given up
	 * @return The server's response. The identifier of this Datapackage should be
	 *         "REPLY" by default, the rest is custom data.
	 */
	public Datapackage sendMessage(Datapackage message, int timeout) {
		try {
			Socket tempSocket;
			if (secureMode) {
				tempSocket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(address.getAddress(),
						address.getPort());
			} else {
				tempSocket = new Socket();
				tempSocket.connect(address, timeout);
			}

			ObjectOutputStream tempOOS = new ObjectOutputStream(tempSocket.getOutputStream());
			tempOOS.writeObject(message);

			ObjectInputStream tempOIS = new ObjectInputStream(tempSocket.getInputStream());
			Object raw = tempOIS.readObject();

			tempOOS.close();
			tempOIS.close();
			tempSocket.close();

			if (raw instanceof Datapackage) {
				return (Datapackage) raw;
			}
		} catch (Exception ex) {
			System.err.println("[Client] Error while sending message:");
			ex.printStackTrace();
		}

		return null;
	}

	/**
	 * Sends a message to the server using a brand new socket and returns the
	 * server's response
	 * 
	 * @param ID
	 *            The ID of the message, allowing the server to decide what to do
	 *            with its content
	 * @param content
	 *            The content of the message
	 * @return The server's response. The identifier of this Datapackage should be
	 *         "REPLY" by default, the rest is custom data.
	 */
	public Datapackage sendMessage(String ID, String... content) {
		return sendMessage(new Datapackage(ID, (Object[]) content));
	}

	/**
	 * Sends a message to the server using a brand new socket and returns the
	 * server's response
	 * 
	 * @param message
	 *            The message to send to the server
	 * @return The server's response. The identifier of this Datapackage should be
	 *         "REPLY" by default, the rest is custom data.
	 */
	public Datapackage sendMessage(Datapackage message) {
		return sendMessage(message, this.timeout);
	}

	/**
	 * Registers a method that will be executed if a message containing
	 * <i>identifier</i> is received
	 * 
	 * @param identifier
	 *            The ID of the message to proccess
	 * @param executable
	 *            The method to be called when a message with <i>identifier</i> is
	 *            received
	 */
	public void registerMethod(String identifier, Executable executable) {
		idMethods.put(identifier, executable);
	}

	/**
	 * Called on the listener's main thread when there is a problem with the
	 * connection. Overwrite this method when extending this class.
	 */
	public void onConnectionProblem() {
		// Overwrite this method when extending this class
	}

	/**
	 * Called on the listener's main thread when there is no problem with the
	 * connection and everything is fine. Overwrite this method when extending this
	 * class.
	 */
	public void onConnectionGood() {
		// Overwrite this method when extending this class
	}

	/**
	 * Called on the listener's main thread when the client logs in to the server.
	 * This happens on the first and every further login (e.g. after a
	 * re-established connection). Overwrite this method when extending this class.
	 */
	public void onReconnect() {
		// Overwrite this method when extending this class
	}

}