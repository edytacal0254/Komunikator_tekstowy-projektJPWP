import java.io.IOException;
import java.net.*;
import java.util.HashMap;


public class Server extends Thread{
    private String serverIP;
    private InetAddress inetAddress;
    private boolean isRunning;
    private ServerSocket ss;
    private Socket s;
    private int serverPort;

    private AcceptingConnections acceptingConnections;

    private static HashMap<Integer,String> clientNamesHM;
    private static HashMap<Integer, ClientHandler> clientHandlersHM;
    private int userListNew = 0;

    private int clientNr = 1; //jak zero to ALL

    public Server() throws SocketException {
        this.setRunning(false);
        this.setS(new Socket());
        getS().setReuseAddress(true);
        setClientNamesHM(new HashMap<>());
        setClientHandlersHM(new HashMap<>());
    }

    public static HashMap<Integer, String> getClientNamesHM() {
        return clientNamesHM;
    }

    public static void setClientNamesHM(HashMap<Integer, String> clientNamesHM) {
        Server.clientNamesHM = clientNamesHM;
    }

    public static HashMap<Integer, ClientHandler> getClientHandlersHM() {
        return clientHandlersHM;
    }

    public static void setClientHandlersHM(HashMap<Integer, ClientHandler> clientHandlersHM) {
        Server.clientHandlersHM = clientHandlersHM;
    }


    public void startServer(int serverPortVar, String serverIp) throws IOException {
        try {
            this.setServerPort(serverPortVar);
            this.setServerIP(serverIp);
            this.setInetAddress(InetAddress.getByName(getServerIP()));
            setSs(new ServerSocket(getServerPort(), 50, getInetAddress()));
            setAcceptingConnections(new AcceptingConnections(this));
            getAcceptingConnections().start();
            this.setRunning(true);
            System.out.println("Server started.");
        }catch (UnknownHostException e){
            System.out.println("Incorrect IP address.");
        }
    }

    public void stopServer() throws IOException {
        this.setRunning(false);
        System.out.println("Accepting connections interrupted");
        broadcast("<SERVER_CLOSED>");
        disconnectAll();
        getAcceptingConnections().interrupt();
        getSs().close();
        System.out.println("Server stopped.");
    }

    public void broadcast(String what) throws IOException {
        for (int currentClient : Server.getClientHandlersHM().keySet()) {
            Server.getClientHandlersHM().get(currentClient).getDos().writeUTF(what);
        }
    }

    public void sendUserList() throws IOException {
        StringBuilder userListS = new StringBuilder();

        if (!getClientNamesHM().isEmpty()) {
            for (Integer clientNumber : getClientNamesHM().keySet()) {
                String user = clientNumber + "=" + Server.getClientNamesHM().get(clientNumber) + ";";
                userListS.append(user);
            }
            userListS.setLength(userListS.length() - 1); //usuwanie zbędnego delimitera
            broadcast("<USER_LIST>&" + userListS.toString());
        }
        setUserListNew(getUserListNew() + 1);
    }

    public void kickOutUser(int clientNumber) throws IOException {
        getClientHandlersHM().get(clientNumber).getDos().writeUTF("<KICKED_OUT>");
        System.out.println("Client nr.: " + clientNumber + " was kicked out of server.");
        if(!getClientNamesHM().get(clientNumber).isEmpty()) {
            broadcast("<SERVER_MSG>&" + getClientNamesHM().get(clientNumber) + " was kicked out of server.");
        }
        getClientHandlersHM().get(clientNumber).disconnectUser();
    }

    public void disconnectAll(){
        for(int clientNr : getClientHandlersHM().keySet()){
            try {
                getClientHandlersHM().get(clientNr).setLoggedIn(false);
                getClientHandlersHM().get(clientNr).getS().close();
            }catch (IOException e) {
                System.out.println("Error in disconnectAll");
                //e.printStackTrace();
            }
        }
        getClientHandlersHM().clear();
        getClientNamesHM().clear();
    }

    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public ServerSocket getSs() {
        return ss;
    }

    public void setSs(ServerSocket ss) {
        this.ss = ss;
    }

    public Socket getS() {
        return s;
    }

    public void setS(Socket s) {
        this.s = s;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public AcceptingConnections getAcceptingConnections() {
        return acceptingConnections;
    }

    public void setAcceptingConnections(AcceptingConnections acceptingConnections) {
        this.acceptingConnections = acceptingConnections;
    }

    public int getUserListNew() {
        return userListNew;
    }

    public void setUserListNew(int userListNew) {
        this.userListNew = userListNew;
    }

    public int getClientNr() {
        return clientNr;
    }

    public void setClientNr(int clientNr) {
        this.clientNr = clientNr;
    }
}
