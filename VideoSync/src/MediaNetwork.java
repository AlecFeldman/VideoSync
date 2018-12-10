import java.io.IOException;

import io.humble.ferry.Buffer;
import io.humble.video.MediaPacket;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;

public class MediaNetwork
{	
	private RunnableVideo video;
	
	public MediaNetwork(Peer client, Number160 videoKey, Number160 audioKey, Number160 indexKey, Number160 codecKey) throws ClassNotFoundException, IOException
	{
		PeerDHT mediaData = new PeerBuilderDHT(client).start();
		
		System.out.println(mediaData.get(videoKey).all().domainKey(indexKey).start().data().object());
		mediaData.get(videoKey).all().domainKey(codecKey).start();
		
		mediaData.get(audioKey).all().domainKey(indexKey).start();
		mediaData.get(audioKey).all().domainKey(codecKey).start();
		
		client.objectDataReply(new ObjectDataReply()
		{
			@Override
			public Object reply(PeerAddress sender, Object request)
			{
				MediaPacketSerialized packetSerialized = (MediaPacketSerialized) request;
				
				byte[] rawData = packetSerialized.getRawData();
				
				MediaPacket packet = MediaPacket.make(Buffer.make(null, rawData, 0, rawData.length));
				
				packet.setPts(packetSerialized.getPresentationTime());
				packet.setDts(packetSerialized.getDecompressionTime());
				packet.setStreamIndex(packetSerialized.getStreamIndex());
				packet.setFlags(packetSerialized.getFlags());
				packet.setDuration(packetSerialized.getDuration());
				packet.setPosition(packetSerialized.getPosition());
				packet.setConvergenceDuration(packetSerialized.getConvergenceDuration());
				
				//System.out.println(packet);
				
				return "success";
			}
		});
	}
}
