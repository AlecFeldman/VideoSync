import java.io.IOException;
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
import net.tomp2p.storage.Data;

public class RunnableAudio implements Runnable
{
	int audioIndex;
	
	private AtomicBoolean isMasterFinished;
	
	private Object audioPacketLock = new Object();
	
	private Decoder audioDecoder;
	
	private PeerDHT audioData;
	
	private Number160 audioKey;
	private Number160 indexKey;
	private Number160 codecKey;
	
	private Queue<MediaPacket> audioPackets = new ArrayDeque<>();
	
	public RunnableAudio(Peer client, Decoder audioDecoder, int audioIndex, Number160 audioKey,
						 Number160 indexKey, Number160 codecKey, AtomicBoolean isMasterFinished)
	{
		this.audioData = new PeerBuilderDHT(client).start();
		this.audioDecoder = audioDecoder;
		this.audioIndex = audioIndex;
		this.audioKey = audioKey;
		this.indexKey = indexKey;
		this.codecKey = codecKey;
		this.isMasterFinished = isMasterFinished;
	}
	
	public void run()
	{
		int offset;
		int bytesRead;
		
		ByteBuffer rawAudio = null;
		
		Queue<MediaPacket> secondPackets = new ArrayDeque<>();
		
		// Don't need this on client end.
		setRouteData();
		
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
				} while (offset < sp.getSize());
				
				// Don't need this on client end.
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
	
	private void setRouteData()
	{
		try
		{
			audioData.put(audioKey).data(new Data(audioIndex)).domainKey(indexKey).start();
			audioData.put(audioKey).data(new Data(audioDecoder.getCodecID())).domainKey(codecKey).start();
		}
		catch (IOException e)
		{
			e.printStackTrace();
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