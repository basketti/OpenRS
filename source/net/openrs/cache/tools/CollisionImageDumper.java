package net.openrs.cache.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.openrs.cache.Constants;
import net.openrs.cache.region.RegionFlag;

public class CollisionImageDumper {
	
	private static final int RENDER_PLANE = 0;

	public static void main(String[] args) throws IOException {
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		
		Map<Integer, Integer[][][]> map = new HashMap<>();
		
		for (Path file : Files.list(Paths.get(Constants.COLLISIONMAPS_PATH)).toArray(Path[]::new)) {
			String filename = file.getFileName().toString();
			if (!filename.startsWith("c")) {
				continue;
			}
			
			String[] fargs = filename.substring(1,filename.indexOf(".")).split("_");

			int regionX = Integer.parseInt(fargs[0]);
			int regionY = Integer.parseInt(fargs[1]);

			minX = Math.min(regionX, minX);
			minY = Math.min(regionY, minY);
			maxX = Math.max(regionX, maxX);
			maxY = Math.max(regionY, maxY);
			
			int id = (regionX << 8) + regionY;
			
			Integer[][][] tileFlags = new Integer[4][64][64];
			byte[] data = Files.readAllBytes(file);
			int dataIndex = 0;
			for (int z = 0; z < 4; z++) {
				for (int x = 0; x < 64; x++) {
					for (int y = 0; y < 64; y++) {
						tileFlags[z][x][y] =
								((0xFF & data[dataIndex++]) << 24)
								| ((0xFF & data[dataIndex++]) << 16)
								| ((0xFF & data[dataIndex++]) << 8)
								| (0xFF & data[dataIndex++]);
					}
				}
			}
			
			map.put(id, tileFlags);
		}
		
		int width = maxX - minX;
		int height = maxY - minY;
		
		int imageWidth = (width + 1) * 128;
		int imageHeight = (height + 1) * 128;
		
		System.out.println("Map size: "+imageWidth+"px by "+imageHeight+"px");
		
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D graphics = (Graphics2D) image.getGraphics();
		
		for (Integer regionId : map.keySet()) {
			Integer[][][] flags = map.get(regionId);
			
			int regionX = regionId >> 8;
			int regionY = regionId & 0xFF;
			int paintBaseX = (regionX - minX) * 128;
			int paintBaseY = ((maxY - minY) * 128) - ((regionY - minY) * 128);
			
			int f;
			int px;
			int py;
			for (int x = 0; x < 64; x++) {
				for (int y = 0; y < 64; y++) {
					f = flags[RENDER_PLANE][x][y];
					px = paintBaseX + (x * 2);
					py = paintBaseY + (128 - (y * 2));
					graphics.setColor(Color.white);
					graphics.fillRect(px, py, 2, 2);
					if (RegionFlag.BLOCKED.test(f)) {
						graphics.setColor(Color.red);
						graphics.fillRect(px, py, 2, 2);
					}
					graphics.setColor(Color.green);
					if (RegionFlag.WALL_NORTH.test(f)) {
						graphics.drawLine(px, py, px + 1, py);
					}
					if (RegionFlag.WALL_SOUTH.test(f)) {
						graphics.drawLine(px, py + 1, px + 1, py + 1);
					}
					if (RegionFlag.WALL_EAST.test(f)) {
						graphics.drawLine(px + 1, py, px + 1, py + 1);
					}
					if (RegionFlag.WALL_WEST.test(f)) {
						graphics.drawLine(px, py, px, py + 1);
					}
//					graphics.setColor(Color.magenta);
//					if (RegionFlag.WALL_NORTHEAST.test(f)) {
//						graphics.drawLine(px + 1, py, px + 1, py);
//					}
//					if (RegionFlag.WALL_SOUTHEAST.test(f)) {
//						graphics.drawLine(px + 1, py + 1, px + 1, py + 1);
//					}
//					if (RegionFlag.WALL_NORTHWEST.test(f)) {
//						graphics.drawLine(px, py, px, py);
//					}
//					if (RegionFlag.WALL_SOUTHWEST.test(f)) {
//						graphics.drawLine(px, py + 1, px, py + 1);
//					}
				}
			}
			
			graphics.setColor(Color.black);
			graphics.drawRect(paintBaseX, paintBaseY + 1, 128, 128);
			graphics.fillRect(paintBaseX, paintBaseY + 1, graphics.getFontMetrics().stringWidth(""+regionId) + 4, 16);
			graphics.setColor(Color.white);
			graphics.drawString(""+regionId, paintBaseX + 2, paintBaseY + 15);
		}
		
		ImageIO.write(image, "png", Paths.get(Constants.COLLISIONMAPS_PATH, "image-z"+RENDER_PLANE+".png").toFile());
	}
	
}
