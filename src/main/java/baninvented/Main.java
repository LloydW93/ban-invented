package baninvented;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import com.coremedia.iso.Hex;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.Utf8;
import com.coremedia.iso.boxes.*;
import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.authoring.AbstractTrack;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.SampleImpl;
import com.googlecode.mp4parser.authoring.TrackMetaData;
import com.googlecode.mp4parser.authoring.builder.DefaultFragmenterImpl;
import com.googlecode.mp4parser.authoring.builder.Fragmenter;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.util.ByteBufferByteChannel;
import com.jamesmurty.utils.XMLBuilder2;

import baninvented.IsoTypeWriter;
import baninvented.EmsgFragmentedMp4Builder;
import baninvented.URIMetaSampleEntry;

public class Main {

	private static final int TIMESCALE = 10_000_000;
	private static final double FRAGMENT_DURATION_SECONDS = 3.84;
	private static final long EPOCH_TIMESTAMP_UTC = 0;

	public static class DASHEventMessageBox extends AbstractFullBox {

		private String scheme_id_uri;
		private String value;
		private long timescale;
		private long presentation_time_delta;
		private long event_duration;
		private long id;
		private String message_data;

		protected DASHEventMessageBox(String scheme_id_uri, String value, long timescale, long presentation_time_delta, long event_duration, long id, String message_data) {
			super("emsg");
			this.scheme_id_uri = scheme_id_uri;
			this.value = value;
			this.timescale = timescale;
			this.presentation_time_delta = presentation_time_delta;
			this.event_duration = event_duration;
			this.id = id;
			this.message_data = message_data;
		}

		@Override protected long getContentSize() {
			ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
			getContent(byteBuffer);
			return byteBuffer.position();
		}

		@Override
		protected void getContent(ByteBuffer byteBuffer) {
			writeVersionAndFlags(byteBuffer);
			IsoTypeWriter.writeUtf8String(byteBuffer, scheme_id_uri);
			IsoTypeWriter.writeUtf8String(byteBuffer, value);
			IsoTypeWriter.writeUInt32(byteBuffer, timescale);
			IsoTypeWriter.writeUInt32(byteBuffer, presentation_time_delta);
			IsoTypeWriter.writeUInt32(byteBuffer, event_duration);
			IsoTypeWriter.writeUInt32(byteBuffer, id);
			IsoTypeWriter.writeUtf8String(byteBuffer, message_data);
		}

		@Override
		protected void _parseDetails(ByteBuffer content) {
			scheme_id_uri = IsoTypeReader.readString(content);
			value = IsoTypeReader.readString(content);
			timescale = IsoTypeReader.readUInt32(content);
			presentation_time_delta = IsoTypeReader.readUInt32(content);
			event_duration = IsoTypeReader.readUInt32(content);
			id = IsoTypeReader.readUInt32(content);
			message_data = IsoTypeReader.readString(content);
		}

	}

	public static class Meta {
		private long from;
		private long to;
		private DASHEventMessageBox box;
		
		public Meta(long from, long to, DASHEventMessageBox box) {
			super();
			this.from = from;
			this.to = to;
			this.box = box;
		}

		public long getFrom() {
			return from;
		}
		public long getTo() {
			return to;
		}
		public DASHEventMessageBox getBox() {
			return box;
		}
	}

	public static class MetaTrackImpl extends AbstractTrack {

		private static final String HANDLER_META = "meta";
		private SampleDescriptionBox sampleDescriptionBox;
		private TrackMetaData trackMetaData;
		private List<Meta> metas;

		public MetaTrackImpl() {
			super("meta");

			sampleDescriptionBox = new SampleDescriptionBox();
			sampleDescriptionBox.addBox(new URIMetaSampleEntry("http://www.bbc.co.uk/2016/dash/emsg"));
			metas = new LinkedList<Meta>();

			trackMetaData = createMetadata();
		}

		private static TrackMetaData createMetadata() {
			TrackMetaData trackMetaData = new TrackMetaData();
			trackMetaData.setCreationTime(new Date());
			trackMetaData.setModificationTime(new Date());
			trackMetaData.setTimescale(TIMESCALE);
			return trackMetaData;
		}
		
		public List<Meta> getMetadataEntries() {
			return metas;
		}

		@Override
		public SampleDescriptionBox getSampleDescriptionBox() {
			return sampleDescriptionBox;
		}

		@Override
		public long[] getSampleDurations() {
			List<Long> decTimes = new ArrayList<Long>();

			long lastEnd = 0;
			for (Meta meta : metas) {
				long silentTime = meta.from - lastEnd;
				if (silentTime < 0) {
					// TODO: can we actually allow overlapping samples?
					throw new Error("Metadata times may not intersect");
				}
				decTimes.add( meta.to - meta.from);
				lastEnd = meta.to;
			}
			long[] decTimesArray = new long[decTimes.size()];
			int index = 0;
			for (Long decTime : decTimes) {
				decTimesArray[index++] = decTime;
			}
			return decTimesArray;
		}

		@Override
		public TrackMetaData getTrackMetaData() {
			return trackMetaData;
		}

		@Override
		public String getHandler() {
			return HANDLER_META;
		}

