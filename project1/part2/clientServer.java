import java.net.*;
import java.nio.ByteBuffer;
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
    private static ServerSocket tcpSocket;

    private static DatagramSocket udpSocket;

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

    @overload
    private static byte[] messageComposer(byte[] payload, int secret, short step, short stu_id) {
        int buffLen = (payload.length % 4 == 0) ? payload.length : payload.length + (4 - payload.length % 4);
        ByteBuffer message = ByteBuffer.allocate(HEADER_LENGTH + buffLen);
        message.putInt(payload.length);
        message.putInt(secret);
        message.putShort(step);
        message.putShort(stu_id);
        message.put(payload);
        return message.array();
    }

    private static boolean verifyMessage(byte[] packet, int msgLenth, int secretNum) {
        ByteBuffer cliRespond = ByteBuffer.wrap(packet);
        cliRespond.position(HEADER_LENGTH);
        int expectedLength = HEADER_LENGTH + msgLenth;
        int payloadLen = cliRespond.getInt();
        int secret = cliRespond.getInt();
        int step = cliRespond.getInt();
        int stuID = cliRespond.getInt();
        // String payload = cliRespond.getBytes();

        return (expectedLength != receiveBuffer || payloadLen != msgLenth || secret != secretNum
                || step != CLIENT_STEP || stuID != STU_ID);
    }

    public static void main(String args[]) {

        // create a socket that bind with the given port

        // create a UDP socket which binds to that port number
        Random rand = new Random();

        while (true) {
            // Stage A receive and send udp packet to client.
            // receive packet from client
            udpSocket = new DatagramSocket(port);
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            udpSocket.receive(packet);

            // ByteBuffer cliRespond = ByteBuffer.wrap(receiveBuffer);
            // cliRespond.position(HEADER_LENGTH);
            // int msgLenth = A1_STRING.getBytes().length;
            // int expectedLength = HEADER_LENGTH + msgLenth;
            // int payloadLen = cliRespond.getInt();
            // int secret = cliRespond.getInt();
            // int step = cliRespond.getInt();
            // int stuID = cliRespond.getInt();
            // String payload = cliRespond.getBytes();

            // checking buffer length
            // if (expectedLength != receiveBuffer || payloadLen != msgLenth || secret !=
            // A_SECRET
            // || step != CLIENT_STEP || stuID != STU_ID) {
            // return;
            // }

            boolean received = verifyMessage(receiveBuffer, receiveBuffer.length);

            if (!received) {
                break;
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

            // Stage B
            // # Starting a new socket and bind to new port that was passed to client
            udpSocket = new DatagramSocket(udpPort);
            int expectedPayloadLen = len + 4;
            expectedPayloadLen = (expectedPayloadLen % 4 == 0) ? expectedPayloadLen
                    : expectedPayloadLen + (4 - expectedPayloadLen % 4);
            int expectedMsgLen = HEADER_LENGTH + expectedPayloadLen;

            int numReceived = 0;
            receiveBuffer = new byte[expectedMsgLen];
            while (numReceived < num) {
                packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.receive(packet);

                boolean ack = rand.nextBoolean();
                // pass this packet
                if (!ack) {
                    continue;
                } else if (!verifyMessage(receiveBuffer, expectedMsgLen)) {
                    break;
                } else {
                    // send the ack id packet back to client.
                    ByteBuffer message = ByteBuffer.allocate(HEADER_LENGTH + 4);
                    message.putInt(4);
                    message.putInt(numReceived);
                    byte[] ackBuffer = message.array();
                    DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length,
                            InetAddress.getByName(HOST), udpPort);
                    udpSocket.send(ackPacket);
                    numReceived++;
                }

            }
            int tcpPort = rand.nextInt();
            int secretB = rand.nextInt();

            // Stage C TCP
            tcpSocket = new Socket(tcpPort);
            tcpSocket.setSoTimeout(10000);
            Socket tcpServer = tcpSocket.accept();
            OutputStream out = tcpServer.getOutputStream();

            // preparing the message
            int num2 = rand.nextInt();
            int len2 = rand.nextInt();
            int secretC = rand.nextInt();
            ByteBuffer payload = ByteBuffer.allocate(16);
            payload.putInt(num2);
            payload.putInt(len2);
            payload.putInt(secretC);
            payload.putChar('c');
            byte[] message = messageComposer(payload.array(), secretC, SERVER_STEP, STU_ID);
            out.write(message);

            // Stage D

        }
    }

}
