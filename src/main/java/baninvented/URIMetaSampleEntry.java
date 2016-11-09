package baninvented;

import com.googlecode.mp4parser.authoring.builder.FragmentedMp4Builder;
import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.fragment.*;
import com.googlecode.mp4parser.boxes.piff.*;
import com.googlecode.mp4parser.authoring.*;
import com.coremedia.iso.boxes.sampleentry.AbstractSampleEntry;
import java.nio.ByteBuffer;
import com.coremedia.iso.Utf8;
import java.nio.channels.WritableByteChannel;
import java.io.IOException;
import com.coremedia.iso.BoxParser;
import com.googlecode.mp4parser.DataSource;

public class URIMetaSampleEntry extends AbstractSampleEntry {
    private String uri;

	public URIMetaSampleEntry(String uri) {
        super("urim");
        this.uri = uri;
	}

    protected long getContentSize() {
        return Utf8.utf8StringLengthInBytes(uri) + 2;
    }

    protected void getContent(ByteBuffer byteBuffer) {
        if (uri != null) {
            byte[] bytes = Utf8.convert(uri);
            byteBuffer.put(bytes);
        }
    }

    @Override
    public void getBox(WritableByteChannel writableByteChannel) throws IOException {
        writableByteChannel.write(getHeader());

        ByteBuffer byteBuffer = ByteBuffer.allocate(8 + (int)getContentSize());
        // 6 reserved bytes
        byteBuffer.position(6);
        IsoTypeWriter.writeUInt16(byteBuffer, dataReferenceIndex);
        getContent(byteBuffer);
        byteBuffer.rewind();
        writableByteChannel.write(byteBuffer);
    }

    public void parse(DataSource dataSource, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {

    }
}
