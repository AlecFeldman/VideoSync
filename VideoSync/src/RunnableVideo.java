import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.humble.video.Decoder;
import io.humble.video.MediaPacket;
import io.humble.video.MediaPicture;
import io.humble.video.awt.ImageFrame;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

public class RunnableVideo implements Runnable
{
	private int videoIndex;
	
	private AtomicBoolean isMasterFinished;
	
	private Object videoPacketLock = new Object();
	
	private Decoder videoDecoder;
	
	private PeerDHT videoData;
	
	private Number160 videoKey;
	private Number160 indexKey;
	private Number160 codecKey;
	
	private Queue<MediaPacket> videoPackets = new ArrayDeque<>();
	
	public RunnableVideo(Decoder videoDecoder, int videoIndex, Number160 videoKey, Number160 indexKey,
						 Number160 codecKey, Peer client, AtomicBoolean isMasterFinished)
	{
		this.videoDecoder = videoDecoder;
		this.videoIndex = videoIndex;
		this.videoKey = videoKey;
		this.indexKey = indexKey;
		this.codecKey = codecKey;
		this.videoData = new PeerBuilderDHT(client).start();
		this.isMasterFinished = isMasterFinished;
	}
	
	public void run()
	{
		int offset;
		int bytesRead;
		
		ImageFrame window = ImageFrame.make();
		
		BufferedImage image = null;
		
		Queue<MediaPacket> secondPackets = new ArrayDeque<>();
		
		// Don't need this on client end.
		setRouteData();
		
		videoDecoder.open(null, null);
		
		MediaPicture videoFrame = MediaPicture.make(
			videoDecoder.getWidth(),
			videoDecoder.getHeight(),
			videoDecoder.getPixelFormat());
		
		MediaPictureConverter videoConverter =
			MediaPictureConverterFactory.createConverter(
				MediaPictureConverterFactory.HUMBLE_BGR_24,
				videoFrame);
		
		while (!(isMasterFinished.get() && isQueueEmpty() && secondPackets.isEmpty()))
		{
			synchronized(videoPacketLock)
			{
				for(MediaPacket vp = videoPackets.poll(); vp != null; vp = videoPackets.poll())
				{
					secondPackets.add(vp);
				}
			}
			
			for(MediaPacket sp = secondPackets.poll(); sp != null; sp = secondPackets.poll())
			{
				offset = 0;
				bytesRead = 0;
				
				do
				{
					bytesRead += videoDecoder.decode(videoFrame, sp, offset);
					if (videoFrame.isComplete())
					{
						image = videoConverter.toImage(image, videoFrame);
						window.setImage(image);
					}
					offset += bytesRead;
				} while (offset < sp.getSize());
				
				// Don't need this on client end.
				videoData.send(videoKey).object(new MediaPacketSerialized(sp)).start();
				
				try
				{
					Thread.sleep(1000 / 30);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		window.dispose();
	}
	
	public void addPacket(MediaPacket packet)
	{
		synchronized(videoPacketLock)
		{
			videoPackets.add(MediaPacket.make(packet, true));
		}
	}
	
	private void setRouteData()
	{
		try
		{
			videoData.put(videoKey).data(new Data(videoIndex)).domainKey(indexKey).start();
			videoData.put(videoKey).data(new Data(videoDecoder.getCodecID())).domainKey(codecKey).start();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private boolean isQueueEmpty()
	{
		synchronized(videoPacketLock)
		{
			return videoPackets.isEmpty();
		}
	}
}