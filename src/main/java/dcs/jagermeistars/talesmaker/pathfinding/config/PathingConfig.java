package dcs.jagermeistars.talesmaker.pathfinding.config;

/**
 * Configuration for pathfinding behavior.
 * Uses builder pattern for flexible configuration.
 */
public class PathingConfig {
    // Pathfinding limits
    private final int maxIterations;
    private final long maxTimeoutMs;
    private final double maxRange;
    private final int maxPathLength;

    // Movement capabilities
    private final int maxFallDistance;
    private final int maxJumpHeight;
    private final boolean canSwim;
    private final boolean canClimb;
    private final boolean canOpenDoors;
    private final boolean canParkour;

    // Entity dimensions
    private final float entityWidth;
    private final float entityHeight;
    private final float stepHeight;

    // Movement costs
    private final double walkCost;
    private final double diagonalCost;
    private final double jumpCost;
    private final double fallCost;
    private final double swimCost;
    private final double doorCost;
    private final double parkourCost;

    private PathingConfig(Builder builder) {
        this.maxIterations = builder.maxIterations;
        this.maxTimeoutMs = builder.maxTimeoutMs;
        this.maxRange = builder.maxRange;
        this.maxPathLength = builder.maxPathLength;
        this.maxFallDistance = builder.maxFallDistance;
        this.maxJumpHeight = builder.maxJumpHeight;
        this.canSwim = builder.canSwim;
        this.canClimb = builder.canClimb;
        this.canOpenDoors = builder.canOpenDoors;
        this.canParkour = builder.canParkour;
        this.entityWidth = builder.entityWidth;
        this.entityHeight = builder.entityHeight;
        this.stepHeight = builder.stepHeight;
        this.walkCost = builder.walkCost;
        this.diagonalCost = builder.diagonalCost;
        this.jumpCost = builder.jumpCost;
        this.fallCost = builder.fallCost;
        this.swimCost = builder.swimCost;
        this.doorCost = builder.doorCost;
        this.parkourCost = builder.parkourCost;
    }

    // Getters
    public int getMaxIterations() { return maxIterations; }
    public long getMaxTimeoutMs() { return maxTimeoutMs; }
    public double getMaxRange() { return maxRange; }
    public int getMaxPathLength() { return maxPathLength; }
    public int getMaxFallDistance() { return maxFallDistance; }
    public int getMaxJumpHeight() { return maxJumpHeight; }
    public boolean canSwim() { return canSwim; }
    public boolean canClimb() { return canClimb; }
    public boolean canOpenDoors() { return canOpenDoors; }
    public boolean canParkour() { return canParkour; }
    public float getEntityWidth() { return entityWidth; }
    public float getEntityHeight() { return entityHeight; }
    public float getStepHeight() { return stepHeight; }
    public double getWalkCost() { return walkCost; }
    public double getDiagonalCost() { return diagonalCost; }
    public double getJumpCost() { return jumpCost; }
    public double getFallCost() { return fallCost; }
    public double getSwimCost() { return swimCost; }
    public double getDoorCost() { return doorCost; }
    public double getParkourCost() { return parkourCost; }

    /**
     * Create default NPC configuration.
     */
    public static PathingConfig defaultNpc() {
        return new Builder().build();
    }

    /**
     * Create configuration for swimming entity.
     */
    public static PathingConfig swimming() {
        return new Builder()
                .canSwim(true)
                .swimCost(1.0)
                .build();
    }

    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for PathingConfig.
     */
    public static class Builder {
        private int maxIterations = 1000;
        private long maxTimeoutMs = 100;
        private double maxRange = 64.0;
        private int maxPathLength = 256;
        private int maxFallDistance = 3;
        private int maxJumpHeight = 1;
        private boolean canSwim = true;
        private boolean canClimb = true;
        private boolean canOpenDoors = true;
        private boolean canParkour = true;
        private float entityWidth = 0.6f;
        private float entityHeight = 1.8f;
        private float stepHeight = 0.6f;
        private double walkCost = 1.0;
        private double diagonalCost = 1.414;
        private double jumpCost = 2.0;
        private double fallCost = 1.5;
        private double swimCost = 2.0;
        private double doorCost = 1.5;
        private double parkourCost = 3.0;

        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public Builder maxTimeoutMs(long maxTimeoutMs) {
            this.maxTimeoutMs = maxTimeoutMs;
            return this;
        }

        public Builder maxRange(double maxRange) {
            this.maxRange = maxRange;
            return this;
        }

        public Builder maxPathLength(int maxPathLength) {
            this.maxPathLength = maxPathLength;
            return this;
        }

        public Builder maxFallDistance(int maxFallDistance) {
            this.maxFallDistance = maxFallDistance;
            return this;
        }

        public Builder maxJumpHeight(int maxJumpHeight) {
            this.maxJumpHeight = maxJumpHeight;
            return this;
        }

        public Builder canSwim(boolean canSwim) {
            this.canSwim = canSwim;
            return this;
        }

        public Builder canClimb(boolean canClimb) {
            this.canClimb = canClimb;
            return this;
        }

        public Builder canOpenDoors(boolean canOpenDoors) {
            this.canOpenDoors = canOpenDoors;
            return this;
        }

        public Builder canParkour(boolean canParkour) {
            this.canParkour = canParkour;
            return this;
        }

        public Builder entityWidth(float entityWidth) {
            this.entityWidth = entityWidth;
            return this;
        }

        public Builder entityHeight(float entityHeight) {
            this.entityHeight = entityHeight;
            return this;
        }

        public Builder stepHeight(float stepHeight) {
            this.stepHeight = stepHeight;
            return this;
        }

        public Builder walkCost(double walkCost) {
            this.walkCost = walkCost;
            return this;
        }

        public Builder diagonalCost(double diagonalCost) {
            this.diagonalCost = diagonalCost;
            return this;
        }

        public Builder jumpCost(double jumpCost) {
            this.jumpCost = jumpCost;
            return this;
        }

        public Builder fallCost(double fallCost) {
            this.fallCost = fallCost;
            return this;
        }

        public Builder swimCost(double swimCost) {
            this.swimCost = swimCost;
            return this;
        }

        public Builder doorCost(double doorCost) {
            this.doorCost = doorCost;
            return this;
        }

        public Builder parkourCost(double parkourCost) {
            this.parkourCost = parkourCost;
            return this;
        }

        public PathingConfig build() {
            return new PathingConfig(this);
        }
    }
}