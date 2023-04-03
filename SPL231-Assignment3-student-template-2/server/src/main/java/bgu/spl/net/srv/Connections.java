package bgu.spl.net.srv;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public interface Connections<T> {
    boolean send(int connectionId, T msg);

    void send(String channel, T msg);

    //String createServerFrameMessage(Integer subscriptionId, T msg);

    void disconnect(int connectionId);

    int getNumOfUsers();
    
    void NumOfUsersPlusOne();

    int getMessageId();

    void increaseMessageId();

    ConcurrentHashMap<String,ConcurrentHashMap<Integer,Integer>> getMappingByTopic();
    
    void addNewUser( String userName,String passcode);

    boolean findUserName(String userName);

    boolean findActiveUser(String userName);

    boolean isCorrectPasscode(String userName,String passcode);

    //void activateUser(int connectionId, String userName);

    boolean isSubscribed(String topic,int connectionId);

    void subscribe(String topic,int connectionId,int subscribtionId);

    void unsubscribe(int connectionId,int subscribtionId);
    
    void addHandler(int connectionId,ConnectionHandler<T> handler);

}
