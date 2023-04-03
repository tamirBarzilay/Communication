package bgu.spl.net.srv;

import java.util.*;
import java.util.concurrent.*;

public class ConnectionsImpl<T> implements Connections<T> {
    public volatile int numOfUsers;
    public volatile int messageId;
    public ConcurrentHashMap<String, String> UserNameDataBase;
    // the other hashmap will hold connectinId and subscriptionId
    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> mappingByTopic;
    public ConcurrentHashMap<Integer, ConnectionHandler<T>> ActiveClients;

    // constructor
    public ConnectionsImpl() {
        numOfUsers = 0;
        messageId = 0;
        UserNameDataBase = new ConcurrentHashMap<String, String>();
        mappingByTopic = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>>();
        ActiveClients = new ConcurrentHashMap<Integer, ConnectionHandler<T>>();
    }

    public synchronized int getNumOfUsers() {
        return numOfUsers;
    }

    public synchronized void NumOfUsersPlusOne() {
        numOfUsers++;
    }

    public synchronized int getMessageId() {
        return messageId;
    }

    public synchronized void increaseMessageId() {
        messageId++;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> getMappingByTopic() {
        return mappingByTopic;
    }

    // finds the active client in the hashmap and calls the send function
    // of the appropriate connection handler
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = ActiveClients.get(connectionId);
        handler.send(msg);
        return true;
    }

    // sends the message to all the clients subscribed to this topic with their
    // subscription id
    // to the topic
    // can he sends to a topic that doesn't exist?????
    public void send(String channel, T msg) {
        ConcurrentHashMap<Integer, Integer> map = mappingByTopic.get(channel);
        Iterator<ConcurrentHashMap.Entry<Integer, Integer>> itr = map.entrySet().iterator();
        while (itr.hasNext()) {
            ConcurrentHashMap.Entry<Integer, Integer> entry = itr.next();
            int connectionId = entry.getKey();
            send(connectionId, msg);
        }
    }

    // delete from active clients list and delete it from every subscribed topic
    public void disconnect(int connectionId) {
        ActiveClients.remove(connectionId);
        // remove user from all topics
        Iterator<ConcurrentHashMap.Entry<String, ConcurrentHashMap<Integer, Integer>>> itr = mappingByTopic.entrySet()
                .iterator();
        while (itr.hasNext()) {
            ConcurrentHashMap.Entry<String, ConcurrentHashMap<Integer, Integer>> entry = itr.next();
            String topic = entry.getKey();
            // map of connectionId and subscrioptionId to a specific topic
            ConcurrentHashMap<Integer, Integer> map = entry.getValue();
            map.remove(connectionId);
            if (map.isEmpty())
                mappingByTopic.remove(topic);
        }
    }

    // adds connection handler to the active client list with an empty username.....
    public void addHandler(int connectionId, ConnectionHandler<T> handler) {
        ActiveClients.put(connectionId, handler);
    }

    // search for a user name in all the users
    public boolean findUserName(String userName) {
        return UserNameDataBase.containsKey(userName);
    }

    // adds a new username and password to the all users database .
    public void addNewUser(String userName, String passcode) {
        UserNameDataBase.put(userName, passcode);

    }

    // search for a username in the active users
    public boolean findActiveUser(String userName) {
        Iterator<ConcurrentHashMap.Entry<Integer, ConnectionHandler<T>>> itr = ActiveClients.entrySet().iterator();
        while (itr.hasNext()) {
            ConcurrentHashMap.Entry<Integer, ConnectionHandler<T>> entry = itr.next();
            ConnectionHandler<T> handler = entry.getValue();
            if (handler.getProtocol().getUserName().equals(userName))
                return true;
        }
        return false;
    }

    // search the username in the big data and check if the pass is correct
    public boolean isCorrectPasscode(String userName, String passcode) {
        return UserNameDataBase.get(userName).equals(passcode);
    }

    // check in topic hashmap if this connectionid is subscribe to this topic
    public boolean isSubscribed(String topic, int connectionId) {
        if (!mappingByTopic.containsKey(topic))
            return false;
        return mappingByTopic.get(topic).containsKey(connectionId);
    }

    // add a new row to the topic hashmap of appropiate
    // topic,connectionId,subscribtionId
    // if there is a request to a new topic, this function has to create one
    public void subscribe(String topic, int connectionId, int subscribtionId) {
        ConcurrentHashMap<Integer, Integer> map = mappingByTopic.get(topic);
        if (map == null) {
            map = new ConcurrentHashMap<Integer, Integer>();
            map.put(connectionId, subscribtionId);
            mappingByTopic.put(topic, map);
        } else {
            map.put(connectionId, subscribtionId);
        }

    }

    // delete a row from topic hashmap
    public void unsubscribe(int connectionId, int subscribtionId) {
        Iterator<ConcurrentHashMap.Entry<String, ConcurrentHashMap<Integer, Integer>>> itr = mappingByTopic.entrySet()
                .iterator();
        boolean removed = false;
        while (itr.hasNext() && !removed) {
            ConcurrentHashMap.Entry<String, ConcurrentHashMap<Integer, Integer>> entry = itr.next();
            ConcurrentHashMap<Integer, Integer> map = entry.getValue();
            if (map.remove(connectionId, subscribtionId)) {
                removed = true;
                if (map.isEmpty())
                    mappingByTopic.remove(entry.getKey());
            }
        }
    }

    /*
     * public static void main(String[] args) {
     * Connections<String> connections = new ConnectionsImpl<>();
     * ConcurrentHashMap<Integer,Integer> map =new
     * ConcurrentHashMap<Integer,Integer>();
     * map.put(1, 2);
     * ConcurrentHashMap<String,ConcurrentHashMap<Integer,Integer>> mapp =
     * connections.getMappingByTopic();
     * mapp.put("omer", map);
     * System.out.println(connections.getMappingByTopic().get("omer").get(1));
     * 
     * }
     */
}
