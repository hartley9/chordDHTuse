import java.util.Vector;
import java.util.Random;
import java.lang.Math;

class Finger{
	public int key;
	public ChordNode node;

}

class Store{
	String key;
	byte[] value;
}

public class ChordNode implements Runnable{

	static final int KEY_BITS = 8;

	//for each peer link that we have, we store a reference to the peer node plus a "cached" copy of that node's key; this means that whenever we change e.g. our successor reference we also set successorKey by doing successorKey = successor.getKey()
	ChordNode successor;
	int successorKey;
	ChordNode n0;


	ChordNode predecessor;
	int predecessorKey;

	//my finger table; note that all "node" entries will initially be "null"; your code should handle this
	int fingerTableLength;
	Finger finger[];
	int nextFingerFix;

	Vector<Store> dataStore = new Vector<Store>();

	//note: you should always use getKey() to get a node's key; this will make the transition to RMI easier
	private int myKey;

	ChordNode(String myKeyString) throws NullPointerException
	{
		myKey = hash(myKeyString);

		successor = this;
		successorKey = myKey;

		//initialise finger table (note all "node" links will be null!)
		finger = new Finger[KEY_BITS];
		for (int i = 0; i < KEY_BITS; i++)
			finger[i] = new Finger();
		fingerTableLength = KEY_BITS;

		System.out.print("Finger table info of <" + this.getKey() + ">" +
				"\n Length -> " + fingerTableLength
		+ "\n");


		//Fill the keys according to this.key + 2^^i-1 % 2^m
		for (int i=1; i<fingerTableLength+1; i++)
		{
			int z = (this.getKey() + (int) Math.pow(2, (i-1))) % (int) Math.pow(2, KEY_BITS);
			finger[i-1].key = z;
			System.out.println(finger[i-1].key);
		}


		//start up the periodic maintenance thread
		new Thread(this).start();
	}

	// -- API functions --

	void put(String key, byte[] value)
	{
		//find the node that should hold this key and add the key and value to that node's local store
	}

	byte[] get(String key)
	{
		//find the node that should hold this key, request the corresponding value from that node's local store, and return it

		return null;
	}

	// -- state utilities --

	int getKey()
	{
		return myKey;
	}

	ChordNode getPredecessor()
	{
		return predecessor;
	}

	// -- topology management functions --
	void join(ChordNode atNode) throws NullPointerException
	{
		predecessor = null;
		predecessorKey = 0;
		successor = atNode.findSuccessor(this.getKey());

		//TODO: fill in the remaining details of this function

	}

	// -- utility functions --
	ChordNode findSuccessor(int key) throws NullPointerException
	{


		//ChordNode n = this;
		if (key > this.getKey() && key <= this.successorKey)
		{
			System.out.println("Node <" + key + ">'s Successor -> " + this.successorKey);
			return this.successor;
		}else
			{
				System.out.println("Visiting Node < " + this.getKey() + " > to find successor of key (" + key +")");
				n0 = closestPrecedingNode(key);
				return n0.findSuccessor(key);
			}

	}


	ChordNode closestPrecedingNode(int key) throws NullPointerException {
			ChordNode n = this;
			for (int i = fingerTableLength - 1; i > 0; i--){


				if (finger[i].key > n.getKey() && finger[i].key < key) {
					return finger[i].node;
				}
			}

			return n;
	}



	// -- range check functions; they deal with the added complexity of range wraps --
	// x is in [a,b] ?
	boolean isInOpenRange(int key, int a, int b) throws NullPointerException
	{
		if (b > a) return key >= a && key <= b;
		else return key >= a || key <= b;
	}

	// x is in (a,b) ?
	boolean isInClosedRange(int key, int a, int b) throws NullPointerException
	{
		if (b > a) return key > a && key < b;
		else return key > a || key < b;
	}

