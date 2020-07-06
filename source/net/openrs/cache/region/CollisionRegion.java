package net.openrs.cache.region;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.ZipException;

import net.openrs.cache.Cache;
import net.openrs.cache.Container;
import net.openrs.cache.util.XTEAManager;

/**
 * 
 * 
 * @author Lemons
 */
public class CollisionRegion extends Region {

	private boolean isValid;
	
	private int[][][] tileFlags = new int[4][64][64];

	public CollisionRegion(Cache cache, int id) throws IOException {
		super(id);
		
		int map = cache.getFileId(5, getTerrainIdentifier());
		int loc = cache.getFileId(5, getLocationsIdentifier());
		
		isValid = map != -1 || loc != -1;
		
		if (!isValid) {
			return;
		}

		if (map != -1) {
			loadTerrain(cache.read(5, map).getData());

			initTileFlags();
		}

		if (loc != -1) {
			ByteBuffer buffer = cache.getStore().read(5, loc);
			try {
				loadLocations(Container.decode(buffer, XTEAManager.lookupMap(id)).getData());
			} catch (ZipException e) {
				// Invalid key?
			} catch (Exception e) {
				if (buffer.limit() == 32) {
					throw e;
				}
			}
		}
	}
	
	private void initTileFlags() {
		for (int plane = 0; plane < 4; plane++) {
			for (int x = 0; x < 64; x++) {
				for (int y = 0; y < 64; y++) {
					initTile(plane, x, y);
				}
			}
		}
	}

	private void initTile(int plane, int x, int y) {
		int aplane = plane;
		if ((getRenderRule(plane, x, y) & 1) == 1) {
			
			if ((getRenderRule(1, x, y) & 2) == 2) {
				aplane--; 
			}
			
			if (aplane >= 0) {
				tileFlags[aplane][x][y] = RegionFlag.BLOCKED.flag;
			}
		}
		
		aplane = plane;
		if (getRegionID() == 13878 && plane == 2) {
			aplane = 3;
		}
		
		if (getOverlayId(aplane, x, y) == 0 && getUnderlayId(aplane, x, y) == 0) {
			tileFlags[aplane][x][y] = RegionFlag.BLOCKED.flag;
		}
	}

	public boolean isValid() {
		return isValid;
	}

	public void addTileFlag(Position p, RegionFlag... flags) {
		addTileFlag(p.getHeight(), p.getXInRegion(), p.getYInRegion(), flags);
	}

	public void addTileFlag(int plane, int x, int y, RegionFlag... flags) {
		for (RegionFlag flag : flags) {
			tileFlags[plane][x][y] |= flag.flag;
		}
	}

	public void setTileFlag(Position p, RegionFlag... flags) {
		setTileFlag(p.getHeight(), p.getXInRegion(), p.getYInRegion(), flags);
	}

	public void setTileFlag(int plane, int x, int y, RegionFlag... flags) {
		tileFlags[plane][x][y] = 0;
		addTileFlag(plane, x, y, flags);
	}

	public boolean isEmpty() {
		for (int plane = 0; plane < 4; plane++) {
			for (int x = 0; x < 64; x++) {
				for (int y = 0; y < 64; y++) {
					int flag = tileFlags[plane][x][y];
					if (!RegionFlag.BLOCKED.test(flag)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public int[][][] getTileFlags() {
		return tileFlags;
	}

}
