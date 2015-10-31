package com.ajawalker.suchvideo;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IRational;

import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

/**
 * An easy-to-use class for creating MP4 videos frame-by-frame.
 */
public class VideoMaker {
	private final IMediaWriter writer;
	private final IRational frameRate;
	private final long nanosPerFrame;

	private int numFrames = 0;

	/**
	 * Creates a new video maker.
	 *
	 * @param outputFile the video file to output to
	 * @param width      the width of the video
	 * @param height     the height of the video
	 * @param fps        the video's frames per second
	 */
	public VideoMaker(String outputFile, int width, int height, int fps) {
		frameRate = IRational.make(fps, 1);
		nanosPerFrame = 1000000000 / fps;
		writer = ToolFactory.makeWriter(outputFile);
		writer.addVideoStream(0, 0, frameRate, width, height);
	}

	/**
	 * Adds a frame to the video.
	 *
	 * @param frame the frame to add
	 * @return the number of frames so far
	 */
	public int addFrame(BufferedImage frame) {
		writer.encodeVideo(
				0,
				prepFrame(frame),
				numFrames * nanosPerFrame,
				TimeUnit.NANOSECONDS);
		return ++numFrames;
	}

	public void finish() {
		writer.close();
	}

	/**
	 * Prepares a frame by ensuring it is of the correct type.
	 *
	 * @param frame the frame to prepare
	 * @return the prepared frame
	 */
	private BufferedImage prepFrame(BufferedImage frame) {
		if (frame.getType() == BufferedImage.TYPE_3BYTE_BGR) {
			// it's already good
			return frame;
		} else {
			// copy to a new frame with correct type
			BufferedImage preppedFrame = new BufferedImage(
					frame.getWidth(),
					frame.getHeight(),
					BufferedImage.TYPE_3BYTE_BGR);
			frame.getGraphics().drawImage(preppedFrame, 0, 0, null);
			return preppedFrame;
		}
	}
}
