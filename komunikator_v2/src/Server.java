import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

public class Server extends Thread{
    String serverIP;
    InetAddress inetAddress;
    private byte[] oktetyIP;
    boolean isRunning;
    ServerSocket ss;
    Socket s;
    int serverPort;

    AcceptingConnections acceptingConnections;

    public static HashMap<Integer,String> clientNamesHM;
    public static HashMap<Integer, ClientHandler> clientHandlersHM;
    int userListNew = 0;

    int clientNr = 1; //jak zero to ALL

    public Server() throws UnknownHostException, SocketException {
        this.isRunning = false;
        this.s = new Socket();
        s.setReuseAddress(true);
        clientNamesHM = new HashMap<>();
        clientHandlersHM = new HashMap<>();
        //this.oktetyIP = InetAddress.getLocalHost().getAddress();
        //this.inetAddress = InetAddress.getByAddress(new byte[]{ oktetyIP[0],oktetyIP[1], oktetyIP[2], oktetyIP[3]});
        //this.serverIP = inetAddress.toString().substring(1);
    }


    public void startServer(int serverPortVar, String serverIp) throws IOException {
        try {
            this.serverPort = serverPortVar;
            this.serverIP = serverIp;
            this.inetAddress = InetAddress.getByName(serverIP);
            ss = new ServerSocket(serverPort, 50, inetAddress);
            acceptingConnections = new AcceptingConnections(this);
            acceptingConnections.start();
            this.isRunning = true;
            System.out.println("Server started.");
        }catch (UnknownHostException e){
            System.out.println("Incorrect IP address.");
        }
    }

    public void stopServer() throws IOException {
        this.isRunning = false;
        //Vector<ClientHandler> tmpClientHandlers = new Vector<>();
        System.out.println("Accepting connections interrupted");
        broadcast("<SERVER_CLOSED>");
        disconnectAll();
        acceptingConnections.interrupt();
        ss.close();
        System.out.println("Server stopped.");
    }

    public void broadcast(String what) throws IOException {
        for (int currentClient : Server.clientHandlersHM.keySet()) {
            Server.clientHandlersHM.get(currentClient).getDos().writeUTF(what);
        }
    }

    public void sendUserList() throws IOException {
        StringBuilder userListS = new StringBuilder();

        if (!clientNamesHM.isEmpty()) {
            for (Integer clientNumber : clientNamesHM.keySet()) {
                String user = clientNumber + "=" + Server.clientNamesHM.get(clientNumber) + ";";
                userListS.append(user);
            }
            userListS.setLength(userListS.length() - 1); //usuwanie zbędnego delimitera
            broadcast("<USER_LIST>&" + userListS.toString());
        }
        userListNew++;
    }

    public void kickOutUser(int clientNumber) throws IOException {
        clientHandlersHM.get(clientNumber).getDos().writeUTF("<KICKED_OUT>");
        System.out.println("Client nr.: " + clientNumber + " was kicked out of server.");
        if(!clientNamesHM.get(clientNumber).isEmpty()) {
            broadcast("<SERVER_MSG>&" + clientNamesHM.get(clientNumber) + " was kicked out of server.");
        }
        clientHandlersHM.get(clientNumber).disconnectUser();
    }

    public void disconnectAll(){
        for(int clientNr : clientHandlersHM.keySet()){
            try {
                clientHandlersHM.get(clientNr).setLoggedIn(false);
                clientHandlersHM.get(clientNr).getS().close();
            }catch (IOException e) {
                System.out.println("Error in disconnectAll");
                //e.printStackTrace();
            }
        }
        clientHandlersHM.clear();
        clientNamesHM.clear();
    }

}
