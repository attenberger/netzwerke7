package edu.hm.cs.netzwerke1.aufgabe7.filereceiver;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

import edu.hm.cs.netzwerke1.aufgabe7.Package;

public class FinateAutomaton {

	private State currentState;
	private SocketAddress currentSender = null;
	private FileOutputStream writer = null;
	
	// 2D array defining all transitions that can occur
	private Transition[][] transition;
	
	
	public FinateAutomaton(){
		currentState = State.WAITNEXTFILE;
		// define all valid state transitions for our state machine
		// (undefined transitions will be ignored)
		transition = new Transition[State.values().length] [Msg.values().length];
		
		transition[State.WAITNEXTFILE.ordinal()] [Msg.START.ordinal()] = new BeginCommunication(); // ACK0 DO
    /*transition[State.WAITNEXTFILE.ordinal()] [Msg.STARTLAST.ordinal()]; // ACK0 DO
		transition[State.WAITNEXTFILE.ordinal()] [Msg.OK0.ordinal()]; // NAK
		transition[State.WAITNEXTFILE.ordinal()] [Msg.OK1.ordinal()]; // NAK
		transition[State.WAITNEXTFILE.ordinal()] [Msg.OK0LAST.ordinal()]; // NAK
    transition[State.WAITNEXTFILE.ordinal()] [Msg.OK1LAST.ordinal()]; // NAK
		transition[State.WAITNEXTFILE.ordinal()] [Msg.CORRUPT.ordinal()]; // NAK
		
		transition[State.WAIT0.ordinal()] [Msg.START.ordinal()]; // NAK
		transition[State.WAIT0.ordinal()] [Msg.STARTLAST.ordinal()]; // NAK
		transition[State.WAIT0.ordinal()] [Msg.OK0.ordinal()]; // ACK DO
		transition[State.WAIT0.ordinal()] [Msg.OK1.ordinal()]; // NAK
    transition[State.WAIT0.ordinal()] [Msg.OK0LAST.ordinal()]; // ACK DO
    transition[State.WAIT0.ordinal()] [Msg.OK1LAST.ordinal()]; // NAK
		transition[State.WAIT0.ordinal()] [Msg.CORRUPT.ordinal()]; // NAK
    transition[State.WAIT0.ordinal()] [Msg.DIFFERENTSENDER.ordinal()]; // NAK
		
		transition[State.WAIT1.ordinal()] [Msg.START.ordinal()]; // NAK
    transition[State.WAIT1.ordinal()] [Msg.STARTLAST.ordinal()]; // NAK
		transition[State.WAIT1.ordinal()] [Msg.OK0.ordinal()]; // NAK
		transition[State.WAIT1.ordinal()] [Msg.OK1.ordinal()]; // ACK DO
    transition[State.WAIT0.ordinal()] [Msg.OK0LAST.ordinal()]; // NAK
    transition[State.WAIT0.ordinal()] [Msg.OK1LAST.ordinal()]; // ACK DO
		transition[State.WAIT1.ordinal()] [Msg.CORRUPT.ordinal()]; // NAK
    transition[State.WAIT1.ordinal()] [Msg.DIFFERENTSENDER.ordinal()]; // NAK*/
	}
	
	public void processMsg(DatagramPacket receivedPacket) throws Exception{
	  Package udpDataPackage = new Package(receivedPacket);
	  Transition trans;
	  if (udpDataPackage.isCorrupt()) {
	    trans = transition[currentState.ordinal()][Msg.CORRUPT.ordinal()];
	  }
	  else if (udpDataPackage.isStart() && udpDataPackage.isLast()) {
	    trans = transition[currentState.ordinal()][Msg.STARTLAST.ordinal()];
	  }
	  else if (udpDataPackage.isStart()) {
      trans = transition[currentState.ordinal()][Msg.START.ordinal()];
    }
    else if (!receivedPacket.getSocketAddress().equals(currentSender)) {
      trans = transition[currentState.ordinal()][Msg.DIFFERENTSENDER.ordinal()];
    }
	  else if (udpDataPackage.getSequencenumber() == 0 && udpDataPackage.isLast()) {
      trans = transition[currentState.ordinal()][Msg.OK0LAST.ordinal()];
    }
	  else if (udpDataPackage.getSequencenumber() == 1 && udpDataPackage.isLast()) {
      trans = transition[currentState.ordinal()][Msg.OK1LAST.ordinal()];
    }
	  else if (udpDataPackage.getSequencenumber() == 0) {
      trans = transition[currentState.ordinal()][Msg.OK0.ordinal()];
    }
    else if (udpDataPackage.getSequencenumber() == 1) {
      trans = transition[currentState.ordinal()][Msg.OK1.ordinal()];
    }
    else {
      throw new Exception("An unexpected package was received!");
    }
	  
	  currentState = trans.execute(receivedPacket);
	}
	
	/**
	 * Abstract base class for all transitions.
	 * Derived classes need to override execute thereby defining the action
	 * to be performed whenever this transition occurs.
	 */
	abstract class Transition {
		abstract public State execute(DatagramPacket receivedPacket);
	}
	
	class BeginCommunication extends Transition {
    @Override
    public State execute(DatagramPacket receivedPacket) {
      currentSender = receivedPacket.getSocketAddress();
      Package udpDataPackage = new Package(receivedPacket);
      try {
        writer = new FileOutputStream(udpDataPackage.getFilename());
        writer.write(udpDataPackage.getContent());
      } catch (IOException e) {
        System.out.println("Error while writing file to disk!");
        System.out.println(e.getMessage());
        try {
          writer.close();
          writer = null;
        }
        catch (IOException e1) {}
        
        return State.WAITNEXTFILE;
      }
      try {
        DatagramSocket sender = new DatagramSocket(currentSender);
        sender.send(new DatagramPacket(new Package(true, 0).getRawData(), 8));
        sender.close();
      } catch (IOException e) {
        System.out.println("Could not send ACK!");
        System.out.println(e.getMessage());
        return State.WAITNEXTFILE;
      }
      
      return State.WAIT1;
    }
	  
	}
}
