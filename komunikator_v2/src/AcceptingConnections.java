import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AcceptingConnections extends Thread {
    private Server server;

    AcceptingConnections(Server s) {
        this.server = s;
    }

    @Override
    public void run() {
        try {
            while (true) {
                server.s = server.ss.accept();
                System.out.println("Aceepted new connection");

                DataInputStream dis = new DataInputStream(server.s.getInputStream());
                DataOutputStream dos = new DataOutputStream(server.s.getOutputStream());
                ClientHandler cH = new ClientHandler(server.s, server.clientNr, dis, dos, server);  //nowy clientHandler
                Thread t = new Thread(cH);  //nowy wątek dla clientHandler
                server.clientHandlersHM.put(server.clientNr, cH); // add this client to active clients list

                t.start(); //uruchom wątek

                System.out.println("Client nr.: " + server.clientNr + " joined server.");
                server.clientNr++;

            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error while accepting connection");
        }

    }
}

