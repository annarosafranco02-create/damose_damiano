package it.damose.dashboard;

import it.damose.controller.ConnectionManager;
import it.damose.controller.StopController;
import it.damose.realtime.RealtimeManager;
import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {

    private final DashboardCalculator calculator;
    private final Timer refreshTimer;

    // Componenti UI
    private JLabel lblTotalBuses;
    private JLabel lblActiveBuses;
    private JLabel lblInactiveBuses;
    private JLabel lblAverageDelay;
    private JLabel lblOnTimeBuses;
    private JLabel lblDelayedBuses;
    private JLabel lblMostDelayed;
    private JLabel lblConnectionStatus;

    public DashboardPanel(StopController stopController, RealtimeManager realtimeManager) {
        this.calculator = new DashboardCalculator(stopController, realtimeManager);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 245, 245));

        initUI();

        // Timer per aggiornamento automatico ogni 10 secondi
        refreshTimer = new Timer(10000, e -> updateStats());
        refreshTimer.start();

        // Primo aggiornamento immediato
        updateStats();
    }

    private void initUI() {
        // Titolo
        JLabel titleLabel = new JLabel("Dashboard Real-Time");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        // Pannello centrale con statistiche
        JPanel statsPanel = new JPanel(new GridLayout(4, 2, 20, 20));
        statsPanel.setOpaque(false);

        // Card 1: Bus Totali
        statsPanel.add(createStatCard("Bus Totali", "0", new Color(52, 152, 219)));
        lblTotalBuses = getLastLabel();

        // Card 2: Bus Attivi
        statsPanel.add(createStatCard("Bus Attivi", "0", new Color(46, 204, 113)));
        lblActiveBuses = getLastLabel();

        // Card 3: Bus Inattivi
        statsPanel.add(createStatCard("Bus Inattivi", "0", new Color(149, 165, 166)));
        lblInactiveBuses = getLastLabel();

        // Card 4: Ritardo Medio
        statsPanel.add(createStatCard("Ritardo Medio", "0 min", new Color(230, 126, 34)));
        lblAverageDelay = getLastLabel();

        // Card 5: Bus In Orario
        statsPanel.add(createStatCard("Bus In Orario", "0", new Color(26, 188, 156)));
        lblOnTimeBuses = getLastLabel();

        // Card 6: Bus In Ritardo
        statsPanel.add(createStatCard("Bus In Ritardo", "0", new Color(231, 76, 60)));
        lblDelayedBuses = getLastLabel();

        // Card 7: Linea Più In Ritardo (occupa 2 colonne)
        JPanel mostDelayedCard = createStatCard("Linea Più In Ritardo", "N/A", new Color(192, 57, 43));
        lblMostDelayed = getLastLabel();
        statsPanel.add(mostDelayedCard);

        // Card 8: Stato Connessione
        JPanel connectionCard = createStatCard("Connessione", "Offline", new Color(52, 73, 94));
        lblConnectionStatus = getLastLabel();
        statsPanel.add(connectionCard);

        add(statsPanel, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setOpaque(false);

        JButton btnRefresh = new JButton("Aggiorna Ora");
        btnRefresh.setFont(new Font("Arial", Font.BOLD, 14));
        btnRefresh.addActionListener(e -> updateStats());
        footerPanel.add(btnRefresh);

        add(footerPanel, BorderLayout.SOUTH);
    }

    private JLabel lastCreatedLabel;

    /**
     * Crea una card statistica
     */
    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 3),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // Titolo
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);

        card.add(Box.createVerticalStrut(10));

        // Valore
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 32));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(valueLabel);

        lastCreatedLabel = valueLabel;

        return card;
    }

    private JLabel getLastLabel() {
        return lastCreatedLabel;
    }

    /**
     * Aggiorna tutte le statistiche
     */
    private void updateStats() {
        SwingUtilities.invokeLater(() -> {
            boolean isOnline = ConnectionManager.getInstance().isOnline();

            if (isOnline) {
                // Calcola statistiche real-time
                DashboardStats stats = calculator.calculateStats();

                lblTotalBuses.setText(String.valueOf(stats.getTotalBuses()));
                lblActiveBuses.setText(String.valueOf(stats.getActiveBuses()));
                lblInactiveBuses.setText(String.valueOf(stats.getInactiveBuses()));
                lblAverageDelay.setText(String.format("%.1f min", stats.getAverageDelay()));
                lblOnTimeBuses.setText(String.valueOf(stats.getOnTimeBuses()));
                lblDelayedBuses.setText(String.valueOf(stats.getDelayedBuses()));
                lblMostDelayed.setText(stats.getMostDelayedRoute() +
                        " (+" + stats.getMostDelayedMinutes() + " min)");
                lblConnectionStatus.setText("Online");
                lblConnectionStatus.setForeground(new Color(46, 204, 113));
            } else {
                // Modalità offline
                lblConnectionStatus.setText("Offline");
                lblConnectionStatus.setForeground(new Color(231, 76, 60));
                lblActiveBuses.setText("N/A");
                lblAverageDelay.setText("N/A");
                lblOnTimeBuses.setText("N/A");
                lblDelayedBuses.setText("N/A");
                lblMostDelayed.setText("Dati non disponibili");
            }
        });
    }

    /**
     * Ferma il timer quando il pannello viene chiuso
     */
    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
}