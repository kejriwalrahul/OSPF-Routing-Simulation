/*
	Program by Rahul Kejriwal
	CS14B023
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

/*
	Contains information pertaining to a single link
*/
class NodeLink{
	int idx;
	int minC;
	int maxC;

	int curr_cost;

	NodeLink(int i, int m, int n){
		idx  = i;
		minC = m;
		maxC = n;

		curr_cost = Integer.MAX_VALUE;
	}

	public String toString(){
		return "Index: " + Integer.toString(idx) 
			+ ", minC: " + Integer.toString(minC)
			+ ", maxC: " + Integer.toString(maxC) 
			+ ", curr_cost: " + Integer.toString(curr_cost) 
			+ " ";
	}
}

/*
	Holds routing info for each destination node
*/
class RoutingPath{
	int dest;
	int parent;
	int path_cost;
	boolean done;

	RoutingPath(int d){
		dest = d;
		parent = -1;
		path_cost = Integer.MAX_VALUE;
		done = false;
	}

	public void reset(){
		parent = -1;
		path_cost = Integer.MAX_VALUE;
		done = false;
	}
}

/*
	Class to convert integer to bytes and store it in an existing byte array 'buf' from 'start' index
*/
class Bytizer{
	static byte[] convert(byte buf[], int start, int i){		
		buf[start]   = (byte) (i >> 24);
		buf[start+1] = (byte) (i >> 16);
		buf[start+2] = (byte) (i >> 8);
		buf[start+3] = (byte) (i);
		
		return buf; 
	} 

	static int invert(byte buf[], int start){
		int res;

		res  = (int)(buf[start])   << 24;
		res |= (int)(buf[start+1]) << 16;
		res |= (int)(buf[start+2]) <<  8;
		res |= (int)(buf[start+3]);

		return res;
	}
}

/*
	Thread for sending Hello Messages periodically
*/
class HelloSendThread extends Thread{
	// Not reqd to be shared
	int hello_interval;
	int my_id;
	InetAddress IPAddress;
	int hello_pkt_size = 5;

	long startTime;
	boolean debug;

	// Shared
	DatagramSocket socket;
	HashMap<Integer, NodeLink> link_info;

