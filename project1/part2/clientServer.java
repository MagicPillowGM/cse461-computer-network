import java.net.*;
import java.io.*;

import java.lang.ClassNotFoundException;
import java.util.Random;

public class clientServer {

    static final String HOST = "attu2.cs.washington.edu";
    private static int port = 12235;

    static final int HEADER_LENGTH = 12;
    static final short CLIENT_STEP = 1;
    static final short SERVER_STEP = 2;
    static final short STU_ID = 397;
    static final int RESEND_TIMEOUT = 1000;

    static final String A1_STRING = "hello world\0";
    static final int A_SECRET = 0;

    // static ServerSocket variable
    private static ServerSocket server;

    private static DatagramSocket udpSocket;

    private static InetAddress clientAdd = null; // client address

    public static byte[] processInput(String message) {

        return null;

    }

    // Compose the message following the protocol
    private static byte[] messageComposer(int num, int len, int udpPort, int secret) {
        int buffLen = (payload.length % 4 == 0) ? payload.length : payload.length + (4 - payload.length % 4);
        ByteBuffer message = ByteBuffer.allocate(HEADER_LENGTH + buffLen);
        message.putInt(num);
        message.putInt(len);
        message.putShort(udpPort);
        message.putShort(secret);
        return message.array();
    }

    public static void main(String args[]) {

        // create a socket that bind with the given port

        // create a UDP socket which binds to that port number
        udpSocket = new DatagramSocket(port);
        Random rand = new Random();

        while (true) {
            // Stage A receive and send udp packet to client.

            // receive packet from client
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            udpSocket.receive(packet);

            ByteBuffer cliRespond = ByteBuffer.wrap(receiveBuffer);
            cliRespond.position(HEADER_LENGTH);
            int msgLenth = A1_STRING.getBytes().length;
            int expectedLength = HEADER_LENGTH + msgLenth;
            int payloadLen = respond.getInt();
            int secret = respond.getInt();
            int step = respond.getInt();
            int stuID = respond.getInt();
            String payload = respond.getBytes();

            // checking buffer length
            if (expectedLength != receiveBuffer || payloadLen != msgLenth || secret != A_SECRET
                    || step != CLIENT_STEP || stuID != STU_ID) {
                return;
            }

            // preparing msg to send to client
            int num = rand.nextInt();
            int len = rand.nextInt();
            int udpPort = rand.nextInt();
            int secretA = rand.nextInt();
            byte[] sendBuffer = messageComposer(16, num, len, udpPort, secretA);
            packet = new DatagramPacket(sendBuffer, sendBuffer.length);
            udpSocket.send(packet);
            udpSocket.close();
        }
    }

}