		@Override
		public List<Sample> getSamples() {
			List<Sample> samples = new ArrayList<Sample>();
			long lastEnd = 0;
			for (Meta meta : metas) {
				long silentTime = meta.from - lastEnd;
				if (silentTime < 0) {
					throw new RuntimeException("Metadata times may not intersect");
				}
				ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int)meta.getBox().getContentSize() + 8);
				try {
					meta.getBox().getBox(new ByteBufferByteChannel(byteBuffer));
				} catch (IOException e) {

				}
				byteBuffer.flip();
				samples.add(new SampleImpl(byteBuffer));
				lastEnd = meta.to;
			}
			return samples;
		}

		@Override
		public void close() throws IOException {
		}
	}
	
	public static class IntentToEnd {
		private Date publishTime;
		private Date endTime;

		public IntentToEnd(Date publishTime, Date endTime) {
			this.publishTime = publishTime;
			this.endTime = endTime;
		}
		public Date getEndTime() {
			return endTime;
		}
		public Date getPublishTime() {
			return publishTime;
		}
	}

	public static void main(String[] args) throws Exception {
		long now = System.currentTimeMillis();
		// the publish time needs to be sufficiently far in the future that
		// any MPD files retrieved just now which still include the old @publishTime value 
		// will have expired by the time that the in-band events indicated an MPD reload is needed
		IntentToEnd end = new IntentToEnd(new Date(now + 120*1000), new Date(now + 180*1000));
		EmsgFragmentedMp4Builder builder = new EmsgFragmentedMp4Builder();
		MetaTrackImpl metaTrack = createMetadataTrack(end, builder);
		Movie movie = new Movie();
		movie.addTrack(metaTrack);
		builder.setFragmenter(new DefaultFragmenterImpl(FRAGMENT_DURATION_SECONDS));
		Container mp4file = builder.build(movie);
		writeOut(mp4file);
	}
	
	private static long millisToMediaTime(Date t) {
		final long MILLIS = 1000;
		// division in brackets so we don't overflow an int64
		return t.getTime() * (TIMESCALE / MILLIS) - EPOCH_TIMESTAMP_UTC;
	}

	private static MetaTrackImpl createMetadataTrack(IntentToEnd end, EmsgFragmentedMp4Builder builder) throws IOException {
		// we start publishing in-band events from the given publish-time onwards (on the media timeline)
		// all these events refer to the same end-time
		// the first emitted in-band event instance will be contained within
		// the fragment that *contains* the given publishTime
		final long fragDurationInMediaTime = (long)(FRAGMENT_DURATION_SECONDS * TIMESCALE);
		final long endTimeInMediaTimeline = millisToMediaTime(end.getEndTime());
		long publishTimeInMediaTime = millisToMediaTime(end.getPublishTime());
		final long publishTimeWithinFragment = publishTimeInMediaTime % fragDurationInMediaTime;
		final long firstFragmentTimestamp = publishTimeInMediaTime - publishTimeWithinFragment;
		final long fragmentCount = 1 + (millisToMediaTime(end.getEndTime()) - firstFragmentTimestamp) / fragDurationInMediaTime;
		MetaTrackImpl metaTrack = new MetaTrackImpl();
		builder.setOffset(firstFragmentTimestamp);
		System.out.println("Chosen publishTime is " + publishTimeInMediaTime);
		System.out.println("Chosen endTime is " + endTimeInMediaTimeline);
		System.out.println("Chosen fragmentCount is " + fragmentCount);
		long fragmentTimestamp = firstFragmentTimestamp;
		// Clients use event identifiers to spot when an event they've seen
		// serialised in an earlier fragment is repeated in a later fragment.
		// when we 
		final long eventId = 1;
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String timestamp = fmt.format(end.getEndTime());
		for (int i=0; i<fragmentCount; i++) {
			// presentationTimeDelta being non-zero indicates that, rather than
			// the event occurring at the timestamp of the containing fragment,
			// it occurs at some later time. The value we want to refer to for
			// this event is the actual end of the stream, so we calculate the
			// difference between the start of the fragment, and the end time we
			// were given,
			long presentationTimeDelta = endTimeInMediaTimeline - fragmentTimestamp;
			Meta meta = new Meta(fragmentTimestamp,
								 fragmentTimestamp+fragDurationInMediaTime,
								 createMetadataDoc("urn:mpeg:dash:event:2012", eventId, presentationTimeDelta, timestamp));
			int metaSize = (int)meta.getBox().getContentSize();
			System.out.println("Timestamp: "+fragmentTimestamp);
			System.out.println(metaSize);
			metaTrack.getMetadataEntries().add(meta);

			File emsgFile = new File("/tmp/emsg_" + i + ".bin");
			FileChannel channel = new FileOutputStream(emsgFile, false).getChannel();
			meta.getBox().getBox(channel);
			channel.close();

			fragmentTimestamp += fragDurationInMediaTime;
		}
		return metaTrack;
	}

	private static DASHEventMessageBox createMetadataDoc(String scheme, long id, long presentationTimeDelta, String messageData) {
		return new DASHEventMessageBox(
			scheme,
			"1",
			TIMESCALE,
			presentationTimeDelta,
			0,
			id,
			messageData);
	}

	private static String asString(XMLBuilder2 doc) {
		Properties props = new Properties();
		props.put(javax.xml.transform.OutputKeys.INDENT, "yes");
		props.put("{http://xml.apache.org/xslt}indent-amount", "2");
		return doc.asString(props);
	}

	private static void writeOut(Container mp4file)
			throws IOException
	{
		FileOutputStream fos = new FileOutputStream(new File("/tmp/output.mp4"));
		try {
			mp4file.writeContainer(fos.getChannel());
		} finally {
			fos.close();
		}
	}
}
