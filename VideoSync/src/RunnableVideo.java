import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.humble.video.Decoder;
import io.humble.video.MediaPacket;
import io.humble.video.MediaPicture;
import io.humble.video.awt.ImageFrame;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

public class RunnableVideo implements Runnable
{
	private AtomicBoolean isMediaRead;
	
	private Object videoPacketLock = new Object();
	
	private Decoder videoDecoder;
	
	private MediaDHT videoData;
	
	private Queue<MediaPacket> videoPackets = new ArrayDeque<>();
	
	public RunnableVideo(MediaDHT mediaData, Decoder videoDecoder, AtomicBoolean isMediaRead)
	{
		videoData = mediaData;
		this.videoDecoder = videoDecoder;
		this.isMediaRead = isMediaRead;
	}
	
	public void run()
	{
		int offset;
		int bytesRead;
		
		ImageFrame window = ImageFrame.make();
		
		BufferedImage image = null;
		
		Queue<MediaPacket> secondPackets = new ArrayDeque<>();
		
		videoDecoder.open(null, null);
		
		MediaPicture videoFrame = MediaPicture.make(
			videoDecoder.getWidth(),
			videoDecoder.getHeight(),
			videoDecoder.getPixelFormat());
		
		MediaPictureConverter videoConverter =
			MediaPictureConverterFactory.createConverter(
				MediaPictureConverterFactory.HUMBLE_BGR_24,
				videoFrame);
		
		while (!(isMediaRead.get() && isQueueEmpty() && secondPackets.isEmpty()))
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
				}
				while (offset < sp.getSize());
				
				videoData.sendPacket(videoData.getVideoKey(), sp);
				
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
	
	private boolean isQueueEmpty()
	{
		synchronized(videoPacketLock)
		{
			return videoPackets.isEmpty();
		}
	}
}