import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
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
		
		Scanner keyboard = new Scanner(System.in);
		
		ServerSocket randomSocket = new ServerSocket(0);
		
		randomSocket.close();
		
		ArrayList<PeerAddress> connectedClients = new ArrayList<>();
		
		Number160 theaterKey = new Number160();
		Number160 clientID = new Number160(new Random());
		
		Peer client = new PeerBuilder(clientID).ports(randomSocket.getLocalPort()).start();
		PeerDHT clientData = new PeerBuilderDHT(client).start();
		
		BootstrapBuilder masterBuilder = new BootstrapBuilder(client);
		FutureBootstrap master;
		FutureGet getData;
		FutureDirect sendData;
		
		System.out.print("1. Create theater\n2. Join theater\nEnter option: ");
		option = keyboard.nextInt();
		keyboard.nextLine();
		
		if (option == 1)
		{
			System.out.print("Create theater: ");
			theaterKey = Number160.createHash(keyboard.nextLine());
			
			master = client.bootstrap().peerAddress(client.peerAddress()).start();
			master.awaitUninterruptibly();
			
			createDataListener(client);
			connectedClients.add(client.peerAddress());
			clientData.put(theaterKey).data(new Data(connectedClients)).start().awaitUninterruptibly();
		}
		else if (option == 2)
		{
			System.out.print("Enter master address: ");
			masterBuilder.inetAddress(InetAddress.getByName(keyboard.nextLine()));
			
			System.out.print("Enter master port: ");
			masterBuilder.ports(keyboard.nextInt());
			keyboard.nextLine();
			
			System.out.print("Enter theater: ");
			theaterKey = Number160.createHash(keyboard.nextLine());
			
			master = masterBuilder.start();
			master.awaitUninterruptibly();
			
			createDataListener(client);
			getData = clientData.get(theaterKey).start();
			getData.awaitUninterruptibly();
			
			connectedClients = (ArrayList<PeerAddress>) getData.dataMap().values().iterator().next().object();
			connectedClients.add(client.peerAddress());
			clientData.put(theaterKey).data(new Data(connectedClients)).start().awaitUninterruptibly();
		}
		
		for(PeerAddress c : connectedClients)
		{
			sendData = client.sendDirect(c).object(message).start();
			sendData.awaitUninterruptibly();
		}
	}
	
	public static void createDataListener(Peer client)
	{
		client.objectDataReply(new ObjectDataReply()
		{
			@Override
			public Object reply(PeerAddress sender, Object request) throws Exception
			{
				System.out.println("Sender: " + sender + " Request: " + request);
				return "success";
			}
		});
	}
}