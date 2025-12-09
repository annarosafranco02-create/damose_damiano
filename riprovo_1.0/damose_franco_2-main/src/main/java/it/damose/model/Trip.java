package it.damose.model;

import java.util.ArrayList;
import java.util.List;

public class Trip {
    private final String id;
    private final String routeId;
    private final String serviceId;
    private final String tripHeadsign;
    private final String tripShortName;
    private final int directionId;
    private final String blockId;
    private final String shapeId;
    private final int wheelchairAccessible;
    private final int exceptional;

    private final List<String> stopIds = new ArrayList<>();
    private final List<StopTime> stopTimes = new ArrayList<>();

    // Costruttore completo
    public Trip(String routeId, String serviceId, String id, String tripHeadsign,
                String tripShortName, int directionId, String blockId,
                String shapeId, int wheelchairAccessible, int exceptional) {
        this.id = id;
        this.routeId = routeId;
        this.serviceId = serviceId;
        this.tripHeadsign = tripHeadsign;
        this.tripShortName = tripShortName;
        this.directionId = directionId;
        this.blockId = blockId;
        this.shapeId = shapeId; // IMPORTANTE: assicurati che non sia null
        this.wheelchairAccessible = wheelchairAccessible;
        this.exceptional = exceptional;
    }

    // Costruttore semplificato (retrocompatibilità)
    public Trip(String id, String routeId, String shapeId) {
        this(routeId, "", id, "", "", 0, "", shapeId, 0, 0);
    }

    /**
     * Crea un Trip da una riga CSV - VERSIONE CORRETTA
     */
    public static Trip fromCsvLine(String line) {
        // Gestione CSV con virgolette
        List<String> parts = parseCsvLine(line);

        if (parts.size() < 8) {
            throw new IllegalArgumentException("Riga CSV malformata (troppo pochi campi): " + line);
        }

        // Ordine colonne in trips.txt:
        // route_id,service_id,trip_id,trip_headsign,trip_short_name,direction_id,block_id,shape_id,wheelchair_accessible,exceptional

        String routeId = parts.get(0);
        String serviceId = parts.get(1);
        String tripId = parts.get(2);
        String tripHeadsign = parts.get(3);
        String tripShortName = parts.get(4);
        int directionId = parseIntOrDefault(parts.get(5), 0);
        String blockId = parts.get(6);
        String shapeId = parts.get(7); // <-- QUESTA È LA COLONNA SHAPE_ID
        int wheelchairAccessible = parts.size() > 8 ? parseIntOrDefault(parts.get(8), 0) : 0;
        int exceptional = parts.size() > 9 ? parseIntOrDefault(parts.get(9), 0) : 0;

        return new Trip(routeId, serviceId, tripId, tripHeadsign, tripShortName,
                directionId, blockId, shapeId, wheelchairAccessible, exceptional);
    }

    /**
     * Parser CSV che gestisce correttamente le virgolette
     */
    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Aggiungi l'ultimo campo
        result.add(current.toString().trim());

        return result;
    }

    /**
     * Converte stringa in int con valore di default
     */
    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }

    public String getTripShortName() {
        return tripShortName;
    }

    public int getDirectionId() {
        return directionId;
    }

    public String getBlockId() {
        return blockId;
    }

    public String getShapeId() {
        return shapeId;
    }

    public int getWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public int getExceptional() {
        return exceptional;
    }

    public void addStopTime(StopTime st) {
        stopTimes.add(st);
        if (!stopIds.contains(st.getStopId())) {
            stopIds.add(st.getStopId());
        }
    }

    public List<StopTime> getStopTimes() {
        return stopTimes;
    }

    public List<String> getStopIds() {
        return stopIds;
    }

    @Override
    public String toString() {
        return "Trip{" +
                "id='" + id + '\'' +
                ", routeId='" + routeId + '\'' +
                ", shapeId='" + shapeId + '\'' +
                ", headsign='" + tripHeadsign + '\'' +
                '}';
    }
}