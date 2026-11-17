package ua.kpi.comsys.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LogAnalyzerTest {

    private LogAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new LogAnalyzer();
    }

    /** Формує коректний рядок у Common Log Format для заданої IP-адреси. */
    private static String line(String ip) {
        return ip + " ident alice [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481";
    }

    /** Повертає список, що містить рядок для IP рівно {@code count} разів. */
    private static List<String> repeat(String ip, int count) {
        return Collections.nCopies(count, line(ip));
    }

    @SafeVarargs
    private static List<String> concat(List<String>... parts) {
        List<String> result = new ArrayList<>();
        for (List<String> part : parts) {
            result.addAll(part);
        }
        return result;
    }

    @Nested
    @DisplayName("getUniqueIps")
    class GetUniqueIps {

        @Test
        @DisplayName("кидає IllegalArgumentException, якщо logLines == null")
        void throwsOnNullList() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> analyzer.getUniqueIps(null));
            assertEquals("logLines cannot be null", ex.getMessage());
        }

        @Test
        @DisplayName("повертає порожню множину для порожнього списку")
        void emptyListGivesEmptySet() {
            assertTrue(analyzer.getUniqueIps(List.of()).isEmpty());
        }

        @Test
        @DisplayName("повертає кожну IP-адресу лише один раз")
        void returnsUniqueIps() {
            List<String> lines = List.of(
                    line("127.0.0.1"),
                    line("10.0.0.5"),
                    line("127.0.0.1"),
                    line("192.168.1.1"),
                    line("10.0.0.5"));

            Set<String> result = analyzer.getUniqueIps(lines);

            assertEquals(Set.of("127.0.0.1", "10.0.0.5", "192.168.1.1"), result);
        }

        @Test
        @DisplayName("приймає рядок із прикладу в завданні")
        void acceptsExampleFromTask() {
            String example = "127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] "
                    + "\"GET /index.html HTTP/1.1\" 200 9481";
            assertEquals(Set.of("127.0.0.1"), analyzer.getUniqueIps(List.of(example)));
        }

        @Test
        @DisplayName("приймає прочерки в полях ident, authuser і розмірі відповіді")
        void acceptsDashFieldsAllowedByClf() {
            String withDashes = "8.8.8.8 - - [10/Oct/2000:13:55:36 -0700] \"GET / HTTP/1.0\" 404 -";
            assertEquals(Set.of("8.8.8.8"), analyzer.getUniqueIps(List.of(withDashes)));
        }

        @Test
        @DisplayName("ігнорує пробіли на початку та в кінці рядка")
        void toleratesSurroundingWhitespace() {
            assertEquals(Set.of("1.2.3.4"), analyzer.getUniqueIps(List.of("  " + line("1.2.3.4") + "  ")));
        }

        @Test
        @DisplayName("ігнорує null-рядки всередині списку")
        void ignoresNullLines() {
            List<String> lines = Arrays.asList(line("1.1.1.1"), null, line("2.2.2.2"));
            assertEquals(Set.of("1.1.1.1", "2.2.2.2"), analyzer.getUniqueIps(lines));
        }

        @ParameterizedTest(name = "ігнорує: {0}")
        @ValueSource(strings = {
            "",
            "   ",
            "not a log line at all",
            "127.0.0.1",
            "abc.def.ghi.jkl ident alice [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200 1",
            "256.1.1.1 ident alice [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200 1",
            "1.1.1 ident alice [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200 1",
            "1.1.1.1 ident [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200 1",
            "1.1.1.1 ident alice 01/May/2025:07:20:10 +0000 \"GET / HTTP/1.1\" 200 1",
            "1.1.1.1 ident alice [2025-05-01 07:20:10] \"GET / HTTP/1.1\" 200 1",
            "1.1.1.1 ident alice [01/May/2025:07:20:10 +0000] GET / HTTP/1.1 200 1",
            "1.1.1.1 ident alice [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" OK 1",
            "1.1.1.1 ident alice [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200",
            "1.1.1.1 ident alice [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200 12kb"
        })
        void ignoresMalformedLines(String malformed) {
            assertTrue(analyzer.getUniqueIps(List.of(malformed)).isEmpty());
        }

        @Test
        @DisplayName("враховує лише коректні рядки зі змішаного списку")
        void mixedValidAndInvalidLines() {
            List<String> lines = List.of(
                    line("10.10.10.10"),
                    "garbage",
                    line("300.1.1.1"),
                    line("20.20.20.20"));
            assertEquals(Set.of("10.10.10.10", "20.20.20.20"), analyzer.getUniqueIps(lines));
        }

        @Test
        @DisplayName("приймає граничні значення октетів 0 і 255")
        void acceptsBoundaryOctets() {
            List<String> lines = List.of(line("0.0.0.0"), line("255.255.255.255"));
            assertEquals(Set.of("0.0.0.0", "255.255.255.255"), analyzer.getUniqueIps(lines));
        }
    }

    @Nested
    @DisplayName("getTopIps")
    class GetTopIps {

        @Test
        @DisplayName("кидає IllegalArgumentException, якщо logLines == null")
        void throwsOnNullList() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> analyzer.getTopIps(null, 3));
            assertEquals("logLines cannot be null", ex.getMessage());
        }

        @ParameterizedTest(name = "topN = {0}")
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("кидає IllegalArgumentException, якщо topN <= 0")
        void throwsOnNonPositiveTopN(int topN) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> analyzer.getTopIps(List.of(line("1.1.1.1")), topN));
            assertEquals("topN must be greater than 0", ex.getMessage());
        }

        @Test
        @DisplayName("перевірка null має пріоритет над перевіркою topN")
        void nullCheckGoesFirst() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> analyzer.getTopIps(null, 0));
            assertEquals("logLines cannot be null", ex.getMessage());
        }

        @Test
        @DisplayName("повертає порожній список для порожнього входу")
        void emptyListGivesEmptyResult() {
            assertTrue(analyzer.getTopIps(List.of(), 5).isEmpty());
        }

        @Test
        @DisplayName("сортує IP за спаданням частоти")
        void sortsByFrequencyDescending() {
            List<String> lines = concat(
                    repeat("1.1.1.1", 1),
                    repeat("2.2.2.2", 3),
                    repeat("3.3.3.3", 2));

            assertEquals(List.of("2.2.2.2", "3.3.3.3", "1.1.1.1"), analyzer.getTopIps(lines, 3));
        }

        @Test
        @DisplayName("обрізає результат до topN")
        void limitsResultToTopN() {
            List<String> lines = concat(
                    repeat("1.1.1.1", 5),
                    repeat("2.2.2.2", 4),
                    repeat("3.3.3.3", 3),
                    repeat("4.4.4.4", 2));

            assertEquals(List.of("1.1.1.1", "2.2.2.2"), analyzer.getTopIps(lines, 2));
        }

        @Test
        @DisplayName("при однаковій частоті сортує IP лексикографічно за зростанням")
        void tieBreakingIsLexicographic() {
            List<String> lines = List.of(
                    line("192.168.0.1"),
                    line("10.0.0.1"),
                    line("172.16.0.1"));

            assertEquals(List.of("10.0.0.1", "172.16.0.1", "192.168.0.1"), analyzer.getTopIps(lines, 3));
        }

        @Test
        @DisplayName("tie-breaking використовує порівняння рядків, а не чисел")
        void tieBreakingIsStringBased() {
            List<String> lines = List.of(line("9.0.0.1"), line("10.0.0.1"));
            // "10..." < "9..." лексикографічно, бо '1' < '9'
            assertEquals(List.of("10.0.0.1", "9.0.0.1"), analyzer.getTopIps(lines, 2));
        }

        @Test
        @DisplayName("частота важливіша за алфавіт, нічия вирішується всередині групи")
        void frequencyThenAlphabet() {
            List<String> lines = concat(
                    repeat("5.5.5.5", 2),
                    repeat("9.9.9.9", 3),
                    repeat("1.1.1.1", 2),
                    repeat("8.8.8.8", 3),
                    repeat("7.7.7.7", 1));

            assertEquals(List.of("8.8.8.8", "9.9.9.9", "1.1.1.1", "5.5.5.5"), analyzer.getTopIps(lines, 4));
        }

        @Test
        @DisplayName("повертає всі IP, якщо унікальних менше, ніж topN")
        void returnsAllWhenFewerThanTopN() {
            List<String> lines = concat(repeat("2.2.2.2", 1), repeat("1.1.1.1", 2));
            assertEquals(List.of("1.1.1.1", "2.2.2.2"), analyzer.getTopIps(lines, 10));
        }

        @Test
        @DisplayName("пропускає некоректні рядки під час підрахунку частот")
        void ignoresMalformedLinesWhenCounting() {
            List<String> lines = new ArrayList<>(repeat("1.1.1.1", 1));
            lines.add("1.1.1.2 broken line");
            lines.add("1.1.1.2 broken line");
            lines.add(null);
            lines.addAll(repeat("3.3.3.3", 2));

            assertEquals(List.of("3.3.3.3", "1.1.1.1"), analyzer.getTopIps(lines, 5));
        }

        @Test
        @DisplayName("повертає рівно topN, коли унікальних IP стільки ж")
        void exactlyTopN() {
            List<String> lines = List.of(line("1.1.1.1"), line("2.2.2.2"));
            assertEquals(List.of("1.1.1.1", "2.2.2.2"), analyzer.getTopIps(lines, 2));
        }
    }
}
