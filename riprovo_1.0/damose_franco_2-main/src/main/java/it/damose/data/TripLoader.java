package it.damose.data;

import it.damose.model.Route;
import it.damose.model.Trip;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class TripLoader {

    /**
     * Carica tutti i trip dal file trips.txt
     */
    public static List<Trip> loadTripsFromResources() {
        List<Trip> trips = new ArrayList<>();

        InputStream input = TripLoader.class.getClassLoader()
                .getResourceAsStream("data/rome_static_gtfs/trips.txt");

        if (input == null) {
            System.err.println("trips.txt non trovato nelle risorse!");
            return trips;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            String headerLine = br.readLine(); // Leggi header
            System.out.println("DEBUG TRIP LOADER - Header: " + headerLine);

            String line;
            int lineCount = 0;
            int successCount = 0;
            int shapeIdCount = 0;

            while ((line = br.readLine()) != null) {
                lineCount++;
                if (line.trim().isEmpty()) continue;

                try {
                    Trip trip = Trip.fromCsvLine(line);
                    trips.add(trip);
                    successCount++;

                    // Debug: controlla se ha shape_id
                    if (trip.getShapeId() != null && !trip.getShapeId().isEmpty()) {
                        shapeIdCount++;

                        // Stampa i primi 5 trip con shape_id per debug
                        if (shapeIdCount <= 5) {
                            System.out.println("DEBUG - Trip con shape_id: " +
                                    trip.getId() + " -> route: " +
                                    trip.getRouteId() + " -> shape: " +
                                    trip.getShapeId());
                        }
                    }

                } catch (Exception e) {
                    if (lineCount <= 10) {
                        System.err.println("Errore parsing trip alla riga " + lineCount);
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("========================================");
            System.out.println("TRIP LOADER REPORT:");
            System.out.println("Righe lette: " + lineCount);
            System.out.println("Trip caricati con successo: " + successCount);
            System.out.println("Trip con shape_id: " + shapeIdCount);
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("Errore caricamento trips.txt");
            e.printStackTrace();
        }

        return trips;
    }

    /**
     * Collega i trip alle loro route
     */
    public static void linkTripsToRoutes(List<Trip> trips, List<Route> routes) {
        Map<String, Route> routeMap = new HashMap<>();
        for (Route route : routes) {
            routeMap.put(route.getId(), route);
        }

        int linkedCount = 0;
        int notFoundCount = 0;

        for (Trip trip : trips) {
            Route route = routeMap.get(trip.getRouteId());
            if (route != null) {
                route.addTrip(trip);
                linkedCount++;
            } else {
                notFoundCount++;
                if (notFoundCount <= 5) {
                    System.err.println("WARN: Route non trovata per trip " +
                            trip.getId() + " -> routeId: " + trip.getRouteId());
                }
            }
        }

        System.out.println("Trip collegati alle route: " + linkedCount);
        if (notFoundCount > 0) {
            System.err.println("Trip senza route: " + notFoundCount);
        }
    }

    /**
     * Debug: verifica quali route hanno shape_id
     */
    public static void debugRouteShapes(List<Route> routes) {
        System.out.println("\n========================================");
        System.out.println("DEBUG: ROUTE CON SHAPE_ID");
        System.out.println("========================================");

        int routesWithShapes = 0;

        for (Route route : routes) {
            String shapeId = route.getShapeId();

            if (shapeId != null && !shapeId.isEmpty()) {
                routesWithShapes++;

                // Stampa le prime 10 route con shape
                if (routesWithShapes <= 10) {
                    System.out.println("Route: " + route.getName() +
                            " (ID: " + route.getId() +
                            ") -> Shape: " + shapeId);
                }
            }
        }

        System.out.println("========================================");
        System.out.println("Route totali: " + routes.size());
        System.out.println("Route con shape_id: " + routesWithShapes);
        System.out.println("Route senza shape_id: " + (routes.size() - routesWithShapes));
        System.out.println("========================================\n");
    }
}