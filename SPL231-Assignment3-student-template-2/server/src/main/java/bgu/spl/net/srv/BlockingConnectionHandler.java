package bgu.spl.net.srv;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    public final MessagingProtocol<T> protocol;// private to public**
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, MessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { // just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    /* T response = */protocol.process(nextMessage);
                    /*
                     * if (response != null) {
                     * out.write(encdec.encode(response));
                     * out.flush();
                     * }
                     */
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        protocol.callDisconnectFromConnections();//remove handler from active clients list
        connected = false;
        sock.close();
    }

    @Override
    public void send(T msg) {
        try{
          if(msg!=null){
            out.write(encdec.encode(msg));
            out.flush();
        }
    }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public MessagingProtocol<T> getProtocol() {
        return protocol;
    }
}
