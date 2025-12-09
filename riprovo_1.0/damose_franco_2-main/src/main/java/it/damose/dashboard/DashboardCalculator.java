package it.damose.dashboard;

import it.damose.controller.StopController;
import it.damose.model.RealtimeArrival;
import it.damose.model.Route;
import it.damose.model.Stop;
import it.damose.model.VehiclePosition;
import it.damose.realtime.RealtimeManager;
import java.util.*;

public class DashboardCalculator {

    private final StopController stopController;
    private final RealtimeManager realtimeManager;

    public DashboardCalculator(StopController stopController, RealtimeManager realtimeManager) {
        this.stopController = stopController;
        this.realtimeManager = realtimeManager;
    }

    /**
     * Calcola tutte le statistiche in tempo reale
     */
    public DashboardStats calculateStats() {
        DashboardStats stats = new DashboardStats();

        // Ottieni veicoli attivi
        Collection<VehiclePosition> vehicles = realtimeManager.getVehiclePositions();

        // Bus attivi/inattivi
        stats.setActiveBuses(vehicles.size());
        stats.setTotalBuses(stopController.getTrips().size());
        stats.setInactiveBuses(stats.getTotalBuses() - stats.getActiveBuses());

        // Calcola ritardi
        calculateDelayStats(stats);

        return stats;
    }

    /**
     * Calcola statistiche sui ritardi
     */
    private void calculateDelayStats(DashboardStats stats) {
        List<Stop> allStops = stopController.getStops();

        int totalDelays = 0;
        int delayCount = 0;
        int onTimeCount = 0;
        int delayedCount = 0;

        Map<String, Integer> routeDelays = new HashMap<>();
        Map<String, Integer> routeCounts = new HashMap<>();

        for (Stop stop : allStops) {
            List<RealtimeArrival> arrivals = realtimeManager.getArrivalsForStop(stop.getId());

            if (arrivals == null || arrivals.isEmpty()) continue;

            for (RealtimeArrival arrival : arrivals) {
                int delay = arrival.getDelay();

                // Somma ritardi
                totalDelays += delay;
                delayCount++;

                // Conta bus in orario vs in ritardo
                if (Math.abs(delay) < 60) { // Meno di 1 minuto
                    onTimeCount++;
                } else {
                    delayedCount++;
                }

                // Traccia ritardi per route
                String routeId = arrival.getRouteId();
                routeDelays.put(routeId, routeDelays.getOrDefault(routeId, 0) + delay);
                routeCounts.put(routeId, routeCounts.getOrDefault(routeId, 0) + 1);
            }
        }

        // Ritardo medio
        if (delayCount > 0) {
            stats.setAverageDelay(totalDelays / (double) delayCount / 60.0); // Converti in minuti
        }

        stats.setOnTimeBuses(onTimeCount);
        stats.setDelayedBuses(delayedCount);

        // Trova la route più in ritardo
        String mostDelayedRoute = "N/A";
        int maxAvgDelay = 0;

        for (Map.Entry<String, Integer> entry : routeDelays.entrySet()) {
            String routeId = entry.getKey();
            int totalDelay = entry.getValue();
            int count = routeCounts.get(routeId);
            int avgDelay = totalDelay / count;

            if (avgDelay > maxAvgDelay) {
                maxAvgDelay = avgDelay;

                Route route = stopController.getRouteById(routeId);
                mostDelayedRoute = (route != null) ? route.getName() : routeId;
            }
        }

        stats.setMostDelayedRoute(mostDelayedRoute);
        stats.setMostDelayedMinutes(maxAvgDelay / 60);
    }
}
