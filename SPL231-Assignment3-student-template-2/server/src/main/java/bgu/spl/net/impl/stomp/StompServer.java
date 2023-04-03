package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocolImpl;
import bgu.spl.net.srv.Server;

public class StompServer {

  public static void main(String[] args) {
    int port =Integer.parseInt(args[0]);
    if(args[1].equals("tpc")){
    Server.threadPerClient(
      port, // port
        () -> new StompMessagingProtocolImpl(), // protocol factory
        StompMessageEncoderDecoder::new // stomp message encoder decoder factory
    ).serve();
  }
    else if(args[1].equals("reactor")){
    Server.reactor(
        Runtime.getRuntime().availableProcessors(),
        port, // port
        () -> new StompMessagingProtocolImpl(), // protocol factory
        StompMessageEncoderDecoder::new // stomp message encoder decoder factory
    ).serve();
  }
}
}
