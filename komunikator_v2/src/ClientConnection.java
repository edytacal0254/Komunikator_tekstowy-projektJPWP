import java.io.IOException;
import java.net.Socket;

public class ClientConnection {
    private Socket socket;
    private boolean isConnected;

    private Thread sendMsgT;
    private Thread readMsgT;

    public ClientConnection(Socket s, Thread readMsgThread, Thread sendMsgThread) throws IOException {
        this.setConnected(true);
        this.setSocket(s);
        this.setReadMsgT(readMsgThread);
        this.setSendMsgT(sendMsgThread);

        this.getReadMsgT().start();
        this.getSendMsgT().start();


    }

    public void close() throws IOException {
        System.out.println("<CLIENT> Connection closed.");
        setConnected(false);
        getReadMsgT().interrupt();
        getSendMsgT().interrupt();
        getSocket().close();

    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public Thread getSendMsgT() {
        return sendMsgT;
    }

    public void setSendMsgT(Thread sendMsgT) {
        this.sendMsgT = sendMsgT;
    }

    public Thread getReadMsgT() {
        return readMsgT;
    }

    public void setReadMsgT(Thread readMsgT) {
        this.readMsgT = readMsgT;
    }
}
