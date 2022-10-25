import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Part1 {
  // TODO: Change DEBUG to false before submitting
  // For testing our server:
  // change the "testing server" to the address of the server address
  static final String HOST = "attu2.cs.washington.edu";

  static final int PORT = 12235;
  static final int HEADER_LENGTH = 12;
  static final short CLIENT_STEP = 1;
  static final short SERVER_STEP = 2;
  static final short STU_ID = 397;
  static final int RESEND_TIMEOUT = 500;
  static final String A1_STRING = "hello world\0";
  static final int A_SECRET = 0;

  static int secretA;
  static int secretB;
  static int secretC;
  static int secretD;

  static int tcpPort;
  static Socket tcpSocket;

  public static void main(String[] args) {
      System.out.println("Program start:");
      System.out.println("--------------------------------");
      System.out.println("Stage A begin:");
      ByteBuffer stageAResult = stageA();
      System.out.println("--------------------------------");
      System.out.println("Stage B begin:");
      ByteBuffer stageBResult = stageB(stageAResult);
      System.out.println("--------------------------------");
      System.out.println("Stage C begin:");
      ByteBuffer stageCResult = stageC(stageBResult);
      System.out.println("--------------------------------");
      System.out.println("Stage D begin:");
      stageD(stageCResult);
      System.out.println("--------------------------------");
      System.out
          .println("secretA: " + secretA + ", secretB: " + secretB + ", secretC: " + secretC + ", secretD: " + secretD);
      System.out.println("Program end.");
  }

  public static ByteBuffer stageA() {
    int num, len, udpPort;

    try {
      // Sent message to the server
      DatagramSocket clientsocket = new DatagramSocket();
      byte[] message = messageComposer(A1_STRING.getBytes(), A_SECRET, CLIENT_STEP, STU_ID);
      DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(HOST), PORT);
      clientsocket.send(packet);
      // Receive message from the server
      byte[] receiveBuffer = new byte[HEADER_LENGTH + 16];
      DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
      clientsocket.receive(receivePacket);
      clientsocket.close();
      // Parse the message
      ByteBuffer respond = ByteBuffer.wrap(receiveBuffer);
      respond.position(HEADER_LENGTH);
      num = respond.getInt();
      len = respond.getInt();
      udpPort = respond.getInt();
      secretA = respond.getInt();
      System.out.println("num: " + num + " len: " + len + " udp_port: " + udpPort + " secretA: " + secretA);
      System.out.println("Stage A completed !!!!!");
      return respond;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static ByteBuffer stageB(ByteBuffer respond) {

    respond.position(HEADER_LENGTH);
    int num = respond.getInt();
    int len = respond.getInt();
    int udpPort = respond.getInt();
    int secretA = respond.getInt();

    int num_recived = 0;

    try {
      // Create a socket to receive the message
      DatagramSocket clientsocket = new DatagramSocket();
      do {
        boolean received = false;
        // Generate the package
        ByteBuffer payload = ByteBuffer.allocate(4 + len);
        payload.putInt(num_recived);
        byte[] message = messageComposer(payload.array(), secretA, CLIENT_STEP, STU_ID);
        // Sent message to the server
        while (!received) {
          DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(HOST), udpPort);
          clientsocket.send(packet);
          clientsocket.setSoTimeout(RESEND_TIMEOUT);
          // Receive message from the server
          byte[] receiveBuffer = new byte[HEADER_LENGTH + 4];
          DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
          try { // If the server does not respond, resend the message
            clientsocket.receive(receivePacket);
          } catch (SocketTimeoutException e) {
            System.out.println("Resend packet " + num_recived);
            continue;
          }
          // Successfully received
          // Parse the message
          ByteBuffer respondBuffer = ByteBuffer.wrap(receiveBuffer);
          respondBuffer.position(HEADER_LENGTH);
          int packetIdRecived = respondBuffer.getInt();
          if (packetIdRecived == num_recived) { // If the packet is the one we want
            System.out.println("Received packet " + num_recived);
            received = true;
            num_recived++;
          } else {
            clientsocket.close();
            throw new Exception("Wrong packet id");
          }
        }
      } while (num_recived != num);
      // Stage B2
      byte[] receiveBuffer = new byte[HEADER_LENGTH + 16];
      DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
      clientsocket.receive(receivePacket);
      clientsocket.close();
      // Parse the message
      ByteBuffer respondBuffer = ByteBuffer.wrap(receiveBuffer);
      respondBuffer.position(HEADER_LENGTH);
      tcpPort = respondBuffer.getInt();
      secretB = respondBuffer.getInt();
      System.out.println("tcpPort: " + tcpPort + " secretB: " + secretB);
      System.out.println("Stage B completed !!!!!");
      return respondBuffer;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static ByteBuffer stageC(ByteBuffer prevResp) {
    prevResp.position(HEADER_LENGTH);
    int tcpPort = prevResp.getInt();

    try {
      // create TCP connection
      tcpSocket = new Socket(InetAddress.getByName(HOST), tcpPort);
      // read from the server
      InputStream input = tcpSocket.getInputStream();
      byte[] buff = new byte[HEADER_LENGTH + 16];
      input.read(buff, 0, buff.length);
      // parse response
      ByteBuffer response = ByteBuffer.wrap(buff);
      response.position(HEADER_LENGTH);
      int num2 = response.getInt();
      int len2 = response.getInt();
      secretC = response.getInt();
      byte c = response.get();
      System.out.println("num2: " + num2 + " len2: " + len2 + " secretC: " + secretC + " c: " + c);
      System.out.println("Stage C completed !!!!!");
      return response;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static void stageD(ByteBuffer prevResp) {
    prevResp.position(HEADER_LENGTH);
    int num2 = prevResp.getInt();
    int len2 = prevResp.getInt();
    int secretC = prevResp.getInt();
    byte c = prevResp.get();

    int numPackageSend = 1;

    try {
      // Sent message to the server
      OutputStream output = tcpSocket.getOutputStream();
      while (numPackageSend <= num2) {
        // Generate the package
        int payloadSize = len2;
        ByteBuffer payload = ByteBuffer.allocate(payloadSize);
        for (int i = 0; i < payloadSize; i++) {
          payload.put(i, c);
        }
        byte[] message = messageComposer(payload.array(), secretC, CLIENT_STEP, STU_ID);
        System.out.println("packet " + numPackageSend + " content: " + Arrays.toString(message));
        // Sent message to the server
        output.write(message);
        System.out.println("Sent packet " + numPackageSend);
        numPackageSend++;
      }

      // read from the server
      InputStream input = tcpSocket.getInputStream();
      byte[] buff = new byte[HEADER_LENGTH + 4];
      input.read(buff, 0, buff.length);
      tcpSocket.close();
      // parse response
      ByteBuffer response = ByteBuffer.wrap(buff);
      response.position(HEADER_LENGTH);
      secretD = response.getInt();
      System.out.println("secretD: " + secretD);
      System.out.println("Stage D completed !!!!!");
    } catch (Exception e) {
      e.printStackTrace();
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
}