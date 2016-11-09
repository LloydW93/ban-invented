package baninvented;

import java.util.List;

import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.util.Mp4Arrays;
import com.googlecode.mp4parser.authoring.builder.Fragmenter;
import com.googlecode.mp4parser.authoring.Sample;

import java.util.Arrays;

/**
 * Finds start samples within a given track so that:
 * <ul>
 * <li>Each segment is at least <code>fragmentLength</code> seconds long</li>
 * <li>The last segment might be almost twice the size of the rest</li>
 * </ul>
 */
public class OffsetFragmenterImpl implements Fragmenter {
    private double fragmentLength = 2.0D;
    private double offset = 0.0D;

    public OffsetFragmenterImpl(double fragmentLength, double offset) {
        this.fragmentLength = fragmentLength;
        this.offset = offset;
    }

    public long[] sampleNumbers(Track track) {
        List<Sample> samples = track.getSamples();
        long[] segmentStartSamples = new long[]{1L};
        long[] sampleDurations = track.getSampleDurations();
        long timescale = track.getTrackMetaData().getTimescale();
        double time = offset;

        for (int i = 0; i < sampleDurations.length; ++i) {
            time += (double) sampleDurations[i] / (double) timescale;
            if (time >= this.fragmentLength) {
                if (i > 0) {
                    segmentStartSamples = Mp4Arrays.copyOfAndAppend(segmentStartSamples, (long) (i + 1));
                }

                time = 0.0D;
            }
        }
        // In case the last Fragment is shorter: make the previous one a bigger and omit the small one
        if (time < fragmentLength && segmentStartSamples.length > 1) {
            long[] nuSegmentStartSamples = new long[segmentStartSamples.length - 1];
            System.arraycopy(segmentStartSamples, 0, nuSegmentStartSamples, 0, segmentStartSamples.length - 1);
            segmentStartSamples = nuSegmentStartSamples;
        }

        return segmentStartSamples;
    }

}