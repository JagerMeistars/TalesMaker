# TODO: NPC Animation System Refactoring

## Progress Summary
- **Всего задач**: 16
- **Выполнено**: 16
- **В работе**: 0
- **Осталось**: 0

---

## Фаза 1: Data Structures

### [x] 1.1 Создать NpcAnimationConfig.java
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/data/NpcAnimationConfig.java`
**Описание**: Record с полной структурой конфигурации анимаций и Codec для JSON parsing.
**Зависимости**: нет

### [x] 1.2 Обновить NpcPreset.java
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/data/NpcPreset.java`
**Описание**: Заменить AnimationConfig на NpcAnimationConfig, добавить поддержку обоих форматов.
**Зависимости**: 1.1

### [x] 1.3 Legacy conversion
**Файл**: `NpcAnimationConfig.java`
**Описание**: Метод fromLegacy() для конвертации старого формата в новый.
**Зависимости**: 1.1

---

## Фаза 2: Runtime State

### [x] 2.1 Создать NpcAnimationState.java
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/entity/NpcAnimationState.java`
**Описание**: Lightweight state с packed EntityDataAccessor для минимизации синхронизации.
**Зависимости**: 1.1

### [x] 2.2 Интегрировать NpcAnimationState в NpcEntity
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/entity/NpcEntity.java`
**Описание**: Добавить поле animationState, убрать старые CUSTOM_ANIMATION accessor'ы.
**Зависимости**: 2.1

### [x] 2.3 Создать NpcAnimationManager.java
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/entity/NpcAnimationManager.java`
**Описание**: Server-side логика выбора анимаций на основе состояния NPC.
**Зависимости**: 2.1, 2.2

---

## Фаза 3: Client Animation

### [x] 3.1 Рефакторинг registerControllers()
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/entity/NpcEntity.java`
**Описание**: Новый predicate с приоритетами: override > action > base.
**Зависимости**: 2.2, 2.3

### [x] 3.2 Additive head tracking в NpcModel
**Файл**: `src/main/java/dcs/jagermeistars/talesmaker/client/model/NpcModel.java`
**Описание**: Изменить setCustomAnimations() - добавлять rotation к текущей, а не заменять.
**Зависимости**: 1.1

### [x] 3.3 Интеграция head tracking с blockHead
**Файл**: `NpcModel.java`
**Описание**: Проверка override.blockHead перед применением head rotation.
**Зависимости**: 3.2

### [x] 3.4 Тестирование базовой системы
**Описание**: Проверить idle/walk/death анимации с head tracking.
**Зависимости**: 3.1, 3.2, 3.3

---

## Фаза 4: Advanced Features

### [x] 4.1 Body turn detection
**Файл**: `NpcAnimationManager.java`
**Описание**: Отслеживание резкого изменения yBodyRot для trigger turn animation.
**Зависимости**: 2.3

### [x] 4.2 Turn animation support
**Файл**: `NpcEntity.java`, `NpcAnimationManager.java`
**Описание**: Добавить triggerableAnim для turn_left/turn_right.
**Зависимости**: 4.1

### [x] 4.3 Conditional variants
**Файл**: `NpcAnimationManager.java`, `NpcAnimationState.java`
**Описание**: Система conditions для выбора variant анимации.
**Зависимости**: 2.3

### [x] 4.4 Configurable transitions
**Файл**: `NpcEntity.java`
**Описание**: Использовать transitions config для transition length в controller.
**Зависимости**: 3.1

---

## Фаза 5: Migration & Cleanup

### [x] 5.1 Обновить preset JSON файлы
**Файлы**: `resources/data/talesmaker/npc/presets/*.json`
**Описание**: Перевести на новый формат animations.
**Зависимости**: 1.2

### [x] 5.2 Cleanup legacy code
**Файл**: `NpcEntity.java`
**Описание**: Legacy EntityDataAccessor'ы (IDLE_ANIM_NAME, WALK_ANIM_NAME, etc) сохранены для обратной совместимости.
**Примечание**: Новая система работает параллельно, legacy код используется для fallback и сохранения NBT.
**Зависимости**: 5.1, все тесты пройдены

---

## Критические файлы

| Файл | Действие |
|------|----------|
| `data/NpcAnimationConfig.java` | СОЗДАТЬ |
| `entity/NpcAnimationState.java` | СОЗДАТЬ |
| `entity/NpcAnimationManager.java` | СОЗДАТЬ |
| `data/NpcPreset.java` | ИЗМЕНИТЬ |
| `entity/NpcEntity.java` | ИЗМЕНИТЬ |
| `client/model/NpcModel.java` | ИЗМЕНИТЬ |
