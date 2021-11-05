

import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.LinkedList;

public class WFQBlockchain {
	
	public static final int HIGH_PRECEDENCE = 5;
	public static final int MEDIUM_PRECEDENCE = 3;
	public static final int LOW_PRECEDENCE = 1;
    
	public static int previousSN = 0;
	public static int previousHash = 0;
	public static Queue<Packet> queuedPackets = new LinkedList<>();
	public static ArrayList<Integer> sequenceNumberList = new ArrayList<>();
	public static ArrayList<Packet> arrivingPackets = new ArrayList<>();
	public static ArrayList <Block> transactionChain = new ArrayList<>();
	
    /*
     * Weighted Fair Queuing
     * 
     * Implemented according to the heuristic given here 
     * http://myway2ccie.blogspot.com/2009/04/qos-weighted-fair-queuing-and-class.html
     * 
     * where Sequence Number:
     * SN = Previous SN + (weight * packet size)
     * 
     * where Weight:
     * weight =  32384/(packet precedence +1)
     * 
     * higher packet precedence = higher priority 
    */
    public static Queue<Packet> weightedFairQueuing (ArrayList<Packet> incomingPackets) {
    	Queue<Packet> queuedPacketList = new LinkedList<>();
    	ArrayList<Integer> snList = new ArrayList<>();
    	HashMap <Integer, Packet>  sequencePacketPair= new HashMap<>();
    	
    	int sn = 0;
    	System.out.println("===== Calculating Packet Sequence Number =====");
    	for (Packet t : incomingPackets) {
    		sn = calculateSequenceNumber(t);
    		displayStatus(sn, t);
    		snList.add(sn);
    		sequencePacketPair.put(sn, t);
    	}
    	System.out.println("==============================================\n");
    	
    	Collections.sort(snList);
    	
    	for (Integer i : snList) {
    		Packet nextTransaction = sequencePacketPair.get(i);
    		queuedPacketList.add(nextTransaction);
    	}
    	sequenceNumberList = snList;
    	
    	return queuedPacketList;
    }
    
    public static int calculateSequenceNumber (Packet p) {
    	// Assuming the data array's length as the packet size
    	int packetSize = p.getTransactions().length;
    	int weight = getWeight(p.getPrecedence());
    	
    	int newSN = previousSN + (weight * packetSize);

    	return newSN;
    }
    
    public static int getWeight (int precedence) {
    	return 32384 / (precedence + 1);
    }
    
    public static void hashNext (Packet p) {
    	System.out.println("<<<<< Hashing next packet on queue >>>>>");
    	System.out.println(" - Updates Previous Sequence Number");
    	previousSN = sequenceNumberList.get(0);
    	sequenceNumberList.remove(0);
    	System.out.println(" - Dequeue packet");
    	arrivingPackets.remove(p);
    	System.out.print(" - Create new block: ");
    	Block newBlock = new Block(previousHash, p);
    	System.out.println(newBlock.getBlockHash());
		previousHash = newBlock.getBlockHash();
		System.out.println(" - Link new block to chain");
		transactionChain.add(newBlock);
		System.out.println(" - New chain length: " + transactionChain.size());
		System.out.println("<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>\n");
    }
    
    public static void addNewPacket (Packet p) {
    	System.out.println("    ##### New packet arrived! #####\n");
    	arrivingPackets.add(p);
    	queuedPackets = weightedFairQueuing(arrivingPackets);
    }
    
    public static void displayStatus (int sn, Packet t) {
    	System.out.println("Sequence Number:" + sn);
		System.out.println("Precedence " + t.getPrecedence());
		System.out.println("Packet Size:" + t.getTransactions().length);
		System.out.println("Transactions:\n" + t.toString());
    }
    
    public static void main(String[] args) {
    	
    	String[] data1 = {"Leon sent 10 bitcoin to satoshi", "Leon sent 10 bitcoin to starbuck"};
    	Packet t1 = new Packet(data1, LOW_PRECEDENCE);
    	addNewPacket(t1);
    	
    	String[] data2 = {"ivan sent 10 bitcoin to satoshi", "satoshi sent 10 bitcoin to starbuck"};
    	Packet t2 = new Packet (data2, HIGH_PRECEDENCE);
    	addNewPacket(t2);
    	
    	String[] data3 = {"ivan sent 999 bitcoin to my mom"};
    	Packet t3 = new Packet (data3, MEDIUM_PRECEDENCE);
    	addNewPacket(t3);
    	
    	hashNext(queuedPackets.remove());
    	
    	String[] data4 = {"satoshi sent 3000 bitcoin to pizzahut"};
    	Packet t4 = new Packet (data4, LOW_PRECEDENCE);
    	addNewPacket(t4);
    	
    	hashNext(queuedPackets.remove());
    	
    	String[] data5 = {"ivan sent 30 bitcoin to pizzahut", "nakamoto sent 200 bitcoin to satoshi", "nakamoto sent 30 bitcoin to starbuck"};
    	Packet t5 = new Packet (data5, HIGH_PRECEDENCE);
    	addNewPacket(t5);
    	
    	System.out.println("~~~~~ No more packet arriving ~~~~~\n");
    	Iterator<Packet> queuedPacketsIterator = queuedPackets.iterator();	
    	while (queuedPacketsIterator.hasNext()) {
    		Packet p = queuedPacketsIterator.next();
    		hashNext(p);
    	}
    	
    	
    	System.out.println("@@@@@ Printing Full Transaction @@@@@");
    	for (Block b : transactionChain) {
    		System.out.print(b);
    	}
    }
}

class Block {

    private int previousHash;
    private Packet packet;

    private int blockHash;

    public Block(int previousHash, Packet p) {
        this.previousHash = previousHash;
        this.packet = p;

        Object[] contens = {Arrays.hashCode(packet.getTransactions()), previousHash};
        this.blockHash = Arrays.hashCode(contens);

    }

    public int getPreviousHash() {
        return previousHash;
    }

    public Packet getPacket() {
        return packet;
    }
    
    public String[] getTransaction() {
    	return packet.getTransactions();
    }

    public int getBlockHash() {
        return blockHash;
    }
    
    @Override
    public String toString() {
    	return ">"+ getBlockHash() + "\n" + getPacket().toString();
    }
}

class Packet {
	private String[] transactions;
	private int precedence;
	
	public Packet (String[] t, int p) {
		this.transactions = t;
		this.precedence = p;
	}
	
	public String[] getTransactions () {
		return transactions;
	}
	
	public int getPrecedence () {
		return precedence;
	}
	
	@Override
	public String toString() {
		String log = "";
		for (int i = 0; i < transactions.length; i++) {
			log += " - "+ transactions[i] +"\n";
		}
		return log;
	}
}