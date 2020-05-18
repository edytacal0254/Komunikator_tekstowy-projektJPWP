import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.StringTokenizer;

public class ClientHandler implements Runnable{
    private Socket s;
    private String name;
    private int clientNumber;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Server server;
    private boolean isLoggedIn;

    public Socket getS() {
        return s;
    }
    public void setS(Socket s) {
        this.s = s;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public int getClientNumber() {
        return clientNumber;
    }
    public void setClientNumber(int clientNumber) {
        this.clientNumber = clientNumber;
    }

    public DataInputStream getDis() {
        return dis;
    }
    public void setDis(DataInputStream dis) {
        this.dis = dis;
    }

    public DataOutputStream getDos() {
        return dos;
    }
    public void setDos(DataOutputStream dos) {
        this.dos = dos;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public ClientHandler(Socket s, int clientNumber, DataInputStream dis, DataOutputStream dos, Server server) {
        this.s = s;
        this.clientNumber = clientNumber;
        this.dis = dis;
        this.dos = dos;
        this.server = server;
        this.isLoggedIn = true;
        this.server = server;

        System.out.println("Created ClientHandler for client nr. " + clientNumber);
    }

    public void disconnectUser() throws IOException {
        this.isLoggedIn = false;
        Server.clientNamesHM.remove(clientNumber);
        Server.clientHandlersHM.remove(clientNumber);

        this.s.close();
        server.sendUserList();
    }



    public void run() {
        String received;

        while (server.isRunning && isLoggedIn){
            try {
                if ((!s.isConnected()) || s.isClosed()) {
                    isLoggedIn = false;
                }

                received = dis.readUTF();
                System.out.println(received);

                StringTokenizer st = new StringTokenizer(received, "&");
                String tag1 = st.nextToken();

                switch (tag1) {
                    case "<C_NAME>":
                        String tmpName = st.nextToken();

                        if (Server.clientNamesHM.containsValue(tmpName)) {
                            dos.writeUTF("<SERVER_C_NAME>&" + "Name already taken.");
                        } else {
                            this.name = tmpName;
                            Server.clientNamesHM.put(clientNumber, name);
                            dos.writeUTF("<SERVER_C_NAME>&" + "Name accepted.");
                            server.sendUserList(); //wysyłamy wszystkim listę użytkowników
                            server.broadcast("<SERVER_MSG>&" + tmpName + " joined chat.");
                            System.out.println("Client nr. " + clientNumber + " set name as : " + tmpName);
                        }
                        break;

                    case "<MSG>":
                        Integer nrRecipent = Integer.parseInt(st.nextToken());
                        String message = st.
                                nextToken();

                        if (nrRecipent == 0) {      ///wiad. dla wszystkich
                            try {
                                server.broadcast("<MSG>&" + this.name + " : " + message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                Server.clientHandlersHM.get(nrRecipent).dos.writeUTF("<MSG>&" + this.name + " -> " + Server.clientNamesHM.get(nrRecipent) + " : " + message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                    case "<LOGOUT>":
                        this.isLoggedIn = false;
                        server.broadcast("<SERVER_MSG>&" + this.name + " left chat.");
                        System.out.println("Client nr.: " + clientNumber + " left server.");
                        break;

                    default:
                        String defaultMsg = tag1;
                        while (st.hasMoreTokens()) {
                            defaultMsg = defaultMsg.concat(st.nextToken());
                        }
                        System.out.println(defaultMsg);

                }
            } catch (IOException e) {
                System.out.println("Error in run()[while] in ClientHandler of client "+clientNumber);
                try {
                    disconnectUser();
                } catch (IOException ex) {
                    System.out.println("Error in run()[while] in ClientHandler of client "+clientNumber + " [disconecting user]");
                    //ex.printStackTrace();
                }
                //e.printStackTrace();
            }
            if(!isLoggedIn){
                try {
                    disconnectUser();
                } catch (IOException e) {
                    System.out.println("Error in run()[if] in ClientHandler of client "+clientNumber + " [disconecting user]");
                    //e.printStackTrace();
                }
            }

        }
    }


}
