import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientConnection {
    Socket socket;
    boolean isConnected;

    Thread sendMsgT;
    Thread readMsgT;

    public ClientConnection(Socket s, Thread readMsgThread, Thread sendMsgThread) throws IOException {
        this.isConnected = true;
        this.socket = s;
        this.readMsgT = readMsgThread;
        this.sendMsgT = sendMsgThread;

        this.readMsgT.start();
        this.sendMsgT.start();


    }

    public void close() throws IOException {
        System.out.println("<CLIENT> Connection closed.");
        isConnected = false;
        readMsgT.interrupt();
        sendMsgT.interrupt();
        socket.close();

    }
}
