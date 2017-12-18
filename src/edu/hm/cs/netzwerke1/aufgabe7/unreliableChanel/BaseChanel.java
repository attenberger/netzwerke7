package edu.hm.cs.netzwerke1.aufgabe7.unreliableChanel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class BaseChanel implements UnreliableChanel {

  public static int PORT = 60000;
  public static int MAXUDPSEGMENTSIZE = 65527;
  
  private DatagramSocket socket;
  
  public BaseChanel() throws SocketException {
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
