package it.damose.map;

import it.damose.controller.ConnectionManager;
import it.damose.controller.StopController;
import it.damose.model.*;
import it.damose.realtime.RealtimeManager;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class Mappa extends JPanel {
    private static final double ROMA_LAT = 41.8902;
    private static final double ROMA_LON = 12.4922;

    private TilesManager tilesManager;
    private int zoom = 13;
    private double centerLat = ROMA_LAT;
    private double centerLon = ROMA_LON;

    private Point dragStart;
    private double dragStartLat;
    private double dragStartLon;

    private Timer refreshTimer;
    private RealtimeManager realtimeManager;
    private StopController stopController;

    private List<VehiclePosition> simulatedVehicleCache = new ArrayList<>();
    private long lastStaticCacheTime = 0;

    private Route currentlyFilteredRoute = null;
    private Stop selectedStop = null;

    // Popup fermata
    private JPanel currentPopup = null;
    private Stop hoveredStop = null;

    public Mappa() {
        tilesManager = new TilesManager();
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.LIGHT_GRAY);
        setLayout(null); // Per posizionare il popup

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
                dragStartLat = centerLat;
                dragStartLon = centerLon;

                // Chiudi popup se clicchi fuori dalle fermate
                Stop clickedStop = findStopAtPoint(e.getPoint());
                if (clickedStop == null && currentPopup != null) {
                    remove(currentPopup);
                    currentPopup = null;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStart = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Click su fermata - mostra popup
                Stop clickedStop = findStopAtPoint(e.getPoint());
                if (clickedStop != null) {
                    showStopPopup(clickedStop, e.getPoint());
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    int dx = e.getX() - dragStart.x;
                    int dy = e.getY() - dragStart.y;

                    double metersPerPixel = 156543.03392 * Math.cos(Math.toRadians(centerLat)) / Math.pow(2, zoom);
                    double deltaLat = dy * metersPerPixel / 111320.0;
                    double deltaLon = -dx * metersPerPixel / (111320.0 * Math.cos(Math.toRadians(centerLat)));

                    centerLat = dragStartLat + deltaLat;
                    centerLon = dragStartLon + deltaLon;

                    centerLat = Math.max(-85, Math.min(85, centerLat));
                    centerLon = Math.max(-180, Math.min(180, centerLon));

                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Cambia cursore se si passa sopra una fermata
                Stop stop = findStopAtPoint(e.getPoint());
                if (stop != null) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    hoveredStop = stop;
                } else {
                    setCursor(Cursor.getDefaultCursor());
                    hoveredStop = null;
                }
                repaint();
            }
        });

        addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            int newZoom = zoom - notches;
            newZoom = Math.max(TilesManager.MIN_ZOOM, Math.min(TilesManager.MAX_ZOOM, newZoom));
            if (newZoom != zoom) {
                zoom = newZoom;
                repaint();
            }
        });

        refreshTimer = new Timer(500, e -> repaint());
        refreshTimer.start();
    }

    // === METODI PUBBLICI ===

    public void setRealtimeManager(RealtimeManager manager) {
        this.realtimeManager = manager;
    }

    public void setStopController(StopController controller) {
        this.stopController = controller;
    }

    public void setFilteredRoute(Route route) {
        this.currentlyFilteredRoute = route;

        // Chiudi popup quando cambia linea
        if (currentPopup != null) {
            remove(currentPopup);
            currentPopup = null;
        }

        if (route != null && stopController != null) {
            centerMapOnRoute(route);
        }

        repaint();
    }

    public void setSelectedStop(Stop stop) {
        this.selectedStop = stop;
        repaint();
    }

    public void centerOn(double lat, double lon) {
        this.centerLat = lat;
        this.centerLon = lon;
        repaint();
    }

    public void setZoom(int newZoom) {
        this.zoom = Math.max(TilesManager.MIN_ZOOM, Math.min(TilesManager.MAX_ZOOM, newZoom));
        repaint();
    }

    public int getZoom() {
        return this.zoom;
    }

    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        if (tilesManager != null) {
            tilesManager.shutdown();
        }
    }

    // === GESTIONE POPUP FERMATE ===

    /**
     * Trova la fermata cliccata/hoveredata
     */
    private Stop findStopAtPoint(Point clickPoint) {
        if (currentlyFilteredRoute == null || stopController == null) {
            return null;
        }

        List<Stop> stops = getOrderedStopsForRoute(currentlyFilteredRoute);

        for (Stop stop : stops) {
            Point p = latLonToScreenPixel(stop.getLat(), stop.getLon());

            // Area cliccabile: cerchio di raggio 12 pixel
            double distance = Math.sqrt(
                    Math.pow(clickPoint.x - p.x, 2) +
                            Math.pow(clickPoint.y - p.y, 2)
            );

            if (distance <= 12) {
                return stop;
            }
        }

        return null;
    }

    /**
     * Mostra il popup con le info della fermata
     */
    private void showStopPopup(Stop stop, Point clickPoint) {
        // Rimuovi popup precedente
        if (currentPopup != null) {
            remove(currentPopup);
        }

        // Crea nuovo popup (SCEGLI LO STILE)
        currentPopup = new StopInfoPopup(stop, stopController, realtimeManager);

        // Dimensione
        currentPopup.setPreferredSize(new Dimension(300, 180));
        currentPopup.setSize(currentPopup.getPreferredSize());

        // Posiziona il popup vicino al click
        int popupX = clickPoint.x + 20;
        int popupY = clickPoint.y - currentPopup.getHeight() / 2;

        // Mantieni dentro lo schermo
        if (popupX + currentPopup.getWidth() > getWidth()) {
            popupX = clickPoint.x - currentPopup.getWidth() - 20;
        }
        if (popupY < 10) {
            popupY = 10;
        }
        if (popupY + currentPopup.getHeight() > getHeight()) {
            popupY = getHeight() - currentPopup.getHeight() - 10;
        }

        currentPopup.setBounds(popupX, popupY,
                currentPopup.getWidth(),
                currentPopup.getHeight());

        add(currentPopup);
        revalidate();
        repaint();

        System.out.println("✓ Popup mostrato per: " + stop.getName());
    }

    // === FERMATE ORDINATE (IMPORTANTE!) ===

    /**
     * Ottiene le fermate NELL'ORDINE CORRETTO del primo trip
     */
    private List<Stop> getOrderedStopsForRoute(Route route) {
        if (route == null || route.getTrips().isEmpty()) {
            return new ArrayList<>();
        }

        Trip firstTrip = route.getTrips().get(0);
        List<StopTime> orderedStopTimes = new ArrayList<>(firstTrip.getStopTimes());

        if (orderedStopTimes.isEmpty()) {
            return new ArrayList<>();
        }

        // Ordina per sequence
        orderedStopTimes.sort((st1, st2) ->
                Integer.compare(st1.getStopSequence(), st2.getStopSequence()));

        List<Stop> orderedStops = new ArrayList<>();
        Set<String> addedStopIds = new HashSet<>();

        for (StopTime st : orderedStopTimes) {
            String stopId = st.getStopId();

            if (!addedStopIds.contains(stopId)) {
                Stop stop = stopController.getStopById(stopId);
                if (stop != null) {
                    orderedStops.add(stop);
                    addedStopIds.add(stopId);
                }
            }
        }

        return orderedStops;
    }

    // === CENTRATURA MAPPA ===

    private void centerMapOnRoute(Route route) {
        List<ShapePoint> shapePoints = stopController.getShapeForRoute(route);

        if (shapePoints.size() >= 2) {
            double minLat = Double.MAX_VALUE;
            double maxLat = -Double.MAX_VALUE;
            double minLon = Double.MAX_VALUE;
            double maxLon = -Double.MAX_VALUE;

            for (ShapePoint sp : shapePoints) {
                minLat = Math.min(minLat, sp.getLat());
                maxLat = Math.max(maxLat, sp.getLat());
                minLon = Math.min(minLon, sp.getLon());
                maxLon = Math.max(maxLon, sp.getLon());
            }

            centerLat = (minLat + maxLat) / 2.0;
            centerLon = (minLon + maxLon) / 2.0;

            double latSpan = maxLat - minLat;
            double lonSpan = maxLon - minLon;
            zoom = calculateOptimalZoom(latSpan, lonSpan);

        } else {
            List<Stop> stops = getOrderedStopsForRoute(route);

            if (stops.size() >= 2) {
                double minLat = Double.MAX_VALUE;
                double maxLat = -Double.MAX_VALUE;
                double minLon = Double.MAX_VALUE;
                double maxLon = -Double.MAX_VALUE;

                for (Stop stop : stops) {
                    minLat = Math.min(minLat, stop.getLat());
                    maxLat = Math.max(maxLat, stop.getLat());
                    minLon = Math.min(minLon, stop.getLon());
                    maxLon = Math.max(maxLon, stop.getLon());
                }

                centerLat = (minLat + maxLat) / 2.0;
                centerLon = (minLon + maxLon) / 2.0;

                double latSpan = maxLat - minLat;
                double lonSpan = maxLon - minLon;
                zoom = calculateOptimalZoom(latSpan, lonSpan);
            }
        }
    }

    private int calculateOptimalZoom(double latSpan, double lonSpan) {
        int width = getWidth() > 0 ? getWidth() : 800;
        int height = getHeight() > 0 ? getHeight() : 600;

        int zoomLat = (int) Math.floor(Math.log(height / 256.0 / latSpan) / Math.log(2));
        int zoomLon = (int) Math.floor(Math.log(width / 256.0 / lonSpan) / Math.log(2));

        int optimalZoom = Math.min(zoomLat, zoomLon);
        optimalZoom = Math.max(optimalZoom - 1, TilesManager.MIN_ZOOM);
        optimalZoom = Math.min(optimalZoom, TilesManager.MAX_ZOOM);

        return optimalZoom;
    }

    // === PAINT COMPONENT ===

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();

        drawTiles(g2d, width, height);
        drawRoutePath(g2d);
        drawRouteStops(g2d);

        boolean isCurrentlyLive = (realtimeManager != null &&
                ConnectionManager.getInstance().isOnline() &&
                realtimeManager.getVehiclePositions().size() > 0);

        if (isCurrentlyLive) {
            drawVehicles(g2d);
        } else {
            drawStaticVehicles(g2d);
        }

        drawSelectedStop(g2d);
        drawInfoPanel(g2d);
    }

    // === DISEGNO TILES ===

    private void drawTiles(Graphics2D g2d, int width, int height) {
        int[] centerTile = TilesManager.latLonToTile(centerLat, centerLon, zoom);
        int centerTileX = centerTile[0];
        int centerTileY = centerTile[1];

        double[] centerTileLatLon = TilesManager.tileToLatLon(centerTileX, centerTileY, zoom);
        double[] centerTileLatLonNext = TilesManager.tileToLatLon(centerTileX + 1, centerTileY + 1, zoom);

        int offsetX = (int) ((centerLon - centerTileLatLon[1]) / (centerTileLatLonNext[1] - centerTileLatLon[1]) * TilesManager.getTileSize());
        int offsetY = (int) ((centerLat - centerTileLatLon[0]) / (centerTileLatLonNext[0] - centerTileLatLon[0]) * TilesManager.getTileSize());

        int tilesX = (width / TilesManager.getTileSize()) + 2;
        int tilesY = (height / TilesManager.getTileSize()) + 2;

        for (int i = -tilesX / 2; i <= tilesX / 2; i++) {
            for (int j = -tilesY / 2; j <= tilesY / 2; j++) {
                int tileX = centerTileX + i;
                int tileY = centerTileY + j;

                int maxTile = (1 << zoom) - 1;
                if (tileX < 0 || tileX > maxTile || tileY < 0 || tileY > maxTile) {
                    continue;
                }

                BufferedImage tile = tilesManager.getTile(zoom, tileX, tileY);
                int drawX = width / 2 + (i * TilesManager.getTileSize()) - offsetX;
                int drawY = height / 2 + (j * TilesManager.getTileSize()) - offsetY;

                if (tile != null) {
                    g2d.drawImage(tile, drawX, drawY, TilesManager.getTileSize(), TilesManager.getTileSize(), null);
                } else {
                    g2d.setColor(new Color(230, 230, 230));
                    g2d.fillRect(drawX, drawY, TilesManager.getTileSize(), TilesManager.getTileSize());
                    g2d.setColor(Color.GRAY);
                    g2d.drawRect(drawX, drawY, TilesManager.getTileSize(), TilesManager.getTileSize());
                }
            }
        }
    }

    // === DISEGNO PERCORSO ===

    private void drawRoutePath(Graphics2D g2d) {
        if (currentlyFilteredRoute == null || stopController == null) {
            return;
        }

        Color routeColor = parseRouteColor(currentlyFilteredRoute.getRouteColor());
        g2d.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(routeColor.getRed(), routeColor.getGreen(), routeColor.getBlue(), 180));

        List<ShapePoint> shapePoints = stopController.getShapeForRoute(currentlyFilteredRoute);

        if (shapePoints.size() >= 2) {
            drawSmoothPath(g2d, shapePoints);
        } else {
            List<Stop> stops = getOrderedStopsForRoute(currentlyFilteredRoute);
            if (stops.size() >= 2) {
                drawStopPath(g2d, stops);
            }
        }
    }

    private void drawSmoothPath(Graphics2D g2d, List<ShapePoint> shapePoints) {
        Path2D.Double path = new Path2D.Double();
        Point startPoint = latLonToScreenPixel(shapePoints.get(0).getLat(), shapePoints.get(0).getLon());
        path.moveTo(startPoint.x, startPoint.y);

        for (int i = 1; i < shapePoints.size(); i++) {
            Point nextPoint = latLonToScreenPixel(shapePoints.get(i).getLat(), shapePoints.get(i).getLon());
            path.lineTo(nextPoint.x, nextPoint.y);
        }

        g2d.draw(path);
    }

    private void drawStopPath(Graphics2D g2d, List<Stop> stops) {
        Path2D.Double path = new Path2D.Double();
        Point startPoint = latLonToScreenPixel(stops.get(0).getLat(), stops.get(0).getLon());
        path.moveTo(startPoint.x, startPoint.y);

        for (int i = 1; i < stops.size(); i++) {
            Point nextPoint = latLonToScreenPixel(stops.get(i).getLat(), stops.get(i).getLon());
            path.lineTo(nextPoint.x, nextPoint.y);
        }

        g2d.draw(path);
    }

    // === DISEGNO FERMATE (CORRETTO!) ===

    private void drawRouteStops(Graphics2D g2d) {
        if (currentlyFilteredRoute == null || stopController == null) {
            return;
        }

        // ⭐ USA IL METODO ORDINATO INVECE DI getStopsForRoute()
        List<Stop> stops = getOrderedStopsForRoute(currentlyFilteredRoute);

        if (stops.isEmpty()) {
            System.out.println("⚠️ Nessuna fermata ordinata trovata per " + currentlyFilteredRoute.getName());
            return;
        }

        Color stopColor = new Color(255, 255, 255, 220);
        Color stopBorder = new Color(60, 60, 60);

        // Se c'è una fermata in hover, evidenziala
        Color hoverColor = new Color(255, 215, 0, 200);

        for (Stop stop : stops) {
            Point p = latLonToScreenPixel(stop.getLat(), stop.getLon());
            int x = p.x;
            int y = p.y;

            int size = 8;

            // Se è la fermata in hover, disegnala più grande
            if (hoveredStop != null && hoveredStop.getId().equals(stop.getId())) {
                g2d.setColor(hoverColor);
                g2d.fillOval(x - 10, y - 10, 20, 20);
                size = 10;
            }

            g2d.setColor(stopColor);
            g2d.fillOval(x - size / 2, y - size / 2, size, size);

            g2d.setColor(stopBorder);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawOval(x - size / 2, y - size / 2, size, size);
        }
    }

    private Color parseRouteColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return new Color(255, 0, 0);
        }

        try {
            if (hexColor.startsWith("#")) {
                hexColor = hexColor.substring(1);
            }

            int r = Integer.parseInt(hexColor.substring(0, 2), 16);
            int g = Integer.parseInt(hexColor.substring(2, 4), 16);
            int b = Integer.parseInt(hexColor.substring(4, 6), 16);

            return new Color(r, g, b);

        } catch (Exception e) {
            return new Color(255, 0, 0);
        }
    }

    // === DISEGNO VEICOLI ===

    private void drawVehicles(Graphics2D g2d) {
        if (realtimeManager == null || stopController == null) {
            return;
        }

        Collection<VehiclePosition> vehicles = realtimeManager.getVehiclePositions();
        if (vehicles.isEmpty()) {
            return;
        }

        AffineTransform oldTransform = g2d.getTransform();
        g2d.setFont(new Font("Arial", Font.BOLD, 10));

        for (VehiclePosition vehicle : vehicles) {
            if (currentlyFilteredRoute != null &&
                    !vehicle.getRouteId().equals(currentlyFilteredRoute.getId())) {
                continue;
            }

            Point p = latLonToScreenPixel(vehicle.getLatitude(), vehicle.getLongitude());
            int x = p.x;
            int y = p.y;
            float bearing = vehicle.getBearing();

            Route route = stopController.getRouteById(vehicle.getRouteId());
            String routeName = (route != null) ? route.getName() : vehicle.getRouteId();

            g2d.translate(x, y);
            if (bearing != -1f) {
                g2d.rotate(Math.toRadians(bearing));
            }

            Polygon triangle = new Polygon();
            triangle.addPoint(0, -8);
            triangle.addPoint(-6, 6);
            triangle.addPoint(6, 6);

            g2d.setColor(new Color(0, 120, 255));
            g2d.fill(triangle);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.draw(triangle);

            g2d.setTransform(oldTransform);

            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(x + 10, y - 6, routeName.length() * 7, 14, 4, 4);
            g2d.setColor(Color.BLACK);
            g2d.drawString(routeName, x + 12, y + 4);
        }

        g2d.setTransform(oldTransform);
    }

    private void drawStaticVehicles(Graphics2D g2d) {
        if (stopController == null) return;

        long now = System.currentTimeMillis();

        if (now - lastStaticCacheTime > 10000) {
            lastStaticCacheTime = now;
            new Thread(() -> {
                simulatedVehicleCache = stopController.getSimulatedVehiclePositions();
            }).start();
        }

        AffineTransform oldTransform = g2d.getTransform();
        g2d.setFont(new Font("Arial", Font.BOLD, 10));

        synchronized (simulatedVehicleCache) {
            for (VehiclePosition vehicle : simulatedVehicleCache) {
                if (currentlyFilteredRoute != null &&
                        !vehicle.getRouteId().equals(currentlyFilteredRoute.getId())) {
                    continue;
                }

                Point p = latLonToScreenPixel(vehicle.getLatitude(), vehicle.getLongitude());
                int x = p.x;
                int y = p.y;
                float bearing = vehicle.getBearing();

                Route route = stopController.getRouteById(vehicle.getRouteId());
                String routeName = (route != null) ? route.getName() : vehicle.getRouteId();

                g2d.translate(x, y);
                if (bearing != -1f) {
                    g2d.rotate(Math.toRadians(bearing));
                }

                Polygon triangle = new Polygon();
                triangle.addPoint(0, -8);
                triangle.addPoint(-6, 6);
                triangle.addPoint(6, 6);

                g2d.setColor(Color.DARK_GRAY);
                g2d.fill(triangle);
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.draw(triangle);

                g2d.setTransform(oldTransform);

                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(x + 10, y - 6, routeName.length() * 7, 14, 4, 4);
                g2d.setColor(Color.BLACK);
                g2d.drawString(routeName, x + 12, y + 4);
            }
        }

        g2d.setTransform(oldTransform);
    }

    private void drawSelectedStop(Graphics2D g2d) {
        if (selectedStop == null) {
            return;
        }

        Point p = latLonToScreenPixel(selectedStop.getLat(), selectedStop.getLon());
        int x = p.x;
        int y = p.y;

        g2d.setColor(new Color(255, 215, 0, 150));
        int haloSize = 20;
        g2d.fillOval(x - haloSize / 2, y - haloSize / 2, haloSize, haloSize);

        g2d.setColor(new Color(230, 100, 0));
        int dotSize = 10;
        g2d.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);
    }

    private void drawInfoPanel(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(10, 10, 180, 90, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Zoom: " + zoom, 20, 30);
        g2d.drawString(String.format("Lat: %.4f", centerLat), 20, 50);
        g2d.drawString(String.format("Lon: %.4f", centerLon), 20, 65);

        if (currentlyFilteredRoute != null) {
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("Linea: " + currentlyFilteredRoute.getName(), 20, 85);
        }
    }
        private Point latLonToScreenPixel(double lat, double lon) {
            int width = getWidth();
            int height = getHeight();

            int[] centerTile = TilesManager.latLonToTile(centerLat, centerLon, zoom);
            double[] centerTileLatLon = TilesManager.tileToLatLon(centerTile[0], centerTile[1], zoom);
            double[] centerTileLatLonNext = TilesManager.tileToLatLon(centerTile[0] + 1, centerTile[1] + 1, zoom);

            int offsetX = (int) ((centerLon - centerTileLatLon[1]) / (centerTileLatLonNext[1] - centerTileLatLon[1]) * TilesManager.getTileSize());
            int offsetY = (int) ((centerLat - centerTileLatLon[0]) / (centerTileLatLonNext[0] - centerTileLatLon[0]) * TilesManager.getTileSize());

            int centerTileScreenX = width / 2 - offsetX;
            int centerTileScreenY = height / 2 - offsetY;

            int[] vehicleTile = TilesManager.latLonToTile(lat, lon, zoom);
            double[] vehicleTileLatLon = TilesManager.tileToLatLon(vehicleTile[0], vehicleTile[1], zoom);
            double[] vehicleTileLatLonNext = TilesManager.tileToLatLon(vehicleTile[0] + 1, vehicleTile[1] + 1, zoom);

            int vehicleOffsetX = (int) ((lon - vehicleTileLatLon[1]) / (vehicleTileLatLonNext[1] - vehicleTileLatLon[1]) * TilesManager.getTileSize());
            int vehicleOffsetY = (int) ((lat - vehicleTileLatLon[0]) / (vehicleTileLatLonNext[0] - vehicleTileLatLon[0]) * TilesManager.getTileSize());

            int tileDiffX = vehicleTile[0] - centerTile[0];
            int tileDiffY = vehicleTile[1] - centerTile[1];

            int finalX = centerTileScreenX + (tileDiffX * TilesManager.getTileSize()) + vehicleOffsetX;
            int finalY = centerTileScreenY + (tileDiffY * TilesManager.getTileSize()) + vehicleOffsetY;

            return new Point(finalX, finalY);
        }
    }
