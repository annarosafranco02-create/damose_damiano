package it.damose.controller;

import it.damose.model.VehiclePosition;
import it.damose.data.RouteLoader;
import it.damose.data.StopsLoader;
import it.damose.data.TripLoader;
import it.damose.data.StopTimesLoader;
import it.damose.model.Route;
import it.damose.model.Stop;
import it.damose.model.Trip;
import it.damose.model.StopTime;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.*;
import java.util.stream.Collectors;
import it.damose.data.ShapeLoader;
import it.damose.model.ShapePoint;
import java.util.Collections;
import java.util.List;

public class StopController {

    private final List<Stop> stops = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private final List<Trip> trips = new ArrayList<>();
    private final Map<String, List<ShapePoint>> shapeMap = new HashMap<>();
    private final Map<String, Stop> stopMap = new HashMap<>();
    private final Map<String, Route> routeMap = new HashMap<>();
    private final Map<String, Trip> tripMap = new HashMap<>();

    public StopController(String s) {
        System.out.println("========================================");
        System.out.println("INIZIALIZZAZIONE STOP CONTROLLER");
        System.out.println("========================================");

        // 1. Carica dati base
        System.out.println("\n[1/7] Caricamento fermate...");
        stopMap.putAll(StopsLoader.loadStopsFromResources());
        stops.addAll(stopMap.values());

        System.out.println("[2/7] Caricamento linee...");
        routeMap.putAll(RouteLoader.loadRoutesFromResources());
        routes.addAll(routeMap.values());

        System.out.println("[3/7] Caricamento viaggi...");
        trips.addAll(TripLoader.loadTripsFromResources());

        System.out.println("[4/7] Caricamento percorsi (shapes)...");
        shapeMap.putAll(ShapeLoader.loadShapesFromResources());

        // 2. Crea mappa trip_id -> Trip
        System.out.println("[5/7] Creazione mappa trip...");
        for (Trip trip : trips) {
            tripMap.put(trip.getId(), trip);
        }

        // 3. ⭐ IMPORTANTE: Carica e collega gli stop_times ai trip
        System.out.println("[6/7] Caricamento e collegamento stop_times...");
        StopTimesLoader.loadAndLinkStopTimes(trips);

        // 4. Collega trip alle route e fermate alle route
        System.out.println("[7/7] Collegamento trip alle route...");
        TripLoader.linkTripsToRoutes(trips, routes);
        StopsLoader.linkStopsToRoutes(stops, routes);

        // 5. Debug finale
        System.out.println("\n========================================");
        System.out.println("REPORT FINALE:");
        System.out.println("========================================");
        System.out.println("Fermate caricate: " + stops.size());
        System.out.println("Linee caricate: " + routes.size());
        System.out.println("Viaggi caricati: " + trips.size());
        System.out.println("Percorsi (shapes) caricati: " + shapeMap.size());

        // 6. Verifica integrità dati
        verifyDataIntegrity();

        // 7. Debug shapes per route
        TripLoader.debugRouteShapes(routes);
    }

    /**
     * Verifica l'integrità dei dati caricati
     */
    private void verifyDataIntegrity() {
        System.out.println("\n========================================");
        System.out.println("VERIFICA INTEGRITÀ DATI");
        System.out.println("========================================");

        // Conta trip con stop_times
        int tripsWithStopTimes = 0;
        int totalStopTimes = 0;

        for (Trip trip : trips) {
            int stopTimeCount = trip.getStopTimes().size();
            if (stopTimeCount > 0) {
                tripsWithStopTimes++;
                totalStopTimes += stopTimeCount;
            }
        }

        System.out.println("Trip con stop_times: " + tripsWithStopTimes + " / " + trips.size());
        System.out.println("Stop_times totali: " + totalStopTimes);

        // Conta route con trip
        int routesWithTrips = 0;
        for (Route route : routes) {
            if (!route.getTrips().isEmpty()) {
                routesWithTrips++;
            }
        }

        System.out.println("Route con trip: " + routesWithTrips + " / " + routes.size());

        // Mostra esempio di route con dati completi
        System.out.println("\nEsempio route con dati completi:");
        for (Route route : routes) {
            if (!route.getTrips().isEmpty() && route.getShapeId() != null) {
                Trip firstTrip = route.getTrips().get(0);
                System.out.println("  Route: " + route.getName() +
                        " | Trip: " + route.getTrips().size() +
                        " | StopTimes nel primo trip: " + firstTrip.getStopTimes().size() +
                        " | Shape: " + route.getShapeId());
                break; // Mostra solo il primo esempio
            }
        }

        System.out.println("========================================\n");
    }