	// x is in [a,b) ?
	boolean isInHalfOpenRangeL(int key, int a, int b) throws NullPointerException
	{
		if (b > a) return key >= a && key < b;
		else return key >= a || key < b;
	}

	// x is in (a,b] ?
	boolean isInHalfOpenRangeR(int key, int a, int b) throws NullPointerException
	{
		if (b > a) return key > a && key <= b;
		else return key > a || key <= b;
	}

	// -- hash functions --
	//this function converts a string "s" to a key that can be used with the DHT's API functions
	int hash(String s)
	{
		int hash = 0;

		for (int i = 0; i < s.length(); i++)
			hash = hash * 31 + (int) s.charAt(i);

		if (hash < 0) hash = hash * -1;

		return hash % ((int) Math.pow(2, KEY_BITS));
	}

	// -- maintenance --
	void notifyNode(ChordNode potentialPredecessor) throws NullPointerException
	{
		if(this.predecessor == null || isInClosedRange(potentialPredecessor.getKey(), this.predecessorKey, this.getKey()))
		{
			this.predecessor = potentialPredecessor;
			this.predecessorKey = potentialPredecessor.getKey();
		}
	}

	void stabilise() throws NullPointerException
	{

		ChordNode x = this.successor.predecessor;

		try {
			if (isInClosedRange(x.getKey(), this.getKey(), this.successorKey)) {
				this.successor = x;
			}
		} catch(NullPointerException e)
		{
			this.successor.notifyNode(this);
			System.out.println("");
		}

	}


	void fixFingers() throws NullPointerException, InterruptedException {
		System.out.println("Fixing fingers...");

		nextFingerFix = nextFingerFix + 1;

		if (nextFingerFix > KEY_BITS)
		{
			nextFingerFix = 1;
		}
		// finger[nextFingerFix].node = findSuccessor(n+2^^nextFingerFix-1)
		int p =  (int) Math.pow(2, nextFingerFix-1);
		finger[nextFingerFix].node = findSuccessor(finger[nextFingerFix].key);
		//finger[nextFingerFix].key = finger[nextFingerFix].node.getKey();


	}

	void checkPredecessor()
	{

		if (this.predecessor == null){

		}


	}

	void checkDataMoveDown()
	{
		//if I'm storing data that my current predecessor should be holding, move it
	}

	public void run() throws NullPointerException
	{
		while (true)
		{
			try{
				Thread.sleep(1000);
			}
			catch (InterruptedException e){
				System.out.println("Interrupted");
			}

			try{
				stabilise();
			}
			catch (Exception e){e.printStackTrace();}

			try{
				fixFingers();
			}
			catch (Exception e){e.printStackTrace();}

			try{
				checkPredecessor();
			}
			catch (Exception e){e.printStackTrace();}

			try{
				checkDataMoveDown();
			}
			catch (Exception e){e.printStackTrace();}
		}
	}

	public static void main(String args[]) throws NullPointerException, InterruptedException {



		// --- local test ---
		ChordNode n1 = new ChordNode("yorkshire");
		ChordNode n2 = new ChordNode("lancashire");
		ChordNode n3 = new ChordNode("cheshire");
		System.out.println("n1 -> " + n1.getKey());
		System.out.println("n2 -> " + n2.getKey());

		System.out.println("Joining nodes to network...");


		n1.join(n2);
		//n3.join(n1);

		// -- wait a bit for stabilisation --

		System.out.println("Waiting for topology to stabilise...");

		try{
			Thread.sleep(7000);
		}
		catch (InterruptedException e){
			System.out.println("Interrupted");
		}

		System.out.println("Inserting keys...");

		String key1 = "alex";
		byte[] data1 = new byte[128];

		String key2 = "sam";
		byte[] data2 = new byte[64];

		String key3 = "jamie";
		byte[] data3 = new byte[256];

		n1.put(key1, data1);
		n1.put(key2, data2);
		n3.put(key3, data3);

		System.out.println("All done (press ctrl-c to quit)");
	}
}