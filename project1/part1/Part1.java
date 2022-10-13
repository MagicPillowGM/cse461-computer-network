import java.net.*;
import java.nio.ByteBuffer;

public class Part1 {
  static final String HOST = "attu2.cs.washington.edu";
  static final int PORT = 12235;

  static final int HEADER_LENGTH = 12;
  static final short CLIENT_STEP = 1;
  static final short SERVER_STEP = 2;
  static final short STU_ID = 397;

  static final String A1_STRING = "hello world\0";
  static final int A_SECRET = 0;

  public static void main(String[] args) {
    stageA();
  }

  public static void stageA() {
    int num, len, udp_port, secretA;
    num = len = udp_port = secretA = -1;

    try {
      // Sent message to the server
      DatagramSocket clientsocket = new DatagramSocket();
      byte[] buffer = messageComposer(A1_STRING.getBytes(), A_SECRET, CLIENT_STEP, STU_ID);
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(HOST), PORT);
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

    } catch (Exception e) {
      System.out.println(e);
    }
  }

  // Compose the message following the protocol
  private static byte[] messageComposer(byte[] payload, int secret, short step, short stu_id) {
    int payloadLength = (payload.length % 4 == 0) ? payload.length : payload.length + (4 - payload.length % 4);
    byte[] message = new byte[HEADER_LENGTH + payloadLength];
    byte[] payload_len_bytes = ByteBuffer.allocate(4).putInt(payloadLength).array();
    byte[] secret_bytes = ByteBuffer.allocate(4).putInt(secret).array();
    byte[] step_bytes = ByteBuffer.allocate(2).putShort(step).array();
    byte[] stu_id_bytes = ByteBuffer.allocate(2).putShort(stu_id).array();

    System.arraycopy(payload_len_bytes, 0, message, 0, 4);
    System.arraycopy(secret_bytes, 0, message, 4, 4);
    System.arraycopy(step_bytes, 0, message, 8, 2);
    System.arraycopy(stu_id_bytes, 0, message, 10, 2);
    System.arraycopy(payload, 0, message, 12, payloadLength);
    return message;
  }
}