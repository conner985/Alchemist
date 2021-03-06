package it.unibo.alchemist.boundary.projectview.utils;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.InputStream;

import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;
import javafx.scene.image.Image;

/**
 * A class with static methods that install a SVG loader and return a SVG image.
 *
 */
public final class SVGImageUtils {

    private SVGImageUtils() {
    }

    /**
     * Install the SVG loader.
     */
    public static void installSvgLoader() {
        SvgImageLoaderFactory.install();
    }

    /**
     * Returns the Image of a SVG image.
     * 
     * @param path
     *            The SVG image position
     * @param width
     *            The percent width of image
     * @param height
     *            The percent height of image
     * @return The image
     */
    public static Image getSvgImage(final String path, final double width, final double height) {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final InputStream imageStream = SVGImageUtils.class.getClassLoader().getResourceAsStream(path);
        final Image image = new Image(imageStream, screenSize.getWidth() * width / 100,
                screenSize.getHeight() * height / 100, true, true);
        return image;
    }

}
