package edu.hm.cs.netzwerke1.aufgabe7.filereceiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Date;

import edu.hm.cs.netzwerke1.aufgabe7.Package;

/**
 * Finate automation for the file receiver.
 * @author Attenberger
 */
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

	/**
	 * Creates a new finate automation.
	 */
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
	
	/**
	 * Runs a timer that ensure that if the acknowledgement of the last package was not received correctly by the file sender, that the
	 * sender has the possibility to retransmit the last package an can get a new acknowledgement.
	 */
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

    /**
     * Decides what to do with a received package.
     * @param receivedPacket DatagramPacket received
     * @throws Exception
     */
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

	/**
	 * Executed if the begin of a new file is received correctly.
	 */
	Transition beginCommunication = (receivedPacket) -> {
		Package innerPackage = new Package(receivedPacket);

		initNewConnection(receivedPacket);
		writeDataToFile(innerPackage.getContent());
		sendAckNak(receivedPacket, true);
		lastReceivedPackage = innerPackage;
		return State.WAIT1;
	};

	/**
	 * Executed if the begin of a new file is received correctly that only contains of one package.
	 */
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

	/**
	 * Executed if the received package was corrupt.
	 */
	Transition corruptUnexpeted = (receivedPacket) -> {
		sendAckNak(receivedPacket, false);
		return currentState;
	};

	/**
	 * Executed if a package with sequencenumber 0 was received correctly and sequencenumber 0 was expected.
	 */
	Transition proccedOk0 = (receivedPacket) -> {
		Package innerPackage = new Package(receivedPacket);
		writeDataToFile(innerPackage.getContent());
		sendAckNak(receivedPacket, true);
		lastReceivedPackage = innerPackage;
		return State.WAIT1;
	};

	/**
	 * Executed if a package was received correctly but another sequencenumber was expected.
	 */
	Transition repeatAck = (receivedPacket) -> {
		sendAckNak(receivedPacket, true);
		lastReceivedPackage = new Package(receivedPacket);
		return currentState;
	};

	/**
	 * Executed if a package was received correctly with the expected sequencenumber, which was the last of the file.
	 */
	Transition proccedOkLast = (receivedPacket) -> {
		Package innerPackage = new Package(receivedPacket);
		writeDataToFile(innerPackage.getContent());
		sendAckNak(receivedPacket, true);
	    lastReceivedPackage = innerPackage;
	    currentLastDuplicateTimer = new Thread(lastDuplicateTimer);
	    currentLastDuplicateTimer.start();
		return State.WAITDUPLIKATELAST;
	};

	/**
	 * Executed if a package with sequencenumber 1 was correctly received, not the last package of the file
	 * and sequencenumber 1 was expected.
	 */
	Transition proccedOk1 = (receivedPacket) -> {
		Package innerPackage = new Package(receivedPacket);
		writeDataToFile(innerPackage.getContent());
		sendAckNak(receivedPacket, true);
    lastReceivedPackage = innerPackage;
		return State.WAIT0;
	};

	/**
	 * Executed if the last package of the file was already received and was now received again correctly.
	 */
	Transition repeatAckLast = (receivedPacket) -> {
	  sendAckNak(receivedPacket, true);
	  currentLastDuplicateTimer.interrupt();
    lastReceivedPackage = new Package(receivedPacket);
	  return currentState;
	};
	
	/**
	 * Executed if a file consisting of only one package is received again correctly.
	 */
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


  /**
   * Sets the parameters for the new file and opens a stream to write the file to the filesystem.
   * @param receivedPacket First package of the file
   * @throws IOException thrown if an error occurs while open the fileoutputstream to write the file to the filesystem.
   */
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

	/**
	 * Closes the fileoutputstream to write file to the filesystem.
	 */
	private void closeConnection() {
		try {
			writer.close();
		} catch (IOException e) {
		} finally {
			writer = null;
		}
	}

	/**
	 * Writes data in the current file in the filesystem.
	 * @param content data to write
	 * @throws IOException thrown if an error occurs while writing the data
	 */
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

	/**
	 * Sends a ACK or NAK to the current sender of the file, to confirm a received package.
	 * @param receivedPacket received package
	 * @param ack true if the package was currect received, else false
	 * @throws IOException thrown if an error occurs while sending the ACK / NAK
	 */
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
