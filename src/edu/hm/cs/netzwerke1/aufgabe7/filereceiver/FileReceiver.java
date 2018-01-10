package edu.hm.cs.netzwerke1.aufgabe7.filereceiver;

import java.net.DatagramPacket;
import java.net.SocketException;

import edu.hm.cs.netzwerke1.aufgabe7.unreliableChannel.*;

public class FileReceiver {

  public static int PORT = 60000;
  public static int MAXUDPSEGMENTSIZE = 65527;
  
  
  public static void main(String... args) {
	FinateAutomaton automaton = new FinateAutomaton();
    UnreliableChannel unreliableChannel = null;
	try {
		unreliableChannel = new BaseChannel();
	} catch (SocketException e) {
		System.out.println(e.getMessage());
	}
    unreliableChannel = new DuplicateChannel(unreliableChannel);
    unreliableChannel = new LostChannel(unreliableChannel);
    unreliableChannel = new BitErrorChannel(unreliableChannel);
    
    DatagramPacket lastReceived = null;
	while (true) {
	  try {
		lastReceived = unreliableChannel.receive();
		automaton.processMsg(lastReceived);
	  } catch (Exception e) {
		System.out.println(e.getMessage());
	  } 
	}
  }
}