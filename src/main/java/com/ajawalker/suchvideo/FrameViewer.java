package com.ajawalker.suchvideo;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A simple viewer for viewing frames as videos are made.
 */
public class FrameViewer {
    private final BufferedImage buffer;
    private final JPanel panel;

	/**
	 * Constructs a new instance.
	 * @param title the title of the frame viewing window
	 * @param width the width of the window
	 * @param height the height of the window
	 */
    public FrameViewer(String title, int width, int height) {
        buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension size = new Dimension(width, height);
        panel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                synchronized (buffer) {
                    g.drawImage(buffer, 0, 0, null);
                }
            }
        };
        panel.setPreferredSize(size);
        panel.setMinimumSize(size);
        panel.setMaximumSize(size);
        panel.setSize(size);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Displays the next frame.
     */
    public void showFrame(BufferedImage frame) {
        synchronized (buffer) {
            buffer.getGraphics().drawImage(frame, 0, 0, null);
        }
        panel.repaint();
    }
}
