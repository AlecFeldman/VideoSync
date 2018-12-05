import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
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

public class Theater
{
	public static void main(String[] args) throws IOException, ClassNotFoundException
	{
		int option;
		
		String message = "Hello there!";
		
		ArrayList<PeerAddress> connectedClients = new ArrayList<>();
		
		Number160 theaterKey = new Number160();
		Number160 clientID = new Number160(new Random());
		
		Peer client = new PeerBuilder(clientID).ports(4000).start();
		PeerDHT clientData = new PeerBuilderDHT(client).start();
		
		BootstrapBuilder masterBuilder = new BootstrapBuilder(client);
		FutureBootstrap master;
		
		Scanner keys = new Scanner(System.in);
		
		System.out.print("1. Create theater\n2. Join theater\nEnter option: ");
		option = keys.nextInt();
		keys.nextLine();
		
		if (option == 1)
		{
			System.out.print("Create theater: ");
			theaterKey = Number160.createHash(keys.nextLine());
			
			master = client.bootstrap().peerAddress(client.peerAddress()).start();
			master.awaitUninterruptibly();
			
			createDataListener(client);
			connectedClients.add(client.peerAddress());
			clientData.put(theaterKey).data(new Data(connectedClients)).start().awaitUninterruptibly();
		}
		else if (option == 2)
		{
			System.out.print("Enter master address: ");
			masterBuilder.inetAddress(InetAddress.getByName(keys.nextLine()));
			
			System.out.print("Enter master port: ");
			masterBuilder.ports(keys.nextInt());
			keys.nextLine();
			
			System.out.print("Enter theater: ");
			theaterKey = Number160.createHash(keys.nextLine());
			
			master = masterBuilder.start();
			master.awaitUninterruptibly();
			
			createDataListener(client);
			FutureGet getData = clientData.get(theaterKey).start();
			getData.awaitUninterruptibly();
			
			connectedClients = (ArrayList<PeerAddress>) getData.dataMap().values().iterator().next().object();
			connectedClients.add(client.peerAddress());
			clientData.put(theaterKey).data(new Data(connectedClients)).start().awaitUninterruptibly();
		}
		
		for(PeerAddress c : connectedClients)
		{
			FutureDirect futureDirect = client.sendDirect(c).object(message).start();
			futureDirect.awaitUninterruptibly();
		}
	}
	
	public static void createDataListener(Peer client)
	{
		client.objectDataReply(new ObjectDataReply()
		{
			public Object reply(PeerAddress sender, Object request) throws Exception
			{
				System.out.println("Sender: " + sender + " Request: " + request);
				return "success";
			}
		});
	}
}