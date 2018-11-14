import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.p2p.builder.BootstrapBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class VideoSync
{
	public static void main(String[] args) throws IOException
	{
		int option;
		int clientPort = 4000;
		
		String theater;
		
		Scanner keys = new Scanner(System.in);
		
		Number160 clientID = new Number160(new Random());
		
		Peer client = new PeerBuilder(clientID).ports(clientPort).start();
		//PeerBuilderDHT clientAddresses = new PeerBuilderDHT(client);
		BootstrapBuilder masterBuilder = new BootstrapBuilder(client);
		FutureBootstrap master;
		
		System.out.print("1. Create server\n2. Join server\nEnter option: ");
		option = keys.nextInt();
		
		keys.nextLine();
		
		if (option == 1)
		{
			masterBuilder.peerAddress(client.peerAddress());
			
			//System.out.print("Create theater: ");
			//theater = keys.nextLine();
		}
		else if (option == 2)
		{
			System.out.print("Enter IP address: ");
			masterBuilder.inetAddress(InetAddress.getByName(keys.nextLine()));
			
			System.out.print("Enter port: ");
			masterBuilder.ports(keys.nextInt());
			
			//keys.nextLine();
			
			//System.out.print("Enter theater: ");
			//theater = keys.nextLine();
		}
		
		master = masterBuilder.start();
		
		System.out.println(master.bootstrapTo().iterator().next());
		
		//master.addListener(new BaseFutureAdapter<FutureBootstrap>()
		//{
		//	@Override
		//	public void operationComplete(FutureBootstrap master)
		//	{
		//		if(master.isSuccess())
		//		{
		//			System.out.println("Successfully connected to " + masterAddress + " on port " + masterPort + ".");
		//			
		//			
		//		}
		//		else
		//		{
		//			System.out.println("Failed to connect to " + masterAddress + " on port " + masterPort + ".");
		//		}
		//	}
		//});
	}
}