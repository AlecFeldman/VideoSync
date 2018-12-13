import java.io.Serializable;

import io.humble.ferry.Buffer;
import io.humble.video.MediaPacket;
import io.humble.video.Rational;

public class SerializedPacket implements Serializable
{	
	private byte[] rawData;
	
	private int index;
	private int flags;
	private int numerator;
	private int denominator;
	
	private static final long serialVersionUID = 1L;
	private long presentationTime;
	private long decompressionTime;
	private long duration;
	private long convergenceDuration;
	private long position;
	private long timeStamp;
	
	private boolean isKey;
	
	public SerializedPacket(MediaPacket packet)
	{
		rawData = packet.getData().getByteArray(0, packet.getSize());
		index = packet.getStreamIndex();
		flags = packet.getFlags();
		numerator = packet.getTimeBase().getNumerator();
		denominator = packet.getTimeBase().getDenominator();
		presentationTime = packet.getPts();
		decompressionTime = packet.getDts();
		duration = packet.getDuration();
		convergenceDuration = packet.getConvergenceDuration();
		position = packet.getPosition();
		timeStamp = packet.getTimeStamp();
		isKey = packet.isKeyPacket();
	}

	public MediaPacket getPacket()
	{
		MediaPacket packet = MediaPacket.make(Buffer.make(null, rawData, 0, rawData.length));
		
		packet.setStreamIndex(index);
		packet.setFlags(flags);
		packet.setPts(presentationTime);
		packet.setDts(decompressionTime);
		packet.setDuration(duration);
		packet.setConvergenceDuration(convergenceDuration);
		packet.setPosition(position);
		packet.setTimeStamp(timeStamp);
		packet.setKeyPacket(isKey);
		packet.setTimeBase(Rational.make(numerator, denominator));
		
		return packet;
	}
}