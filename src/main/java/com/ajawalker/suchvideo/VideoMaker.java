package com.ajawalker.suchvideo;

import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * An easy-to-use class for creating videos frame-by-frame.
 */
public class VideoMaker {
	private final Muxer muxer;
	private final Encoder encoder;
	private final FrameViewer viewer;
	private final MediaPicture picture;
	private final MediaPacket packet;

	private MediaPictureConverter converter = null;

	private int numFrames = 0;

	/**
	 * Creates a new video maker.
	 *
	 * @param outputFile the video file to output to
	 * @param width      the width of the video
	 * @param height     the height of the video
	 * @param fps        the video's frames per second
	 */
	public VideoMaker(String outputFile, int width, int height, int fps) throws IOException, InterruptedException {
		Rational framerate = Rational.make(1, fps);
		muxer = Muxer.make(outputFile, null, null);
		MuxerFormat format = muxer.getFormat();
		Codec codec = Codec.findEncodingCodec(format.getDefaultVideoCodecId());
		encoder = Encoder.make(codec);
		encoder.setWidth(width);
		encoder.setHeight(height);
		encoder.setPixelFormat(PixelFormat.Type.PIX_FMT_YUV420P);
		encoder.setTimeBase(framerate);
		if (format.getFlag(MuxerFormat.Flag.GLOBAL_HEADER)) {
			encoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);
		}
		encoder.open(null, null);
		muxer.addNewStream(encoder);
		muxer.open(null, null);
		picture = MediaPicture.make(encoder.getWidth(), encoder.getHeight(), PixelFormat.Type.PIX_FMT_YUV420P);
		picture.setTimeBase(framerate);
		packet = MediaPacket.make();
		viewer = new FrameViewer("Such Video", width, height);
	}

	/**
	 * Adds a frame to the video.
	 *
	 * @param frame the frame to add
	 * @return the number of frames so far
	 */
	public int addFrame(BufferedImage frame) {
		frame = prepFrame(frame);
		if (converter == null) {
			converter = MediaPictureConverterFactory.createConverter(frame, picture);
		}
		converter.toPicture(picture, frame, numFrames);
		do {
			encoder.encode(packet, picture);
			if (packet.isComplete()) {
				muxer.write(packet, false);
			}
		} while (packet.isComplete());

		viewer.showFrame(frame);
		return ++numFrames;
	}

	public void finish() {
		do {
			encoder.encode(packet, null);
			if (packet.isComplete()) {
				muxer.write(packet, false);
			}
		} while (packet.isComplete());
		muxer.close();
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
			preppedFrame.getGraphics().drawImage(frame, 0, 0, null);
			return preppedFrame;
		}
	}
}
