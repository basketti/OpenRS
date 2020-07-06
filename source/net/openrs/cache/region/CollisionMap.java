package net.openrs.cache.region;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import net.openrs.cache.Cache;
import net.openrs.cache.type.TypeListManager;
import net.openrs.cache.type.objects.ObjectType;

/**
 * Loads all the regions and compiles the collision maps.
 * 
 * @author Lemons
 */
public class CollisionMap {
	
	public static final int MAX_REGION = 32768;
	
	private static final int[][][] WALL_OFFSETS = {
			{ {-1, 0}, {0, 1}, {1, 0}, {0, -1} },
			{ {-1, 1}, {1, 1}, {1, -1}, {-1, -1} },
	};
	
	private Map<Integer, CollisionRegion> regions = new HashMap<>();
	
	private int loaded = 0;

	private Predicate<Location> filter;
	
	public CollisionMap(Cache cache) {
		this(cache, loc -> true);
	}

	public CollisionMap(Cache cache, Predicate<Location> filter) {
		this.filter = filter;
		
		for (int i = 0; i < MAX_REGION; i++) {
			try {
				CollisionRegion region = new CollisionRegion(cache, i);
				if (region.isValid()) {
					regions.put(region.getRegionID(), region);
					loaded++;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for (CollisionRegion region : regions.values()) {
			for (Location loc : region.getLocations()) {
				markLocation(loc);
			}
		}
	}
	
	private void markLocation(Location loc) {
		if (!filter.test(loc)) {
			markSolidOccupant(loc, TypeListManager.lookupObjectTypes(loc.getId()).get(0), RegionFlag.CLEAR);
			return;
		}
		
		for (ObjectType def : TypeListManager.lookupObjectTypes(loc.getId())) {
			markLocation(loc, def);
		}
	}
	
	private void markLocation(Location loc, ObjectType def) {
		Position pos = loc.getPosition();
		CollisionRegion region = getRegion(loc);

		final int renderRule = region.getRenderRule(pos.getHeight(), pos.getXInRegion(), pos.getYInRegion());
		final int baseRenderRule;
		
		if (pos.getHeight() == 0) {
			baseRenderRule = renderRule;
		} else {
			baseRenderRule = region.getRenderRule(0, pos.getXInRegion(), pos.getYInRegion());
		}

		if (def.getClipType() == 0 || def.isSolid()) {
			return;
		}
		
		if ((baseRenderRule & 0x10) == 0 || (renderRule & 2) == 2) {
			final int bridgeRenderRule;
			if (pos.getHeight() == 1) {
				bridgeRenderRule = renderRule;
			} else {
				bridgeRenderRule = region.getRenderRule(1, pos.getXInRegion(), pos.getYInRegion());
			}
			if ((bridgeRenderRule & 0x2) == 2) {
				if (pos.getHeight() == 0)
					return;

				pos = new Position(pos.getHeight() - 1, pos.getXInRegion(), pos.getYInRegion());
			}
			
			if (loc.getType() == 22) {
				if (def.getClipType() == 1 && (def.getAnInt2088() != 0 || def.isBlocksGround())) {
					addFlags(loc.getPosition(), RegionFlag.BLOCKED);
				}
			} else if (loc.getType() >= 9) {
				markSolidOccupant(loc, def, RegionFlag.OBJECT_TILE, RegionFlag.OBJECT_BLOCK);
			} else if (loc.getType() <= 3) {
				markWall(loc, def);
			} else {
				// Wall decoration
			}
		}
	}

	private void markSolidOccupant(Location loc, ObjectType def, RegionFlag... flags) {
		Position pos = loc.getPosition();
		
		final int width;
		final int height;
		
		if (loc.getOrientation() % 2 == 0) {
			width = def.getSizeX();
			height = def.getSizeY();
		} else {
			width = def.getSizeY();
			height = def.getSizeX();
		}

		for (int tX = 0; tX < width; tX++) {
			for (int tY = 0; tY < height; tY++) {
				Position p = new Position(pos.getX() + tX, pos.getY() + tY, pos.getHeight());
				if (flags[0] == RegionFlag.CLEAR) {
					setFlags(p, RegionFlag.CLEAR);
				} else {
					addFlags(p, flags);
				}
			}
		}
	}
	
	private void markWall(Location loc, ObjectType def) {
		RegionFlag[] flags = new RegionFlag[] {
			RegionFlag.WALL_WEST.turn(loc.getOrientation()),
			RegionFlag.WALL_BLOCK_WEST.turn(loc.getOrientation())
		};
		
		Position p = loc.getPosition();
		
		if (loc.getType() == 1 || loc.getType() == 3) {
			for (int a = 0; a < flags.length; a++) {
				flags[a] = flags[a].turn45(1);
			}
		}
		
		addFlags(p, flags);
			
		for (int a = 0; a < flags.length; a++) {
			flags[a] = flags[a].turn45(loc.getType() == 2 ? 3 : 4);
		}
		
		int[] offset = WALL_OFFSETS[loc.getType() % 2][loc.getOrientation()];
		addFlags(new Position(p.getX() + offset[0], p.getY() + offset[1], p.getHeight()), flags);
		if (loc.getType() == 2) {
			for (int a = 0; a < flags.length; a++) {
				flags[a] = flags[a].turn(1);
			}
			offset = WALL_OFFSETS[0][(loc.getOrientation() + 1) % 4];
			addFlags(new Position(p.getX() + offset[0], p.getY() + offset[1], p.getHeight()), flags);
		}
	}
	
	private void addFlags(Position p, RegionFlag... flags) {
		CollisionRegion region = regions.get(p.getRegionID());
		if (region != null) {
			region.addTileFlag(p, flags);
		}
	}
	
	private void setFlags(Position p, RegionFlag... flags) {
		CollisionRegion region = regions.get(p.getRegionID());
		if (region != null) {
			region.setTileFlag(p, flags);
		}
	}

	public CollisionRegion getRegion(Location loc) {
		return getRegion(loc.getPosition());
	}
	
	public CollisionRegion getRegion(Position pos) {
		return regions.get(pos.getRegionID());
	}

	public int getLoaded() {
		return loaded;
	}
	
	public Collection<CollisionRegion> getRegions() {
		return regions.values();
	}

	public int getTileFlag(Position c) {
		return getRegion(c).getTileFlags()[c.getHeight()][c.getXInRegion()][c.getYInRegion()];
	}
}
