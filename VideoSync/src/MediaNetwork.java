import java.io.IOException;

import io.humble.ferry.Buffer;
import io.humble.video.Codec;
import io.humble.video.Codec.ID;
import io.humble.video.Decoder;
import io.humble.video.MediaPacket;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;

public class MediaNetwork
{
	private int videoIndex;
	private int audioIndex;
	
	private PeerDHT mediaData;
	
	private RunnableVideo video;
	private RunnableAudio audio;
	
	public MediaNetwork(Peer client, Number160 videoKey, Number160 audioKey,
						Number160 indexKey, Number160 codecKey) throws ClassNotFoundException, IOException
	{
		ID videoCodecID;
		ID audioCodecID;
		
		Decoder videoDecoder;
		Decoder audioDecoder;
		
		Thread videoThread;
		Thread audioThread;
		
		mediaData = new PeerBuilderDHT(client).start();
		
		videoIndex = (int) getMediaData(videoKey, indexKey);
		audioIndex = (int) getMediaData(audioKey, indexKey);
		
		videoCodecID = (ID) getMediaData(videoKey, codecKey);
		videoDecoder = Decoder.make(Codec.findDecodingCodec(videoCodecID));
		
		audioCodecID = (ID) getMediaData(audioKey, codecKey);
		audioDecoder = Decoder.make(Codec.findDecodingCodec(audioCodecID));
		
		video = new RunnableVideo(videoDecoder);
		audio = new RunnableAudio(audioDecoder);
		
		videoThread = new Thread(video);
		audioThread = new Thread(audio);
		
		videoThread.start();
		audioThread.start();
		
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
				
				if (packet.getStreamIndex() == videoIndex)
				{
					video.addPacket(packet);
				}
				else if (packet.getStreamIndex() == audioIndex)
				{
					audio.addPacket(packet);
				}
				
				return "success";
			}
		});
	}
	
	private Object getMediaData(Number160 mediaKey, Number160 domainKey) throws ClassNotFoundException, IOException
	{
		FutureGet getMedia = mediaData.get(mediaKey).all().domainKey(domainKey).start();
		
		getMedia.awaitUninterruptibly();
		
		return getMedia.data().object();
	}
}
