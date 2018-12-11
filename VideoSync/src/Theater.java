import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;
import java.util.Scanner;

import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.p2p.builder.BootstrapBuilder;
import net.tomp2p.peers.Number160;

public class Theater
{
	public static void main(String[] args) throws IOException, InterruptedException
	{
		int option;
		
		Scanner keyboard = new Scanner(System.in);
		
		ServerSocket randomSocket = new ServerSocket(0);
		
		randomSocket.close();
		
		Number160 clientID = new Number160(new Random());
		Number160 videoKey = Number160.createHash("video");
		Number160 audioKey = Number160.createHash("audio");
		Number160 indexKey = Number160.createHash("index");
		Number160 codecKey = Number160.createHash("codec");
		
		Peer client = new PeerBuilder(clientID).ports(randomSocket.getLocalPort()).start();
		
		MediaDHT mediaData = new MediaDHT(client, videoKey, audioKey, indexKey, codecKey);
		
		BootstrapBuilder masterBuilder = new BootstrapBuilder(client);
		FutureBootstrap master;
		
		System.out.print("1. Create theater\n2. Join theater\nEnter option: ");
		option = keyboard.nextInt();
		keyboard.nextLine();
		
		if (option == 1)
		{
			master = client.bootstrap().peerAddress(client.peerAddress()).start();
			master.awaitUninterruptibly();
			
			// Temporary
			System.out.println("IP: " + client.peerAddress().inetAddress().getHostAddress() +
							   "\nPort: " + client.peerAddress().tcpPort());
			keyboard.nextLine();
			// Temporary
			
			Media mediaMaster = new Media("./resources/test.mp4", mediaData);
			mediaMaster.playMedia();
		}
		else if (option == 2)
		{
			System.out.print("Enter master address: ");
			masterBuilder.inetAddress(InetAddress.getByName(keyboard.nextLine()));
			
			System.out.print("Enter master port: ");
			masterBuilder.ports(keyboard.nextInt());
			keyboard.nextLine();
			
			master = masterBuilder.start();
			master.awaitUninterruptibly();
			
			Media mediaClient = new Media(client, mediaData);
			mediaClient.waitForMedia();
		}
		
		keyboard.close();
	}
}