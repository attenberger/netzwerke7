package edu.hm.cs.netzwerke1.aufgabe7.receiver;


public class Receiver {


	private State currentState;
	// 2D array defining all transitions that can occur
	private Transition[][] transition;
	
	
	public Receiver(){
		currentState = State.WAITNEXTFILE;
		// define all valid state transitions for our state machine
		// (undefined transitions will be ignored)
		transition = new Transition[State.values().length] [Msg.values().length];
		transition[State.WAITNEXTFILE.ordinal()] [Msg.START.ordinal()]; // ACK0 DO
		transition[State.WAITNEXTFILE.ordinal()] [Msg.OK0.ordinal()]; // NAK
		transition[State.WAITNEXTFILE.ordinal()] [Msg.OK1.ordinal()]; // NAK
		transition[State.WAITNEXTFILE.ordinal()] [Msg.REPEAT0.ordinal()]; // NAK
		transition[State.WAITNEXTFILE.ordinal()] [Msg.REPEAT1.ordinal()]; // NAK
		transition[State.WAITNEXTFILE.ordinal()] [Msg.CORRUPT.ordinal()]; // NAK
		
		transition[State.WAIT0.ordinal()] [Msg.START.ordinal()]; // NAK
		transition[State.WAIT0.ordinal()] [Msg.OK0.ordinal()]; // ACK DO
		transition[State.WAIT0.ordinal()] [Msg.OK1.ordinal()]; // NAK
		transition[State.WAIT0.ordinal()] [Msg.REPEAT1.ordinal()]; // ACK
		transition[State.WAIT0.ordinal()] [Msg.CORRUPT.ordinal()]; // NAK
		
		transition[State.WAIT1.ordinal()] [Msg.START.ordinal()]; // NAK
		transition[State.WAIT1.ordinal()] [Msg.OK0.ordinal()]; // NAK
		transition[State.WAIT1.ordinal()] [Msg.OK1.ordinal()]; // ACK DO
		transition[State.WAIT1.ordinal()] [Msg.REPEAT0.ordinal()]; // ACK
		transition[State.WAIT1.ordinal()] [Msg.CORRUPT.ordinal()]; // NAK
	}
	
	/**
	 * Process a message (a condition has occurred).
	 * @param input Message or condition that has occurred.
	 */
	public void processMsg(Msg input){
		System.out.println("INFO Received "+input+" in state "+currentState);
		Transition trans = transition[currentState.ordinal()][input.ordinal()];
		if(trans != null){
			currentState = trans.execute(input);
		}
		System.out.println("INFO State: "+currentState);
	}
	
	/**
	 * Abstract base class for all transitions.
	 * Derived classes need to override execute thereby defining the action
	 * to be performed whenever this transition occurs.
	 */
	abstract class Transition {
		abstract public State execute(Msg input);
	}
	
	class SayHi extends Transition {
		@Override
		public State execute(Msg input) {
			System.out.println("Hi!");
			return State.HI_WAIT;
		}
	}
	
	class AskForTime extends Transition {
		@Override
		public State execute(Msg input) {
			System.out.println("Time?");
			return State.TIME_WAIT;
		}
	}
	
	class Finish extends Transition {
		@Override
		public State execute(Msg input) {
			System.out.println("Thank you.");
			return State.IDLE;
		}
	}
}
