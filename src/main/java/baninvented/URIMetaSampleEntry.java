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
import com.googlecode.mp4parser.util.ByteBufferByteChannel;

import baninvented.URIBox;

public class URIMetaSampleEntry extends AbstractSampleEntry {

	public URIMetaSampleEntry(String uri) {
        super("urim");
        addBox(new URIBox(uri));
	}

    // AbstractSampleEntry's getSize doesn't take into account the 8 byte headers (6 bytes reserved, 2 bytes dataReferenceIndex)
    @Override
    public long getSize() {
        long s = getContainerSize();
        return s + ((largeBox || (s + 8) >= (1L << 32)) ? 16 : 8) + 8;
    }

    public void getBox(WritableByteChannel writableByteChannel) throws IOException {
        writableByteChannel.write(getHeader());
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.position(6);
        IsoTypeWriter.writeUInt16(bb, dataReferenceIndex);
        writableByteChannel.write((ByteBuffer) bb.rewind());
        writeContainer(writableByteChannel);
    }

    public void parse(DataSource dataSource, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {
        throw new IOException("Not implemented");
    }
}
