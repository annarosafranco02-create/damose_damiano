package it.damose.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rappresenta una linea di trasporto pubblico
 */
public class Route {
    private final String id;
    private final String agencyId;
    private final String shortName;
    private final String longName;
    private final int routeType;
    private final String routeUrl;
    private final String routeColor;
    private final String routeTextColor;

    private final List<String> stopIds = new ArrayList<>();
    private final List<Trip> trips = new ArrayList<>();

    public Route(String id, String agencyId, String shortName, String longName,
                 int routeType, String routeUrl, String routeColor, String routeTextColor) {
        this.id = id;
        this.agencyId = agencyId;
        this.shortName = shortName;
        this.longName = longName;
        this.routeType = routeType;
        this.routeUrl = routeUrl;
        // Assicura che i colori non siano mai null o vuoti
        this.routeColor = (routeColor != null && !routeColor.isEmpty()) ? routeColor : "FF0000";
        this.routeTextColor = (routeTextColor != null && !routeTextColor.isEmpty()) ? routeTextColor : "FFFFFF";
    }

    public Route(String id, String name, String type) {
        this(id, "", name, "", parseRouteType(type), "", "FF0000", "FFFFFF");
    }

    public static Route fromCsvLine(String line) {
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replace("\"", "").trim();
        }

        String routeId = parts[0];
        String agencyId = parts.length > 1 ? parts[1] : "";
        String shortName = parts.length > 2 ? parts[2] : "";
        String longName = parts.length > 3 ? parts[3] : "";
        int routeType = parts.length > 4 && !parts[4].isEmpty() ?
                Integer.parseInt(parts[4]) : 3;
        String routeUrl = parts.length > 5 ? parts[5] : "";
        String routeColor = parts.length > 6 && !parts[6].isEmpty() ? parts[6] : "FF0000";
        String routeTextColor = parts.length > 7 && !parts[7].isEmpty() ? parts[7] : "FFFFFF";

        return new Route(routeId, agencyId, shortName, longName,
                routeType, routeUrl, routeColor, routeTextColor);
    }

    private static int parseRouteType(String type) {
        if (type == null) return 3;
        try {
            return Integer.parseInt(type);
        } catch (NumberFormatException e) {
            return 3;
        }
    }

    public String getId() {
        return id;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public int getRouteType() {
        return routeType;
    }

    public String getRouteUrl() {
        return routeUrl;
    }

    public String getRouteColor() {
        return routeColor;
    }

    public String getRouteTextColor() {
        return routeTextColor;
    }

    public String getName() {
        if (!shortName.isEmpty()) {
            return shortName;
        }
        if (!longName.isEmpty()) {
            return longName;
        }
        return id;
    }

    public String getType() {
        switch (routeType) {
            case 0: return "Tram";
            case 1: return "Metro";
            case 2: return "Treno";
            case 3: return "Bus";
            case 4: return "Traghetto";
            case 5: return "Funicolare";
            case 6: return "Cabinovia";
            case 7: return "Funicolare";
            default: return "Altro";
        }
    }

    public void addStopId(String stopId) {
        if (!stopIds.contains(stopId)) {
            stopIds.add(stopId);
        }
    }

    public List<String> getAllStopIds() {
        Set<String> allStopIds = new HashSet<>();
        for (Trip trip : trips) {
            allStopIds.addAll(trip.getStopIds());
        }
        return new ArrayList<>(allStopIds);
    }

    public void addTrip(Trip trip) {
        trips.add(trip);
        for (String stopId : trip.getStopIds()) {
            addStopId(stopId);
        }
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public String getShapeId() {
        for (Trip trip : trips) {
            String shapeId = trip.getShapeId();
            if (shapeId != null && !shapeId.isEmpty()) {
                return shapeId;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getName() + " (" + getType() + ")";
    }
}