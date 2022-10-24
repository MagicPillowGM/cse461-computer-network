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
    // static final short STU_ID = 397;
    static final int RESEND_TIMEOUT = 1000;
    static final int CLOSE_TIMEOUT = 3000;

    static final String A1_STRING = "hello world\0";
    static final int A_SECRET = 0;

    static final int B_SECRET = 0;

    // static ServerSocket variable
    // private static ServerSocket server;
    //
    // private static DatagramSocket udpSocket;

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
                datagramSocket.receive(clientPacket); // block until receive

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

        private ServerSocket tcpSocketCD;

        private final DatagramSocket stageBSocket;
        private final DatagramPacket stageBPacket;
        private final byte[] clientStageB;

        // private final Socket stageCSocket;
        // private final byte[] clientStageC;

        public ClientHandler(DatagramSocket socket, DatagramPacket packet, byte[] buff) {
            this.udpSocketA = socket;
            this.clientPacketA = packet;
            this.stageABuff = buff;
        }

        public int[] stageA() {
            // TODO: validate clientStageA
            if (!verifyMessage(clientStageA, A1_STRING.getBytes().length, A_SECRET, stuID)) {
                return null;
            }
            ByteBuffer clientBuff = ByteBuffer.wrap(clientStageA);
            stuID = clientBuff.getShort(HEADER_LENGTH - 2);
            // make payload
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
            // compose response
            byte[] buff = messageComposer(payload.array(), A_SECRET, SERVER_STEP, stuID);
            DatagramPacket response = new DatagramPacket(buff, buff.length,
                    stageAPacket.getAddress(), stageAPacket.getPort());
            try {
                // send response
                this.stageASocket.send(response);
                return new int[] { num, len, udpPort, secretA };
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                stageASocket.close();
            }
            return null;
        }

        public int[] stageB(int[] resultA) {
            int len = resultA[1];
            int num = resultA[0];
            int newPort = resultA[2];
            int pSecret = resultA[3];

            // variable num, len, udpPort are result from stage A.
            int expectedPayloadLen = len + 4;
            expectedPayloadLen = (expectedPayloadLen % 4 == 0) ? expectedPayloadLen
                    : expectedPayloadLen + (4 - expectedPayloadLen % 4);
            int expectedMsgLen = HEADER_LENGTH + expectedPayloadLen;

            Random boolRand = new Random();
            DatagramSocket datagramSocket = null;
            try {
                datagramSocket = new DatagramSocket(newPort);
                datagramSocket.setReuseAddress(true);
                int numReceived = 0;
                while (numReceived < num) {
                    // listen for client
                    byte[] clientBuff = new byte[expectedMsgLen];
                    DatagramPacket clientPacket = new DatagramPacket(clientBuff, clientBuff.length);
                    datagramSocket.receive(clientPacket);  // block until receive

                    // after receive
                    // get pacId
                    ByteBuffer clientBuffer = ByteBuffer.wrap(clientBuff);
                    clientBuffer.position(HEADER_LENGTH);
                    int pacId = clientBuffer.getInt();
                    // verify packet
                    if (numReceived != pacId || !verifyMessage(clientBuff, len, pSecret, stuID)) {  // TODO: verifyMessage specify input
                        datagramSocket.close();
                        break;
                    }
                    // then randomly decide
                    boolean ack = boolRand.nextBoolean();
                    if (ack) {
                        // make payload
                        ByteBuffer payload = ByteBuffer.allocate(4);
                        payload.putInt(pacId);
                        byte[] buff = messageComposer(payload.array(), pSecret, SERVER_STEP, stuID);  // ? server or client step
                        DatagramPacket response = new DatagramPacket(buff, buff.length,
                                clientPacket.getAddress(), clientPacket.getPort());
                        datagramSocket.send(response);
                        numReceived++;
                    }

                    if (numReceived == num) {
                        // after all packets are acknowledged
                        // make response payload
                        Random portRand = new Random(PORT_RANGE);
                        int tcpPort = portRand.nextInt();
                        int secretB = portRand.nextInt();
                        ByteBuffer payload = ByteBuffer.allocate(8);
                        payload.putInt(tcpPort);
                        payload.putInt(secretB);
                        // compose response packet
                        byte[] buff = messageComposer(payload.array(), pSecret, SERVER_STEP, stuID);
                        DatagramPacket response = new DatagramPacket(buff, buff.length,
                                clientPacket.getAddress(), clientPacket.getPort());
                        datagramSocket.send(response);
                        datagramSocket.close();
                        return new int[] { tcpPort, secretB };
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                if (datagramSocket != null) {
                    datagramSocket.close();
                }
            }
            return null;
        }

        public boolean stageC() {
            // tcpPort are result from stage B
            ServerSocket tcpSocket = new ServerSocket(tcpPort);
            tcpSocket.setSoTimeout(10000);
            Socket tcpServer = tcpSocket.accept();
            OutputStream out = tcpServer.getOutputStream();

            Random numRandom = new Random(PORT_RANGE);
            // preparing the message
            int num2 = numRandom.nextInt();
            int len2 = numRandom.nextInt();
            int secretC = numRandom.nextInt();
            ByteBuffer payload = ByteBuffer.allocate(13);
            payload.putInt(num2);
            payload.putInt(len2);
            payload.putInt(secretC);
            payload.putChar('c');
            byte[] message = messageComposer(payload.array(), secretC, SERVER_STEP, stuID);
            out.write(message);

            try {
                out.write(message);
                return new int[] {};
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {

            }
            return null;
        }

        public boolean stageD() {

            // step D-2
            OutputStream out = tcpServer.getOutputStream();
            Random numRandom = new Random(PORT_RANGE);
            // preparing the message
            int secretD = numRandom.nextInt();
            ByteBuffer payload = ByteBuffer.allocate(4);
            payload.putInt(secretD);
            byte[] message = messageComposer(payload.array(), secretD, SERVER_STEP, stuID);
            out.write(message);
            return true;
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

        // verify the header of every packet received
        private static boolean verifyMessage(byte[] packet, int payloadLenth, int secretNum, int stu_id) {
            // Verify header information
            ByteBuffer cliRespond = ByteBuffer.wrap(packet);
            int payloadLen = cliRespond.getInt();
            int packetSecret = cliRespond.getInt();
            short clientStep = cliRespond.getShort();
            short stuID = cliRespond.getShort();

            // at this point, buffer should be pointing at msg payload.
            // cliRespond.position(HEADER_LENGTH);
            int expectedLength = HEADER_LENGTH + payloadLenth;

            // Q: How do we know the content of the payload from stageB and on?
            return (expectedLength != packet.length || payloadLen != payloadLenth || packetSecret != secretNum
                    || clientStep != CLIENT_STEP || stuID != stu_id);
        }
    }
}
