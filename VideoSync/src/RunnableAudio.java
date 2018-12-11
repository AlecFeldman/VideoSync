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

import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;

public class RunnableAudio implements Runnable
{	
	private AtomicBoolean isMasterFinished;
	
	private Object audioPacketLock = new Object();
	
	private Decoder audioDecoder;
	
	private PeerDHT audioData;
	
	private Number160 audioKey;
	
	private Queue<MediaPacket> audioPackets = new ArrayDeque<>();
	
	public RunnableAudio(Decoder audioDecoder, Number160 audioKey, Peer client, AtomicBoolean isMasterFinished)
	{
		audioData = new PeerBuilderDHT(client).start();
		
		this.audioDecoder = audioDecoder;
		this.audioKey = audioKey;
		this.isMasterFinished = isMasterFinished;
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
		
		while (!(isMasterFinished.get() && isQueueEmpty() && secondPackets.isEmpty()))
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
				
				audioData.send(audioKey).object(new MediaPacketSerialized(sp)).start();
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