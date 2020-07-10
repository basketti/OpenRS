package net.openrs.cache.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.openrs.cache.type.TypeListManager;
import net.openrs.cache.type.objects.ObjectType;

public class ZShiftDetector {

	private static final Position[] EMPTY_LINK = new Position[0];
	
	private static final String[] STAIR_NAMES = {
			"Stairs",
			"Staircase",
			"Ladder"
	};
	
	private List<Location> regionStairs = new ArrayList<>();
	private Set<Location> closedStairs = new HashSet<>();
	private Map<CollisionRegion, List<Position[]>> linkMap = new HashMap<>();
	private CollisionMap map;

	public ZShiftDetector(CollisionMap map) {
		this.map = map;
		
		int loaded = map.getLoaded();
		
		for (CollisionRegion region : map.getRegions()) {
			if (region.isEmpty()) {
				loaded--;
				continue;
			}
			
			regionStairs.clear();
			for (Location l : region.getLocations()) {
				if (isStairs(l)) {
					regionStairs.add(l);
				}
			}
			
			closedStairs.clear();
			
			for (Location l : regionStairs) {
				if (closedStairs.contains(l))
					continue;
				
				detectZLevel(l);
			}
			//regionStairs.put(region, stairs);
		}
	}

	private void detectZLevel(Location location) {
		ObjectType objType = TypeListManager.lookupObject(location.getId());
		
		if (objType.getSizeX() == 2 && objType.getSizeY() == 2) {
			//System.out.println("SpiralStairs @ " + location.getPosition());
			solveSpiralStaircase(location, objType);
		} else if ((objType.getSizeX() == 2 && objType.getSizeY() == 3)
				|| (objType.getSizeX() == 2 && objType.getSizeY() == 4)
				|| (objType.getSizeX() == 3 && objType.getSizeY() == 2)
				|| (objType.getSizeX() == 4 && objType.getSizeY() == 2)
				|| (objType.getSizeX() == 3 && objType.getSizeY() == 1)
				|| (objType.getSizeX() == 1 && objType.getSizeY() == 3)) {
			//System.out.println("ShiftStairs @ " + location.getPosition());
			solveShiftStaircase(location, objType);
		} else if (objType.getSizeX() == 1 && objType.getSizeY() == 1) {
			//System.out.println("Ladder @ " + location.getPosition());
			solveLadder(location, objType);
		} else {
			System.out.println("Unknown object type " + objType.getName()
					+ " @ " + location.getPosition() + " Size: "
					+ objType.getSizeX() + "x" + objType.getSizeY());
		}
	}

	private Set<Location> getSimilarLocations(Location location, ObjectType def) {
		Set<Location> set = new HashSet<>();
	
		for (Location l : regionStairs) {
			if (isStairs(location)
					&& location.getPosition().getX() <= l.getPosition().getX()
					&& location.getPosition().getX() + location.getWidth() >= l.getPosition().getX()
					&& location.getPosition().getY() <= l.getPosition().getY()
					&& location.getPosition().getY() + location.getLength() >= l.getPosition().getY()) {
				for (ObjectType ldef : TypeListManager.lookupObjectTypes(l.getId())) {
					if (ldef.getName().equals(def.getName())) {
						set.add(l);
						break;
					}
				}
			}
		}
		
		return set;
	}

	private void solveLadder(Location location, ObjectType def) {
		Set<Location> locations = getSimilarLocations(location, def);
		
		// Add some links, as long as the X-Y exists
		for (Location l : locations) {
			for (Location o : locations) {
				if (l == o)
					continue;
				Position a = l.getPosition();
				Position b = o.getPosition();
				if (a.getHeight() == b.getHeight() + 1) {
					// Link goes down
					RegionFlag flag = RegionFlag.WALL_EAST;
					Position[] link;
					for (int i = 0; i < 4; i++) {
						link = getLadderFaceLinks(a, b, flag);
						if (link.length > 0)
							addLink(link);
						flag = flag.turn(1);
					}
				}
			}
		}

		// Add locations to stairs
		closedStairs.addAll(locations);
	}

	private Position[] getLadderFaceLinks(Position a, Position b, RegionFlag flag) {
		int i, j;
		
		switch (flag) {
		case WALL_EAST:
			i = -1;
			j = 0;
			break;
		case WALL_WEST:
			i = 1;
			j = 0;
			break;
		case WALL_NORTH:
			i = 0;
			j = 1;
			break;
		case WALL_SOUTH:
			i = 0;
			j = -1;
			break;
		default:
			return EMPTY_LINK;
		}
		
		Position c = new Position(a.getX() + i, a.getY() + j, a.getHeight());
		Position d = new Position(b.getX() + i, b.getY() + j, b.getHeight());
		
		int e = map.getTileFlag(c);
		int f = map.getTileFlag(d);
		
		if (!RegionFlag.BLOCKED.test(e) && !RegionFlag.BLOCKED.test(f)
				&& !flag.test(e) && !flag.test(f)) {
			return new Position[] { c, d };
		}
		return EMPTY_LINK;
	}

