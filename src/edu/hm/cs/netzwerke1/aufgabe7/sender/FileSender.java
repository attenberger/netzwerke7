package edu.hm.cs.netzwerke1.aufgabe7.sender;

import edu.hm.cs.netzwerke1.aufgabe7.Package;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileSender {

	private File file;

	private final InetAddress address;

	private final int port;

	private FileInputStream fis;

	private DatagramSocket socket;

	private volatile SenderState currentState = SenderState.START;

	private Package lastPackage;

	private boolean finished = false;

	private Transition[][] transitions = new Transition[SenderState.values().length][SenderMessage.values().length];

	private ExecutorService executorService = Executors.newSingleThreadExecutor();

	{
		transitions[SenderState.START.ordinal()][SenderMessage.SEND.ordinal()] = p -> {
			System.out.println("Sending initial package.");
			p = new Package();
			p.setFilename(file.getName());
			p.setSequencenumber(0);

			byte[] content = getNextPart(p.getRemainingSize());
			p.setContent(content);
			p.setLast(p.getRemainingSize() > 0);

			sendNext(p);
			return SenderState.WAIT_FOR_ACK_0;
		};

		transitions[SenderState.WAIT_FOR_ACK_0.ordinal()][SenderMessage.RESEND.ordinal()] = p -> {
			System.out.println("Sending package 0 again.");
			sendNext(lastPackage);

			return SenderState.WAIT_FOR_ACK_0;
		};

		transitions[SenderState.WAIT_FOR_ACK_0.ordinal()][SenderMessage.OK.ordinal()] = p -> {
			System.out.println("ACK 0 received. Sending next package.");
			p = new Package();
			p.setSequencenumber(1);

			byte[] content = getNextPart(p.getRemainingSize());
			p.setContent(content);
			p.setLast(p.getRemainingSize() > 0);

			sendNext(p);
			return SenderState.WAIT_FOR_ACK_1;
		};

		transitions[SenderState.WAIT_FOR_ACK_1.ordinal()][SenderMessage.RESEND.ordinal()] = p -> {
			System.out.println("Sending package 1 again.");
			sendNext(lastPackage);

			return SenderState.WAIT_FOR_ACK_1;
		};

		transitions[SenderState.WAIT_FOR_ACK_1.ordinal()][SenderMessage.OK.ordinal()] = p -> {
			System.out.println("ACK 1 received. Sending next package.");
			p = new Package();
			p.setSequencenumber(0);

			byte[] content = getNextPart(p.getRemainingSize());
			p.setContent(content);
			p.setLast(p.getRemainingSize() > 0);

			sendNext(p);
			return SenderState.WAIT_FOR_ACK_0;
		};

	}

	public FileSender(File file, InetAddress address, int port) throws IOException {
		this.file = file;
		this.address = address;
		this.port = port;

		fis = new FileInputStream(file);
		socket = new DatagramSocket();

		socket.setSoTimeout(500);

		// Send first package.
		processMsg(SenderMessage.SEND);

		while (!finished) {
			waitForAck();
		}
	}

	private void sendNext(Package p) throws IOException {
		DatagramPacket packet = p.toDatagramPacket();
		packet.setAddress(address);
		packet.setPort(port);

		socket.send(packet);

		lastPackage = p;
	}

	private void waitForAck() {
		// Wait for answer or timeout.
		DatagramPacket receivePacket = new DatagramPacket(new byte[Package.MAX_PACKAGE_SIZE], Package.MAX_PACKAGE_SIZE);
		try {
			socket.receive(receivePacket);

			Package received = new Package(receivePacket);
			processMsg(received);
		} catch (SocketTimeoutException e) {
			try {
				processMsg(SenderMessage.RESEND);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private byte[] getNextPart(int size) throws IOException {
		byte[] buffer = new byte[size];

		int length = fis.read(buffer);

		byte[] result = null;
		if (length == -1) {
			result = new byte[0];
			finished = true;
		} else {
			result = new byte[length];
			System.arraycopy(buffer, 0, result, 0, length);
		}

		return result;
	}

	public void processMsg(Package p) throws IOException {
		Transition t = null;

		if (p == null) {
			t = transitions[currentState.ordinal()][SenderMessage.SEND.ordinal()];
		} else if (!p.isOk() || (p.getSequencenumber() == lastPackage.getSequencenumber() && !p.isAck())) {
			t = transitions[currentState.ordinal()][SenderMessage.RESEND.ordinal()];
		} else if (p.getSequencenumber() == lastPackage.getSequencenumber() && p.isAck()) {
			t = transitions[currentState.ordinal()][SenderMessage.OK.ordinal()];
		}

		if (t != null) {
			currentState = t.execute(p);
		}
	}

	public void processMsg(SenderMessage message) throws IOException {
		Transition t = transitions[currentState.ordinal()][message.ordinal()];

		if (t != null) {
			currentState = t.execute(null);
		}
	}

	@FunctionalInterface
	private interface Transition {

		SenderState execute(Package p) throws IOException;

	}

	public static void main(String... args) {
		try {
			new FileSender(new File(args[0]), InetAddress.getByName(args[1]), 60000);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("As parameters for the program you have to give the name of the file to transmit and the IP or FQDN of the receiver!");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

}
