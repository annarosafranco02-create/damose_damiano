package it.damose.data;

import it.damose.model.StopTime;
import it.damose.model.Trip;
import java.io.*;
import java.util.*;

public class StopTimesLoader {

    /**
     * Carica tutti gli stop_times dal file stop_times.txt
     */
    public static List<StopTime> loadStopTimesFromResources() {
        List<StopTime> stopTimes = new ArrayList<>();

        InputStream input = StopTimesLoader.class.getClassLoader()
                .getResourceAsStream("data/rome_static_gtfs/stop_times.txt");

        if (input == null) {
            System.err.println("stop_times.txt non trovato nelle risorse!");
            return stopTimes;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            String header = br.readLine(); // Salta intestazione
            System.out.println("StopTimesLoader - Header: " + header);

            String line;
            int lineCount = 0;
            int successCount = 0;

            while ((line = br.readLine()) != null) {
                lineCount++;
                if (line.trim().isEmpty()) continue;

                try {
                    // Split che gestisce virgolette
                    String[] p = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                    // Pulisce i valori
                    for (int i = 0; i < p.length; i++) {
                        p[i] = p[i].replace("\"", "").trim();
                    }

                    if (p.length >= 5) {
                        String tripId = p[0];
                        String arrivalTime = p[1];
                        String departureTime = p[2];
                        String stopId = p[3];
                        int stopSequence = p[4].isEmpty() ? 0 : Integer.parseInt(p[4]);

                        StopTime st = new StopTime(tripId, arrivalTime, departureTime,
                                stopId, stopSequence);
                        stopTimes.add(st);
                        successCount++;
                    }

                } catch (Exception e) {
                    if (lineCount <= 10) { // Mostra solo i primi 10 errori
                        System.err.println("Errore parsing stop_time alla riga " + lineCount);
                    }
                }
            }

            System.out.println("========================================");
            System.out.println("STOP_TIMES LOADER REPORT:");
            System.out.println("Righe lette: " + lineCount);
            System.out.println("Stop times caricati: " + successCount);
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("Errore caricamento stop_times.txt");
            e.printStackTrace();
        }

        return stopTimes;
    }

    /**
     * Carica gli stop_times e li collega direttamente ai Trip
     */
    public static void loadAndLinkStopTimes(List<Trip> trips) {
        // Crea mappa trip_id -> Trip per accesso veloce
        Map<String, Trip> tripMap = new HashMap<>();
        for (Trip trip : trips) {
            tripMap.put(trip.getId(), trip);
        }

        InputStream input = StopTimesLoader.class.getClassLoader()
                .getResourceAsStream("data/rome_static_gtfs/stop_times.txt");

        if (input == null) {
            System.err.println("stop_times.txt non trovato nelle risorse!");
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            br.readLine(); // Salta intestazione

            String line;
            int linked = 0;
            int notFound = 0;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                try {
                    String[] p = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                    for (int i = 0; i < p.length; i++) {
                        p[i] = p[i].replace("\"", "").trim();
                    }

                    if (p.length >= 5) {
                        String tripId = p[0];
                        String arrivalTime = p[1];
                        String departureTime = p[2];
                        String stopId = p[3];
                        int stopSequence = p[4].isEmpty() ? 0 : Integer.parseInt(p[4]);

                        Trip trip = tripMap.get(tripId);

                        if (trip != null) {
                            StopTime st = new StopTime(tripId, arrivalTime, departureTime,
                                    stopId, stopSequence);
                            trip.addStopTime(st);
                            linked++;
                        } else {
                            notFound++;
                        }
                    }

                } catch (Exception e) {
                    // Ignora errori di parsing
                }
            }

            System.out.println("Stop times collegati ai trip: " + linked);
            if (notFound > 0) {
                System.out.println("Stop times senza trip corrispondente: " + notFound);
            }

        } catch (Exception e) {
            System.err.println("Errore collegamento stop_times");
            e.printStackTrace();
        }
    }
}