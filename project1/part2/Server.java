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
    static Random Rand = new Random();

    static int num;
    static int len;
    static int udpPort;
    static int tcpPort;
    static int secretB;
    static int secretA;
    static int secretC;
    static int secretD;
    static int num2;
    static int len2;
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
            try {
                stageA();
                stageB();
                stageC();
                stageD();
                System.out.println("client " + stuID + " thread finished!");
                System.out.println("secretA: " + secretA + " secretB: " + secretB + " secretC: " + secretC);
                System.out.println("================================");
            } catch (Exception e) {
                System.out.println("program end with exception: ");
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally { // close all the socket
                if (udpSocket != null) {
                    udpSocket.close();
                }
                if (tcpSocketCD != null) {
                    try {
                        tcpSocketCD.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void stageA() {
            try {
                // validate clientStageA
                if (!verifyMessage2(stageABuff, stageABuff.length, A1_STRING.length(), A_SECRET,
                        CLIENT_STEP, A1_STRING.getBytes())) {
                    throw new Exception("client stageA message is not valid!");
                }
                // get client's stuID
                ByteBuffer clientBuff = ByteBuffer.wrap(stageABuff);
                stuID = clientBuff.getShort(HEADER_LENGTH - 2);
                System.out.println("client: " + stuID + " recived stageA");
                // make payload
                num = Rand.nextInt(30);
                len = Rand.nextInt(30);
                udpPort = Rand.nextInt(65535 - 256) + 256;
                secretA = Rand.nextInt(100);
                ByteBuffer payload = ByteBuffer.allocate(16);
                payload.putInt(num);
                payload.putInt(len);
                payload.putInt(udpPort);
                payload.putInt(secretA);
                // compose response
                byte[] buff = messageComposer(payload.array(), A_SECRET, SERVER_STEP, stuID);
                DatagramPacket response = new DatagramPacket(buff, buff.length,
                        clientPacketA.getAddress(), clientPacketA.getPort());

                // send response
                this.udpSocket.send(response);

                // setup new upd socket fot stageB
                udpSocket = new DatagramSocket(udpPort, InetAddress.getLocalHost());
                udpSocket.setSoTimeout(TIMEOUT);
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        public void stageB() throws Exception {
            try {
                System.out.println("client: " + stuID + " recived stageB");
                // variable num, len, udpPort are result from stage A.
                int exp_PayloadLen = len + 4;
                exp_PayloadLen = (exp_PayloadLen % 4 == 0) ? exp_PayloadLen : exp_PayloadLen + (4 - exp_PayloadLen % 4);
                int exp_MsgLen = HEADER_LENGTH + exp_PayloadLen;
                // receive client's stageB
                int numReceived = 0;
                byte[] recivedBuff = new byte[exp_MsgLen];
                DatagramPacket clientPacket = new DatagramPacket(recivedBuff, recivedBuff.length);

                while (numReceived < num) {
                    // listen for client
                    udpSocket.receive(clientPacket); // block until receive

                    // after receive a packet, check if it is valid
                    byte[] exp_payload = ByteBuffer.allocate(len + 4).putInt(numReceived).array();
                    if (!verifyMessage2(recivedBuff, exp_MsgLen, len + 4, secretA,
                            CLIENT_STEP, exp_payload)) {
                        throw new Exception("client stageB message is not valid!");
                    }
                    // then randomly decide to drop the packet or not
                    Random boolRand = new Random();
                    boolean ack = boolRand.nextBoolean();
                    if (ack) {
                        // make payload
                        byte[] payload = ByteBuffer.allocate(4).putInt(numReceived).array();
                        byte[] buff = messageComposer(payload, secretA, SERVER_STEP, stuID);
                        DatagramPacket response = new DatagramPacket(buff, buff.length,
                                clientPacket.getAddress(), clientPacket.getPort());
                        udpSocket.send(response);
                        numReceived++;
                    }
                }

                // after all packets are acknowledged
                // make response payload
                tcpPort = Rand.nextInt(65535 - 256) + 256;
                secretB = Rand.nextInt(30);
                ByteBuffer payload = ByteBuffer.allocate(8);
                payload.putInt(tcpPort);
                payload.putInt(secretB);

                // set up tcp socket
                tcpSocketCD = new ServerSocket(tcpPort);
                tcpSocketCD.setSoTimeout(TIMEOUT);

                // compose response packet
                byte[] buff = messageComposer(payload.array(), secretA, SERVER_STEP, stuID);
                DatagramPacket response = new DatagramPacket(buff, buff.length,
                        clientPacket.getAddress(), clientPacket.getPort());
                udpSocket.send(response);
                System.out.println("client: " + stuID + " stageB finished");
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                if (udpSocket != null) {
                    udpSocket.close();
                }
            }
        }

        public void stageC() {
            try {
                System.out.println("client: " + stuID + " recived stageC");
                Socket client = tcpSocketCD.accept();
                tcpOutStream = new BufferedOutputStream(client.getOutputStream());
                tcpInput = new BufferedInputStream(client.getInputStream());

                // preparing the message
                num2 = Rand.nextInt(30);
                len2 = Rand.nextInt(30);
                secretC = Rand.nextInt(100);
                c = (byte) (Rand.nextInt(94) + 33);
                ByteBuffer payload = ByteBuffer.allocate(13);
                payload.putInt(num2);
                payload.putInt(len2);
                payload.putInt(secretC);
                payload.put(c);
                byte[] message = messageComposer(payload.array(), secretB, SERVER_STEP, stuID);
                tcpOutStream.write(message);
                tcpOutStream.flush();
                System.out.println("client: " + stuID + " stageC finished");
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        public void stageD() {
            try {
                System.out.println("client: " + stuID + " recived stageD");
                // receive client's stageD messages
                int paddedPayloadLen = len2 % 4 == 0 ? len2 : len2 + (4 - len2 % 4);
                byte[] recivedBuff = new byte[HEADER_LENGTH + paddedPayloadLen];
                for (int i = 0; i < num2; i++) {
                    int bytesReceived = tcpInput.read(recivedBuff);
                    byte[] exp_payload = new byte[len2];
                    Arrays.fill(exp_payload, c);
                    if (!verifyMessage2(recivedBuff, bytesReceived, len2,
                            secretC, CLIENT_STEP, exp_payload)) {
                        throw new Exception("client stageD message is not valid!");
                    }
                }
                // step D-2
                secretD = new Random().nextInt(100);
                byte[] payload = ByteBuffer.allocate(4).putInt(secretD).array();
                byte[] message = messageComposer(payload, secretC, SERVER_STEP, stuID);
                tcpOutStream.write(message);
                tcpOutStream.flush();
                tcpSocketCD.close();
                System.out.println("client: " + stuID + " stageD finished");
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                if (tcpSocketCD != null) {
                    try {
                        tcpSocketCD.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
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
