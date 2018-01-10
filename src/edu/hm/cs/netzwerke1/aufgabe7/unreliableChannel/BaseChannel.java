package edu.hm.cs.netzwerke1.aufgabe7.unreliableChannel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Channel that receives data per UDP at Port 60000. The channel can be decorated
 * with unreliabilities.
 * @author Attenberger
 *
 */
public class BaseChannel implements UnreliableChannel {

  public static int PORT = 60000;
  public static int MAXUDPSEGMENTSIZE = 65527;
  
  private DatagramSocket socket;
  
  /**
   * Creates a new BaseChannel.
   * @throws SocketException thrown if an error in the socketconnection occurs.
   */
  public BaseChannel() throws SocketException {
	  socket = new DatagramSocket(PORT);
  }
  
  @Override
  public void finalize() {
	  if (socket != null)
		  socket.close();
  }

  @Override
  public DatagramPacket receive() throws IOException {
    DatagramPacket receivedPacket = new DatagramPacket(new byte[MAXUDPSEGMENTSIZE], MAXUDPSEGMENTSIZE);
    socket.receive(receivedPacket);
    return receivedPacket;
  }
}
