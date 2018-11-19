import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

public class VideoSync
{
	public static void main(String[] args) throws IOException
	{
		int option;
		int clientPort;
		int masterPort = 0;
		
		String clientAddress;
		String masterAddress = new String();
		String theater = new String();
		
		Scanner keys = new Scanner(System.in);
		
		ArrayList<PeerAddress> connectedClients = new ArrayList<>();
		
		ServerSocket randomSocket;
		
		Number160 clientID;
		Number160 theaterKey;
		
		Peer client;
		PeerDHT clientData;
		
		FutureBootstrap master;
		FuturePut futureData;
		
		System.out.print("1. Create theater\n2. Join theater\nEnter option: ");
		option = keys.nextInt();
		keys.nextLine();
		
		clientID = new Number160(new Random());
		randomSocket = new ServerSocket(0);
		randomSocket.close();
		clientPort = randomSocket.getLocalPort();
		client = new PeerBuilder(clientID).ports(clientPort).start();
		clientAddress = client.peerAddress().inetAddress().getHostAddress();
		
		clientData = new PeerBuilderDHT(client).start();
		
		if (option == 1)
		{
			masterAddress = clientAddress;
			masterPort = clientPort;
			
			System.out.print("Create theater: ");
			theater = keys.nextLine();
		}
		else if (option == 2)
		{
			System.out.print("Enter master address: ");
			masterAddress = keys.nextLine();
			
			System.out.print("Enter master port: ");
			masterPort = keys.nextInt();
			keys.nextLine();
			
			System.out.print("Enter theater: ");
			theater = keys.nextLine();
		}
		
		master = client.bootstrap().inetAddress(InetAddress.getByName(masterAddress)).ports(masterPort).start();
		
		master.addListener(new BaseFutureAdapter<FutureBootstrap>()
		{
			@Override
			public void operationComplete(FutureBootstrap master)
			{
				if(master.isSuccess())
				{
					System.out.println("Successfully connected to " +
									   master.bootstrapTo().iterator().next().inetAddress().getHostAddress() +
									   " on port " +
									   master.bootstrapTo().iterator().next().tcpPort() +
									   ".");
				}
				else
				{
					System.out.println("Failed to connect to " +
									   master.bootstrapTo().iterator().next().inetAddress().getHostAddress() +
							   		   " on port " +
							   		   master.bootstrapTo().iterator().next().tcpPort() +
							   		   ".");
				}
			}
		});
		
		theaterKey = Number160.createHash(theater);
		futureData = clientData.put(theaterKey).data(new Data("test")).start();
		futureData.awaitUninterruptibly();
	}
}