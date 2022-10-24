import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class Server {
    static final boolean DEBUG = true;
    static final int UDP_PORT = DEBUG ? 12281 : 12235;
    static final int HEADER_LENGTH = 12;
    static final short CLIENT_STEP = 1;
    static final short SERVER_STEP = 2;
    static final int TIMEOUT = 3000;
    static final String A1_STRING = "hello world\0";
    static final int A_SECRET = 0;

    static int num2;
    static int len2;
    static int secretC;
    static byte c;

    public static void main(String[] args) {
        DatagramSocket serverSocket = null;
        try {
            // create ServerSocket
            InetAddress hostAddress = InetAddress.getLocalHost();
            serverSocket = new DatagramSocket(UDP_PORT, hostAddress);
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
        private DatagramSocket udpSocket;
        private ServerSocket tcpSocketCD;
        private DatagramPacket clientPacketA;
        private final byte[] stageABuff;
        private short stuID;
        private BufferedOutputStream tcpOutStream;
        private BufferedInputStream tcpInput;

        public ClientHandler(DatagramSocket socket, DatagramPacket packet, byte[] buff) {
            this.udpSocket = socket;
            this.clientPacketA = packet;
            this.stageABuff = buff;
        }

        public void run() {
            int[] resultA = stageA();
            int[] resultB = stageB(resultA);
            stageC(resultB);
            stageD();
        }

        public int[] stageA() {
            // validate clientStageA
            if (!verifyMessage2(stageABuff, stageABuff.length, A1_STRING.length(), A_SECRET,
                    CLIENT_STEP, A1_STRING.getBytes())) {
                System.out.println("not valid!!!!");
                return null;
            }
            ByteBuffer clientBuff = ByteBuffer.wrap(stageABuff);
            stuID = clientBuff.getShort(HEADER_LENGTH - 2);
            System.out.println("client: " + stuID + " recived stageA");
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
                this.udpSocket.send(response);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                this.udpSocket = new DatagramSocket(udpPort, InetAddress.getLocalHost());
                udpSocket.setSoTimeout(TIMEOUT);
                return new int[] { num, len, udpPort, secretA };
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Thread.currentThread().interrupt();
            }
            return null;
        }

        public int[] stageB(int[] resultA) {
            int len = resultA[1];
            int num = resultA[0];
            int pSecret = resultA[3];

            System.out.println("client: " + stuID + " recived stageB");

            // variable num, len, udpPort are result from stage A.
            int expectedPayloadLen = len + 4;
            expectedPayloadLen = (expectedPayloadLen % 4 == 0) ? expectedPayloadLen
                    : expectedPayloadLen + (4 - expectedPayloadLen % 4);
            int expectedMsgLen = HEADER_LENGTH + expectedPayloadLen;

            int numReceived = 0;
            DatagramPacket clientPacket = null;
            try {
                while (numReceived < num) {
                    // listen for client
                    byte[] recivedBuff = new byte[expectedMsgLen];
                    clientPacket = new DatagramPacket(recivedBuff, recivedBuff.length);
                    udpSocket.receive(clientPacket); // block until receive

                    // after receive a packet, check if it is valid
                    byte[] exp_payload = ByteBuffer.allocate(len + 4).putInt(numReceived).array();
                    if (!verifyMessage2(recivedBuff, expectedMsgLen, len + 4, pSecret,
                            CLIENT_STEP, exp_payload)) {
                        System.out.println("Stage B not valid!!!!");
                        udpSocket.close();
                        return null;
                    }
                    // then randomly decide to drop the packet or not
                    Random boolRand = new Random();
                    boolean ack = boolRand.nextBoolean();
                    if (ack) {
                        // make payload
                        byte[] payload = ByteBuffer.allocate(4).putInt(numReceived).array();
                        byte[] buff = messageComposer(payload, pSecret, SERVER_STEP, stuID);
                        DatagramPacket response = new DatagramPacket(buff, buff.length,
                                clientPacket.getAddress(), clientPacket.getPort());
                        udpSocket.send(response);
                        numReceived++;
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Thread.currentThread().interrupt();
            }

            // after all packets are acknowledged
            try {
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
                udpSocket.send(response);
                udpSocket.close();
                System.out.println("client: " + stuID + " stageB finished");
                return new int[] { tcpPort, secretB };
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            return null;
        }

        public int[] stageC(int[] resultB) {
            int pSecret = resultB[1];
            try {
                System.out.println("client: " + stuID + " recived stageC");
                Socket client = tcpSocketCD.accept();
                tcpOutStream = new BufferedOutputStream(client.getOutputStream());
                tcpInput = new BufferedInputStream(client.getInputStream());

                Random numRandom = new Random();
                // preparing the message
                num2 = numRandom.nextInt(30);
                len2 = numRandom.nextInt(30);
                secretC = numRandom.nextInt(100);
                c = (byte) (numRandom.nextInt(94) + 33);
                ByteBuffer payload = ByteBuffer.allocate(13);
                payload.putInt(num2);
                payload.putInt(len2);
                payload.putInt(secretC);
                payload.put(c);
                byte[] message = messageComposer(payload.array(), pSecret, SERVER_STEP, stuID);
                tcpOutStream.write(message);
                tcpOutStream.flush();
                System.out.println("client: " + stuID + " stageC finished");
                return null;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Thread.currentThread().interrupt();
            }
            return null;
        }

        public void stageD() {
            int paddedPayloadLen = len2 % 4 == 0 ? len2 : len2 + (4 - len2 % 4);

            try {
                System.out.println("client: " + stuID + " recived stageD");

                byte[] recivedBuff = new byte[HEADER_LENGTH + paddedPayloadLen];
                for (int i = 0; i < num2; i++) {
                    int bytesReceived = tcpInput.read(recivedBuff);

                    byte[] exp_payload = new byte[len2];
                    Arrays.fill(exp_payload, c);
                    if (!verifyMessage2(recivedBuff, bytesReceived, len2,
                            secretC, CLIENT_STEP, exp_payload)) {
                        System.out.println("Stage D not valid!!!!");
                        tcpSocketCD.close();
                        return;
                    }
                }

                if (tcpInput.available() != 0) {
                    System.out.println("Too many packets sent");
                    tcpSocketCD.close();
                    return;
                }
            } catch (Exception e) {
                System.out.println(e);
            }
            // step D-2
            try {
                int secretD = new Random().nextInt(100);
                byte[] payload = ByteBuffer.allocate(4).putInt(secretD).array();
                byte[] message = messageComposer(payload, secretC, SERVER_STEP, stuID);
                tcpOutStream.write(message);
                tcpOutStream.flush();
                tcpSocketCD.close();
                System.out.println("client: " + stuID + " stageD finished");
                return;
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }

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

        private static boolean verifyMessage2(byte[] data, int dataLen, int expPayLen,
                int expSecret, int expStep, byte[] expPay) {
            // verify total (padded) length including header
            int padding = expPayLen % 4 == 0 ? 0 : 4 - expPayLen % 4;
            int expPadLen = expPayLen + padding;
            if (expPadLen + HEADER_LENGTH != dataLen) {
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
                return false;
            }

            // verify payload
            for (int i = 0; i < expPayLen; i++) {
                if (buf.get() != expPay[i]) {
                    return false;
                }
            }
            // verify padding
            for (int i = 0; i < padding; i++) {
                if (buf.get() != 0) {
                    return false;
                }
            }
            return true;
        }

    }
}
