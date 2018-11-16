import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.DHTBuilder;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.p2p.builder.BootstrapBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class VideoSync
{
	public static void main(String[] args) throws IOException, ClassNotFoundException
	{
		int option;
		int clientPort = 0;
		int masterPort = 0;
		
		String clientAddress = new String();
		String masterAddress = new String();
		String theater = new String();
		
		Scanner keys = new Scanner(System.in);
		
		Number160 clientID = new Number160(new Random());
		Number160 theaterHash;
		
		Peer client = new PeerBuilder(clientID).ports(clientPort).start();
		
		FutureBootstrap master;
		
		System.out.print("1. Create server\n2. Join server\nEnter option: ");
		option = keys.nextInt();
		
		keys.nextLine();
		
		if (option == 1)
		{	
			System.out.print("Create theater: ");
			theater = keys.nextLine();
			
			masterAddress = clientAddress;
			masterPort = clientPort;
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
		
		
		
		//client.objectDataReply(new ObjectDataReply()
		//{
		//	@Override
		//	public Object reply(PeerAddress sender, Object request)
		//	{
		//		System.out.println("Sender: " + sender + " Request: " + request);
		//		return "success";
		//	}
		//});
		
		//clientAddresses.add(client.peerAddress());
		//clientData.put(theaterHash).data(new Data(clientAddresses)).start().awaitUninterruptibly();
		
		//for (PeerAddress address : clientAddresses)
		//{
		//	FutureDirect futureDirect = client.sendDirect(address).object("Hello from desktop!").start();
		//	futureDirect.awaitUninterruptibly();
		//}
	}
}