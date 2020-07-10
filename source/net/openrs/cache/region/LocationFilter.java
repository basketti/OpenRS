package net.openrs.cache.region;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.openrs.cache.type.TypeListManager;
import net.openrs.cache.type.objects.ObjectType;

/**
 * Uses a file with Object names and Positions to modify collision maps as needed.
 * 
 * Reads in the `/repository/collision-maps/filter.txt` file normally.
 * 
 * <pre>
 * # Example file
 * Door # Will filter out all objects named "Door", clearing the block/wall flags it has.
 * 3333,3333,0 # This clears a tile's flags
 * !3333,3333,0 # This adds a BLOCKED flag to this space for blocking named objects that are false positives
 * 
 * @author person
 */
public class LocationFilter implements Predicate<Location> {
	
	private final Set<Position> positionWhitelist = new HashSet<>();
	
	private final Set<Position> positionBlacklist = new HashSet<>();
	
	private final Set<String> names = new HashSet<>();

	public LocationFilter(Path path) {
		try {
			if (!Files.exists(path)) {
				System.out.println("Create "+path.toString()+" to filter objects/positions.");
				return;
			}
			List<String> strs = Files.readAllLines(path);
			for (String raw : strs) {
				String base = raw.split("#")[0].trim();
				if (base.isEmpty()) {
					continue;
				}
				
				String[] bases = base.split(",");
				if (bases.length == 3) {
					boolean blacklist = bases[0].startsWith("!");
					if (blacklist)
						bases[0] = bases[0].substring(1);
					Position pos = new Position(
							Integer.parseInt(bases[0].trim()),
							Integer.parseInt(bases[1].trim()),
							Integer.parseInt(bases[2].trim()));
					if (blacklist) {
						System.out.println("Blocking position "+pos);
						positionBlacklist.add(pos);
					} else {
						System.out.println("Clearing position "+pos);
						positionWhitelist.add(pos);
					}
				} else {
					System.out.println("Filtering name "+base.trim());
					names.add(base.trim());
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public boolean test(Location t) {
		if (positionWhitelist.contains(t.getPosition())) {
			return true;
		}
		if (positionBlacklist.contains(t.getPosition())) {
			return false;
		}
		for (ObjectType def : TypeListManager.lookupObjectTypes(t.getId())) {
			if (names.contains(def.getName())) {
				return false;
			}
		}
		return true;
	}
	
}
