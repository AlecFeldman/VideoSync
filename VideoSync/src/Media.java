import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.humble.ferry.Buffer;
import io.humble.video.Decoder;
import io.humble.video.Demuxer;
import io.humble.video.DemuxerStream;
import io.humble.video.MediaDescriptor;
import io.humble.video.MediaPacket;

import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;

public class Media
{
	private int videoIndex = -1;
	private int audioIndex = -1;
	
	private String mediaFile;
	
	private Peer client;
	
	private PeerDHT mediaData;
	
	private Number160 mediaKey;
	
	private Decoder videoDecoder = null;
	private Decoder audioDecoder = null;
	
	private RunVideo video;
	private RunAudio audio;
	
	private Thread videoThread;
	private Thread audioThread;
	
	private SerializedStream streamData;
	
	public Media(String mediaFile, Peer client, Number160 mediaKey)
	{
		this.mediaFile = mediaFile;
		this.client = client;
		this.mediaKey = mediaKey;
	}
	
	public Media(Peer client, Number160 mediaKey)
	{
		this.client = client;
		this.mediaKey = mediaKey;
	}
	
	public void playMedia() throws InterruptedException, IOException
	{
		int totalStreams;
		
		AtomicBoolean isMediaRead = new AtomicBoolean(false);
		
		Demuxer mediaContainer = Demuxer.make();
		DemuxerStream stream = null;
		
		Decoder mediaDecoder = null;
		
		MediaPacket packet = MediaPacket.make();
		
		mediaContainer.open(mediaFile, null, false, true, null, null);
		totalStreams = mediaContainer.getNumStreams();
		
		for (int i = 0; i < totalStreams; i++)
		{
			stream = mediaContainer.getStream(i);
			mediaDecoder = stream.getDecoder();
			
			if (mediaDecoder != null)
			{
				if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_VIDEO)
				{
					videoIndex = i;
					videoDecoder = mediaDecoder;
				}
				else if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_AUDIO)
				{
					audioIndex = i;
					audioDecoder = mediaDecoder;
				}
			}
	    }
		
		streamData = new SerializedStream(videoIndex, audioIndex, videoDecoder, audioDecoder);
		mediaData = new PeerBuilderDHT(client).start();
		mediaData.put(mediaKey).object(streamData).start();
		
		video = new RunVideo(videoDecoder, mediaData, mediaKey, isMediaRead);
		audio = new RunAudio(audioDecoder, mediaData, mediaKey, isMediaRead);
		
		videoThread = new Thread(video);
		audioThread = new Thread(audio);
		
		videoThread.start();
		audioThread.start();
		
		while (mediaContainer.read(packet) >= 0)
		{
			if (packet.getStreamIndex() == videoIndex)
			{
				video.addPacket(packet);
			}
			else if (packet.getStreamIndex() == audioIndex)
			{
				audio.addPacket(packet);
			}
		}
		
		isMediaRead.set(true);
		
		videoThread.join();
		audioThread.join();
		
		mediaContainer.close();
	}
	
	public void waitForMedia()
	{
		client.objectDataReply(new ObjectDataReply()
		{
			@Override
			public Object reply(PeerAddress sender, Object request)
			{
				SerializedPacket packetSerialized = (SerializedPacket) request;
				
				byte[] rawData = packetSerialized.getRawData();
				
				MediaPacket packet = MediaPacket.make(Buffer.make(null, rawData, 0, rawData.length));
				
				packet.setPts(packetSerialized.getPresentationTime());
				packet.setDts(packetSerialized.getDecompressionTime());
				packet.setStreamIndex(packetSerialized.getStreamIndex());
				packet.setFlags(packetSerialized.getFlags());
				packet.setDuration(packetSerialized.getDuration());
				packet.setPosition(packetSerialized.getPosition());
				packet.setConvergenceDuration(packetSerialized.getConvergenceDuration());
				
				System.out.println(packet);
				
				return "success";
			}
		});
	}
}
