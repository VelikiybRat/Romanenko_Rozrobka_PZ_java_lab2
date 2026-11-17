package ua.kpi.comsys.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Аналізатор логів веб-серверів у форматі Common Log Format (CLF).
 *
 * <p>Формат рядка:
 * {@code host ident authuser [dd/MMM/yyyy:HH:mm:ss zone] "request" status bytes},
 * наприклад:
 * {@code 127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] "GET /index.html HTTP/1.1" 200 9481}.
 *
 * <p>Рядки, що не відповідають формату (а також {@code null}-рядки), ігноруються.
 * Підтримуються IPv4-адреси.
 */
public class LogAnalyzer {

    private static final String NULL_LINES_MESSAGE = "logLines cannot be null";
    private static final String INVALID_TOP_N_MESSAGE = "topN must be greater than 0";

    private static final Pattern CLF_PATTERN = Pattern.compile(
            "^(?<ip>\\d{1,3}(?:\\.\\d{1,3}){3})"           // IPv4-адреса клієнта
                    + " \\S+"                                // ident
                    + " \\S+"                                // authuser
                    + " \\[\\d{2}/[A-Za-z]{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2} [+-]\\d{4}]" // дата
                    + " \"[^\"]*\""                          // запит
                    + " \\d{3}"                              // HTTP-статус
                    + " (?:\\d+|-)$"                         // розмір відповіді
    );

    private static final int MAX_OCTET = 255;

    /**
     * Повертає множину всіх унікальних IP-адрес із коректних рядків логу.
     *
     * @param logLines рядки логу
     * @return множина унікальних IP у порядку першої появи
     * @throws IllegalArgumentException якщо {@code logLines == null}
     */
    public Set<String> getUniqueIps(List<String> logLines) {
        requireLines(logLines);

        Set<String> uniqueIps = new LinkedHashSet<>();
        for (String line : logLines) {
            extractIp(line).ifPresent(uniqueIps::add);
        }
        return uniqueIps;
    }

    /**
     * Повертає {@code topN} найчастіших IP-адрес, відсортованих за спаданням частоти.
     * IP з однаковою частотою впорядковуються лексикографічно за зростанням.
     *
     * @param logLines рядки логу
     * @param topN     кількість адрес у результаті
     * @return список IP (не довший за {@code topN})
     * @throws IllegalArgumentException якщо {@code logLines == null} або {@code topN <= 0}
     */
    public List<String> getTopIps(List<String> logLines, int topN) {
        requireLines(logLines);
        if (topN <= 0) {
            throw new IllegalArgumentException(INVALID_TOP_N_MESSAGE);
        }

        Map<String, Integer> frequencies = new HashMap<>();
        for (String line : logLines) {
            extractIp(line).ifPresent(ip -> frequencies.merge(ip, 1, Integer::sum));
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(frequencies.entrySet());
        entries.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));

        List<String> topIps = new ArrayList<>(Math.min(topN, entries.size()));
        for (Map.Entry<String, Integer> entry : entries) {
            if (topIps.size() == topN) {
                break;
            }
            topIps.add(entry.getKey());
        }
        return topIps;
    }

    private static void requireLines(List<String> logLines) {
        if (logLines == null) {
            throw new IllegalArgumentException(NULL_LINES_MESSAGE);
        }
    }

    private static Optional<String> extractIp(String line) {
        if (line == null) {
            return Optional.empty();
        }
        Matcher matcher = CLF_PATTERN.matcher(line.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String ip = matcher.group("ip");
        return isValidIpv4(ip) ? Optional.of(ip) : Optional.empty();
    }

    private static boolean isValidIpv4(String ip) {
        for (String octet : ip.split("\\.")) {
            if (Integer.parseInt(octet) > MAX_OCTET) {
                return false;
            }
        }
        return true;
    }
}
