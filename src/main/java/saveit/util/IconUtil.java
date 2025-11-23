package saveit.util;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class IconUtil {

    /** DELETE / Trashcan icon */
    public static SVGPath deleteIcon(double scale, Color color) {
        SVGPath icon = new SVGPath();
        icon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 "
                + "2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setFill(color);
        return icon;
    }

    /** EDIT / Pencil icon */
    public static SVGPath editIcon(double scale, Color color) {
        SVGPath icon = new SVGPath();
        icon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75"
                + "L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41"
                + "l-2.34-2.34c-.39-.39-1.02-.39-1.41 0"
                + "l-1.83 1.83 3.75 3.75 1.83-1.83z");
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setFill(color);
        return icon;
    }

    /** ADD / Plus icon */
    public static SVGPath plusIcon(double scale, Color color) {
        SVGPath icon = new SVGPath();
        icon.setContent("M19 13H13V19H11V13H5V11H11V5H13V11H19V13Z");
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setFill(color);
        return icon;
    }

    /** ARROW UP / Sorting indicator (Chevron Up) */
    public static SVGPath arrowUpIcon(double scale, Color color) {
        SVGPath icon = new SVGPath();
        // Path for a standard "keyboard arrow up" or "expand less"
        icon.setContent("M4.5 18.5L11.5 10.5L13.5 14.5L20.5 6.5 M20.5 11V6.5H16");
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setFill(color);
        return icon;
    }

    /** ARROW DOWN / Sorting indicator (Chevron Down) */
    public static SVGPath arrowDownIcon(double scale, Color color) {
        SVGPath icon = new SVGPath();
        // Path for a standard "keyboard arrow down" or "expand more"
        icon.setContent("M6 13L16.3385 25.5L26.1846 19.5L41 35");
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setFill(color);
        return icon;
    }
}