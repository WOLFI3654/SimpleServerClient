package com.blogspot.debukkitsblog.net;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.IllegalBlockingModeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLServerSocketFactory;

/**
 * A very simple-to-use Server class for Java network applications<br>
 * originally created on 09.03.2016 in Horstmar, NRW, Germany
 * 
 * @author Leonard Bienbeck 
 * @version 2.1.1
 */
public abstract class Server {
    
    private HashMap<String, Executable> idMethods = new HashMap<String, Executable>();
    
    private ServerSocket server;
    private int port;
    private HashMap<String, Socket> clients;
    private ArrayList<String> toBeDeleted;
    
    private Thread listeningThread;
    
    private boolean autoRegisterEveryClient;
    private boolean secureMode;
    private final int pingtime;
    
    private boolean muted;
    
    /**
     * Executed the preStart()-Method,<br>
     * creates a Server on <i>port</i><br>
     * and starts the listening loop on its own thread.<br>
     * <br>
     * The servers stores every client connecting in a list<br>
     * to make them reachable using <i>broadcastMessage()</i> method.
     * The connecting to server will be kept alive by<br>
     * sending a little datapackage every 30 seconds.
     * The connecting will be encrypted using SSL (beta stage!)
     * @param port 
     *          The port the Server shall work on
     */
    public Server(int port) {
        this(port, true, true, true, 30000);
    }

    /**
     * Executed the preStart()-Method,<br>
     * creates a Server on <i>port</i><br>
     * and starts the listening loop on its own thread.
     * 
     * @param port
     *            the server shall work on
     * @param autoRegisterEveryClient
     *            whether every clients connecting<br>
     *            shall be registered or not
     * @param keepConnectionAlive
     *            whether the server shall try everything to keep the<br>
     *            connection alive by sending a little datapackage every 30
     *            seconds
     * @param useSSL
     *            whether SSL should be used to encrypt communication
     * @param pingtime
     *            Time between the pings to check the connections
     */
    public Server(int port, boolean autoRegisterEveryClient, boolean keepConnectionAlive, boolean useSSL, int pingtime) {
        clients = new HashMap<>();
        this.port = port;
        this.autoRegisterEveryClient = autoRegisterEveryClient;
        this.muted = false;
        this.pingtime = pingtime;
        if (secureMode = useSSL) {
            System.setProperty("javax.net.ssl.keyStore", "ssc.store");
            System.setProperty("javax.net.ssl.keyStorePassword", "SimpleServerClient");
        }
        if (autoRegisterEveryClient) {
            registerLoginMethod();
        }
        preStart();
        
        start();
        
        if (keepConnectionAlive) {
            startPingThread();
        }
    }

    /**
     * Sets whether the server shall print information or not. Exception and
     * errors are always printed.
     * 
     * @param muted
     *            <b>true</b> for no debug output at all, <b>false</b> for all
     *            output. Default is false (and all output printed).
     */
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    /**
     * Executed while constructing the Server instance,<br>
     * just before listening to data from the network starts.
     */
    public abstract void preStart();

    /**
     * Overwrite this method to react on a client registered (logged in)<br>
     * to the server. That happens always, when a Datapackage<br>
     * with identifier <i>_INTERNAL_LOGIN_</i> is received from a client.
     */
    public void onClientRegistered() {
        // Overwrite this method when extending this class
    }

    /**
     * Overwrite this method to react on a client registered (logged in)<br>
     * to the server. That happens always, when a Datapackage<br>
     * with identifier <i>_INTERNAL_LOGIN_</i> is received from a client.
     * @param pack 
     *          The Datapackage of the CLient that connected
     * @param socket 
     *          The corresbonding Socket
     */
    public void onClientRegistered(Datapackage pack, Socket socket) {
        // Overwrite this method when extending this class
    }

    /**
     * Called whenever a bad or erroneous socket is removed<br>
     * from the ArrayList of registered sockets.
     * 
     * @param clientid
     *            The Client that has been removed from the list
     * @param socket
     *            The Socket of the client
     */
    public void onClientRemoved(String clientid, Socket socket) {
        // Overwrite this method when extending this class
    }

    /**
     * Starts a thread pinging every client every 'pingtime' seconds to keep the
     * connection alive.
     */
    private void startPingThread() {
        new Thread(() ->
        {
            while (server != null) {
                try {
                    Thread.sleep(pingtime);
                } catch (InterruptedException e) {}
                broadcastMessage("_INTERNAL_PING_", "OK");
            }
        }).start();
    }

