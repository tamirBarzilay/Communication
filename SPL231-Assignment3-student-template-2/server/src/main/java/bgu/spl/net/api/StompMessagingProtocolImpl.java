package bgu.spl.net.api;

import bgu.spl.net.srv.Connections;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<String> {
    public boolean loggedIn = false;
    public int connectionId = -1;
    public Connections<String> connections;
    private boolean shouldTerminate = false;
    public String userName = "";

    public void start(int connectionId, Connections<String> connections) {
        this.connections = connections;
        this.connectionId = connectionId;
    }

    public void process(String message) {
        String[] arrOfMessage = message.split("\n", 0);
        String thisMessageForErrorFrame = message.substring(0, message.length() - 2);
        // CONNECT
        if (arrOfMessage[0].equals("CONNECT")) {
            // error check
            String error = ErrorCheck.connectError(arrOfMessage);
            if (!error.equals("")) {
                connections.send(connectionId, "ERROR\nmessage:malformed frame received\n\nThe message:\n-----\n"
                        + thisMessageForErrorFrame + "\n-----\n" + error + "\n");
            } else {// no error in the frame structure
                String userName = arrOfMessage[3].substring(6);
                String passcode = arrOfMessage[4].substring(9);
                // client already logged in
                if (loggedIn) { // not neccecary(took  care of that in the client section)
                    connections.send(connectionId, "The client is already logged in, log out before trying again");
                }
                // new user
                else if (!connections.findUserName(userName)) {
                    connections.addNewUser(userName, passcode);
                    loggedIn = true;
                    this.userName = userName;
                    // send frame connected
                    connections.send(connectionId, "CONNECTED\nversion:1.2\n\n");
                }
                // user is already logged in
                else if (connections.findActiveUser(userName)) {
                    connections.send(connectionId, "ERROR\nmessage:User already logged in\n\nThe message:\n-----\n"
                            + thisMessageForErrorFrame + "\n-----\n\n");
                    // close connection
            
                    connections.disconnect(connectionId);
                    shouldTerminate = true;
                }
                // wrong password
                else if (!connections.isCorrectPasscode(userName, passcode)) {
                    connections.send(connectionId,
                            "ERROR\nmessage:Wrong password\n\nThe message:\n-----\n" + thisMessageForErrorFrame
                                    + "\n-----\n\n");
                    // close connection????
                    connections.disconnect(connectionId);
                    shouldTerminate = true;
                }
                // User exists
                else {
                    // connections.activateUser(connectionId,userName);
                    loggedIn = true;
                    this.userName = userName;
                    connections.send(connectionId, "CONNECTED\nversion:1.2\n\n");
                }

            }

        } else if (!loggedIn) {
            connections.send(connectionId, "ERROR\nmessage:User is not logged in\n\n");
        }
        // SEND FRAME
        else if (arrOfMessage[0].equals("SEND")) {
            // error check
            String error = ErrorCheck.sendError(arrOfMessage);
            if (!error.equals("")) {
                connections.send(connectionId, "ERROR\nmessage:malformed frame received\n\nThe message:\n-----\n"
                        + thisMessageForErrorFrame + "\n-----\n" + error + "\n");
                // close connection
                connections.disconnect(connectionId);
                shouldTerminate = true;
            } else {// no error in the frame structure
                String[] destination = arrOfMessage[1].split("/", 0);
                String topic = destination[1];
                if (!connections.isSubscribed(topic, connectionId)) {
                    connections.send(connectionId,
                            "ERROR\nmessage:User isn't subscribed to this topic\n\nThe message:\n-----\n"
                                    + thisMessageForErrorFrame + "\n-----\n\n");
                    // close connection????
                    connections.disconnect(connectionId);
                    shouldTerminate = true;
                } else {// can he sends to a topic that doesn't exist?????
                    // connections.send(topic, message);
                    ConcurrentHashMap<Integer, Integer> map = (ConcurrentHashMap<Integer, Integer>) connections.getMappingByTopic().get(topic);
                    Iterator<ConcurrentHashMap.Entry<Integer, Integer>> itr = map.entrySet().iterator();
                    while (itr.hasNext()) {
                        ConcurrentHashMap.Entry<Integer, Integer> entry = itr.next();
                        int connectionId = entry.getKey();
                        int subscriptionId = entry.getValue();
                        connections.send(connectionId, (createServerFrameMessage(subscriptionId, message)));
                    }
                }
            }
        }
        // SUBSCRIBE FRAME
        else if (arrOfMessage[0].equals("SUBSCRIBE")) {
            // error check
            String error = ErrorCheck.subscribeError(arrOfMessage);
            if (!error.equals("")) {
                connections.send(connectionId, "ERROR\nmessage:malformed frame received\n\nThe message:\n-----\n"
                        + thisMessageForErrorFrame + "\n-----\n" + error + "\n");
                // close connection????
                connections.disconnect(connectionId);
                shouldTerminate = true;
            } else {// no error in the frame structure
                String[] destination = arrOfMessage[1].split("/", 0);
                String topic = destination[1];
                int subscribtionId = Integer.parseInt(arrOfMessage[2].substring(3));
                // System.out.println("sub id:"+subscribtionId);test
                String receiptId = arrOfMessage[3].substring(8);
                if (!connections.isSubscribed(topic, connectionId)) {
                    connections.subscribe(topic, connectionId, subscribtionId);
                    connections.send(connectionId, "RECEIPT\nreceipt-id:" + receiptId + "\n\n");
                } else
                    connections.send(connectionId, "The user is already subscribed to this topic");
            }
        }
        // UNSUBSCRIBE FRAME
        else if (arrOfMessage[0].equals("UNSUBSCRIBE")) {
            // error check
            String error = ErrorCheck.unsubscribeError(arrOfMessage);
            if (!error.equals("")) {
                connections.send(connectionId, "ERROR\nmessage:malformed frame received\n\nThe message:\n-----\n"
                        + thisMessageForErrorFrame + "\n-----\n" + error + "\n");
                // close connection????
                connections.disconnect(connectionId);
                shouldTerminate = true;
            } else {// no error in the frame structure
                int subscribtionId = Integer.parseInt(arrOfMessage[1].substring(3));
                String receiptId = arrOfMessage[2].substring(8);
                connections.unsubscribe(connectionId, subscribtionId);
                connections.send(connectionId, "RECEIPT\nreceipt-id:" + receiptId + "\n\n");
            }
        }
        // DISCONNECT FRAME
        else if (arrOfMessage[0].equals("DISCONNECT")) {
            // error check
            String error = ErrorCheck.disconnectError(arrOfMessage);
            if (!error.equals("")) {
                connections.send(connectionId, "ERROR\nmessage:malformed frame received\n\nThe message:\n-----\n"
                        + thisMessageForErrorFrame + "\n-----\n" + error + "\n");
                // close connection????
                connections.disconnect(connectionId);
                shouldTerminate = true;
            } else {// no error in the frame structure
                String receiptId = arrOfMessage[1].substring(8);
                // close connection????
                connections.send(connectionId, "RECEIPT\nreceipt-id:" + receiptId + "\n\n");
                //// client needs to wait until client gets reciept
                connections.disconnect(connectionId);
                shouldTerminate = true;

            }
        } else {// wrong frame type
            connections.send(connectionId,
                    "ERROR\nmessage:Wrong frame type\n\nThe message:\n-----\n" + thisMessageForErrorFrame
                            + "\n-----\n\n");
            // close connection????
            connections.disconnect(connectionId);
            shouldTerminate = true;
        }
    }

    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public String getUserName() {
        return userName;
    }

    public String createServerFrameMessage(Integer subscriptionId, String msg) {
        int messageId = connections.getMessageId();
        connections.increaseMessageId();
        String newMessage = "MESSAGE\nsubscription:" + subscriptionId + "\nmessage-id:"+ messageId + "\n" +
                ((String) msg).substring(5);
        return newMessage;
    }

    public void callDisconnectFromConnections(){
        connections.disconnect(connectionId);
    }
}