	// Constructing the thread
	HelloSendThread(int hi, DatagramSocket s, HashMap<Integer, NodeLink> l, int id, long sT, boolean d){
		// Acquire Params
		hello_interval = hi;
		my_id = id;
		startTime = sT;
		debug = d;

		socket = s;
		link_info = l;
		
		try{
			IPAddress = InetAddress.getByName("localhost");
		}
		catch(UnknownHostException e){
			System.out.println("Error in getting IP Address!");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void run(){
		byte[] buf = new byte[hello_pkt_size];
		
		// 0 - HELLO msg code
		buf[0] = 0;
		Bytizer.convert(buf, 1, 10000+my_id);

		// ISSUE: how many times?
		while(true){
			for(int i: link_info.keySet()){
				DatagramPacket hello_pkt = new DatagramPacket(buf, hello_pkt_size, IPAddress, 10000+i);
				
				if(debug)	
					System.out.println("Helloing " + Integer.toString(10000+i) + " at t=" + 
						Long.toString((System.nanoTime() - startTime)/1000_000_000));
				
				try{
					socket.send(hello_pkt);				
				}
				catch(IOException e){
					System.out.println("Unable to send HELLO pkt! Possibly Dead Peer Router!");
				}
			}

			try{
				Thread.sleep(hello_interval * 1000);		
			}
			catch(InterruptedException e){
				System.out.println("Unable to sleep!");
				System.exit(1);
			}
		}
	}
}

/*
	Thread to process received messages:
		Hello Msgs
		HelloReply Msgs
		LSA Msgs
*/
class RecvThread extends Thread{
	// Doesnt need to be shared 
	int nodeNum;
	int my_id;
	HashMap<Integer, Integer>  last_seq_seen = new HashMap<Integer, Integer>();

	InetAddress IPAddress;
	long startTime;
	boolean debug;

	// Probably shared
	DatagramSocket socket;
	HashMap<Integer, NodeLink> link_info;
	HashMap<Integer, HashMap<Integer, Integer>> adj_list;

	// Construct thread
	RecvThread(DatagramSocket s, int n, int id, HashMap<Integer, NodeLink> li, long sT, boolean	d, 
		HashMap<Integer, HashMap<Integer, Integer>> al){
		socket  = s;
		nodeNum = n;
		my_id 	= id;
		link_info = li;
		adj_list  = al;
		startTime = sT;
		debug = d;

		try{
			IPAddress = InetAddress.getByName("localhost");
		}
		catch(UnknownHostException e){
			System.out.println("Error in getting IP Address!");
			e.printStackTrace();
			System.exit(1);
		}

		for(int i=0; i<nodeNum; i++)
			last_seq_seen.put(i, -1);
	}

	public void run(){
		// Build recv packet
		int max_length = (nodeNum-1)*8 + 12 + 1;
 		byte buf[] = new byte[max_length];

		DatagramPacket recvd_pkt = new DatagramPacket(buf, max_length);

		while(true){
			if(debug)	System.out.println("Attempting read!");

			// Accept a packet
			try{
				socket.receive(recvd_pkt);
			}
			catch(IOException e){
				System.out.println("Unable to recv pkts!");
				e.printStackTrace();
				System.exit(1);
			}

			/*
				Dispatch Packets
			*/

			// Received Hello Packet
			if(buf[0] == 0){

				if(debug)
					System.out.println("Received HELLO pkt from " + Integer.toString(recvd_pkt.getPort()-10000));

				// Make buffer for reply
				int reply_size = 13;
				byte[] reply = new byte[reply_size];
				
				// 1 - HELLOREPLY msg
				reply[0] = 1;
				Bytizer.convert(reply, 1, my_id+10000);
				Bytizer.convert(reply, 5, recvd_pkt.getPort());

				NodeLink neighborLink = link_info.get(recvd_pkt.getPort()-10000);
				int new_cost = new Random(System.nanoTime()).nextInt(neighborLink.maxC - neighborLink.minC) +  neighborLink.minC; 
				Bytizer.convert(reply, 9, new_cost);

				// Update adj_list fr self node
				adj_list.get(my_id).put(recvd_pkt.getPort()-10000, new_cost);

				// Build and send hello reply pkt
				DatagramPacket reply_pkt = new DatagramPacket(reply, reply_size);
				try{
					reply_pkt.setAddress(recvd_pkt.getAddress());
					reply_pkt.setPort(recvd_pkt.getPort());
					socket.send(reply_pkt);				
				}
				catch(IOException e){
					System.out.println("Unable to send pkt!");
					System.exit(1);
				}
			}
			// Received Hello Reply Packet
			else if(buf[0] == 1){
				if(debug)
					System.out.println("Received HELLOREPLY pkt from " 
						+ Integer.toString(Bytizer.invert(buf, 1)-10000) );

				int curr_cost = Bytizer.invert(buf,9);
				link_info.get(Bytizer.invert(buf, 1)-10000).curr_cost = curr_cost;
				adj_list.get(my_id).put(recvd_pkt.getPort()-10000, curr_cost);
			}
			// Received LSA Packet
			else{
				int senderNode = Bytizer.invert(buf, 1); 
				int seq_no     = Bytizer.invert(buf, 5); 

				if(debug)
					System.out.println("Received LSA pkt from " 
						+ Integer.toString(senderNode) 
						+ " and seq_no: " 
						+ Integer.toString(seq_no));

				
				// If already seen
				if(seq_no <= last_seq_seen.get(senderNode-10000)){
					if(debug)	
						System.out.println("Received already seen LSA pkt from " + Integer.toString(senderNode) + 
							" with seq_no: " + Integer.toString(seq_no));
				}
				// If not yet seen
				else{
					// Update last seen
					last_seq_seen.put(senderNode-10000, seq_no);
				
					// Store info
					int no_of_entries = Bytizer.invert(buf,9);
					HashMap<Integer, Integer> sender_adj_list = adj_list.get(senderNode-10000);
					for(int i=0; i<no_of_entries; i++){
						int sender_neighbour = Bytizer.invert(buf, 13+8*i);
						int sender_neighbour_cost = Bytizer.invert(buf, 17+8*i);
						sender_adj_list.put(sender_neighbour, sender_neighbour_cost);	
					}

					// Broadcast info
					for(int i: link_info.keySet()){
						DatagramPacket lsa_forward = new DatagramPacket(buf, recvd_pkt.getLength(), IPAddress, i+10000);

						if(i+10000 == recvd_pkt.getPort())
							continue;

						if(debug)	
							System.out.println("Forwarding LSA from " + senderNode + " to " + Integer.toString(i+10000) + " at t=" + 
								Long.toString((System.nanoTime() - startTime)/1000_000_000));
						
						try{
							socket.send(lsa_forward);				
						}
						catch(IOException e){
							System.out.println("Unable to forward LSA pkt! Possibly Dead Peer Router!");
						}
	
					}

				}
			}	
		}

	}
}

/*
	Thread for sending out LSA packets
*/
class LSASendThread extends	Thread{
	// Doesnt need to be shared
	int lsai;
	int self_idx;

	long startTime;
	boolean debug;

	InetAddress IPAddress;
	int LSA_PKT_SIZE;
	int no_of_neighbours;
	int last_seq_sent = 0;

	// Shared
	DatagramSocket socket;
	HashMap<Integer, NodeLink> link_info;

	LSASendThread(DatagramSocket s, int l, long sT, HashMap<Integer, NodeLink> li, int idx, boolean d){
		lsai = l;
		self_idx = idx;
		startTime = sT;
		debug = d;

		socket = s;
		link_info = li;

		no_of_neighbours = link_info.keySet().size();
		LSA_PKT_SIZE = 13 + 8 * no_of_neighbours;

		try{
			IPAddress = InetAddress.getByName("localhost");
		}
		catch(UnknownHostException e){
			System.out.println("Error in getting IP Address!");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void run(){
		byte buf[] = new byte[LSA_PKT_SIZE];
		// 2 - LSA msg
		buf[0] = 2;
		Bytizer.convert(buf, 1, self_idx+10000);
		Bytizer.convert(buf, 9, no_of_neighbours);

		while(true){
			// Sleep for lsai
			try{
				Thread.sleep(lsai * 1000);		
			}
			catch(InterruptedException e){
				System.out.println("Unable to sleep!");
				System.exit(1);
			}

			// Build LSA pkt
			Bytizer.convert(buf, 5, last_seq_sent++);

			Object[] keys_arr = link_info.keySet().toArray();
			for(int i=0; i<no_of_neighbours; i++){
				Bytizer.convert(buf, 13+8*i, link_info.get(keys_arr[i]).idx + 10000);
				Bytizer.convert(buf, 17+8*i, link_info.get(keys_arr[i]).curr_cost);
			}

			// BroadCast LSA to neighbours
			for(Object iter: keys_arr){
				int i = (int) iter;

				DatagramPacket lsa_pkt = new DatagramPacket(buf, LSA_PKT_SIZE, IPAddress, i + 10000);
					
				if(debug)	
					System.out.println("LSA Advertised to " + Integer.toString(10000+i) + " at t=" + 
						Long.toString((System.nanoTime() - startTime)/1000_000_000));
				
				try{
					socket.send(lsa_pkt);				
				}
				catch(IOException e){
					System.out.println("Unable to send LSA pkt! Possibly Dead Peer Router!");
				}
			}
		}

	}
}

/*
	Thread to compute shortest path info
*/
class ShortestPathThread extends Thread{
	// Doesnt need to be shared
	String ofile;
	int spfi;
	int nodeNum;
	int self_idx;

	long startTime;
	boolean debug;

	// Possibly Shared
	HashMap<Integer, NodeLink> link_info;
	HashMap<Integer, HashMap<Integer, Integer>> adj_list;

	// Final routing info
	HashMap<Integer, RoutingPath> routes = new HashMap<Integer, RoutingPath>();

	// Construct thread
	ShortestPathThread(String of, int s, long sT, boolean d, HashMap<Integer, NodeLink> li
		, HashMap<Integer, HashMap<Integer, Integer>> al, int nN, int si){
		
		startTime = sT;
		link_info = li;
		adj_list  = al;
		nodeNum   = nN;
		self_idx  = si;
		ofile = of;
		spfi  = s;
		debug = d;

		for(int i=0; i<nodeNum; i++)
			routes.put(i, new RoutingPath(i));
	}

	String find_path(RoutingPath r, boolean last){
		String parent_path;

		if(r.parent == -1) 
			parent_path = "1-";
		else
			parent_path = find_path(routes.get(r.parent), true);
		
		parent_path += r.dest;
		if(!last)
			parent_path += "-";

		return parent_path; 
	}

	public void run(){
		while(true){
			/*
				Step 1: Sleep for spfi
			*/
			try{
				Thread.sleep(spfi * 1000);		
			}
			catch(InterruptedException e){
				System.out.println("Unable to sleep!");
				System.exit(1);
			}

			/*
				Step 2: Finding shortest paths
			*/

			// Clear earlier paths
			for(int i=0; i<nodeNum; i++)
				routes.get(i).reset();

			// Define comparator for Priority Queue
			Comparator<RoutingPath> RoutingPathComparator = new Comparator<RoutingPath>() {
			    @Override
			    public int compare(RoutingPath left, RoutingPath right) {
			        return left.path_cost - right.path_cost;
			    }
			};

			// Initialize Priority Queue
			PriorityQueue<RoutingPath> pQ = new PriorityQueue<RoutingPath>(nodeNum, RoutingPathComparator);
			// Add root node
			routes.get(self_idx).path_cost = 0;
			pQ.add(routes.get(self_idx));

			// Iterate over priority Queue
			while(pQ.size() != 0){

				// Get head of pQ
				RoutingPath curr_min = pQ.poll();
				// Mark it as final
				curr_min.done = true;
				if(debug)
					System.out.println("current shortest path for " + Integer.toString(curr_min.dest));

				// Iterate over neighbors
				for(int i: adj_list.get(curr_min.dest).keySet()){
					// Get neighbour info
					RoutingPath neighbour_route = routes.get(i);
					if(debug)
						System.out.println("neighbor " 
							+ Integer.toString(i));
					
					// If the path to this node is final, skip
					if(neighbour_route.done)
						continue;

					// If a shorter path can be achieved via current node, modify path to neighbor 
					// and update the pQ
					if(neighbour_route.path_cost > curr_min.path_cost + adj_list.get(curr_min.dest).get(i)){
						pQ.remove(neighbour_route);
						neighbour_route.parent = curr_min.dest;
						neighbour_route.path_cost = curr_min.path_cost + adj_list.get(curr_min.dest).get(i);
						pQ.add(neighbour_route);
					}
				}				
			} 
		
			/*
				Step 3: Write to file
			*/

			if(debug)
				System.out.println("Writing known routes to file");

			try{
				FileWriter opfile = new FileWriter(ofile);
				opfile.write("Routing Table for Node No. " + Integer.toString(self_idx) + " at Time " 
					+ (System.nanoTime() - startTime)/1000_000_000 + "\n");
				
				opfile.write(String.format("%-5s%-10s%-5s\n", "Dest", "Path", "Cost"));
				for(int i=0; i<nodeNum; i++){
					RoutingPath curr_route = routes.get(i);
					if(curr_route.path_cost == Integer.MAX_VALUE)
						continue;

					if(debug)
						System.out.println("writing shortest path for " + Integer.toString(curr_route.dest));
					
					opfile.write(String.format("%-5d%-10s%-5d\n", curr_route.dest, 
						find_path(curr_route, true), curr_route.path_cost));
				}

				opfile.close();
			}
			catch(IOException e){
				System.out.println("Error in writing routing tables!");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}


/*
	Class that simulates a single node
*/
class NodeSimulator{
	// Node Parameters
	String ifile, ofile;
	int idx;
	int hi, lsai, spfi;

	boolean debug;

	// Node Link Info
	int nodeNum;
	HashMap<Integer, NodeLink> link_info = new HashMap<Integer, NodeLink>();

	// Node Socket
	DatagramSocket socket;

	// Router start time
	long startTime;

	// Adjacency List of other nodes
	HashMap<Integer, HashMap<Integer, Integer>> adj_list = new HashMap<Integer, HashMap<Integer, Integer>>();

	/*
		Constructs the simulated Node
	*/
	NodeSimulator(int i, String in, String out, int h, int a, int s, boolean d){
		// Setup parameters
		ifile = in;
		ofile = out;
		idx = i;
		hi = h;
		lsai = a;
		spfi = s;

		debug = d;

		// Read Link Information
		try{
			read_file(ifile);
		}
		catch(Exception e){
			System.out.println("Unable to read file!");
			System.exit(1);
		}

		// Test read contents
		if(debug){
			System.out.print("link_info: ");
			System.out.println(link_info);
		}	

		// Setup sockets and packets
		try{
			socket = new DatagramSocket(10000 + idx);
		}
		catch(SocketException e){
			System.out.println("Unable to open node socket!");
			e.printStackTrace();
			System.exit(1);
		}

		// Build blank adjacency lists
		for(i=0; i<nodeNum; i++)
			adj_list.put(i, new HashMap<Integer, Integer>());

		for(int j:link_info.keySet())
			adj_list.get(idx).put(j, Integer.MAX_VALUE);

		// Initialize router start time
		startTime = System.nanoTime();

		// Start the node
		start_node();
	}

	/*
		Reads the Link Information file
	*/
	void read_file (String in) throws Exception{
		int m, k;

		BufferedReader f = new BufferedReader(new FileReader(in));
		
		String[] tokens = f.readLine().split(" ");
		nodeNum = Integer.parseInt(tokens[0]);
		m 		= Integer.parseInt(tokens[1]);

		int i, j, minC, maxC;
		for(k=0; k<m; k++){
			tokens = f.readLine().split(" ");
			i 	 = Integer.parseInt(tokens[0]);
			j 	 = Integer.parseInt(tokens[1]);
			minC = Integer.parseInt(tokens[2]);
			maxC = Integer.parseInt(tokens[3]);

			if(i == idx)
				link_info.put(j, new NodeLink(10000+j, minC, maxC));
			else if(j == idx)
				link_info.put(i, new NodeLink(10000+i, minC, maxC));
		}
	}

	void start_node(){

		// Construct Threads
		HelloSendThread helloThread = new HelloSendThread(hi, socket, link_info, idx, startTime, debug);
		RecvThread      recvrThread = new RecvThread(socket, nodeNum, idx, link_info, startTime, debug, adj_list);
		LSASendThread   lsaThread   = new LSASendThread(socket, lsai, startTime, link_info, idx, debug);
		ShortestPathThread spfThread= new ShortestPathThread(ofile, spfi, startTime, debug, link_info, adj_list, nodeNum, idx);

		// Fire up threads here
		helloThread.start();
		lsaThread.start();
		recvrThread.start();
		spfThread.start();

		// Join threads
		try{
			helloThread.join();
			lsaThread.join();
			recvrThread.join();
			spfThread.join();			
		}
		catch(InterruptedException e){
			System.out.println("Unable to join threads!");
			e.printStackTrace();
			System.exit(1);
		}

		// Close router socket on end
		socket.close();
	}
}

/*
	Main class for parsing cmd line args and starting node
*/
public class simulator{
	
	/*
		Prints err msg and exits
	*/
	public static void errorExit(String s){
		System.out.println("Error: " + s + "!!");
		System.out.println("Aborting...");
		System.exit(1);
	}

	/*
		Main: Collect args and create node
	*/
	public static void main(String[] args){
		String ifile="ipfile", ofile;
		int i=1;
		int h=1, a=5, s=20;

		boolean debug = false;

		// Process Command Line Args
		int next_arg = 0;
		for(String arg: args){
			if(next_arg == 0){
				if(arg.equals("-i"))
					next_arg = 1;
				else if(arg.equals("-f"))
					next_arg = 2;
				else if(arg.equals("-o"))
					next_arg = 3;
				else if(arg.equals("-h"))
					next_arg = 4;
				else if(arg.equals("-a"))
					next_arg = 5;
				else if(arg.equals("-s"))
					next_arg = 6;
				else if(arg.equals("-d"))
					debug = true;
				else
					errorExit("Incorrect Usage!");
			}
			else{
				switch(next_arg){			
					case 1: i = Integer.parseInt(arg);
							break;
					case 2: ifile = arg;
							break;
					case 3: ofile = arg;
							break;
					case 4: h = Integer.parseInt(arg);
							break;
					case 5: a = Integer.parseInt(arg);
							break;
					case 6: s = Integer.parseInt(arg);
							break;
					default: errorExit("Incorrect Usage!");
				}
				next_arg = 0;
			}
		}

		ofile = "outfile-" + Integer.toString(i) + ".txt";

		// Create object, call constructor
		NodeSimulator node = new NodeSimulator(i, ifile, ofile, h, a, s, debug);
	}
}