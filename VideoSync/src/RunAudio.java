import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.humble.video.Decoder;
import io.humble.video.MediaAudio;
import io.humble.video.MediaPacket;
import io.humble.video.javaxsound.AudioFrame;
import io.humble.video.javaxsound.MediaAudioConverter;
import io.humble.video.javaxsound.MediaAudioConverterFactory;

import net.tomp2p.dht.PeerDHT;
import net.tomp2p.peers.Number160;

public class RunAudio implements Runnable
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
	private boolean isMaster;
	
	private AtomicBoolean isMediaRead;
	
	private Object audioPacketLock = new Object();
	
	private Decoder audioDecoder;
	
	private PeerDHT mediaData;
	
	private Number160 mediaKey;
	
	private Queue<MediaPacket> audioPackets = new ArrayDeque<>();
	
	public RunAudio(Decoder audioDecoder, PeerDHT mediaData, Number160 mediaKey, AtomicBoolean isMediaRead)
	{
		this.audioDecoder = audioDecoder;
		this.mediaData = mediaData;
		this.mediaKey = mediaKey;
		this.isMediaRead = isMediaRead;
		isMaster = true;
	}
	
	public RunAudio(Decoder audioDecoder)
	{
		this.audioDecoder = audioDecoder;
		isMediaRead = new AtomicBoolean(false);
		isMaster = false;
	}
	
	public void run()
	{
		int offset;
		int bytesRead;
		
		ByteBuffer rawAudio = null;
		
		Queue<MediaPacket> secondPackets = new ArrayDeque<>();
		
		audioDecoder.open(null, null);
		
		MediaAudio samples = MediaAudio.make(
			audioDecoder.getFrameSize(),
			audioDecoder.getSampleRate(),
			audioDecoder.getChannels(),
			audioDecoder.getChannelLayout(),
			audioDecoder.getSampleFormat());
			
		MediaAudioConverter audioConverter =
			MediaAudioConverterFactory.createConverter(
				MediaAudioConverterFactory.DEFAULT_JAVA_AUDIO,
				samples);
			
		AudioFrame audioConnection = AudioFrame.make(audioConverter.getJavaFormat());
		
		while (!(isMediaRead.get() && isQueueEmpty() && secondPackets.isEmpty()))
		{	
			synchronized(audioPacketLock)
			{
				for(MediaPacket ap = audioPackets.poll(); ap != null; ap = audioPackets.poll())
				{
					secondPackets.add(ap);
				}
			}
			
			for (MediaPacket sp = secondPackets.poll(); sp != null; sp = secondPackets.poll())
			{
				offset = 0;
				bytesRead = 0;
				
				do
				{
					bytesRead += audioDecoder.decode(samples, sp, offset);
					if (samples.isComplete())
					{
						rawAudio = audioConverter.toJavaAudio(rawAudio, samples);
						audioConnection.play(rawAudio);
					}
					offset += bytesRead;
				}
				while (offset < sp.getSize());
				
				if (isMaster)
				{
					mediaData.send(mediaKey).object(new SerializedPacket(sp)).start();
				}
			}
		}
		
		audioConnection.dispose();
	}
	
	public void addPacket(MediaPacket packet)
	{
		synchronized(audioPacketLock)
		{
			audioPackets.add(MediaPacket.make(packet, true));
		}
	}
	
	private boolean isQueueEmpty()
	{
		synchronized(audioPacketLock)
		{
			return audioPackets.isEmpty();
		}
	}
}