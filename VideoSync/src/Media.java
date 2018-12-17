import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.humble.video.Decoder;
import io.humble.video.Demuxer;
import io.humble.video.DemuxerStream;
import io.humble.video.MediaDescriptor;
import io.humble.video.MediaPacket;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;

public class Media
{
	/**
	 * @author Alec Feldman
	 * @author Liam Gibbons	
	 * @author Rutvi Patel
	 * @author Nick Cheng
	 * @version 1.0.1
	 * @since 12/16/2018
	 *
	 */
	
	// Setting these to -1 until they're ready to be used.
	private int videoIndex = -1;
	private int audioIndex = -1;
	
	// Where the data's coming from.
	private String mediaFile;
	
	// Establishes a peer connection for clients to join
	private Peer client;
	
	// Establish the Data Hash Table for the player.
	private PeerDHT mediaData;
	
	// Sets up the numeric key data for the key value setup.
	private Number160 mediaKey;
	
	// Declares decoders to be set later down the line.
	private Decoder videoDecoder = null;
	private Decoder audioDecoder = null;
	
	// Sets up the audio and video streams.
	private RunVideo video;
	private RunAudio audio;
	
	// Sets up the audio and video threads
	private Thread videoThread;
	private Thread audioThread;
	
	// Sets up a serialized stream for the audio and video threads to run on.
	private SerializedStream streamData;
	
	// Creating the media data to be played.
	public Media(String mediaFile, Peer client, Number160 mediaKey)
	{
		this.mediaFile = mediaFile;
		this.client = client;
		this.mediaKey = mediaKey;
	}
	// Same as before, but allowing the connection to be made without a file being sent yet.
	public Media(Peer client, Number160 mediaKey)
	{
		this.client = client;
		this.mediaKey = mediaKey;
	}
	
	/**
	The bread and butter of the class. This method plays the media streams through assigning the decoders, and is used to
	differentiate between audio and video packets.
	*/
	public void playMedia() throws InterruptedException, IOException
	{
		// How many streams there are.
		int totalStreams;
		
		// Setting up an AtomicBoolean to ensure whether or not the media data has been read once recieved.
		AtomicBoolean isMediaRead = new AtomicBoolean(false);
		
		// Setting up the demuxer and the demuxer stream.
		// This basically decodes mp4 files into their audio/video components.
		Demuxer mediaContainer = Demuxer.make();
		DemuxerStream stream = null;
		
		// Declaring the media decoder, to be set further in.
		Decoder mediaDecoder = null;
		
		// Creating a data packet to be sent across the network.
		MediaPacket packet = MediaPacket.make();
		
		// Opens the media container, thus opening the stream.
		mediaContainer.open(mediaFile, null, false, true, null, null);
		totalStreams = mediaContainer.getNumStreams();

		// Sets the video and audio decoders.
		for (int i = 0; i < totalStreams; i++)
		{
			stream = mediaContainer.getStream(i);
			mediaDecoder = stream.getDecoder();
			// Setting the video decoder
			if (mediaDecoder != null)
			{
				if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_VIDEO)
				{
					videoIndex = i;
					videoDecoder = mediaDecoder;
				}
				// Then the audio
				else if (mediaDecoder.getCodecType() == MediaDescriptor.Type.MEDIA_AUDIO)
				{
					audioIndex = i;
					audioDecoder = mediaDecoder;
				}
			}
	    }
		
		// Sets the serialized stream, it's position in the video, and the decoders used.
		// Then the data hash table is opened.
		// And finally the stream begins to send packets.
		streamData = new SerializedStream(videoIndex, audioIndex, videoDecoder, audioDecoder);
		mediaData = new PeerBuilderDHT(client).start();
		mediaData.put(mediaKey).object(streamData).start();
		
		// Video thread opens, and starts to decode data being
		video = new RunVideo(videoDecoder, mediaData, mediaKey, isMediaRead);
		videoThread = new Thread(video);
		videoThread.start();
		
		// Then the audio
		audio = new RunAudio(audioDecoder, mediaData, mediaKey, isMediaRead);
		audioThread = new Thread(audio);
		audioThread.start();
		
		// Keeps the packets moving while the stream is open
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
		
		// Sets the status of the atomic boolean to true.
		isMediaRead.set(true);
		
		// Keeps the clients connected until the threads close.
		videoThread.join();
		audioThread.join();
		
		mediaContainer.close();
	}
	// 
	public void waitForMedia() throws ClassNotFoundException, IOException
	{
		FutureGet mediaGet;
		
		mediaData = new PeerBuilderDHT(client).start();
		mediaGet = mediaData.get(mediaKey).start();
		mediaGet.awaitUninterruptibly();
		streamData = (SerializedStream) mediaGet.data().object();
		
		videoIndex = streamData.getVideoIndex();
		videoDecoder = streamData.getVideoDecoder();
		video = new RunVideo(videoDecoder);
		videoThread = new Thread(video);
		videoThread.start();
		
		/* Commenting this out for now, something's broken
		audioIndex = streamData.getAudioIndex();
		audioDecoder = streamData.getAudioDecoder();
		audio = new RunAudio(audioDecoder);
		audioThread = new Thread(audio);
		audioThread.start();
		*/
		
		// Used to send Object Data over the network. 
		client.objectDataReply(new ObjectDataReply()
		{
			@Override
			public Object reply(PeerAddress sender, Object request)
			{
				SerializedPacket packetRequest = (SerializedPacket) request;
				
				MediaPacket packet = packetRequest.getPacket();
				
				if (packet.getStreamIndex() == videoIndex)
				{
					video.addPacket(packet);
				}
				//else if (packet.getStreamIndex() == audioIndex)
				//{
				//	audio.addPacket(packet);
				//}
				
				return "success";
			}
		});
	}
}
