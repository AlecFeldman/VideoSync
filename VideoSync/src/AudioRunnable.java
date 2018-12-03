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

public class AudioRunnable implements Runnable
{
	private int offset;
	private int bytesRead;
	
	private Object audioPacketLock = new Object();
	
	private AtomicBoolean runAudio = new AtomicBoolean(true);
	
	private Decoder audioDecoder;
	
	private MediaAudio samples;
	
	private MediaAudioConverter audioConverter;
	
	private AudioFrame audioConnection;
	
	private ByteBuffer rawAudio = null;
	
	private Queue<MediaPacket> audioPackets = new ArrayDeque<>();
	
	public AudioRunnable(Decoder audioDecoder)
	{
		this.audioDecoder = audioDecoder;
		this.audioDecoder.open(null, null);
		
		samples = MediaAudio.make(
			this.audioDecoder.getFrameSize(),
			this.audioDecoder.getSampleRate(),
			this.audioDecoder.getChannels(),
			this.audioDecoder.getChannelLayout(),
			this.audioDecoder.getSampleFormat());
		
		audioConverter =
			MediaAudioConverterFactory.createConverter(
				MediaAudioConverterFactory.DEFAULT_JAVA_AUDIO,
				samples);
		
		audioConnection = AudioFrame.make(audioConverter.getJavaFormat());
	}
	
	public void run()
	{
		while (runAudio.get())
		{
			synchronized(audioPacketLock)
			{
				for(MediaPacket ap = audioPackets.poll(); ap != null; ap = audioPackets.poll())
				{
					offset = 0;
					bytesRead = 0;
					
					do
					{
						bytesRead += audioDecoder.decode(samples, ap, offset);
						if (samples.isComplete())
						{
							rawAudio = audioConverter.toJavaAudio(rawAudio, samples);
							audioConnection.play(rawAudio);
						}
						offset += bytesRead;
					} while (offset < ap.getSize());
				}
			}
		}
		
		audioConnection.dispose();
	}
	
	public void addAudioPacket(MediaPacket packet)
	{
		synchronized(audioPacketLock)
		{
			audioPackets.add(MediaPacket.make(packet, true));
		}
	}
	
	public void stopAudio()
	{
		runAudio.set(false);
	}
}
