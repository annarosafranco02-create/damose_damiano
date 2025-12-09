package it.damose.data;

import it.damose.model.ShapePoint;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Loader per caricare gli shapes (percorsi) dalla risorsa shapes.txt
 */
public class ShapeLoader {

    /**
     * Carica tutti gli shapes raggruppati per shape_id
     * @return Map con chiave=shape_id e valore=lista ordinata di ShapePoint
     */
    public static Map<String, List<ShapePoint>> loadShapesFromResources() {
        Map<String, List<ShapePoint>> shapeMap = new HashMap<>();

        // Prova percorsi diversi per trovare il file
        String[] possiblePaths = {
                "/shapes.txt",
                "shapes.txt",
                "/data/rome_static_gtfs/shapes.txt",
                "data/rome_static_gtfs/shapes.txt"
        };

        InputStream is = null;
        String usedPath = null;

        // Cerca il file in tutti i percorsi possibili
        for (String path : possiblePaths) {
            is = ShapeLoader.class.getResourceAsStream(path);
            if (is == null) {
                is = ShapeLoader.class.getClassLoader().getResourceAsStream(path);
            }
            if (is != null) {
                usedPath = path;
                break;
            }
        }

        if (is == null) {
            System.err.println("⚠️ shapes.txt NON TROVATO in nessuno di questi percorsi:");
            for (String path : possiblePaths) {
                System.err.println("  - " + path);
            }
            System.err.println("Il percorso sarà disegnato usando le fermate (linee rette).");
            return shapeMap;
        }

        System.out.println("✓ shapes.txt trovato in: " + usedPath);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String headerLine = br.readLine(); // Salta header
            System.out.println("ShapeLoader - Header: " + headerLine);

            String line;
            int lineCount = 0;
            int successCount = 0;

            while ((line = br.readLine()) != null) {
                lineCount++;
                if (line.trim().isEmpty()) continue;

                try {
                    ShapePoint point = ShapePoint.fromCsvLine(line);

                    shapeMap.computeIfAbsent(point.getShapeId(), k -> new ArrayList<>())
                            .add(point);
                    successCount++;

                } catch (Exception e) {
                    if (lineCount <= 10) {
                        System.err.println("Errore parsing shape alla riga " + lineCount);
                    }
                }
            }

            // Ordina i punti per sequence in ogni shape
            for (List<ShapePoint> points : shapeMap.values()) {
                Collections.sort(points);
            }

            System.out.println("========================================");
            System.out.println("SHAPE LOADER REPORT:");
            System.out.println("Righe lette: " + lineCount);
            System.out.println("Punti caricati: " + successCount);
            System.out.println("Shapes unici caricati: " + shapeMap.size());

            // Mostra esempi
            if (!shapeMap.isEmpty()) {
                System.out.println("\nEsempi di shapes caricati:");
                shapeMap.entrySet().stream()
                        .limit(5)
                        .forEach(entry ->
                                System.out.println("  Shape ID: " + entry.getKey() +
                                        " -> " + entry.getValue().size() + " punti"));
            }
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("Errore caricamento shapes.txt");
            e.printStackTrace();
        }

        return shapeMap;
    }

    /**
     * Carica solo gli shape point per uno specifico shape_id
     */
    public static List<ShapePoint> loadShapeById(String shapeId) {
        List<ShapePoint> points = new ArrayList<>();

        InputStream is = ShapeLoader.class.getResourceAsStream("/shapes.txt");
        if (is == null) {
            is = ShapeLoader.class.getClassLoader()
                    .getResourceAsStream("data/rome_static_gtfs/shapes.txt");
        }

        if (is == null) {
            return points;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line = br.readLine(); // Salta header

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                try {
                    ShapePoint point = ShapePoint.fromCsvLine(line);
                    if (point.getShapeId().equals(shapeId)) {
                        points.add(point);
                    }
                } catch (Exception e) {
                    // Ignora righe malformate
                }
            }

            Collections.sort(points);

        } catch (Exception e) {
            System.err.println("Errore caricamento shape " + shapeId);
        }

        return points;
    }
}
