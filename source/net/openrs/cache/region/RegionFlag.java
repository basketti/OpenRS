package net.openrs.cache.region;

public enum RegionFlag {
    WALL_NORTHWEST(0x1),
    WALL_NORTH(0x2),
    WALL_NORTHEAST(0x4),
    WALL_EAST(0x8),
    WALL_SOUTHEAST(0x10),
    WALL_SOUTH(0x20),
    WALL_SOUTHWEST(0x40),
    WALL_WEST(0x80),

    OBJECT_TILE(0x100),

    WALL_BLOCK_NORTHWEST(0x200),
    WALL_BLOCK_NORTH(0x400),
    WALL_BLOCK_NORTHEAST(0x800),
    WALL_BLOCK_EAST(0x1000),
    WALL_BLOCK_SOUTHEAST(0x2000),
    WALL_BLOCK_SOUTH(0x4000),
    WALL_BLOCK_SOUTHWEST(0x8000),
    WALL_BLOCK_WEST(0x10000),

    OBJECT_BLOCK(0x20000),
    DECORATION_BLOCK(0x40000),

    OBJECT_BLOCKED(0x200000),

    WALL_ALLOW_RANGE_NORTHWEST(0x400000),
    WALL_ALLOW_RANGE_NORTH(0x800000),
    WALL_ALLOW_RANGE_NORTHEAST(0x1000000),
    WALL_ALLOW_RANGE_EAST(0x2000000),
    WALL_ALLOW_RANGE_SOUTHEAST(0x4000000),
    WALL_ALLOW_RANGE_SOUTH(0x8000000),
    WALL_ALLOW_RANGE_SOUTHWEST(0x10000000),
    WALL_ALLOW_RANGE_WEST(0x20000000),

    OBJECT_ALLOW_RANGE(0x40000000),

    BLOCKED(0x1280100),
    
    CLEAR(0x0);
	
	public final int flag;

	private RegionFlag(int flag) {
		this.flag = flag;
	}
	
	public boolean test(int c) {
		return (this.flag & c) != 0;
	}
	
	public RegionFlag flip() {
		return turn45(4);
	}
	
	public RegionFlag turn(int turns) {
		return turn45(turns * 2);
	}
    
    public RegionFlag turn45(int turns) {
    	while (turns < 0)
    		turns += 8;
    	
    	RegionFlag done = this;
    	for (int i = 0; i < turns; i++)
	    	switch (done) {
	        case WALL_NORTHWEST: done = WALL_NORTH; break;
	        case WALL_NORTH: done = WALL_NORTHEAST; break;
	        case WALL_NORTHEAST: done = WALL_EAST; break;
	        case WALL_EAST: done = WALL_SOUTHEAST; break;
	        case WALL_SOUTHEAST: done = WALL_SOUTH; break;
	        case WALL_SOUTH: done = WALL_SOUTHWEST; break;
	        case WALL_SOUTHWEST: done = WALL_WEST; break;
	        case WALL_WEST: done = WALL_NORTHWEST; break;

	        case WALL_BLOCK_NORTHWEST: done = WALL_BLOCK_NORTH; break;
	        case WALL_BLOCK_NORTH: done = WALL_BLOCK_NORTHEAST; break;
	        case WALL_BLOCK_NORTHEAST: done = WALL_BLOCK_EAST; break;
	        case WALL_BLOCK_EAST: done = WALL_BLOCK_SOUTHEAST; break;
	        case WALL_BLOCK_SOUTHEAST: done = WALL_BLOCK_SOUTH; break;
	        case WALL_BLOCK_SOUTH: done = WALL_BLOCK_SOUTHWEST; break;
	        case WALL_BLOCK_SOUTHWEST: done = WALL_BLOCK_WEST; break;
	        case WALL_BLOCK_WEST: done = WALL_BLOCK_NORTHWEST; break;
	
	        case WALL_ALLOW_RANGE_NORTHWEST: done = WALL_ALLOW_RANGE_NORTH; break;
	        case WALL_ALLOW_RANGE_NORTH: done = WALL_ALLOW_RANGE_NORTHEAST; break;
	        case WALL_ALLOW_RANGE_NORTHEAST: done = WALL_ALLOW_RANGE_EAST; break;
	        case WALL_ALLOW_RANGE_EAST: done = WALL_ALLOW_RANGE_SOUTHEAST; break;
	        case WALL_ALLOW_RANGE_SOUTHEAST: done = WALL_ALLOW_RANGE_SOUTH; break;
	        case WALL_ALLOW_RANGE_SOUTH: done = WALL_ALLOW_RANGE_SOUTHWEST; break;
	        case WALL_ALLOW_RANGE_SOUTHWEST: done = WALL_ALLOW_RANGE_WEST; break;
	        case WALL_ALLOW_RANGE_WEST: done = WALL_ALLOW_RANGE_NORTHWEST; break;
	
			default: break;
	    	}
    	return done;
    }
    
}