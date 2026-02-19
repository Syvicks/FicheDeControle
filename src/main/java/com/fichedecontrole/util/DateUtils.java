package com.fichedecontrole.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Classe utilitaire pour la manipulation des dates
 */
public final class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Constructeur privé pour empêcher l'instanciation
     */
    private DateUtils() {
        throw new UnsupportedOperationException("Classe utilitaire - ne peut pas être instanciée");
    }

    /**
     * Formate une Date en String au format dd/MM/yyyy
     * @param date La date à formater
     * @return La date formatée
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        LocalDate localDate = date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        return localDate.format(FORMATTER);
    }

    /**
     * Formate une LocalDate en String au format dd/MM/yyyy
     * @param date La date à formater
     * @return La date formatée
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(FORMATTER);
    }

    /**
     * Parse une String en LocalDate au format dd/MM/yyyy
     * @param dateStr La date en String
     * @return La LocalDate parsée
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr, FORMATTER);
    }
}
