import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.humble.ferry.Buffer;
import io.humble.video.Codec;
import io.humble.video.Codec.ID;
import io.humble.video.Decoder;
import io.humble.video.Demuxer;
import io.humble.video.DemuxerStream;
import io.humble.video.MediaDescriptor;
import io.humble.video.MediaPacket;

import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class Media
{
	private String mediaFile;
	
	private Peer client;
	
	private Decoder videoDecoder = null;
	private Decoder audioDecoder = null;
	
	private RunVideo video;
	private RunAudio audio;
	
	private Thread videoThread;
	private Thread audioThread;
	
	private MediaDHT mediaData;
	
	public Media(String mediaFile, MediaDHT mediaData)
	{
		this.mediaFile = mediaFile;
		this.mediaData = mediaData;
	}
	
	public Media(Peer client, MediaDHT mediaData)
	{
		this.client = client;
		this.mediaData = mediaData;
	}
	
	public void playMedia() throws InterruptedException, IOException
	{
		int totalStreams;
		int videoIndex = -1;
		int audioIndex = -1;
		
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
					videoDecoder = mediaDecoder;
					videoIndex = i;
				}
				else if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_AUDIO)
				{
					audioDecoder = mediaDecoder;
					audioIndex = i;
				}
			}
	    }
		
		mediaData.putData(mediaData.getVideoKey(), mediaData.getIndexKey(), new Data(videoIndex));
		mediaData.putData(mediaData.getVideoKey(), mediaData.getCodecKey(), new Data(videoDecoder.getCodecID()));
		
		mediaData.putData(mediaData.getAudioKey(), mediaData.getIndexKey(), new Data(audioIndex));
		mediaData.putData(mediaData.getAudioKey(), mediaData.getCodecKey(), new Data(audioDecoder.getCodecID()));
		
		video = new RunVideo(videoDecoder, mediaData, isMediaRead);
		audio = new RunAudio(audioDecoder, mediaData, isMediaRead);
		
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
	
	public void waitForMedia() throws ClassNotFoundException, IOException
	{
		int videoIndex = (int) mediaData.getData(mediaData.getVideoKey(), mediaData.getIndexKey());
		int audioIndex = (int) mediaData.getData(mediaData.getAudioKey(), mediaData.getIndexKey());
		
		ID videoCodecID = (ID) mediaData.getData(mediaData.getVideoKey(), mediaData.getCodecKey());
		ID audioCodecID = (ID) mediaData.getData(mediaData.getAudioKey(), mediaData.getCodecKey());
		
		videoDecoder = Decoder.make(Codec.findDecodingCodec(videoCodecID));
		audioDecoder = Decoder.make(Codec.findDecodingCodec(audioCodecID));
		
		video = new RunVideo(videoDecoder);
		audio = new RunAudio(audioDecoder);
		
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
}
