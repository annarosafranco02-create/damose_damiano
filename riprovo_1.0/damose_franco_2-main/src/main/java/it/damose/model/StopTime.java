package it.damose.model;

public class StopTime {
    private final String tripId;
    private final String arrivalTime;
    private final String departureTime;
    private final String stopId;
    private final int stopSequence;

    public StopTime(String tripId, String arrivalTime, String departureTime,
                    String stopId, int stopSequence) {
        this.tripId = tripId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopId = stopId;
        this.stopSequence = stopSequence;
    }

    /**
     * Crea uno StopTime da una riga CSV del file stop_times.txt
     * Formato: trip_id,arrival_time,departure_time,stop_id,stop_sequence,...
     */
    public static StopTime fromCsvLine(String line) {
        String[] parts = line.split(",");

        // Pulisce i valori
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replace("\"", "").trim();
        }

        String tripId = parts[0];
        String arrivalTime = parts[1];
        String departureTime = parts[2];
        String stopId = parts[3];
        int stopSequence = parts.length > 4 && !parts[4].isEmpty() ?
                Integer.parseInt(parts[4]) : 0;

        return new StopTime(tripId, arrivalTime, departureTime, stopId, stopSequence);
    }

    public String getTripId() {
        return tripId;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public String getStopId() {
        return stopId;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    @Override
    public String toString() {
        return "StopTime{" +
                "tripId='" + tripId + '\'' +
                ", stopId='" + stopId + '\'' +
                ", arrival='" + arrivalTime + '\'' +
                ", sequence=" + stopSequence +
                '}';
    }
}