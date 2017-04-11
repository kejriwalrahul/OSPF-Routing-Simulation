/*
	Program by Rahul Kejriwal
	CS14B023
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

class NodeLink{
	int idx;
	int minC;
	int maxC;

	NodeLink(int i, int m, int n){
		idx  = i;
		minC = m;
		maxC = n;
	}
}

class Bytizer{
	static byte[] convert(byte buf[], int start, int i){		
		buf[start]   = (byte) (i >> 24);
		buf[start+1] = (byte) (i >> 16);
		buf[start+2] = (byte) (i >> 8);
		buf[start+3] = (byte) (i);
		
		return buf; 
	} 
}

class HelloSendThread extends Thread{
	// Not reqd to be shared
	int hello_interval;
	int my_id;
	InetAddress IPAddress;
	int hello_pkt_size = 6;

	// Shared
	DatagramSocket socket;
	HashMap<Integer, NodeLink> link_info;


	HelloSendThread(int hi, DatagramSocket s, HashMap<Integer, NodeLink> l, int id){
		hello_interval = hi;
		socket = s;
		link_info = l;
		my_id = id;
		
		try{
			IPAddress = InetAddress.getByName("localhost");
		}
		catch(UnknownHostException e){
			System.out.println("Could not find IP!");
			System.exit(1);
		}
	}

	public void run(){
		byte[] buf = new byte[hello_pkt_size];
		
		// 00 - HELLO msg code
		buf[0] = 0; buf[1] = 0;
		Bytizer.convert(buf, 2, 10000+my_id);

		// ISSUE: how many times?
		while(true){
			for(int i: link_info.keySet()){
				DatagramPacket hello_pkt = new DatagramPacket(buf, hello_pkt_size, IPAddress, 10000+i);
				try{
					socket.send(hello_pkt);				
				}
				catch(IOException e){
					System.out.println("Unable to send pkt!");
					System.exit(1);
				}
			}

			try{
				Thread.sleep(hello_interval);		
			}
			catch(InterruptedException e){
				System.out.println("Unable to sleep!");
				System.exit(1);
			}
		}
	}
}

class RecvThread extends Thread{
	// Doesnt need to be shares 
	int nodeNum;
	int my_id;

	// Probably shares 
	DatagramSocket socket;
	HashMap<Integer, NodeLink> link_info;

	RecvThread(DatagramSocket s, int n, int id, HashMap<Integer, NodeLink> li){
		socket  = s;
		nodeNum = n;
		my_id 	= id;
		link_info = li;
	}

	public void run(){
		int max_length = (nodeNum-1)*8 + 12 + 2;
 		byte buf[] = new byte[max_length];

		DatagramPacket recvd_pkt = new DatagramPacket(buf, max_length);
		try{
			socket.receive(recvd_pkt);
		}
		catch(IOException e){
			System.out.println("Unable to recv pkts!");
			System.exit(1);
		}

		// Received Hello Packet
		if(buf[0] == 0 && buf[1] == 0){
			// Make buffer for reply
			int reply_size = 14;
			byte[] reply = new byte[reply_size];
			// 01 - HELLOREPLY msg
			reply[0] = 0; reply[1] = 1;
			Bytizer.convert(reply, 2, recvd_pkt.getPort());
			Bytizer.convert(reply, 6, my_id+10000);

			NodeLink neighborLink = link_info.get(recvd_pkt.getPort()-10000);
			Bytizer.convert(reply, 10, 
				new Random(System.nanoTime()).nextInt(neighborLink.maxC - neighborLink.minC) +  neighborLink.minC);

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
		else if(buf[0] == 0 && buf[1] == 0){

		}
		// Received LSA Packet
		else{

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

		// Setup sockets and packets
		try{
			socket = new DatagramSocket(10000 + idx);
		}
		catch(SocketException e){
			System.out.println("Unable to open node socket!");
			e.printStackTrace();
			System.exit(1);
		}

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