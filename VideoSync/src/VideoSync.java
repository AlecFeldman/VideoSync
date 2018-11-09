import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;
import java.util.Scanner;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

public class VideoSync
{
	public static void main(String[] args) throws IOException
	{
		//int option;
		
		Random rnd = new Random();
		
		//Scanner keys = new Scanner(System.in);
		
		//System.out.print("1. Create server\n2. Join server\nEnter: ");
		//option = keys.nextInt();
		
		//if (option == 1)
		//{
		//	
		//}
		//else if (option == 2)
		//{
		//	
		//}
		
		Peer peer = new PeerBuilder(new Number160(rnd)).ports(4000).start();
		FutureBootstrap future = peer.bootstrap().peerAddress(peer.peerAddress()).start();
		future.awaitUninterruptibly();
		
		PeerDHT pdht = new PeerBuilderDHT(peer).start();
		Data data = new Data("Hello from desktop!");
		Number160 nr = new Number160(rnd);
		FuturePut futurePut = pdht.put(nr).data(data).start();
		futurePut.awaitUninterruptibly();
		
		FutureGet futureGet = pdht.get(nr).start();
		futureGet.data();
		
		futureGet.addListener(new BaseFutureAdapter<FutureGet>() {
		 @Override
		 public void operationComplete(FutureGet future) throws Exception {
		  if(future.isSuccess()) { // this flag indicates if the future was successful
		   System.out.println(futureGet.dataMap().values().iterator().next().object().toString());
		  } else {
		   System.out.println("failure");
		  }
		 }
		});
	}
}