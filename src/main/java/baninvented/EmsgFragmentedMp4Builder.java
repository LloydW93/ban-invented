package baninvented;

import com.googlecode.mp4parser.authoring.builder.FragmentedMp4Builder;
import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.fragment.*;
import com.googlecode.mp4parser.boxes.piff.*;
import com.googlecode.mp4parser.authoring.*;

public class EmsgFragmentedMp4Builder extends FragmentedMp4Builder {
	private long offset = 0L;

	// Dumb down the original version and always just add an nmhd
	protected Box createMinf(Track track, Movie movie) {
		MediaInformationBox minf = new MediaInformationBox();
		minf.addBox(new NullMediaHeaderBox());
		minf.addBox(createDinf(movie, track));
		minf.addBox(createStbl(movie, track));
		return minf;
	}

	protected void createTfdt(long startSample, Track track, TrackFragmentBox parent) {
        TrackFragmentBaseMediaDecodeTimeBox tfdt = new TrackFragmentBaseMediaDecodeTimeBox();
        tfdt.setVersion(1);
        tfdt.setBaseMediaDecodeTime(getBaseMediaDecodeTime(startSample, track));
        parent.addBox(tfdt);
    }

    protected void createTfxd(long startSample, Track track, MovieFragmentBox parent) {
        TfxdBox tfxd = new TfxdBox();
        tfxd.fragmentAbsoluteTime = getBaseMediaDecodeTime(startSample, track);
        tfxd.fragmentAbsoluteDuration = getSampleDuration(startSample, track);
        parent.addBox(tfxd);
    }

    protected Box createMoof(long startSample, long endSample, Track track, int sequenceNumber) {
        MovieFragmentBox moof = new MovieFragmentBox();
        createMfhd(startSample, endSample, track, sequenceNumber, moof);
        createTraf(startSample, endSample, track, sequenceNumber, moof);
        createTfxd(startSample, track, moof);

        TrackRunBox firstTrun = moof.getTrackRunBoxes().get(0);
        firstTrun.setDataOffset(1); // dummy to make size correct
        firstTrun.setDataOffset((int) (8 + moof.getSize())); // mdat header + moof size

        return moof;
    }

    protected long getBaseMediaDecodeTime(long startSample, Track track) {
    	long startTime = offset;
        long[] times = track.getSampleDurations();
        for (int i = 1; i < startSample; i++) {
            startTime += times[i - 1];
        }
        return startTime;
    }

    protected long getSampleDuration(long startSample, Track track) {
    	long times[] = track.getSampleDurations();
    	return times[(int)startSample - 1];
    }

    protected void setOffset(long offset) {
    	this.offset = offset;
    }
}
