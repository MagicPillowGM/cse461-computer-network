import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;

public class Part1 {
  static final String HOST = "attu2.cs.washington.edu";
  static final int PORT = 12235;

  static final int HEADER_LENGTH = 12;
  static final short CLIENT_STEP = 1;
  static final short SERVER_STEP = 2;
  static final short STU_ID = 397;
  static final int RESEND_TIMEOUT = 1000;

  static final String A1_STRING = "hello world\0";
  static final int A_SECRET = 0;

  public static void main(String[] args) {
    System.out.println("Program start:");
    System.out.println("--------------------------------");
    System.out.println("Stage A begin:");
    ByteBuffer stageAResule = stageA();
    System.out.println("--------------------------------");
    System.out.println("Stage B begin:");
    stageB(stageAResule);
    System.out.println("--------------------------------");
  }

  public static ByteBuffer stageA() {
    int num, len, udp_port, secretA;
    num = len = udp_port = secretA = -1;

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
      udp_port = respond.getInt();
      secretA = respond.getInt();
      System.out.println("num: " + num + " len: " + len + " udp_port: " + udp_port + " secretA: " + secretA);
      System.out.println("Stage A completed !!!!!");
      return respond;
    } catch (Exception e) {
      System.out.println(e);
      return null;
    }
  }

  public static void stageB(ByteBuffer respond) {

    respond.position(HEADER_LENGTH);
    int num = respond.getInt();
    int len = respond.getInt();
    int udp_port = respond.getInt();
    int secretA = respond.getInt();

    int num_recived = 0;

    try {
      do {
        boolean received = false;
        // Generate the package
        int payloadSize = (len % 4 == 0) ? len : len + (4 - len % 4);
        ByteBuffer payload = ByteBuffer.allocate(4 + payloadSize);
        payload.putInt(num_recived);
        byte[] message = messageComposer(payload.array(), secretA, CLIENT_STEP, STU_ID);
        // Sent message to the server
        while (!received) {
          DatagramSocket clientsocket = new DatagramSocket();
          DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(HOST), udp_port);
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
          // Successfully recived
          // Parse the message
          ByteBuffer respondBuffer = ByteBuffer.wrap(receiveBuffer);
          respondBuffer.position(HEADER_LENGTH);
          int packet_id_recived = respondBuffer.getInt();
          if (packet_id_recived == num_recived) { // If the packet is the one we want
            System.out.println("Received packet " + num_recived);
            received = true;
            num_recived++;
          } else {
            clientsocket.close();
            throw new Exception("Wrong packet id");
          }
          clientsocket.close();
        }
      } while (num_recived != num);
      System.out.println("Stage B completed !!!!!");
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  public static ByteBuffer stageC(ByteBuffer prevResp) {
    prevResp.position(HEADER_LENGTH);
    int tcpPort = prevResp.getInt();
    int secretB = prevResp.getInt();

    try (Socket socket = new Socket(InetAddress.getByName(HOST), tcpPort)) {
      // ? Seems we don't need to send anything to the server?
      //
      // read from the server
      InputStream input = socket.getInputStream();
      byte[] buff = new byte[HEADER_LENGTH + 16];
      int numRead = input.read(buff);
      while (numRead > 0) {
        numRead = input.read(buff);
      }
      // parse response
      ByteBuffer response = ByteBuffer.wrap(buff);
      response.position(HEADER_LENGTH);
      int num2 = response.getInt();
      int len2 = response.getInt();
      int secretC = response.getInt();
      char c = response.getChar();
      System.out.println("num2:" + num2 + " len2:" + len2 + "secretC:" + secretC + "c:" + c);
      return response;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return null;
    }
  }

  // Compose the message following the protocol
  private static byte[] messageComposer(byte[] payload, int secret, short step, short stu_id) {
    int payloadLength = (payload.length % 4 == 0) ? payload.length : payload.length + (4 - payload.length % 4);
    ByteBuffer message = ByteBuffer.allocate(HEADER_LENGTH + payloadLength);
    message.putInt(payloadLength);
    message.putInt(secret);
    message.putShort(step);
    message.putShort(stu_id);
    message.put(payload);
    return message.array();
  }
}