package baninvented;

import com.coremedia.iso.boxes.*;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;
import com.coremedia.iso.Utf8;

public class URIBox extends AbstractFullBox {
    private String uri;

	public URIBox(String uri) {
        super("uri ");
        this.uri = uri;
        setVersion(0);
        setFlags(0);
	}

    @Override
    protected long getContentSize() {
        return Utf8.utf8StringLengthInBytes(uri) + 5;
    }

    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUtf8String(byteBuffer, uri);
    }

    protected void _parseDetails(ByteBuffer byteBuffer) {

    }
}
