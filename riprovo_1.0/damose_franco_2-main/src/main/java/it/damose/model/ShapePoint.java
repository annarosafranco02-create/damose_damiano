package it.damose.model;

/**
 * Rappresenta un punto di un percorso (shape) su mappa
 */
public class ShapePoint implements Comparable<ShapePoint> {
    private final String shapeId;
    private final double lat;
    private final double lon;
    private final int sequence;
    private final double distTraveled;

    public ShapePoint(String shapeId, double lat, double lon, int sequence, double distTraveled) {
        this.shapeId = shapeId;
        this.lat = lat;
        this.lon = lon;
        this.sequence = sequence;
        this.distTraveled = distTraveled;
    }

    /**
     * Crea uno ShapePoint da una riga CSV del file shapes.txt
     * Formato: shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence,shape_dist_traveled
     */
    public static ShapePoint fromCsvLine(String line) {
        // Split che gestisce le virgolette
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        // Pulisce i valori
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replace("\"", "").trim();
        }

        if (parts.length < 4) {
            throw new IllegalArgumentException("Riga shapes.txt malformata: " + line);
        }

        String shapeId = parts[0];
        double lat = Double.parseDouble(parts[1]);
        double lon = Double.parseDouble(parts[2]);
        int sequence = Integer.parseInt(parts[3]);
        double distTraveled = parts.length > 4 && !parts[4].isEmpty() ?
                Double.parseDouble(parts[4]) : 0.0;

        return new ShapePoint(shapeId, lat, lon, sequence, distTraveled);
    }

    public String getShapeId() {
        return shapeId;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public int getSequence() {
        return sequence;
    }

    public double getDistTraveled() {
        return distTraveled;
    }

    @Override
    public int compareTo(ShapePoint other) {
        return Integer.compare(this.sequence, other.sequence);
    }

    @Override
    public String toString() {
        return "ShapePoint{" +
                "shapeId='" + shapeId + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", seq=" + sequence +
                '}';
    }
}