package edu.hm.cs.netzwerke1.aufgabe7.sender;

import edu.hm.cs.netzwerke1.aufgabe7.Package;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class FileSender {

	private File file;

	private FileInputStream fis;

	private DatagramSocket socket;

	private SenderState currentState = SenderState.START;

	private Package lastPackage;

	private Transition[][] transitions = new Transition[SenderState.values().length][SenderMessage.values().length];

	{
		transitions[SenderState.START.ordinal()][SenderMessage.SEND.ordinal()] = p -> {
			p = new Package();
			p.setFilename(file.getName());
			p.setSequencenumber(0);

			byte[] content = getNextPart(p.getRemainingSize());
			p.setLast(content.length == 0);
			p.setContent(content);

			sendNext(p);
			return SenderState.WAIT_FOR_ACK_0;
		};

		transitions[SenderState.WAIT_FOR_ACK_0.ordinal()][SenderMessage.RESEND.ordinal()] = p -> {
			sendNext(lastPackage);

			return SenderState.WAIT_FOR_ACK_0;
		};

		transitions[SenderState.WAIT_FOR_ACK_0.ordinal()][SenderMessage.OK.ordinal()] = p -> {
			p = new Package();
			p.setSequencenumber(1);

			byte[] content = getNextPart(p.getRemainingSize());
			p.setLast(content.length == 0);
			p.setContent(content);

			sendNext(p);
			return SenderState.WAIT_FOR_ACK_1;
		};

		transitions[SenderState.WAIT_FOR_ACK_1.ordinal()][SenderMessage.RESEND.ordinal()] = p -> {
			sendNext(lastPackage);

			return SenderState.WAIT_FOR_ACK_1;
		};

		transitions[SenderState.WAIT_FOR_ACK_1.ordinal()][SenderMessage.OK.ordinal()] = p -> {
			p = new Package();
			p.setSequencenumber(0);

			byte[] content = getNextPart(p.getRemainingSize());
			p.setLast(content.length == 0);
			p.setContent(content);

			sendNext(p);
			return SenderState.WAIT_FOR_ACK_0;
		};

	}

	public FileSender(File file, InetAddress address, int port) throws IOException {
		this.file = file;

		fis = new FileInputStream(file);
		socket = new DatagramSocket(port, address);

		socket.setSoTimeout(500);

		// Send first package.
		processMsg(SenderMessage.SEND);
	}

	private void sendNext(Package p) throws IOException {
		socket.send(p.toDatagramPacket());
		lastPackage = p;

		// Wait for answer or timeout.
		DatagramPacket packet = new DatagramPacket(new byte[Package.MAX_PACKAGE_SIZE], Package.MAX_PACKAGE_SIZE);
		try {
			socket.receive(packet);

			Package received = new Package(packet);
			processMsg(received);
		} catch (SocketTimeoutException e) {
			processMsg(SenderMessage.RESEND);
		}
	}

	private byte[] getNextPart(int size) throws IOException {
		byte[] buffer = new byte[size];

		fis.read(buffer);

		return buffer;
	}

	public void processMsg(Package p) throws IOException {
		Transition t = null;

		if (p == null) {
			t = transitions[currentState.ordinal()][SenderMessage.SEND.ordinal()];
		} else if (!p.isOk()) {
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

	public static void main(String... args) throws IOException {
		new FileSender(new File(args[0]), InetAddress.getLoopbackAddress(), 54321);
	}

}
