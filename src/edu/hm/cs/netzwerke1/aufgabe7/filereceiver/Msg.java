package edu.hm.cs.netzwerke1.aufgabe7.filereceiver;

/**
 * Sorts of messages that can occur when receiving a package.
 * @author Attenberger
 */
public enum Msg {
  START, STARTLAST, OK0, OK1, OK0LAST, OK1LAST, CORRUPT, DIFFERENTSENDER 
}