    /**
     * Starts a thread listening for incoming connections and Datapackages by
     * registered and non-registered clients
     */
    private void startListening() {
        if (listeningThread == null && server != null) {
            listeningThread = new Thread(() ->
            {
                while (server != null) {
                    
                    try {
                        if (!muted) System.out.println("[Server] Waiting for connection" + (secureMode ? " using SSL..." : "..."));
                        final Socket tempSocket = server.accept();
                        
                        ObjectInputStream ois = new ObjectInputStream(tempSocket.getInputStream());
                        Object raw = ois.readObject();
                        
                        if (raw instanceof Datapackage) {
                            final Datapackage msg = (Datapackage) raw;
                            if (!muted) System.out.println("[Server] Message received: " + msg);
                            
                            INNER_LOOP:
                            for (final String current : idMethods.keySet()) {
                                if (msg.id().equalsIgnoreCase(current)) {
                                    if (!muted) System.out.println("[Server] Executing method for identifier '" + msg.id() + "'");
                                    new Thread(() ->
                                    {
                                        idMethods.get(current).run(msg, tempSocket);
                                    }).start();
                                    break;
                                }
                            }
                        }
                    } catch (EOFException e) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
                    } catch (IllegalBlockingModeException | ClassNotFoundException | IOException e) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            });
            listeningThread.start();
        }
    }

    /**
     * Sends a Datapackage to a Socket
     * 
     * @param clientid
     *            The ID of the Client the Datapackage shall be delivered to
     * @param ID
     *            The identifier for later identification
     * @param content
     *            The content to deliver
     */
    public synchronized void sendMessage(String clientid, String ID, Object... content) {
        sendPackage(new Datapackage("Server", ID, content), clientid);
    }

    /**
     * Sends a Datapackage to a Socket
     * 
     * @param pack
     *            The Datapackage to be delivered
     * @param clientid
     *            The ID of the Client the Datapackage shall be delivered to
     */
    private synchronized void sendPackage(Datapackage pack, String clientid) {
        try {
            // Nachricht senden
            if (!clients.get(clientid).isConnected()) {
                throw new Exception("Client not connected.");
            }
            ObjectOutputStream out = new ObjectOutputStream(clients.get(clientid).getOutputStream());
            out.writeObject(pack);
        } catch (Exception e) {
            System.err.println("[SendMessage] Fehler: " + e.getMessage());

            // Bei Fehler: Socket aus Liste loeschen
            if (toBeDeleted != null) {
                toBeDeleted.add(clientid);
            } else {
                onClientRemoved(clientid, clients.get(clientid));
                clients.remove(clientid);
            }
        }
    }

    /**
     * Broadcasts a Datapackage to every single logged-in socket,<br>
     * one after another on the calling thread.<br>
     * Every erroneous (unreachable etc.) socket is being removed in the end
     * 
     * @param ID 
     *          The identifier for later identification of different Datapackages
     * @param content 
     *          The content to deliver
     * @return The number of reachable the Datapackage has been delivered to
     */
    public synchronized int broadcastMessage(String ID, Object... content) {
        toBeDeleted = new ArrayList<>();
        
        // Nachricht an alle Sockets senden
        for (String current : clients.keySet()) {
            sendPackage(new Datapackage("Server", ID, content), current);
        }
        
        // Alle Sockets, die fehlerhaft waren, im Anschluss loeschen
        for (String current : toBeDeleted) {
            onClientRemoved(current, clients.get(current));
            clients.remove(current);
        }
        
        toBeDeleted = null;
        
        return clients.size();
    }

    /**
     * Registers an Executable to be executed by the server<br>
     * on an incoming Datapackage has <i>identifier</i> as its identifier.
     * 
     * @param identifier
     *            The identifier the Executable is triggered by
     * @param executable
     *            The Executable to be executed on arriving identifier
     */
    public void registerMethod(String identifier, Executable executable) {
        if (identifier.equalsIgnoreCase("_INTERNAL_LOGIN_") && autoRegisterEveryClient) {
            throw new IllegalArgumentException("Identifier may not be '_INTERNAL_LOGIN_'. "
                                            + "Since v1.0.1 the server automatically registers new clients. "
                                            + "To react on new client registed, use the onClientRegisters() Listener by overwriting it.");
        } else {
            idMethods.put(identifier, executable);
        }
    }

    private void registerLoginMethod() {
        idMethods.put("_INTERNAL_LOGIN_", (Executable) (Datapackage pack, Socket socket) ->
        {
            registerClient(pack.clientid(), socket);
            onClientRegistered(pack, socket);
            onClientRegistered();
        });
    }

    /**
     * Registers a new client. From now on this Socket will receive broadcast
     * messages.
     * 
     * @param clientid
     *            The id of the Client
     * @param newClientSocket
     *            The Socket to be registerd
     */
    public synchronized void registerClient(String clientid, Socket newClientSocket) {
        clients.put(clientid, newClientSocket);
    }

    private void start() {
        server = null;
        try {

            if(secureMode){
                server = ((SSLServerSocketFactory) SSLServerSocketFactory.getDefault()).createServerSocket(port);
            } else {			
                server = new ServerSocket(port);
            }

        } catch (IOException e) {
            System.err.println("Error opening ServerSocket");
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
        }
        startListening();
    }

    /**
     * Interrupts the listening thread and closes the server
     */
    public void stop() {
        if (listeningThread.isAlive()) {
            listeningThread.interrupt();
        }
        
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }
    
    /**
     * @return The number of connected clients
     */
    public synchronized int getClientCount() {
        return clients.size();
    }
    
    /**
     * @return The client ids
     */
    public synchronized Set getClientIDs() {
        return clients.keySet();
    }
}