	private void solveShiftStaircase(Location location, ObjectType def) {
		Position p = location.getPosition();
		
		int len = location.getWidth() == 2 ? location.getLength() : location.getWidth();
		Set<Location> locations = getSimilarLocations(location, def);
		
		if (locations.isEmpty())
			return;
		
		Location top = null;
		for (Location l : locations) {
			if (top == null || l.getPosition().getHeight() > top.getPosition().getHeight())
				top = l;
		}

		// Now we got the objects find corresponding tiles
		Position topPos = top.getPosition();
		Position[] set = new Position[2];
		if (topPos.getX() == p.getX() && topPos.getY() == p.getY()) {
			if (location.getWidth() == 2) {
				set[0] = new Position(p.getX(), p.getY() + len, p.getHeight());
				set[1] = new Position(p.getX(), p.getY() - 1, topPos.getHeight());
			} else {
				set[0] = new Position(p.getX() + len, p.getY(), p.getHeight());
				set[1] = new Position(p.getX() - 1, p.getY(), topPos.getHeight());
			}
		} else if (topPos.getX() == p.getX() && topPos.getY() - (len - 2) == p.getY()) {
			set[0] = new Position(p.getX(), p.getY() - 1, p.getHeight());
			set[1] = new Position(p.getX(), p.getY() + len, topPos.getHeight());
		} else if (topPos.getX() - (len - 2) == p.getX() && topPos.getY() == p.getY()) {
			set[0] = new Position(p.getX() - 1, p.getY(), p.getHeight());
			set[1] = new Position(p.getX() + len, p.getY(), topPos.getHeight());
		} else {
			return;
		}

		addLink(set);

		closedStairs.addAll(locations);
	}

	private void solveSpiralStaircase(Location location, ObjectType def) {
		Set<Location> stackedLocations = getSimilarLocations(location, def);

		Location top = null;
		Location bottom = null;
		
		for (Location l : regionStairs) {
			ObjectType ldef = TypeListManager.lookupObject(l.getId());
			// We want the top to be the lowest 1x1 stair available, if these seem backwards
			if (ldef.getSizeX() == 1) {
				if (top == null || l.getPosition().getHeight() < top.getPosition().getHeight()) {
					top = l;
				}
			} else if (bottom == null || l.getPosition().getHeight() < bottom.getPosition().getHeight()) {
				bottom = l;
			}
		}
		
		if (top == null) {
			System.out.println("   Unable to find top for "+location.getPosition());
			for (Location l : stackedLocations) {
				System.out.println("      "+location.getPosition());
			}
			closedStairs.addAll(stackedLocations);
			return;
		}
		
		Set<Location> locations = new HashSet<>();
		
		for (Location l : stackedLocations) {
			if (l.getPosition().getHeight() <= top.getPosition().getHeight()) {
				locations.add(l);
			}
		}
		
		closedStairs.addAll(locations);

		// Now we got the objects find corresponding tiles
		int[] translation;
		int[] linkTranslation;
		if (top.getPosition().getX() == bottom.getPosition().getX()) {
			if (top.getPosition().getY() == bottom.getPosition().getY()) {
				translation = new int[] {0, -1};
				linkTranslation = new int[] {-1, 1};
			} else {
				translation = new int[] {-1, 1};
				linkTranslation = new int[] {1, 1};
			}
		} else {
			if (top.getPosition().getY() == bottom.getPosition().getY()) {
				translation = new int[] {2, 0};
				linkTranslation = new int[] {-1, 0};
			} else {
				translation = new int[] {1, 2};
				linkTranslation = new int[] {1, -1};
			}
		}
		
		Position linkPos = new Position(bottom.getPosition().getX() + translation[0],
				bottom.getPosition().getY() + translation[1],
				bottom.getPosition().getHeight());

		// Add some links, as long as the X-Y exists
		for (Location l : locations) {
			for (Location o : locations) {
				if (l == o)
					continue;
				
				if (l.getPosition().getHeight() == o.getPosition().getHeight() - 1) {
					Position[] set = new Position[2];
					int x = linkPos.getX() + linkTranslation[0];
					int y = linkPos.getY() + linkTranslation[1];
					set[0] = new Position(x, y, l.getPosition().getHeight());
					set[1] = new Position(x, y, o.getPosition().getHeight());
					addLink(set);
				}
			}
		}
	}
	
	private void addLink(Position[] link) {
		CollisionRegion region = map.getRegion(link[0]);
		List<Position[]> list = linkMap.get(region);
		if (list == null) {
			list = new ArrayList<>();
			linkMap.put(region, list);
		}
		list.add(link);
	}
	
	private boolean isStairs(Location l) {
		for (ObjectType def : TypeListManager.lookupObjectTypes(l.getId())) {
			for (String name : STAIR_NAMES) {
				if (name.equals(def.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public List<Position[]> getLinks(CollisionRegion region) {
		List<Position[]> links = linkMap.get(region);
		if (links == null) {
			links = Collections.EMPTY_LIST;
		}
		return links;
	}
	
}
