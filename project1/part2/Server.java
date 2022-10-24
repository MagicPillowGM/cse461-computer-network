import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class Server {
    static final int UDP_PORT = 12281;// 12235;
    static final int HEADER_LENGTH = 12;
    static final short CLIENT_STEP = 1;
    static final short SERVER_STEP = 2;
    static final int TIMEOUT = 3000;
    static final String A1_STRING = "hello world\0";
    static final int A_SECRET = 0;

    public static void main(String[] args) {
        DatagramSocket serverSocket = null;
        try {
            // create ServerSocket
            InetAddress hostAddress = InetAddress.getLocalHost();
            serverSocket = new DatagramSocket(UDP_PORT, hostAddress);
            // serverSocket.setSoTimeout(TIMEOUT);
            // keep listening to client's UDP packet of stage A
            while (true) {
                byte[] buff = new byte[HEADER_LENGTH + A1_STRING.length()];
                DatagramPacket clientPacket = new DatagramPacket(buff, buff.length);
                serverSocket.receive(clientPacket); // block until receive
                ClientHandler handler = new ClientHandler(serverSocket, clientPacket, buff);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            if (serverSocket != null) {
                serverSocket.close();
            }
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private static final int NUM_RANGE = 32;
        private static final int PORT_RANGE = 65536;

        private DatagramSocket udpSocketA;
        private DatagramPacket clientPacketA;
        private final byte[] stageABuff;
        private short stuID;

        private ServerSocket tcpSocketCD;

        public ClientHandler(DatagramSocket socket, DatagramPacket packet, byte[] buff) {
            this.udpSocketA = socket;
            this.clientPacketA = packet;
            this.stageABuff = buff;
        }

        public void run() {
            int[] resultA = stageA();
            int[] resultB = stageB(resultA);
            int[] resultC = stageC(resultB);
        }

        public int[] stageA() {
            System.out.println("recived stageA");
            // validate clientStageA
            if (!verifyMessage2(stageABuff, stageABuff.length, A1_STRING.length(), A_SECRET,
                    CLIENT_STEP, A1_STRING.getBytes())) {
                System.out.println("not valid!!!!");
                return null;
            }
            ByteBuffer clientBuff = ByteBuffer.wrap(stageABuff);
            stuID = clientBuff.getShort(HEADER_LENGTH - 2);
            // make payload
            Random Rand = new Random();
            int num = Rand.nextInt(30);
            int len = Rand.nextInt(30);
            int udpPort = Rand.nextInt(65535 - 256) + 256;
            int secretA = Rand.nextInt(100);
            ByteBuffer payload = ByteBuffer.allocate(16);
            payload.putInt(num);
            payload.putInt(len);
            payload.putInt(udpPort);
            payload.putInt(secretA);
            // compose response
            byte[] buff = messageComposer(payload.array(), A_SECRET, SERVER_STEP, stuID);
            DatagramPacket response = new DatagramPacket(buff, buff.length,
                    clientPacketA.getAddress(), clientPacketA.getPort());
            try {
                // send response
                this.udpSocketA.send(response);
                this.udpSocketA = new DatagramSocket(udpPort, InetAddress.getLocalHost());
                return new int[] { num, len, udpPort, secretA };
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return null;
        }

        public int[] stageB(int[] resultA) {
            int len = resultA[1];
            int num = resultA[0];
            // int newPort = resultA[2];
            int pSecret = resultA[3];

            System.out.println("recived stageB");

            // variable num, len, udpPort are result from stage A.
            int expectedPayloadLen = len + 4;
            expectedPayloadLen = (expectedPayloadLen % 4 == 0) ? expectedPayloadLen
                    : expectedPayloadLen + (4 - expectedPayloadLen % 4);
            int expectedMsgLen = HEADER_LENGTH + expectedPayloadLen;

            Random boolRand = new Random();
            // DatagramSocket datagramSocket = null;

            // int acked = 0;
            // boolean droppedAck = false;
            int numReceived = 0;
            DatagramPacket clientPacket = null;
            try {
                // datagramSocket = new DatagramSocket(newPort);
                // datagramSocket.setReuseAddress(true);
                while (numReceived < num) {
                    // listen for client
                    byte[] recivedBuff = new byte[expectedMsgLen];
                    clientPacket = new DatagramPacket(recivedBuff, recivedBuff.length);
                    udpSocketA.receive(clientPacket); // block until receive

                    System.out.println("recived stageB packet" + numReceived);

                    // after receive
                    // get pacId
                    // ByteBuffer clientBuffer = ByteBuffer.wrap(recivedBuff);
                    // clientBuffer.position(HEADER_LENGTH);
                    // int pacId = clientBuffer.getInt();
                    // verify packet
                    byte[] exp_payload = ByteBuffer.allocate(len + 4).putInt(numReceived).array();
                    if (!verifyMessage2(recivedBuff, expectedMsgLen, len + 4, pSecret,
                            CLIENT_STEP, exp_payload)) {
                        System.out.println("Stage B not valid!!!!");
                        udpSocketA.close();
                        return null;
                    }
                    // if (numReceived != pacId || !verifyMessage(recivedBuff, len, pSecret, stuID))
                    // {
                    // datagramSocket.close();
                    // break;
                    // }
                    // then randomly decide
                    boolean ack = boolRand.nextBoolean();
                    if (ack) {
                        // make payload
                        byte[] payload = ByteBuffer.allocate(4).putInt(numReceived).array();
                        byte[] buff = messageComposer(payload, pSecret, SERVER_STEP, stuID);
                        DatagramPacket response = new DatagramPacket(buff, buff.length,
                                clientPacket.getAddress(), clientPacket.getPort());
                        udpSocketA.send(response);
                        numReceived++;
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            try {
                // after all packets are acknowledged
                // make response payload
                Random Rand = new Random();
                int tcpPort = Rand.nextInt(65535 - 256) + 256;
                int secretB = Rand.nextInt(30);
                ByteBuffer payload = ByteBuffer.allocate(8);
                payload.putInt(tcpPort);
                payload.putInt(secretB);

                // set up tcp socket
                this.tcpSocketCD = new ServerSocket(tcpPort);
                tcpSocketCD.setSoTimeout(TIMEOUT);

                // compose response packet
                byte[] buff = messageComposer(payload.array(), pSecret, SERVER_STEP, stuID);
                DatagramPacket response = new DatagramPacket(buff, buff.length,
                        clientPacket.getAddress(), clientPacket.getPort());
                udpSocketA.send(response);
                udpSocketA.close();
                return new int[] { tcpPort, secretB };
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public int[] stageC(int[] resultB) {
            int tcpPort = resultB[0]; // tcpPort are result from stage B
            int pSecret = resultB[1];
            try {
                tcpSocketCD = new ServerSocket(tcpPort, 1, InetAddress.getLocalHost());
                tcpSocketCD.setSoTimeout(TIMEOUT);
                tcpSocketCD.setReuseAddress(true);
                Socket tcpServer = tcpSocketCD.accept();
                OutputStream out = tcpServer.getOutputStream();

                Random numRandom = new Random();
                // preparing the message
                int num2 = numRandom.nextInt(30);
                int len2 = numRandom.nextInt(30);
                int secretC = numRandom.nextInt(100);
                byte c = (byte) (numRandom.nextInt(94) + 33);
                ByteBuffer payload = ByteBuffer.allocate(13);
                payload.putInt(num2);
                payload.putInt(len2);
                payload.putInt(secretC);
                payload.put(c);
                byte[] message = messageComposer(payload.array(), pSecret, SERVER_STEP, stuID);
                out.write(message);
                out.flush();
                ;
                return new int[] {};
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return null;
        }

        // public boolean stageD() {
        // try {
        // OutputStream out = tcpServer.getOutputStream();
        // Random numRandom = new Random(PORT_RANGE);
        // // preparing the message
        // int secretD = numRandom.nextInt();
        // ByteBuffer payload = ByteBuffer.allocate(4);
        // payload.putInt(secretD);
        // byte[] message = messageComposer(payload.array(), secretD, SERVER_STEP,
        // stuID);
        // out.write(message);
        // return true;
        // } catch (Exception e) {
        // System.out.println(e);
        //
        // }
        // // step D-2
        // }

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

        private static boolean verifyMessage2(byte[] data, int dataLen, int expPayLen,
                int expSecret, int expStep, byte[] expPay) {
            // verify total (padded) length including header
            int padding = expPayLen % 4 == 0 ? 0 : 4 - expPayLen % 4;
            int expPadLen = expPayLen + padding;
            if (expPadLen + HEADER_LENGTH != dataLen) {
                System.out.print("expPadLen: " + expPadLen + "  dataLen: " + dataLen);
                System.out.println();
                System.out.println("padding not valid.");
                return false;
            }
            // Get byte buffer
            ByteBuffer buf = ByteBuffer.wrap(data);
            // verify header contents
            boolean paylen = buf.getInt() != expPayLen;
            boolean secret = buf.getInt() != expSecret;
            boolean step = buf.getShort() != expStep;
            short stu_id = buf.getShort();
            if (paylen || secret || step) {
                System.out.println("Paylen: " + paylen + "  secret: " + secret + "  step: " + step);
                System.out.println("header not valid.");
                return false;
            }

            // verify payload
            for (int i = 0; i < expPayLen; i++) {
                if (buf.get() != expPay[i]) {
                    System.out.println("payload not valid.");
                    return false;
                }
            }
            // verify padding
            for (int i = 0; i < padding; i++) {
                if (buf.get() != 0) {
                    System.out.println("payload padding not valid.");
                    return false;
                }
            }
            return true;
        }

    }
}
