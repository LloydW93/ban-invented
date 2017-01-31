package baninvented;

import com.coremedia.iso.boxes.*;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;
import com.coremedia.iso.Utf8;
import com.coremedia.iso.IsoTypeReader;

public class KindBox extends AbstractFullBox {
	private String scheme_id_uri;
	private String value;

	protected KindBox(String scheme_id_uri, String value) {
		super("kind");
		this.scheme_id_uri = scheme_id_uri;
		this.value = value;
	}

	@Override
	protected long getContentSize() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
		getContent(byteBuffer);
		return byteBuffer.position();
	}

	@Override
	protected void getContent(ByteBuffer byteBuffer) {
		writeVersionAndFlags(byteBuffer);
		IsoTypeWriter.writeUtf8String(byteBuffer, this.scheme_id_uri);
		IsoTypeWriter.writeUtf8String(byteBuffer, this.value);
	}

	@Override
	protected void _parseDetails(ByteBuffer content) {
		scheme_id_uri = IsoTypeReader.readString(content);
		value = IsoTypeReader.readString(content);
	}
}