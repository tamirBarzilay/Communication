package bgu.spl.net.api;

public class ErrorCheck {
    
    public static String connectError(String[] arrayOfMessage){
        if(!arrayOfMessage[1].replaceAll("\\s", "").equals("accept-version:1.2"))
            return "Didn't contain a version header or wrong version";
        if(!arrayOfMessage[2].replaceAll("\\s", "").equals("host:stomp.cs.bgu.ac.il"))
            return "Didn't contain a host header or wrong host"; 
        String  [] login=arrayOfMessage[3].split(":",0);
        if(!login[0].replaceAll("\\s", "").equals("login"))
            return "Didn't contain a login header";
        String  [] pass=arrayOfMessage[4].split(":",0); 
        if(!pass[0].replaceAll("\\s", "").equals("passcode"))
            return "Didn't contain a passcode header";       
        return "";
    }
    public static String sendError(String[] arrayOfMessage){
        String  [] dest=arrayOfMessage[1].split(":",0);
        if(!dest[0].replaceAll("\\s", "").equals("destination"))
            return "Didn't contain a destination header";
        return "";
    }
    public static String subscribeError(String[] arrayOfMessage){
        String  [] dest=arrayOfMessage[1].split(":",0);
        if(!dest[0].replaceAll("\\s", "").equals("destination"))
            return "Didn't contain a destination header";
        String  [] id=arrayOfMessage[2].split(":",0);
        if(!id[0].replaceAll("\\s", "").equals("id"))
                return "Didn't contain an id header";
        return "";
    }
    public static String unsubscribeError(String[] arrayOfMessage){
        String  [] id=arrayOfMessage[1].split(":",0);
        if(!id[0].replaceAll("\\s", "").equals("id"))
            return "Didn't contain an id header";
        return "";
    }
    public static String disconnectError(String[] arrayOfMessage){
        String  [] receipt=arrayOfMessage[1].split(":",0);
        if(!receipt[0].replaceAll("\\s", "").equals("receipt"))
            return "Didn't contain a receipt header";
        return "";
    }

}
