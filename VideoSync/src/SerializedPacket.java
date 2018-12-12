import java.io.Serializable;

import io.humble.video.MediaPacket;

public class SerializedPacket implements Serializable
{
	private byte[] rawData;
	
	private int streamIndex;
	private int flags;
	
	private static final long serialVersionUID = 1L;
	private long presentationTime;
	private long decompressionTime;
	private long duration;
	private long position;
	private long convergenceDuration;
	
	public SerializedPacket(MediaPacket packet)
	{
		rawData = packet.getData().getByteArray(0, packet.getSize());
		presentationTime = packet.getPts();
		decompressionTime = packet.getDts();
		streamIndex = packet.getStreamIndex();
		flags = packet.getFlags();
		duration = packet.getDuration();
		position = packet.getPosition();
		convergenceDuration = packet.getConvergenceDuration();
	}

	public int getStreamIndex()
	{
		return streamIndex;
	}

	public int getFlags()
	{
		return flags;
	}

	public long getPresentationTime()
	{
		return presentationTime;
	}

	public long getDecompressionTime()
	{
		return decompressionTime;
	}

	public long getDuration()
	{
		return duration;
	}

	public long getPosition()
	{
		return position;
	}

	public long getConvergenceDuration()
	{
		return convergenceDuration;
	}
	
	public byte[] getRawData()
	{
		return rawData;
	}
}