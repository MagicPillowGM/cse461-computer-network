//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//import java.net.Socket;
//import java.nio.ByteBuffer;
//
//public class Client {
//    static final String HOST = "attu2.cs.washington.edu";
//    static final int INITIAL_PORT = 12235;
//    final DatagramSocket UDP_SOCKET = null;
////    static int tcpPort;
////    static Socket tcpSocket;
//
//    static final int HEADER_LENGTH = 12;
//    static final short CLIENT_STEP = 1;
//    static final short SERVER_STEP = 2;
//    static final short STU_ID = 397;
//    static final int RESEND_TIMEOUT = 1000;
//    static final String A1_STRING = "hello world\0";
//    static final int A_SECRET = 0;
//
//    public static void main(String[] args) {
//        System.out.println("Program start:");
//        System.out.println("--------------------------------");
//        System.out.println("Stage A begin:");
//        ByteBuffer stageAResult = stageA();
//        System.out.println("--------------------------------");
//        System.out.println("Stage B begin:");
//        ByteBuffer stageBResult = stageB(stageAResult);
//        System.out.println("--------------------------------");
//        System.out.println("Stage C begin:");
//        ByteBuffer stageCResult = stageC(stageBResult);
//        System.out.println("--------------------------------");
//        System.out.println("Stage D begin:");
//        stageD(stageCResult);
//        System.out.println("--------------------------------");
//        System.out.println("Program end.");
//    }
//
//    public static ByteBuffer stageA() {
//        try {
//
//            // Sent message to the server
//            DatagramSocket clientsocket = new DatagramSocket();
//            byte[] message = messageComposer(A1_STRING.getBytes(), A_SECRET, CLIENT_STEP, STU_ID);
//            DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(HOST), PORT);
//            clientsocket.send(packet);
//            // Receive message from the server
//            byte[] receiveBuffer = new byte[HEADER_LENGTH + 16];
//            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
//            clientsocket.receive(receivePacket);
//            clientsocket.close();
//            // Parse the message
//            ByteBuffer respond = ByteBuffer.wrap(receiveBuffer);
//            respond.position(HEADER_LENGTH);
//            int num = respond.getInt();
//            int len = respond.getInt();
//            int udpPort = respond.getInt();
//            int secretA = respond.getInt();
//            System.out.println("num: " + num + " len: " + len + " udp_port: " + udpPort + " secretA: " + secretA);
//            System.out.println("Stage A completed !!!!!");
//            return respond;
//        } catch (Exception e) {
//            System.out.println(e);
//            return null;
//        }
//    }
//
//    // Compose the message following the protocol
//    private static byte[] messageComposer(byte[] payload, int secret, short step, short stu_id) {
//        int payloadLength = (payload.length % 4 == 0) ? payload.length : payload.length + (4 - payload.length % 4);
//        ByteBuffer message = ByteBuffer.allocate(HEADER_LENGTH + payloadLength);
//        message.putInt(payloadLength);
//        message.putInt(secret);
//        message.putShort(step);
//        message.putShort(stu_id);
//        message.put(payload);
//        return message.array();
//    }
//}
