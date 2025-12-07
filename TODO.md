# TODO: Центрирование широких NPC и обработка дверей

## Progress Summary
- **Всего задач**: 10
- **Выполнено**: 9
- **В работе**: 0
- **Осталось**: 1

---

## Фаза 1: PassageAnalyzer (анализ проходов)

### [x] 1.1 Создать базовый класс PassageAnalyzer
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/PassageAnalyzer.java`
**Описание**: Создать утилитный класс с заглушками методов для анализа проходов.
**Зависимости**: нет

### [x] 1.2 Реализовать measurePassageWidth
**Файл**: `PassageAnalyzer.java`
**Описание**: Метод измерения ширины прохода в указанном направлении.
**Зависимости**: 1.1

### [x] 1.3 Реализовать calculateOptimalX/Z
**Файл**: `PassageAnalyzer.java`
**Описание**: Методы вычисления оптимальных координат для центрирования широких NPC.
**Зависимости**: 1.2

### [x] 1.4 Реализовать getDoorOffset
**Файл**: `PassageAnalyzer.java`
**Описание**: Метод вычисления смещения центра прохода из-за хитбокса двери.
**Зависимости**: 1.1

---

## Фаза 2: Интеграция в MovementHelper

### [x] 2.1 Добавить calculateTargetPosition в MovementHelper
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/MovementHelper.java`
**Описание**: Унифицированный метод для вычисления целевой позиции с учетом ширины NPC.
**Зависимости**: 1.3, 1.4

---

## Фаза 3: Обновление Movement классов

### [x] 3.1 Обновить MovementTraverse
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/movements/MovementTraverse.java`
**Описание**: Заменить `Vec3.atBottomCenterOf(dest)` на `MovementHelper.calculateTargetPosition()`.
**Зависимости**: 2.1

### [x] 3.2 Обновить MovementDiagonal
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/movements/MovementDiagonal.java`
**Описание**: Аналогичная замена для диагонального движения.
**Зависимости**: 2.1

### [x] 3.3 Обновить MovementDoor
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/pathfinding/movement/movements/MovementDoor.java`
**Описание**: Обновить с учетом смещения хитбокса двери.
**Зависимости**: 2.1

### [x] 3.4 Обновить MovementAscend и MovementDescend
**Файлы**: `MovementAscend.java`, `MovementDescend.java`
**Описание**: Обновить вертикальные движения.
**Зависимости**: 2.1

### [x] 3.5 Обновить остальные Movement классы
**Файлы**: `MovementParkour.java`, `MovementFall.java`, `MovementSwim.java`
**Описание**: Обновить оставшиеся классы движений.
**Зависимости**: 2.1

---

## Фаза 4: Тестирование

### [ ] 4.1 Тестирование с разными ширинами NPC
**Описание**: Протестировать NPC с width=0.6, 1.5, 2.0, 2.5 в проходах 1-3 блока и через двери.
**Зависимости**: 3.1-3.5

---

## Критические файлы

| Файл | Действие |
|------|----------|
| `pathfinding/movement/PassageAnalyzer.java` | СОЗДАН |
| `pathfinding/movement/MovementHelper.java` | ИЗМЕНЕН |
| `pathfinding/movement/movements/MovementTraverse.java` | ИЗМЕНЕН |
| `pathfinding/movement/movements/MovementDoor.java` | ИЗМЕНЕН |
| `pathfinding/movement/movements/MovementDiagonal.java` | ИЗМЕНЕН |
| `pathfinding/movement/movements/MovementAscend.java` | ИЗМЕНЕН |
| `pathfinding/movement/movements/MovementDescend.java` | ИЗМЕНЕН |
| `pathfinding/movement/movements/MovementParkour.java` | ИЗМЕНЕН |
| `pathfinding/movement/movements/MovementFall.java` | ИЗМЕНЕН |
