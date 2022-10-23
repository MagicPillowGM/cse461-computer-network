import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class Server {
    static final int UDP_PORT = 12235;
    static final String HOST = "attu2.cs.washington.edu";

    static final int HEADER_LENGTH = 12;
    static final short CLIENT_STEP = 1;
    static final short SERVER_STEP = 2;
//    static final short STU_ID = 397;
    static final int RESEND_TIMEOUT = 1000;

    static final String A1_STRING = "hello world\0";
    static final int A_SECRET = 0;

    // static ServerSocket variable
//    private static ServerSocket server;
//
//    private static DatagramSocket udpSocket;

    private static InetAddress clientAdd = null; // client address

    public static void main(String[] args) {
        DatagramSocket datagramSocket = null;
        try {
            // create DatagramSocket and bind
            datagramSocket = new DatagramSocket(UDP_PORT);
            datagramSocket.setReuseAddress(true);
            // keep listening to client's UDP packet of stage A
            while (true) {
                byte[] buff = new byte[HEADER_LENGTH + A1_STRING.length()];
                DatagramPacket clientPacket = new DatagramPacket(buff, buff.length);
                datagramSocket.receive(clientPacket);  // block until receive

                ClientHandler handler = new ClientHandler(datagramSocket, clientPacket, buff);
                new Thread(handler).start();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }

    private static class ClientHandler implements Runnable {
        public static final int NUM_RANGE = 32;
        public static final int PORT_RANGE = 65536;

        private final DatagramSocket stageASocket;
        private final DatagramPacket stageAPacket;
        private final byte[] clientStageA;
        private short stuID;

        public ClientHandler(DatagramSocket socket, DatagramPacket packet, byte[] buff) {
            this.stageASocket = socket;
            this.stageAPacket = packet;
            this.clientStageA = buff;
        }

        public int[] stageA() {
            // TODO: validate clientStageA
            ByteBuffer clientBuff = ByteBuffer.wrap(clientStageA);
            stuID = clientBuff.getShort(HEADER_LENGTH - 2);
            // make response
            Random numRand = new Random(NUM_RANGE);
            Random portRand = new Random(PORT_RANGE);
            int num = numRand.nextInt();
            int len = numRand.nextInt();
            int udpPort = portRand.nextInt();
            int secretA = portRand.nextInt();
            ByteBuffer payload = ByteBuffer.allocate(16);
            payload.putInt(num);
            payload.putInt(len);
            payload.putInt(udpPort);
            payload.putInt(secretA);
            byte[] buff = messageComposer(payload.array(), A_SECRET, SERVER_STEP, stuID);
            DatagramPacket response = new DatagramPacket(buff, buff.length,
                    stageAPacket.getAddress(), stageAPacket.getPort());
            try {
                this.stageASocket.send(response);
                return new int[]{num, len, udpPort, secretA};
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {

            }
            return null;
        }

        public void run() {


//            Random rand = new Random();
//
//            while (true) {
//                // Stage A receive and send udp packet to client.
//
//                // receive packet from client
//                byte[] receiveBuffer = new byte[1024];
//                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
//                udpSocket.receive(packet);
//
//                ByteBuffer cliRespond = ByteBuffer.wrap(receiveBuffer);
//                cliRespond.position(HEADER_LENGTH);
//                int msgLenth = A1_STRING.getBytes().length;
//                int expectedLength = HEADER_LENGTH + msgLenth;
//                int payloadLen = respond.getInt();
//                int secret = respond.getInt();
//                int step = respond.getInt();
//                int stuID = respond.getInt();
//                String payload = respond.getBytes();
//
//                // checking buffer length
//                if (expectedLength != receiveBuffer || payloadLen != msgLenth || secret != A_SECRET
//                        || step != CLIENT_STEP || stuID != STU_ID) {
//                    return;
//                }
//
//                // preparing msg to send to client
//                int num = rand.nextInt();
//                int len = rand.nextInt();
//                int udpPort = rand.nextInt();
//                int secretA = rand.nextInt();
//                byte[] sendBuffer = messageComposer(16, num, len, udpPort, secretA);
//                packet = new DatagramPacket(sendBuffer, sendBuffer.length);
//                udpSocket.send(packet);
//                udpSocket.close();
//            }
        }

        // Compose the message following the protocol
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
    }
}
