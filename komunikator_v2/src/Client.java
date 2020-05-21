import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Client {
    private int serverPort;
    private String serverIp;
    private InetAddress serverInetAddress;

    private int clientPort;
    private String clientIp;
    private InetAddress clientInetAddress;

    private Socket socket;
    private ClientConnection clientConnection;
    private boolean isConnected;
    private DataInputStream dis;
    private DataOutputStream dos;

    private String clientName;
    private boolean ifServerAcceptedName;

    private HashMap<String,Integer> avUsers;
    private int userListNew = 0;
    private String newMsg;
    private volatile boolean toSend;
    private int recipentNr;
    private String recipentName;

    public Client() {
        this.setIfServerAcceptedName(false);
        this.setConnected(false);
        this.setToSend(false);

        setAvUsers(new HashMap<>());

    }

    public void startClient() throws IOException {
        try {
            setServerInetAddress(InetAddress.getByName(getServerIp()));
            setClientInetAddress(InetAddress.getByName(getClientIp()));

            setSocket(new Socket());
            getSocket().setReuseAddress(true);

            getSocket().bind(new InetSocketAddress(getClientInetAddress(), getClientPort()));
            getSocket().connect(new InetSocketAddress(getServerInetAddress(), getServerPort()));

            setDis(new DataInputStream(getSocket().getInputStream()));
            setDos(new DataOutputStream(getSocket().getOutputStream()));

            setConnected(true);


            Thread sendMessageT = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isConnected()) {
                        if (isToSend()) {
                            try {
                                String[] prepMsg = normalizeMsg(getNewMsg());
                                setRecipentNr(getRecipentName());
                                for (String partMsg : prepMsg) {
                                    getDos().writeUTF("<MSG>&" + getRecipentNr() + "&" + partMsg);

                                    if (getRecipentNr() == 0) {
                                        //System.out.println(clientName + " : " + partMsg);
                                    } else {
                                        System.out.println(getClientName() + " -> " + getRecipentName() + " : " + partMsg);
                                    }
                                }
                                setToSend(false);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });

            Thread readMessageT = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isConnected()) {
                        try {

                            String received = getDis().readUTF();
                            StringTokenizer st = new StringTokenizer(received, "&");
                            String tag1 = st.nextToken();
                            String tmp;

                            switch (tag1) {
                                case "<MSG>":
                                    String message = st.nextToken();
                                    System.out.println(message);
                                    break;

                                case "<USER_LIST>":
                                    String newUserList = st.nextToken();
                                    setAvUsers(newUserList);
                                    break;

                                case "<SERVER_C_NAME>":
                                    tmp = st.nextToken();
                                    System.out.println("<SERVER> " + tmp);
                                    if (tmp.equals("Name accepted.")) {
                                        setIfServerAcceptedName(true);
                                    } else if (tmp.equals("Name already taken.")) {
                                        getClientConnection().close();
                                    }
                                    break;

                                case "<SERVER_MSG>":
                                    tmp = st.nextToken();
                                    System.out.println("<SERVER> " + tmp);
                                    break;

                                case "<KICKED_OUT>":
                                    System.out.println("You were kicked out of server.");
                                    getClientConnection().close();
                                    break;

                                case "<SERVER_CLOSED>":
                                    System.out.println("Server closed.");
                                    getClientConnection().close();
                                    break;

                                default:
                                    tmp = tag1;
                                    while (st.hasMoreTokens()) {
                                        tmp = tmp.concat(st.nextToken());
                                    }
                                    System.out.println("<ERROR_WHILE_PROCESSING_MSG> " + tmp);
                                    break;
                            }

                        } catch (IOException e) {
                            try {
                                setConnected(false);
                                getClientConnection().close();
                            } catch (IOException ex) {
                                System.out.println("Server unexpectedly closed.");
                                ex.printStackTrace();
                            }

                            //e.printStackTrace();
                        }
                    }

                }
            });

            setClientConnection(new ClientConnection(getSocket(), readMessageT, sendMessageT));
            askForName();
        }catch (BindException e2){
            System.out.println("That port is either being used or in TIME_OUT. Use different port.");
        }catch (ConnectException e3){
            System.out.println("Server is not running on this address");
        }catch (UnknownHostException e4){
            System.out.println("Incorrect IP address.");
        }
    }

    void askForName() throws IOException {
        if(checkIfNameIsValid(getClientName()).equals("true")) {
            getDos().writeUTF("<C_NAME>&" + getClientName());
        }else {
            System.out.println(checkIfNameIsValid(getClientName()));
        }
    }

    void loggingOut() throws IOException {
        System.out.println("Logging out.");
        getDos().writeUTF("<LOGOUT>");
        setIfServerAcceptedName(false);
        setConnected(false);
        getClientConnection().close();
    }

    public String checkIfNameIsValid (String testedName){
        if ( (testedName.length()<5) || (testedName.length()>20) ){
            return "Name should be at least 5 symbols long and not longer than 20 symbols.";
        }else if(testedName.matches("[^A-Za-z0-9._-]+")) {
            return "Name contains forbidden symbols.";
        }else {
            return "true";
        }
    }

    public String[] normalizeMsg(String testedMsg){
        String replacedMsg = testedMsg.replaceAll("&","_AND_");
        String[] dividedMsg = replacedMsg.split("(?<=\\G.{195})");
        if (replacedMsg.length()>200) {
            int i = 0;
            for (String onePart : dividedMsg) {
                dividedMsg[i] = "<" + (i + 1) + ">: " + dividedMsg[i];
                i++;
            }
        }
        return dividedMsg;
    }

    public void setRecipentNr(String recipentName) {
        if (recipentName == null) {
            this.setRecipentNr(0);
        } else {
            this.setRecipentNr(getAvUsers().get(recipentName));
        }
    }

    public void setAvUsers(String userList) {
        getAvUsers().clear();
        getAvUsers().put("ALL",0);

        StringTokenizer avUs = new StringTokenizer(userList,";");
        while (avUs.hasMoreTokens()){
            String[] tmpUser = avUs.nextToken().split("=");
            getAvUsers().put(tmpUser[1],Integer.parseInt(tmpUser[0]));
        }
        setUserListNew(getUserListNew() + 1);
    }


    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public InetAddress getServerInetAddress() {
        return serverInetAddress;
    }

    public void setServerInetAddress(InetAddress serverInetAddress) {
        this.serverInetAddress = serverInetAddress;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public InetAddress getClientInetAddress() {
        return clientInetAddress;
    }

    public void setClientInetAddress(InetAddress clientInetAddress) {
        this.clientInetAddress = clientInetAddress;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public ClientConnection getClientConnection() {
        return clientConnection;
    }

    public void setClientConnection(ClientConnection clientConnection) {
        this.clientConnection = clientConnection;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public boolean isIfServerAcceptedName() {
        return ifServerAcceptedName;
    }

    public void setIfServerAcceptedName(boolean ifServerAcceptedName) {
        this.ifServerAcceptedName = ifServerAcceptedName;
    }

    public HashMap<String, Integer> getAvUsers() {
        return avUsers;
    }

    public void setAvUsers(HashMap<String, Integer> avUsers) {
        this.avUsers = avUsers;
    }

    public int getUserListNew() {
        return userListNew;
    }

    public void setUserListNew(int userListNew) {
        this.userListNew = userListNew;
    }

    public String getNewMsg() {
        return newMsg;
    }

    public void setNewMsg(String newMsg) {
        this.newMsg = newMsg;
    }

    public boolean isToSend() {
        return toSend;
    }

    public void setToSend(boolean toSend) {
        this.toSend = toSend;
    }

    public int getRecipentNr() {
        return recipentNr;
    }

    public void setRecipentNr(int recipentNr) {
        this.recipentNr = recipentNr;
    }

    public String getRecipentName() {
        return recipentName;
    }

    public void setRecipentName(String recipentName) {
        this.recipentName = recipentName;
    }
}
