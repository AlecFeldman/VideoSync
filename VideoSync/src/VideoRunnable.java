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

public class VideoRunnable implements Runnable
{
	private int offset;
	private int bytesRead;
	
	private Object videoPacketLock = new Object();
	
	private AtomicBoolean runVideo = new AtomicBoolean(true);
	
	private Decoder videoDecoder;
	
	private MediaPicture videoFrame;
	
	private MediaPictureConverter videoConverter;
	
	private ImageFrame window = ImageFrame.make();
	
	private BufferedImage image = null;
	
	private Queue<MediaPacket> videoPackets = new ArrayDeque<>();
	
	public VideoRunnable(Decoder videoDecoder)
	{
		this.videoDecoder = videoDecoder;
		this.videoDecoder.open(null, null);
		
		videoFrame = MediaPicture.make(
			this.videoDecoder.getWidth(),
			this.videoDecoder.getHeight(),
			this.videoDecoder.getPixelFormat());
		
		videoConverter =
			MediaPictureConverterFactory.createConverter(
				MediaPictureConverterFactory.HUMBLE_BGR_24,
				videoFrame);
	}
	
	public void run()
	{
		while (runVideo.get())
		{
			synchronized(videoPacketLock)
			{
				for(MediaPacket vp = videoPackets.poll(); vp != null; vp = videoPackets.poll())
				{
					try
					{
						Thread.sleep(1000 / 30);
					} catch (InterruptedException e)
					{
						e.printStackTrace();
					}
					
					offset = 0;
					bytesRead = 0;
					
					do
					{
						bytesRead += videoDecoder.decode(videoFrame, vp, offset);
						if (videoFrame.isComplete())
						{
							image = videoConverter.toImage(image, videoFrame);
							window.setImage(image);
						}
						offset += bytesRead;
					} while (offset < vp.getSize());
				}
			}
		}
		
		window.dispose();
	}
	
	public void addVideoPacket(MediaPacket packet)
	{
		synchronized(videoPacketLock)
		{
			videoPackets.add(MediaPacket.make(packet, true));
		}
	}
	
	public void stopVideo()
	{
		runVideo.set(false);
	}
}
