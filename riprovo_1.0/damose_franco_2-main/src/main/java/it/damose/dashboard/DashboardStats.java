package it.damose.dashboard;

public class DashboardStats {
    private int totalBuses;
    private int activeBuses;
    private int inactiveBuses;
    private double averageDelay;
    private int onTimeBuses;
    private int delayedBuses;
    private String mostDelayedRoute;
    private int mostDelayedMinutes;

    public DashboardStats() {
        this.totalBuses = 0;
        this.activeBuses = 0;
        this.inactiveBuses = 0;
        this.averageDelay = 0.0;
        this.onTimeBuses = 0;
        this.delayedBuses = 0;
        this.mostDelayedRoute = "N/A";
        this.mostDelayedMinutes = 0;
    }

    // Getters e Setters
    public int getTotalBuses() { return totalBuses; }
    public void setTotalBuses(int totalBuses) { this.totalBuses = totalBuses; }

    public int getActiveBuses() { return activeBuses; }
    public void setActiveBuses(int activeBuses) { this.activeBuses = activeBuses; }

    public int getInactiveBuses() { return inactiveBuses; }
    public void setInactiveBuses(int inactiveBuses) { this.inactiveBuses = inactiveBuses; }

    public double getAverageDelay() { return averageDelay; }
    public void setAverageDelay(double averageDelay) { this.averageDelay = averageDelay; }

    public int getOnTimeBuses() { return onTimeBuses; }
    public void setOnTimeBuses(int onTimeBuses) { this.onTimeBuses = onTimeBuses; }

    public int getDelayedBuses() { return delayedBuses; }
    public void setDelayedBuses(int delayedBuses) { this.delayedBuses = delayedBuses; }

    public String getMostDelayedRoute() { return mostDelayedRoute; }
    public void setMostDelayedRoute(String mostDelayedRoute) { this.mostDelayedRoute = mostDelayedRoute; }

    public int getMostDelayedMinutes() { return mostDelayedMinutes; }
    public void setMostDelayedMinutes(int mostDelayedMinutes) { this.mostDelayedMinutes = mostDelayedMinutes; }
}