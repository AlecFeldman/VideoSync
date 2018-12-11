import java.io.IOException;

import io.humble.video.MediaPacket;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

public class MediaDHT
{
	private PeerDHT mediaData;
	
	private Number160 videoKey;
	private Number160 audioKey;
	private Number160 indexKey;
	private Number160 codecKey;
	
	public MediaDHT(Peer client, Number160 videoKey, Number160 audioKey, Number160 indexKey, Number160 codecKey)
	{
		mediaData = new PeerBuilderDHT(client).start();
		
		this.videoKey = videoKey;
		this.audioKey = audioKey;
		this.indexKey = indexKey;
		this.codecKey = codecKey;
	}
	
	public Number160 getVideoKey()
	{
		return videoKey;
	}

	public Number160 getAudioKey()
	{
		return audioKey;
	}

	public Number160 getIndexKey()
	{
		return indexKey;
	}

	public Number160 getCodecKey()
	{
		return codecKey;
	}

	public void putData(Number160 mediaKey, Number160 dKey, Data md)
	{
		FuturePut mediaPut = mediaData.put(mediaKey).data(md).domainKey(dKey).start();
		
		mediaPut.awaitUninterruptibly();
	}
	
	public Object getData(Number160 mediaKey, Number160 domainKey) throws ClassNotFoundException, IOException
	{
		FutureGet mediaGet = mediaData.get(mediaKey).all().domainKey(domainKey).start();
		
		mediaGet.awaitUninterruptibly();
		
		return mediaGet.data().object();
	}
	
	public void sendPacket(Number160 mediaKey, MediaPacket packet)
	{
		mediaData.send(mediaKey).object(new MediaPacketSerialized(packet)).start();
	}
}
