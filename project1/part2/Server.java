import java.io.*;
import java.net.*;

public class Server {
    static final int UDP_PORT = 12235;
    public static void main(String[] args) {
        ServerSocket server = null;
        try {
            // create and bind
            server = new ServerSocket(UDP_PORT);
            server.setReuseAddress(true);

            while (true) {
                Socket client = server.accept();
                ClientHandler handler = new ClientHandler(client);
                new Thread(handler).start();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket client) {
            this.clientSocket = client;
        }
        public void run() {
//            InputStream
        }
    }
}
