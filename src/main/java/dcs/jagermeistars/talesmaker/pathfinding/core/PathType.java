package dcs.jagermeistars.talesmaker.pathfinding.core;

/**
 * Типы проходимости для pathfinding
 */
public enum PathType {
    WALKABLE(0.0f, true),      // Обычный проходимый блок
    OPEN(0.0f, true),          // Воздух, пустота
    WATER(8.0f, true),         // Вода - проходима с штрафом
    LAVA(-1.0f, false),        // Лава - непроходима
    BLOCKED(-1.0f, false),     // Твёрдый блок - непроходим
    DANGER(16.0f, true),       // Опасный блок (кактус, магма)
    FENCE(-1.0f, false),       // Забор - непроходим
    DOOR(1.0f, true),          // Дверь - проходима
    TRAPDOOR(1.0f, true),      // Люк - проходим
    STICKY(4.0f, true);        // Замедляющий (паутина, мёд)

    private final float malus;     // Штраф стоимости
    private final boolean passable; // Можно ли пройти

    PathType(float malus, boolean passable) {
        this.malus = malus;
        this.passable = passable;
    }

    public float getMalus() { return malus; }
    public boolean isPassable() { return passable; }
}
