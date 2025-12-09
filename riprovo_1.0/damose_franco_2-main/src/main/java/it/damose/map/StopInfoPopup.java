package it.damose.map;

import it.damose.controller.StopController;
import it.damose.model.RealtimeArrival;
import it.damose.model.Route;
import it.damose.model.Stop;
import it.damose.realtime.RealtimeManager;
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Popup che mostra le informazioni di una fermata sulla mappa
 */
public class StopInfoPopup extends JPanel {
    private final Stop stop;
    private final StopController stopController;
    private final RealtimeManager realtimeManager;
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");

    public StopInfoPopup(Stop stop, StopController stopController, RealtimeManager realtimeManager) {
        this.stop = stop;
        this.stopController = stopController;
        this.realtimeManager = realtimeManager;

        setLayout(new BorderLayout());
        setBackground(new Color(255, 255, 255, 240));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 2),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        initUI();
    }

    private void initUI() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Titolo (nome fermata)
        JLabel titleLabel = new JLabel(stop.getName());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);

        contentPanel.add(Box.createVerticalStrut(5));

        // Separator
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(250, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(separator);

        contentPanel.add(Box.createVerticalStrut(5));

        // Prossimi arrivi
        JLabel arrivalsLabel = new JLabel("Prossimi arrivi:");
        arrivalsLabel.setFont(new Font("Arial", Font.BOLD, 11));
        arrivalsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(arrivalsLabel);

        contentPanel.add(Box.createVerticalStrut(3));

        // Lista arrivi
        List<RealtimeArrival> arrivals = realtimeManager.getArrivalsForStop(stop.getId());

        if (arrivals != null && !arrivals.isEmpty()) {
            int count = 0;
            for (RealtimeArrival arrival : arrivals) {
                if (count >= 3) break; // Mostra max 3 arrivi

                Route route = stopController.getRouteById(arrival.getRouteId());
                String routeName = (route != null) ? route.getName() : arrival.getRouteId();
                String time = timeFormatter.format(new Date(arrival.getArrivalTime() * 1000));

                // Calcola minuti mancanti
                long now = System.currentTimeMillis() / 1000;
                long minutesLeft = (arrival.getArrivalTime() - now) / 60;

                String arrivalText = String.format("%s - %s (%d min)",
                        routeName, time, minutesLeft);

                JLabel arrivalLabel = new JLabel(arrivalText);
                arrivalLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
                arrivalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                contentPanel.add(arrivalLabel);

                count++;
            }
        } else {
            JLabel noDataLabel = new JLabel("Nessun arrivo disponibile");
            noDataLabel.setFont(new Font("Arial", Font.ITALIC, 10));
            noDataLabel.setForeground(Color.GRAY);
            noDataLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(noDataLabel);
        }

        add(contentPanel, BorderLayout.CENTER);

        // Imposta dimensione preferita
        setPreferredSize(new Dimension(280, 120));
    }

    public Stop getStop() {
        return stop;
    }
}