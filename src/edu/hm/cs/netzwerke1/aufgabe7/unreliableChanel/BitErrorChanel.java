package edu.hm.cs.netzwerke1.aufgabe7.unreliableChanel;

import java.io.IOException;
import java.net.DatagramPacket;

public class BitErrorChanel implements UnreliableChanel {


  private static final double PROBABILITYBITERROR = 0.05;
  
  private UnreliableChanel chanel;

  public BitErrorChanel(UnreliableChanel chanel) {
    this.chanel = chanel;
  }

  @Override
  public DatagramPacket receive() throws IOException {
	DatagramPacket packet = chanel.receive();
	if (Math.random() < PROBABILITYBITERROR) {
	  byte[] data = packet.getData();
      byte errorpattern = 0b00001000;
      data[(int)(data.length * 0.5f)] = (byte)(data[(int)(data.length * 0.5f)] ^ errorpattern);
      packet.setData(data);
	}  
    return packet;
  }
}
