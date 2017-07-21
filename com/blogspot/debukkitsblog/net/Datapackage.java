package com.blogspot.debukkitsblog.net;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Datapackage implements Serializable {

    private static final long serialVersionUID = 5816841623141578069L;

    private ArrayList<Object> memory;
    private String clientid;
    private String id;

    /**
     * Creats a serializable Datapackage consiting of an identifier for identification (head)<br>
     * and lots of objects in its body.<br>
     * Alle the data including the identifier is stores in an ArrayList of Objects,<br>
     * where index=0 is the identifier and index>=1 is the data.
     * @param clientid The id of the client that deliveres the package
     * @param id The identifier for later identification and separation
     * @param o The contents of the package (lots of Objects)
     */
    public Datapackage(String clientid, String id, Object... o){		
        memory = new ArrayList<>();
        this.clientid = clientid;
        this.id = id;
        memory.addAll(Arrays.asList(o));
    }

    /**
     * @return the identifier of the datapackage
     */
    public String id(){
        return id;
    }

    /**
     * @return the identifier of the datapackage
     */
    public String clientid(){
        return clientid;
    }

    /**
     * @param i index of the element of the corresponding ArrayList of objects to be returned
     * @return The element of the corresponding ArrayList of objects with the given index <i>i</i>
     */
    public Object get(int i){
        return memory.get(i);
    }

    /**
     * @return the whole corresponding ArrayList of objects laying behind this class, including the identifier (index=0)
     */
    public ArrayList<Object> open(){
        return memory;
    }

    /**
     * returns the String-representation of the corresponding ArrayList of objects laying behind this class: [id, data1, data2, ...]
     */
    @Override
    public String toString() {
        return memory.toString();
    }

}
