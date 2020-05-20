import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Client {
    int serverPort;
    String serverIp;
    InetAddress serverInetAddress;

    int clientPort;
    String clientIp;
    private byte[] oktetyIp;
    InetAddress clientInetAddress;

    Socket socket;
    ClientConnection clientConnection;
    boolean isConnected;
    DataInputStream dis;
    DataOutputStream dos;

    String clientName;
    boolean ifServerAcceptedName;

    HashMap<String,Integer> avUsers;
    int userListNew = 0;
    String newMsg;
    volatile boolean toSend;
    int recipentNr;
    String recipentName;

    public Client() throws UnknownHostException, SocketException {
        this.ifServerAcceptedName = false;
        this.isConnected = false;
        this.toSend = false;

        avUsers = new HashMap<>();

        //this.oktetyIp = InetAddress.getLocalHost().getAddress();
        //this.clientInetAddress = InetAddress.getByAddress(new byte[]{ oktetyIp[0],oktetyIp[1], oktetyIp[2], oktetyIp[3]});

    }

    public void startClient() throws IOException {
        try {
            //String sIp, int sPort, int localPort
            //serverIp = sIp;
            //serverPort = sPort;
            serverInetAddress = InetAddress.getByName(serverIp);
            clientInetAddress = InetAddress.getByName(clientIp);
            //clientPort = localPort;

            socket = new Socket();
            socket.setReuseAddress(true);

            socket.bind(new InetSocketAddress(clientInetAddress, clientPort));
            socket.connect(new InetSocketAddress(serverInetAddress, serverPort));

            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            isConnected = true;


            Thread sendMessageT = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isConnected) {
                        if (toSend) {
                            try {
                                String[] prepMsg = normalizeMsg(newMsg);
                                setRecipentNr(recipentName);
                                for (String partMsg : prepMsg) {
                                    dos.writeUTF("<MSG>&" + recipentNr + "&" + partMsg);

                                    if (recipentNr == 0) {
                                        //System.out.println(clientName + " : " + partMsg);
                                    } else {
                                        System.out.println(clientName + " -> " + recipentName + " : " + partMsg);
                                    }
                                }
                                toSend = false;

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
                    while (isConnected) {
                        try {

                            String received = dis.readUTF();
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
                                        ifServerAcceptedName = true;
                                    } else if (tmp.equals("Name already taken.")) {
                                        clientConnection.close();
                                    }
                                    break;

                                case "<SERVER_MSG>":
                                    tmp = st.nextToken();
                                    System.out.println("<SERVER> " + tmp);
                                    break;

                                case "<KICKED_OUT>":
                                    System.out.println("You were kicked out of server.");
                                    clientConnection.close();
                                    break;

                                case "<SERVER_CLOSED>":
                                    System.out.println("Server closed.");
                                    clientConnection.close();
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
                                isConnected = false;
                                clientConnection.close();
                            } catch (IOException ex) {
                                System.out.println("Server unexpectedly closed.");
                                ex.printStackTrace();
                            }

                            //e.printStackTrace();
                        }
                    }

                }
            });

            clientConnection = new ClientConnection(socket, readMessageT, sendMessageT);
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
        if(checkIfNameIsValid(clientName).equals("true")) {
            dos.writeUTF("<C_NAME>&" + clientName);
        }else {
            System.out.println(checkIfNameIsValid(clientName));
        }
    }

    void loggingOut() throws IOException {
        System.out.println("Logging out.");
        dos.writeUTF("<LOGOUT>");
        ifServerAcceptedName = false;
        isConnected = false;
        clientConnection.close();
    }

    public String checkIfNameIsValid (String testedName){
        if ( (testedName.length()<5) || (testedName.length()>20) ){
            return "Name should be at least 5 symbols long and not longer than 20 symbols.";
        }else if(testedName.matches("[^A-Za-z0-9._-]+")) {
            return "Name contains forbidden symbols.";
        }else if(testedName.equals("ALL")){
            return "You can not set your name as ALL";
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
            this.recipentNr = 0;
        } else {
            this.recipentNr = avUsers.get(recipentName);
        }
    }

    public void setAvUsers(String userList) {
        avUsers.clear();
        avUsers.put("ALL",0);

        StringTokenizer avUs = new StringTokenizer(userList,";");
        while (avUs.hasMoreTokens()){
            String[] tmpUser = avUs.nextToken().split("=");
            avUsers.put(tmpUser[1],Integer.parseInt(tmpUser[0]));
        }
        userListNew++;
    }



}
