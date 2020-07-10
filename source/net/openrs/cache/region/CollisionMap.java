package net.openrs.cache.region;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.openrs.cache.Cache;
import net.openrs.cache.type.TypeListManager;
import net.openrs.cache.type.objects.ObjectType;

/**
 * Loads all the regions and compiles the collision maps.
 * 
 * @author person
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
		if (!filter.test(loc) && (loc.getType() > 7 || !isWallBlocked(loc))) {
			System.out.println("Clearing "+TypeListManager.lookupObjectTypes(loc.getId()).get(0).getName()
					+" at "+loc.getPosition()+" type "+loc.getType());
			markSolidOccupant(loc, TypeListManager.lookupObjectTypes(loc.getId()).get(0), RegionFlag.CLEAR);
			return;
		}
		
		for (ObjectType def : TypeListManager.lookupObjectTypes(loc.getId())) {
			markLocation(loc, def);
			break;
		}
	}
	
	private boolean isWallBlocked(Location loc) {
		RegionFlag flag = RegionFlag.WALL_WEST;
		for (int i = 0; i < loc.getOrientation(); i++) {
			flag = flag.turn(1);
		}
		Position pair = flag.getMirrorPosition(loc.getPosition());
		CollisionRegion region = getRegion(pair);
		return region == null || flag.flip().test(region.getTileFlag(pair));
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
		if (loc.getType() == 2) {
			for (int a = 0; a < flags.length; a++) {
				flags[a] = flags[a].turn(1);
			}
			addFlags(p, flags);
		}
	}
	
	private void addFlags(Position p, RegionFlag... flags) {
		addPositionFlags(p, flags);
		for (RegionFlag flag : flags) {
			addPositionFlags(flag.getMirrorPosition(p), flag.flip());
		}
	}

	private void addPositionFlags(Position p, RegionFlag... flags) {
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
