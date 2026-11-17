# Лабораторна робота №2

**Дисципліна:** Технології розробки на платформі Java (2026)

**Тема:** Збірка проєкту за допомогою Gradle та робота з Java Collections Framework

**Мета:** Навчитися конфігурувати систему збірки Gradle (із використанням Version Catalogs), застосовувати стандартні колекції Java та обробляти граничні випадки.

**Виконав:** Романенко Іван Євгенович

**Група:** ІО-45

## Визначення варіанта

Варіант визначається остачею від ділення номера залікової книжки на 3.

- Номер залікової книжки: **4509**.
- Формула: `V = N mod 3`.
- Обчислення: `4509 mod 3 = 0` (4509 = 3 · 1503).

**Варіант 0 — аналізатор логів серверів (`LogAnalyzer`).**

## Опис реалізації

Клас `ua.kpi.comsys.collections.LogAnalyzer` обробляє рядки журналу у форматі **Common Log Format**:

```text
127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] "GET /index.html HTTP/1.1" 200 9481
```

| Метод | Поведінка |
|---|---|
| `Set<String> getUniqueIps(List<String> logLines)` | Повертає множину всіх унікальних IP-адрес (`LinkedHashSet`, у порядку першої появи). |
| `List<String> getTopIps(List<String> logLines, int topN)` | Повертає `topN` найчастіших IP за спаданням частоти. При однаковій частоті IP впорядковуються лексикографічно за зростанням. Якщо унікальних IP менше за `topN`, повертаються всі. |

**Розбір рядка.** Кожен рядок перевіряється регулярним виразом, що описує всі поля CLF: IPv4-адресу, `ident`, `authuser`, дату у квадратних дужках (`dd/MMM/yyyy:HH:mm:ss ±zzzz`), запит у лапках, тризначний статус і розмір відповіді (число або `-`). Додатково перевіряється, що кожен октет IP не перевищує 255. Рядки, що не відповідають формату, а також `null`-рядки ігноруються.

Згідно з уточненням викладача, IP-адреса в рядку логу є завжди, а підтримки IPv4 достатньо.

**Підрахунок частот і сортування.** Частоти накопичуються в `HashMap<String, Integer>` за допомогою `Map.merge`, після чого записи сортуються компаратором:

```java
Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
        .thenComparing(Map.Entry.comparingByKey())
```

Для tie-breaking використовується саме рядкове (лексикографічне) порівняння, як вимагає завдання, тому, наприклад, `"10.0.0.1"` йде перед `"9.0.0.1"`.

**Граничні випадки.**

- `logLines == null` → `IllegalArgumentException("logLines cannot be null")` (в обох методах);
- `topN <= 0` → `IllegalArgumentException("topN must be greater than 0")`;
- порожній список → порожній результат.

Використано лише стандартні колекції `java.util.*`.

## Структура проєкту

```text
Romanenko_Rozrobka_PZ_java_lab2/
├── gradle/
│   ├── libs.versions.toml          # Version Catalog
│   └── wrapper/
├── src/
│   ├── main/java/ua/kpi/comsys/collections/
│   │   └── LogAnalyzer.java
│   └── test/java/ua/kpi/comsys/collections/
│       └── LogAnalyzerTest.java
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── README.md
```

## Середовище та технології

- **Java:** JDK 25 (Gradle Toolchain, версія задається в `libs.versions.toml`).
- **Система збирання:** Gradle 9.1 (Gradle Wrapper), Kotlin DSL.
- **Керування залежностями:** Version Catalog `gradle/libs.versions.toml`.
- **Тестування:** JUnit Jupiter 5.11.4 (через JUnit BOM).
- **Покриття:** JaCoCo 0.8.15.

## Запуск збірки та тестів

Потрібен встановлений JDK 25. Gradle окремо встановлювати не потрібно, бо використовується Wrapper.

```bash
# Linux / macOS
./gradlew build
./gradlew test

# Windows
gradlew.bat build
gradlew.bat test
```

Після `test` автоматично формується звіт JaCoCo: `build/reports/jacoco/test/html/index.html`.
Звіт JUnit: `build/reports/tests/test/index.html`.

В **IntelliJ IDEA**: *File → Open* → вибрати папку проєкту (Gradle підтягнеться автоматично), потім панель **Gradle → Tasks → verification → test**.

## Тестування

Тести (`LogAnalyzerTest`) згруповано через `@Nested` за методами та перевіряють:

- винятки й точні тексти повідомлень для `null` та `topN <= 0`;
- пріоритет перевірки `null` над перевіркою `topN`;
- унікальність IP, приклад із завдання, прочерки (`-`) у полях CLF, пробіли навколо рядка;
- ігнорування `null`-рядків і 14 видів некоректних рядків (`@ParameterizedTest`), зокрема IP з октетом > 255;
- сортування за частотою, обрізання до `topN`, tie-breaking за алфавітом (у тому числі рядкове порівняння `10.x` < `9.x`);
- випадок, коли унікальних IP менше, ніж `topN`, або рівно `topN`.

## Висновок

Під час виконання лабораторної роботи налаштовано Gradle-проєкт із Version Catalog та Gradle Wrapper і реалізовано аналізатор логів на стандартних колекціях Java (`Set`, `Map`, `List`) з компаратором, що враховує правило розриву нічиєї. Коректність роботи та обробку граничних випадків перевірено модульними тестами JUnit 5.
