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

public class RunAudio implements Runnable
{
	private boolean isMaster;
	
	private AtomicBoolean isMediaRead;
	
	private Object audioPacketLock = new Object();
	
	private Decoder audioDecoder;
	
	private MediaDHT audioData;
	
	private Queue<MediaPacket> audioPackets = new ArrayDeque<>();
	
	public RunAudio(Decoder audioDecoder, MediaDHT mediaData, AtomicBoolean isMediaRead)
	{
		this.audioDecoder = audioDecoder;
		this.isMediaRead = isMediaRead;
		audioData = mediaData;
		isMaster = true;
	}
	
	public RunAudio(Decoder audioDecoder)
	{
		this.audioDecoder = audioDecoder;
		isMediaRead.set(true);
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
					audioData.sendPacket(audioData.getAudioKey(), sp);
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