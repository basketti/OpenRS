package net.openrs.cache.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.openrs.cache.Cache;
import net.openrs.cache.Constants;
import net.openrs.cache.FileStore;
import net.openrs.cache.region.CollisionMap;
import net.openrs.cache.region.CollisionRegion;
import net.openrs.cache.region.LocationFilter;
import net.openrs.cache.region.Position;
import net.openrs.cache.region.ZShiftDetector;
import net.openrs.cache.type.TypeListManager;
import net.openrs.cache.util.XTEAManager;

public class CollisionDumper {

	private void initialize(Cache cache) {
		TypeListManager.initialize(cache);
		XTEAManager.touch();
		
		CollisionMap map = new CollisionMap(cache,
				new LocationFilter(Paths.get(Constants.COLLISIONMAPS_PATH, "filter.txt")));
		
		int loaded = map.getLoaded();
		for (CollisionRegion region : map.getRegions()) {
			if (region.isEmpty()) {
				loaded--;
				continue;
			}
			
			// This region is ready to dump
			byte[] data = new byte[65536];
			int dataIndex = 0;
			
			int[][][] tileFlags = region.getTileFlags();
			
			for (int z = 0; z < tileFlags.length; z++) {
				for (int x = 0; x < tileFlags[z].length; x++) {
					for (int y = 0; y < tileFlags[z][x].length; y++) {
						int flag = tileFlags[z][x][y];
						data[dataIndex++] = (byte) ((flag >> 24) & 0xFF);
						data[dataIndex++] = (byte) ((flag >> 16) & 0xFF);
						data[dataIndex++] = (byte) ((flag >> 8) & 0xFF);
						data[dataIndex++] = (byte) (flag & 0xFF);
					}
				}
			}
			
			int x = (region.getRegionID() >> 8);
			int y = (region.getRegionID() & 0xFF);
			
			Path file = Paths.get(Constants.COLLISIONMAPS_PATH, "c" + x + "_" + y + ".dat");
			
			try {
				Files.write(file, data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Wrote "+loaded+" collision maps!");
		System.out.println("Detecting ZShift's using location data...");
		
		ZShiftDetector zshift = new ZShiftDetector(map);
		
		for (CollisionRegion region : map.getRegions()) {
			if (region.isEmpty()) {
				continue;
			}
			
			List<Position[]> links = zshift.getLinks(region);
			
			if (links.isEmpty()) {
				continue;
			}
			
			// This region is ready to dump
			byte[] data = new byte[links.size() * 8];
			int dataIndex = 0;
			
			for (Position[] link : links) {
				for (Position p : link) {
					int flag = (p.getX() << 17) | (p.getY() << 2) | p.getHeight();
					data[dataIndex++] = (byte) ((flag >> 24) & 0xFF);
					data[dataIndex++] = (byte) ((flag >> 16) & 0xFF);
					data[dataIndex++] = (byte) ((flag >> 8) & 0xFF);
					data[dataIndex++] = (byte) (flag & 0xFF);
				}
			}
			
			int x = (region.getRegionID() >> 8);
			int y = (region.getRegionID() & 0xFF);
			
			Path file = Paths.get(Constants.COLLISIONMAPS_PATH, "z" + x + "_" + y + ".dat");
			
			try {
				Files.write(file, data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		long ms = System.currentTimeMillis();
		CollisionDumper dumper = new CollisionDumper();
		
		Path collisionmapsPath = Paths.get(Constants.COLLISIONMAPS_PATH);
		
		if (!Files.exists(collisionmapsPath)) {
			Files.createDirectories(collisionmapsPath);
		}

		try (Cache cache = new Cache(FileStore.open(Constants.CACHE_PATH)))
		{
			dumper.initialize(cache);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		System.out.printf("Completed collision dump in %s seconds\n",
				TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - ms));
	}
	
}
