import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientConnection {
    Socket socket;
    boolean isConnected;

    DataInputStream dis;
    DataOutputStream dos;

    Thread sendMsgT;
    Thread readMsgT;

    public ClientConnection(Socket s, Thread readMsgThread, Thread sendMsgThread) throws IOException {
        this.isConnected = true;
        this.socket = s;
        this.readMsgT = readMsgThread;
        this.sendMsgT = sendMsgThread;
    }

    public void close() throws IOException {
        isConnected = false;
        readMsgT.interrupt();
        sendMsgT.interrupt();
        socket.close();
        System.out.println("<CLIENT> Connection closed.");
    }
}
