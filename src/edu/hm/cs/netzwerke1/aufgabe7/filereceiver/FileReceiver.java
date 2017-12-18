package edu.hm.cs.netzwerke1.aufgabe7.filereceiver;

import java.net.DatagramPacket;
import java.net.SocketException;

import edu.hm.cs.netzwerke1.aufgabe7.unreliableChanel.*;

public class FileReceiver {

  public static int PORT = 60000;
  public static int MAXUDPSEGMENTSIZE = 65527;
  
  
  public static void main(String... args) {
	FinateAutomaton automaton = new FinateAutomaton();
    UnreliableChanel unreliableChanel = null;
	try {
		unreliableChanel = new BaseChanel();
	} catch (SocketException e) {
		System.out.println(e.getMessage());
	}
    unreliableChanel = new DuplicateChanel(unreliableChanel);
    unreliableChanel = new LostChanel(unreliableChanel);
    unreliableChanel = new BitErrorChanel(unreliableChanel);
    
    DatagramPacket lastReceived = null;
	while (true) {
	  try {
		lastReceived = unreliableChanel.receive();
		automaton.processMsg(lastReceived);
	  } catch (Exception e) {
		System.out.println(e.getMessage());
	  } 
	}
  }
}