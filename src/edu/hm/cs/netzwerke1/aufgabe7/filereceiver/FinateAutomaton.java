package edu.hm.cs.netzwerke1.aufgabe7.filereceiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Date;

import edu.hm.cs.netzwerke1.aufgabe7.Package;

public class FinateAutomaton {

	private static final int WAITTIMELASTDUPPLICATE = 5000;
  
  private State currentState;
	private SocketAddress currentSender = null;
	private FileOutputStream writer = null;
	private Date lastTransmitionStart = null;
	private int bytesCurrentTransmition = 0;
	private Thread currentLastDuplicateTimer = null;
	private Package lastReceivedPackage = null;

	// 2D array defining all transitions that can occur
	private Transition[][] transition;


	public FinateAutomaton() {
		currentState = State.WAITNEXTFILE;
		// define all valid state transitions for our state machine
		// (undefined transitions will be ignored)
		transition = new Transition[State.values().length][Msg.values().length];

		transition[State.WAITNEXTFILE.ordinal()][Msg.START.ordinal()] = beginCommunication;
		transition[State.WAITNEXTFILE.ordinal()][Msg.STARTLAST.ordinal()] = beginEndCommunication;
		transition[State.WAITNEXTFILE.ordinal()][Msg.OK0.ordinal()] = corruptUnexpeted;
		transition[State.WAITNEXTFILE.ordinal()][Msg.OK1.ordinal()] = corruptUnexpeted;
		transition[State.WAITNEXTFILE.ordinal()][Msg.OK0LAST.ordinal()] = corruptUnexpeted;
		transition[State.WAITNEXTFILE.ordinal()][Msg.OK1LAST.ordinal()] = corruptUnexpeted;
		transition[State.WAITNEXTFILE.ordinal()][Msg.CORRUPT.ordinal()] = corruptUnexpeted;
		transition[State.WAITNEXTFILE.ordinal()][Msg.DIFFERENTSENDER.ordinal()] = null;

		transition[State.WAIT0.ordinal()][Msg.START.ordinal()] = corruptUnexpeted;
		transition[State.WAIT0.ordinal()][Msg.STARTLAST.ordinal()] = corruptUnexpeted;
		transition[State.WAIT0.ordinal()][Msg.OK0.ordinal()] = proccedOk0;
		transition[State.WAIT0.ordinal()][Msg.OK1.ordinal()] = repeatAck;
		transition[State.WAIT0.ordinal()][Msg.OK0LAST.ordinal()] = proccedOkLast;
		transition[State.WAIT0.ordinal()][Msg.OK1LAST.ordinal()] = null;
		transition[State.WAIT0.ordinal()][Msg.CORRUPT.ordinal()] = corruptUnexpeted;
		transition[State.WAIT0.ordinal()][Msg.DIFFERENTSENDER.ordinal()] = corruptUnexpeted;

		transition[State.WAIT1.ordinal()][Msg.START.ordinal()] = corruptUnexpeted;
		transition[State.WAIT1.ordinal()][Msg.STARTLAST.ordinal()] = corruptUnexpeted;
		transition[State.WAIT1.ordinal()][Msg.OK0.ordinal()] = repeatAck;
		transition[State.WAIT1.ordinal()][Msg.OK1.ordinal()] = proccedOk1;
		transition[State.WAIT1.ordinal()][Msg.OK0LAST.ordinal()] = null;
		transition[State.WAIT1.ordinal()][Msg.OK1LAST.ordinal()] = proccedOkLast;
		transition[State.WAIT1.ordinal()][Msg.CORRUPT.ordinal()] = corruptUnexpeted;
		transition[State.WAIT1.ordinal()][Msg.DIFFERENTSENDER.ordinal()] = corruptUnexpeted;
		
		transition[State.WAITDUPLIKATELAST.ordinal()][Msg.START.ordinal()] = corruptUnexpeted;
    transition[State.WAITDUPLIKATELAST.ordinal()][Msg.STARTLAST.ordinal()] = repeatAckLastStart;
    transition[State.WAITDUPLIKATELAST.ordinal()][Msg.OK0.ordinal()] = corruptUnexpeted;
    transition[State.WAITDUPLIKATELAST.ordinal()][Msg.OK1.ordinal()] = corruptUnexpeted;
    transition[State.WAITDUPLIKATELAST.ordinal()][Msg.OK0LAST.ordinal()] = repeatAckLast;
    transition[State.WAITDUPLIKATELAST.ordinal()][Msg.OK1LAST.ordinal()] = repeatAckLast;
    transition[State.WAITDUPLIKATELAST.ordinal()][Msg.CORRUPT.ordinal()] = corruptUnexpeted;
    transition[State.WAITDUPLIKATELAST.ordinal()][Msg.DIFFERENTSENDER.ordinal()] = corruptUnexpeted;
	}
	