    public List<Stop> getStops() {
        return stops;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public List<StopTime> getNextArrivals(Stop stop, int limit) {
        if (stop == null) {
            return Collections.emptyList();
        }

        List<StopTime> allArrivals = new ArrayList<>();

        for (Trip trip : tripMap.values()) {
            for (StopTime st : trip.getStopTimes()) {
                if (st.getStopId().equals(stop.getId())) {
                    allArrivals.add(st);
                }
            }
        }

        allArrivals.sort(Comparator.comparing(StopTime::getArrivalTime));
        return allArrivals.stream().limit(limit).collect(Collectors.toList());
    }

    public String getArrivalInfo(StopTime st) {
        Trip trip = tripMap.get(st.getTripId());
        if (trip == null) return "N/A";

        Route route = routeMap.get(trip.getRouteId());
        String routeName = (route != null) ? route.getName() : trip.getRouteId();

        return routeName + " - " + st.getArrivalTime();
    }

    public List<Route> searchRoutes(String query) {
        List<Route> results = new ArrayList<>();
        String q = query.toLowerCase();

        for (Route r : routes) {
            String name = r.getName().toLowerCase();
            String shortName = r.getShortName().toLowerCase();
            String id = r.getId().toLowerCase();

            if (name.contains(q) || shortName.contains(q) || id.equals(q)) {
                results.add(r);
            }
        }

        // Ordina per rilevanza: exact match prima
        results.sort((r1, r2) -> {
            boolean r1Exact = r1.getShortName().equalsIgnoreCase(query) || r1.getId().equalsIgnoreCase(query);
            boolean r2Exact = r2.getShortName().equalsIgnoreCase(query) || r2.getId().equalsIgnoreCase(query);

            if (r1Exact && !r2Exact) return -1;
            if (!r1Exact && r2Exact) return 1;

            return r1.getName().compareTo(r2.getName());
        });

        return results;
    }

    public List<Stop> searchStops(String query) {
        List<Stop> results = new ArrayList<>();
        String q = query.toLowerCase();

        for (Stop s : stops) {
            if (s.getName().toLowerCase().contains(q) || s.getId().equalsIgnoreCase(q)) {
                results.add(s);
            }
        }

        return results;
    }

    public List<Stop> getStopsForRoute(Route route) {
        if (route == null || route.getAllStopIds() == null) {
            return Collections.emptyList();
        }

        List<Stop> result = new ArrayList<>();
        for (String stopId : route.getAllStopIds()) {
            Stop s = stopMap.get(stopId);
            if (s != null) result.add(s);
        }

        return result;
    }

    public List<Route> getRoutesForStop(Stop stop) {
        if (stop == null) {
            return Collections.emptyList();
        }

        Set<String> routeIds = new HashSet<>();

        for (Trip trip : tripMap.values()) {
            List<StopTime> stopTimes = trip.getStopTimes();
            if (stopTimes != null) {
                for (StopTime st : stopTimes) {
                    if (st.getStopId().equals(stop.getId())) {
                        routeIds.add(trip.getRouteId());
                        break;
                    }
                }
            }
        }

        List<Route> result = new ArrayList<>();
        for (String routeId : routeIds) {
            Route r = routeMap.get(routeId);
            if (r != null) {
                result.add(r);
            }
        }

        result.sort(Comparator.comparing(Route::getName));
        return result;
    }

    public Route getRouteById(String id) {
        return routeMap.get(id);
    }

    public Stop getStopById(String id) {
        return stopMap.get(id);
    }

    private long timeToSeconds(String time) {
        if (time.startsWith("24")) time = time.replace("24", "00");
        if (time.startsWith("25")) time = time.replace("25", "01");

        try {
            String[] parts = time.split(":");
            long hours = Long.parseLong(parts[0]);
            long minutes = Long.parseLong(parts[1]);
            long seconds = Long.parseLong(parts[2]);
            return (hours * 3600) + (minutes * 60) + seconds;
        } catch (Exception e) {
            return -1;
        }
    }

    private float calculateBearing(Stop s1, Stop s2) {
        double lat1 = Math.toRadians(s1.getLat());
        double lon1 = Math.toRadians(s1.getLon());
        double lat2 = Math.toRadians(s2.getLat());
        double lon2 = Math.toRadians(s2.getLon());

        double dLon = lon2 - lon1;
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (float) (bearing + 360) % 360;
    }

    public List<VehiclePosition> getSimulatedVehiclePositions() {
        List<VehiclePosition> simulatedVehicles = new ArrayList<>();

        java.time.LocalTime now = java.time.LocalTime.now();
        long nowInSeconds = now.toSecondOfDay();

        for (Trip trip : trips) {
            List<StopTime> stopTimes = trip.getStopTimes();
            if (stopTimes.size() < 2) continue;

            for (int i = 0; i < stopTimes.size() - 1; i++) {
                StopTime st1 = stopTimes.get(i);
                StopTime st2 = stopTimes.get(i + 1);

                long time1 = timeToSeconds(st1.getArrivalTime());
                long time2 = timeToSeconds(st2.getArrivalTime());

                if (time1 != -1 && time2 != -1 && time1 <= nowInSeconds && time2 >= nowInSeconds) {
                    Stop s1 = stopMap.get(st1.getStopId());
                    Stop s2 = stopMap.get(st2.getStopId());

                    if (s1 == null || s2 == null) continue;

                    double segmentDuration = time2 - time1;
                    double timeElapsed = nowInSeconds - time1;
                    double progress = (segmentDuration == 0) ? 0 : (timeElapsed / segmentDuration);

                    double lat = s1.getLat() + (s2.getLat() - s1.getLat()) * progress;
                    double lon = s1.getLon() + (s2.getLon() - s1.getLon()) * progress;

                    float bearing = calculateBearing(s1, s2);

                    VehiclePosition vp = new VehiclePosition(trip.getId(), trip.getRouteId(), lat, lon, bearing);
                    simulatedVehicles.add(vp);

                    break;
                }
            }
        }

        return simulatedVehicles;
    }

    /**
     * Ottiene i punti del percorso (shape) per una route specifica
     */
    public List<ShapePoint> getShapeForRoute(Route route) {
        if (route == null) {
            System.out.println("DEBUG SHAPE: Route è null");
            return Collections.emptyList();
        }

        if (route.getTrips().isEmpty()) {
            System.out.println("DEBUG SHAPE: La route '" + route.getName() +
                    "' non ha trip collegati (trips.size = 0)");
            return Collections.emptyList();
        }

        // Cerca il primo shape_id disponibile
        String shapeId = null;
        int tripCount = 0;

        for (Trip trip : route.getTrips()) {
            tripCount++;
            String currentShapeId = trip.getShapeId();

            if (currentShapeId != null && !currentShapeId.isEmpty()) {
                shapeId = currentShapeId;
                System.out.println("DEBUG SHAPE: Trovato shape_id '" + shapeId +
                        "' nel trip #" + tripCount + " (ID: " + trip.getId() +
                        ") della route '" + route.getName() + "'");
                break;
            }
        }

        if (shapeId == null) {
            System.out.println("DEBUG SHAPE: Nessuno shape_id trovato nei " + tripCount +
                    " trip della route '" + route.getName() + "' (ID: " + route.getId() + ")");
            return Collections.emptyList();
        }

        // Cerca i punti dello shape
        List<ShapePoint> points = shapeMap.getOrDefault(shapeId, Collections.emptyList());

        if (points.isEmpty()) {
            System.out.println("DEBUG SHAPE: Shape_id '" + shapeId +
                    "' trovato ma nessun punto presente in shapes.txt");
            System.out.println("  Shapes disponibili in memoria: " + shapeMap.size());
            System.out.println("  Primi 5 shape_id disponibili:");
            shapeMap.keySet().stream().limit(5).forEach(id ->
                    System.out.println("    - " + id + " (" + shapeMap.get(id).size() + " punti)"));
        } else {
            System.out.println("DEBUG SHAPE: ✓ Trovati " + points.size() +
                    " punti per shape_id '" + shapeId +
                    "' della route '" + route.getName() + "'");
        }

        return points;
    }
}