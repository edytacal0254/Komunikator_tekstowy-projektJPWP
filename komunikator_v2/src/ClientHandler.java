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
        this.setS(s);
        this.setClientNumber(clientNumber);
        this.setDis(dis);
        this.setDos(dos);
        this.setServer(server);
        this.setLoggedIn(true);
        this.setServer(server);

        System.out.println("Created ClientHandler for client nr. " + clientNumber);
    }

    public void disconnectUser() throws IOException {
        this.setLoggedIn(false);
        Server.getClientNamesHM().remove(getClientNumber());
        Server.getClientHandlersHM().remove(getClientNumber());

        this.getS().close();
        getServer().sendUserList();
    }



    public void run() {
        String received;

        while (getServer().isRunning() && isLoggedIn()){
            try {
                if ((!getS().isConnected()) || getS().isClosed()) {
                    setLoggedIn(false);
                }

                received = getDis().readUTF();

                StringTokenizer st = new StringTokenizer(received, "&");
                String tag1 = st.nextToken();

                switch (tag1) {
                    case "<C_NAME>":
                        String tmpName = st.nextToken();

                        if (Server.getClientNamesHM().containsValue(tmpName)) {
                            getDos().writeUTF("<SERVER_C_NAME>&" + "Name already taken.");
                        } else {
                            this.setName(tmpName);
                            Server.getClientNamesHM().put(getClientNumber(), getName());
                            getDos().writeUTF("<SERVER_C_NAME>&" + "Name accepted.");
                            getServer().sendUserList(); //wysyłamy wszystkim listę użytkowników
                            getServer().broadcast("<SERVER_MSG>&" + tmpName + " joined chat.");
                            System.out.println("Client nr. " + getClientNumber() + " set name as : " + tmpName);
                        }
                        break;

                    case "<MSG>":
                        Integer nrRecipent = Integer.parseInt(st.nextToken());
                        String message = st.
                                nextToken();

                        if (nrRecipent == 0) {      ///wiad. dla wszystkich
                            try {
                                getServer().broadcast("<MSG>&" + this.getName() + " : " + message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                Server.getClientHandlersHM().get(nrRecipent).getDos().writeUTF("<MSG>&" + this.getName() + " -> " + Server.getClientNamesHM().get(nrRecipent) + " : " + message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                    case "<LOGOUT>":
                        this.setLoggedIn(false);
                        getServer().broadcast("<SERVER_MSG>&" + this.getName() + " left chat.");
                        System.out.println("Client nr.: " + getClientNumber() + " left server.");
                        break;

                    default:
                        String defaultMsg = tag1;
                        while (st.hasMoreTokens()) {
                            defaultMsg = defaultMsg.concat(st.nextToken());
                        }
                        System.out.println(defaultMsg);

                }
            } catch (IOException e) {
                System.out.println("Error in run()[while] in ClientHandler of client "+ getClientNumber());
                try {
                    disconnectUser();
                } catch (IOException ex) {
                    System.out.println("Error in run()[while] in ClientHandler of client "+ getClientNumber() + " [disconecting user]");
                    //ex.printStackTrace();
                }
                //e.printStackTrace();
            }
            if(!isLoggedIn()){
                try {
                    disconnectUser();
                } catch (IOException e) {
                    System.out.println("Error in run()[if] in ClientHandler of client "+ getClientNumber() + " [disconecting user]");
                    //e.printStackTrace();
                }
            }

        }
    }


    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }
}