	private Runnable lastDuplicateTimer = new Runnable() {
    @Override
    public void run() {
      if (bytesCurrentTransmition == 0)
        System.out.println("No data transmitted.");
      else {
        double transmitionDuration = (new Date().getTime() - lastTransmitionStart.getTime()) / 1000.0;
        double rate = bytesCurrentTransmition / 1048576.0 / transmitionDuration;
        System.out.printf("File transmitted. %d Bytes in %f sec = %f MB/s\r\n", bytesCurrentTransmition, transmitionDuration, rate);
      }
      bytesCurrentTransmition = 0;
      lastTransmitionStart = null;
      
      boolean interrupted = true;
      while (interrupted) {
        interrupted = false;
        try {
          Thread.sleep(WAITTIMELASTDUPPLICATE);
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
      closeConnection();
      currentState = State.WAITNEXTFILE;
    }
    
  };

	public void processMsg(DatagramPacket receivedPacket) throws Exception {
		Package udpDataPackage = new Package(receivedPacket);
		Transition trans;

		if (!udpDataPackage.isOk()) {
			trans = transition[currentState.ordinal()][Msg.CORRUPT.ordinal()];
		} else if (udpDataPackage.isStart() && currentState != State.WAITNEXTFILE && currentSender.equals(receivedPacket.getSocketAddress())) {
		  trans = transition[currentState.ordinal()][Msg.OK0.ordinal()];
		} else if (udpDataPackage.isStart() && currentState != State.WAITNEXTFILE) {
			trans = transition[currentState.ordinal()][Msg.DIFFERENTSENDER.ordinal()];
		} else if (udpDataPackage.isStart() && udpDataPackage.isLast()) {
			trans = transition[currentState.ordinal()][Msg.STARTLAST.ordinal()];
		} else if (udpDataPackage.isStart()) {
			trans = transition[currentState.ordinal()][Msg.START.ordinal()];
		} else if (!receivedPacket.getSocketAddress().equals(currentSender)) {
			trans = transition[currentState.ordinal()][Msg.DIFFERENTSENDER.ordinal()];
		} else if (udpDataPackage.getSequencenumber() == 0 && udpDataPackage.isLast()) {
			trans = transition[currentState.ordinal()][Msg.OK0LAST.ordinal()];
		} else if (udpDataPackage.getSequencenumber() == 1 && udpDataPackage.isLast()) {
			trans = transition[currentState.ordinal()][Msg.OK1LAST.ordinal()];
		} else if (udpDataPackage.getSequencenumber() == 0) {
			trans = transition[currentState.ordinal()][Msg.OK0.ordinal()];
		} else if (udpDataPackage.getSequencenumber() == 1) {
			trans = transition[currentState.ordinal()][Msg.OK1.ordinal()];
		} else {
			throw new Exception("An unexpected package was received!");
		}

		currentState = trans.execute(receivedPacket);
	}

	Transition beginCommunication = (receivedPacket) -> {
		Package innerPackage = new Package(receivedPacket);

		initNewConnection(receivedPacket);
		writeDataToFile(innerPackage.getContent());
		sendAckNak(receivedPacket, true);
		lastReceivedPackage = innerPackage;
		return State.WAIT1;
	};

	Transition beginEndCommunication = (receivedPacket) -> {
		Package innerPackage = new Package(receivedPacket);

		initNewConnection(receivedPacket);
		writeDataToFile(innerPackage.getContent());
		sendAckNak(receivedPacket, true);
    lastReceivedPackage = innerPackage;
		currentLastDuplicateTimer = new Thread(lastDuplicateTimer);
		currentLastDuplicateTimer.start();
		return State.WAITDUPLIKATELAST;
	};

	Transition corruptUnexpeted = (receivedPacket) -> {
		sendAckNak(receivedPacket, false);
		return currentState;
	};

	Transition proccedOk0 = (receivedPacket) -> {
		Package innerPackage = new Package(receivedPacket);
		writeDataToFile(innerPackage.getContent());
		sendAckNak(receivedPacket, true);
    lastReceivedPackage = innerPackage;
		return State.WAIT1;
	};

	Transition repeatAck = (receivedPacket) -> {
		sendAckNak(receivedPacket, true);
    lastReceivedPackage = new Package(receivedPacket);
		return currentState;
	};

	Transition proccedOkLast = (receivedPacket) -> {
		Package innerPackage = new Package(receivedPacket);
		writeDataToFile(innerPackage.getContent());
		sendAckNak(receivedPacket, true);
    lastReceivedPackage = innerPackage;
    currentLastDuplicateTimer = new Thread(lastDuplicateTimer);
    currentLastDuplicateTimer.start();
		return State.WAITDUPLIKATELAST;
	};

	Transition proccedOk1 = (receivedPacket) -> {
		Package innerPackage = new Package(receivedPacket);
		writeDataToFile(innerPackage.getContent());
		sendAckNak(receivedPacket, true);
    lastReceivedPackage = innerPackage;
		return State.WAIT0;
	};

	Transition repeatAckLast = (receivedPacket) -> {
	  sendAckNak(receivedPacket, true);
	  currentLastDuplicateTimer.interrupt();
    lastReceivedPackage = new Package(receivedPacket);
	  return currentState;
	};
	
	Transition repeatAckLastStart = (receivedPacket) -> {
    Package innerPackage = new Package(receivedPacket);
    if (innerPackage.equals(lastReceivedPackage)) {
      sendAckNak(receivedPacket, true);
      currentLastDuplicateTimer.interrupt();
      lastReceivedPackage = new Package(receivedPacket);
    }
    else {
      sendAckNak(receivedPacket, false); // Equals unexpected / currupt
    }
    return currentState;
  };


	private void initNewConnection(DatagramPacket receivedPacket) throws IOException {
		currentSender = receivedPacket.getSocketAddress();
		lastTransmitionStart = new Date();
		bytesCurrentTransmition = 0;
		Package innerPackage = new Package(receivedPacket);

		try {
			File file = new File(innerPackage.getFilename());
			System.out.println("Writing file to: " + file.getAbsolutePath());
			writer = new FileOutputStream(file, false);
		} catch (IOException e) {
			try {
				writer.close();
				writer = null;
			} catch (IOException e1) {
			}
			throw new IOException("Error while writing file to disk!" + e.getMessage());
		}
	}

	private void closeConnection() {
		try {
			writer.close();
		} catch (IOException e) {
		} finally {
			writer = null;
		}
	}

	private void writeDataToFile(byte[] content) throws IOException {
		bytesCurrentTransmition += content.length;
		try {
			writer.write(content);
		} catch (IOException e) {
			try {
				writer.close();
				writer = null;
			} catch (IOException e1) {
			}
			throw new IOException("Error while writing file to disk!" + e.getMessage());
		}
	}

	private void sendAckNak(DatagramPacket receivedPacket, boolean ack) throws IOException {
		Package innerPackage = new Package(receivedPacket);
		Package ackPackage = new Package(ack, innerPackage.getSequencenumber());
		DatagramSocket sender = null;
		try {
			sender = new DatagramSocket();
			sender.send(new DatagramPacket(ackPackage.getRawData(), ackPackage.getRawData().length, receivedPacket.getAddress(), receivedPacket.getPort()));
		} catch (IOException e) {
			throw new IOException("Could not send ACK! " + e.getMessage());
		} finally {
			sender.close();
		}
	}
}
