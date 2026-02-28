import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;

/**
 * Poly Pixelator — Windows 95/98 aesthetic (Sora's Pixel Converter style).
 *
 * Pipeline: Load -> True Block Downsampling (N×N avg) -> Redmean Palette Mapping
 * -> Optional 8×8 Bayer Ordered Dithering -> Optional Ghost / Outline FX
 * All processing uses BufferedImage with no external libraries.
 */
public class PolyPixelator extends JFrame {

    // ═══════════════════════════════════════════════════════════════════════════
    // PALETTES
    // ═══════════════════════════════════════════════════════════════════════════

    private static final Color[] PALETTE_FRUTIGER_AERO = {
        new Color(0x1a, 0x3d, 0x5c), new Color(0x2d, 0x6b, 0x9a), new Color(0x4a, 0x9f, 0xd4),
        new Color(0x7e, 0xb8, 0xda), new Color(0xa8, 0xcf, 0xe8), new Color(0xd0, 0xe8, 0xf5),
        new Color(0xec, 0xf4, 0xfa), new Color(0xff, 0xff, 0xff),
        new Color(0x2e, 0xcc, 0x71), new Color(0x34, 0x98, 0xdb), new Color(0x9b, 0x59, 0xb6),
        new Color(0xe7, 0x4c, 0x3c), new Color(0xf3, 0x9c, 0x12), new Color(0x1a, 0xbc, 0x9c),
        new Color(0xfd, 0xad, 0xb9), new Color(0xb8, 0xe0, 0xf0),
    };

    private static final Color[] PALETTE_PICO8 = {
        new Color(0x00, 0x00, 0x00), new Color(0x1d, 0x2b, 0x53), new Color(0x7e, 0x25, 0x53),
        new Color(0x00, 0x87, 0x51), new Color(0xab, 0x52, 0x36), new Color(0x5f, 0x57, 0x4f),
        new Color(0xc2, 0xc3, 0xc7), new Color(0xff, 0xf1, 0xe8), new Color(0xff, 0x00, 0x4d),
        new Color(0xff, 0xa3, 0x00), new Color(0xff, 0xec, 0x27), new Color(0x00, 0xe4, 0x36),
        new Color(0x29, 0xad, 0xff), new Color(0x83, 0x76, 0x9c), new Color(0xff, 0x77, 0xa8),
        new Color(0xff, 0xcc, 0xaa),
    };

    private static final Color[] PALETTE_CGA = {
        new Color(0x00, 0x00, 0x00), new Color(0x55, 0x55, 0x55), new Color(0xaa, 0xaa, 0xaa),
        new Color(0xff, 0xff, 0xff), new Color(0x00, 0x00, 0xaa), new Color(0x55, 0x55, 0xff),
        new Color(0x00, 0xaa, 0x00), new Color(0x55, 0xff, 0x55), new Color(0x00, 0xaa, 0xaa),
        new Color(0x55, 0xff, 0xff), new Color(0xaa, 0x00, 0x00), new Color(0xff, 0x55, 0x55),
        new Color(0xaa, 0x00, 0xaa), new Color(0xff, 0x55, 0xff), new Color(0xaa, 0x55, 0x00),
        new Color(0xff, 0xff, 0x55),
    };

    private static final Color[] PALETTE_C64 = {
        new Color(0x00, 0x00, 0x00), new Color(0xff, 0xff, 0xff), new Color(0x88, 0x39, 0x32),
        new Color(0x67, 0xb6, 0xbd), new Color(0x8b, 0x3f, 0x96), new Color(0x55, 0xa0, 0x49),
        new Color(0x40, 0x31, 0x8d), new Color(0xbf, 0xce, 0x72), new Color(0x8b, 0x54, 0x29),
        new Color(0x57, 0x42, 0x00), new Color(0xb8, 0x69, 0x62), new Color(0x50, 0x50, 0x50),
        new Color(0x78, 0x78, 0x78), new Color(0x94, 0xe0, 0x89), new Color(0x78, 0x69, 0xc4),
        new Color(0x9f, 0x9f, 0x9f),
    };

    private static final Color[] PALETTE_WINDOWS95 = {
        new Color(0x00, 0x00, 0x00), new Color(0x00, 0x80, 0x80), new Color(0x80, 0x00, 0x00),
        new Color(0x00, 0x00, 0x80), new Color(0x80, 0x80, 0x00), new Color(0x80, 0x00, 0x80),
        new Color(0x00, 0x80, 0x00), new Color(0x80, 0x80, 0x80), new Color(0xc0, 0xc0, 0xc0),
        new Color(0x00, 0xff, 0xff), new Color(0xff, 0x00, 0x00), new Color(0x00, 0x00, 0xff),
        new Color(0xff, 0xff, 0x00), new Color(0xff, 0x00, 0xff), new Color(0x00, 0xff, 0x00),
        new Color(0xff, 0xff, 0xff),
    };

    private static final Color[] PALETTE_VAPORWAVE = {
        new Color(0x0d, 0x02, 0x22), new Color(0x1a, 0x05, 0x3d), new Color(0x2d, 0x0a, 0x5c),
        new Color(0x4b, 0x0d, 0x8a), new Color(0xff, 0x71, 0xce), new Color(0x01, 0xcd, 0xfe),
        new Color(0x05, 0xff, 0xa1), new Color(0xb9, 0x67, 0xff), new Color(0xff, 0xfb, 0x96),
        new Color(0xff, 0x6e, 0xb4), new Color(0x7d, 0xf9, 0xff), new Color(0x39, 0xff, 0x14),
        new Color(0xe0, 0xac, 0x69), new Color(0x94, 0x00, 0xd3), new Color(0x00, 0x00, 0x80),
        new Color(0xff, 0x14, 0x93),
    };

    private static final Color[] PALETTE_APPLEII = {
        new Color(0x00, 0x00, 0x00), new Color(0x55, 0x55, 0x55), new Color(0xff, 0xff, 0xff),
        new Color(0x00, 0x88, 0x00), new Color(0x00, 0xff, 0x00), new Color(0x00, 0x00, 0xff),
        new Color(0x55, 0x55, 0xff), new Color(0xff, 0x00, 0x00), new Color(0xff, 0x55, 0x55),
        new Color(0xff, 0x88, 0x00), new Color(0xff, 0xff, 0x00), new Color(0x88, 0x00, 0x88),
        new Color(0xff, 0x00, 0xff), new Color(0x00, 0x88, 0x88), new Color(0x00, 0xff, 0xff),
        new Color(0xaa, 0xaa, 0xaa),
    };

    private static final Color[] PALETTE_CYBERPUNK = {
        new Color(0x0a, 0x0a, 0x12), new Color(0x12, 0x12, 0x25), new Color(0x1e, 0x1e, 0x3c),
        new Color(0x2a, 0x2a, 0x50), new Color(0xff, 0x00, 0x99), new Color(0x00, 0xff, 0xee),
        new Color(0xff, 0x66, 0x00), new Color(0x99, 0x00, 0xff), new Color(0x00, 0xcc, 0xff),
        new Color(0xff, 0x00, 0xff), new Color(0x33, 0xff, 0x99), new Color(0xff, 0xcc, 0x00),
        new Color(0x66, 0x33, 0xff), new Color(0xff, 0x33, 0x66), new Color(0x66, 0xff, 0xff),
        new Color(0xff, 0xff, 0x00),
    };

    private static final Color[] PALETTE_MONOCHROME = {
        new Color(0x00, 0x00, 0x00), new Color(0x11, 0x11, 0x11), new Color(0x22, 0x22, 0x22),
        new Color(0x33, 0x33, 0x33), new Color(0x44, 0x44, 0x44), new Color(0x55, 0x55, 0x55),
        new Color(0x66, 0x66, 0x66), new Color(0x77, 0x77, 0x77), new Color(0x88, 0x88, 0x88),
        new Color(0x99, 0x99, 0x99), new Color(0xaa, 0xaa, 0xaa), new Color(0xbb, 0xbb, 0xbb),
        new Color(0xcc, 0xcc, 0xcc), new Color(0xdd, 0xdd, 0xdd), new Color(0xee, 0xee, 0xee),
        new Color(0xff, 0xff, 0xff),
    };

    private static final Color[][] PALETTES = {
        PALETTE_FRUTIGER_AERO, PALETTE_PICO8, PALETTE_CGA,
        PALETTE_C64, PALETTE_WINDOWS95, PALETTE_VAPORWAVE, PALETTE_APPLEII,
        PALETTE_CYBERPUNK, PALETTE_MONOCHROME,
    };
    private static final String[] PALETTE_NAMES = {
        "Frutiger Aero", "PICO-8", "CGA", "C64",
        "Windows 95", "Vaporwave", "Apple II", "Cyberpunk", "Monochrome",
    };

    private static final Color WIN_GRAY = new Color(0xC0, 0xC0, 0xC0);
    private static final Color NAVY = new Color(0x00, 0x00, 0x80);
    private static final Font WIN_FONT = new Font("Dialog", Font.PLAIN, 12);

    private static final int[][] BAYER_8X8 = {
        {  0, 32,  8, 40,  2, 34, 10, 42 },
        { 48, 16, 56, 24, 50, 18, 58, 26 },
        { 12, 44,  4, 36, 14, 46,  6, 38 },
        { 60, 28, 52, 20, 62, 30, 54, 22 },
        {  3, 35, 11, 43,  1, 33,  9, 41 },
        { 51, 19, 59, 27, 49, 17, 57, 25 },
        { 15, 47,  7, 39, 13, 45,  5, 37 },
        { 63, 31, 55, 23, 61, 29, 53, 21 },
    };

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════

    private BufferedImage originalImage;
    private BufferedImage processedImage;
    private volatile int blockSize = 16;
    private volatile boolean ditherEnabled = false;
    private volatile boolean ghostEnabled = false;
    private volatile boolean outlineEnabled = false;
    private volatile int paletteIndex = 0;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    private final ImageCanvas canvas;
    private final JLabel statusLabel;
    private volatile String backgroundMode = "greenish"; // "greenish" (default) or "checkered"

    // Dialog overlay state
    private volatile boolean dialogEnabled = false;
    private volatile boolean dialogStyleJRPG = true; // true = JRPG Box, false = Terminal
    private volatile int dialogVerticalPos = 80;
    private JTextField dialogNameField;
    private JTextArea dialogTextArea;

    // ═══════════════════════════════════════════════════════════════════════════
    // UI BUILD (Windows 95/98)
    // ═══════════════════════════════════════════════════════════════════════════

    public PolyPixelator() {
        super("Poly Pixelator — Sora's Pixel Converter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        SwitchableBackgroundPanel root = new SwitchableBackgroundPanel(new BorderLayout(0, 0), () -> backgroundMode);

        // ── CENTER: Image Canvas + Right sidebar ─────────────────────────────
        JPanel centerWrap = new SwitchableBackgroundPanel(new BorderLayout(0, 0), () -> backgroundMode);
        canvas = new ImageCanvas();
        JScrollPane canvasScroll = new JScrollPane(canvas);
        canvasScroll.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        canvasScroll.setViewportView(new CheckerboardCanvasContainer(canvas));
        canvasScroll.getViewport().setBackground(WIN_GRAY);

        centerWrap.add(canvasScroll, BorderLayout.CENTER);

        // Right sidebar: Background toggle
        JPanel rightSidebar = new SwitchableBackgroundPanel(new BorderLayout(0, 0), () -> backgroundMode);
        rightSidebar.setPreferredSize(new Dimension(220, 0));
        rightSidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY));
        JPanel rightContent = new JPanel(new BorderLayout(0, 4));
        rightContent.setOpaque(false);
        rightContent.add(createTitledSection("Background colours", createBackgroundOptions()), BorderLayout.NORTH);
        rightContent.add(createTitledSection("DIALOG", createDialogPanel()), BorderLayout.CENTER);
        rightSidebar.add(rightContent, BorderLayout.CENTER);
        centerWrap.add(rightSidebar, BorderLayout.EAST);

        root.add(centerWrap, BorderLayout.CENTER);

        // ── SOUTH: Bottom taskbar dock — all controls ───────────────────────
        JPanel bottomDock = new SwitchableBackgroundPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), () -> backgroundMode);

        // Load / Save
        JPanel fileSection = createGrayPanel();
        JButton loadBtn = createWinButton("Load");
        loadBtn.addActionListener(e -> loadImage());
        JButton saveBtn = createWinButton("Save PNG");
        saveBtn.addActionListener(e -> saveImage());
        fileSection.add(loadBtn);
        fileSection.add(saveBtn);
        bottomDock.add(fileSection);

        addDockSeparator(bottomDock);

        // Size — 4, 8, 16, 32, 128px
        JPanel sizeContent = createGrayPanel();
        ButtonGroup sizeGroup = new ButtonGroup();
        int[] sizes = { 4, 8, 16, 32, 128 };
        for (int sz : sizes) {
            JToggleButton btn = createWinToggleButton(sz + "px");
            final int s = sz;
            btn.addActionListener(e -> { blockSize = s; scheduleProcess(); });
            sizeGroup.add(btn);
            sizeContent.add(btn);
            if (sz == 16) btn.setSelected(true);
        }
        bottomDock.add(createTitledSection("Size", sizeContent));

        addDockSeparator(bottomDock);

        // FX — Dither, Ghost, Outline
        JPanel fxContent = createGrayPanel();
        JToggleButton ditherBtn = createWinToggleButton("Dither");
        ditherBtn.addActionListener(e -> {
            ditherEnabled = ditherBtn.isSelected();
            scheduleProcess();
        });
        JToggleButton ghostBtn = createWinToggleButton("Ghost");
        ghostBtn.addActionListener(e -> {
            ghostEnabled = ghostBtn.isSelected();
            scheduleProcess();
        });
        JToggleButton outlineBtn = createWinToggleButton("Outline");
        outlineBtn.addActionListener(e -> {
            outlineEnabled = outlineBtn.isSelected();
            scheduleProcess();
        });
        fxContent.add(ditherBtn);
        fxContent.add(ghostBtn);
        fxContent.add(outlineBtn);
        bottomDock.add(createTitledSection("FX", fxContent));

        addDockSeparator(bottomDock);

        // Palette (LOWERED bevel = sunken inset)
        JPanel paletteGrid = createPaletteGrid();
        paletteGrid.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        JScrollPane paletteScroll = new JScrollPane(paletteGrid);
        paletteScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        paletteScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        paletteScroll.setBorder(BorderFactory.createEmptyBorder());
        paletteScroll.getViewport().setBackground(WIN_GRAY);
        paletteScroll.setPreferredSize(new Dimension(380, 58));
        JPanel paletteContent = createGrayPanel();
        paletteContent.add(paletteScroll);
        bottomDock.add(createTitledSection("Palette", paletteContent));

        // ── South: dock + status bar ───────────────────────────────────────
        JPanel southContainer = new SwitchableBackgroundPanel(new BorderLayout(0, 0), () -> backgroundMode);
        southContainer.add(bottomDock, BorderLayout.CENTER);

        statusLabel = new JLabel(" Ready — load an image to begin");
        statusLabel.setFont(WIN_FONT);
        statusLabel.setForeground(Color.BLACK);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        statusLabel.setBackground(WIN_GRAY);
        statusLabel.setOpaque(true);
        southContainer.add(statusLabel, BorderLayout.SOUTH);
        root.add(southContainer, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(1000, 680);
        setMinimumSize(new Dimension(760, 500));
        setLocationRelativeTo(null);
    }

    private JPanel createGrayPanel() {
        JPanel p = new PatternedPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        return p;
    }

    private JPanel createTitledSection(String title, JComponent content) {
        JPanel outer = new PatternedPanel(new BorderLayout(0, 0));

        JLabel titleBar = new JLabel(" " + title);
        titleBar.setBackground(NAVY);
        titleBar.setForeground(Color.WHITE);
        titleBar.setOpaque(true);
        titleBar.setFont(WIN_FONT.deriveFont(Font.BOLD));

        outer.add(titleBar, BorderLayout.NORTH);
        outer.add(content, BorderLayout.CENTER);
        return outer;
    }

    private JButton createWinButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(WIN_FONT);
        btn.setBackground(WIN_GRAY);
        btn.setForeground(Color.BLACK);
        btn.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        btn.setFocusPainted(false);
        return btn;
    }

    private JToggleButton createWinToggleButton(String text) {
        JToggleButton btn = new Win95ToggleButton(text);
        return btn;
    }

    private JPanel createBackgroundOptions() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        ButtonGroup bgGroup = new ButtonGroup();
        JToggleButton greenBtn = createWinToggleButton("piss 1");
        greenBtn.setPreferredSize(new Dimension(120, 48));
        greenBtn.setMinimumSize(new Dimension(120, 48));
        greenBtn.setMaximumSize(new Dimension(200, 48));
        greenBtn.setSelected(true);
        greenBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        greenBtn.addActionListener(e -> {
            backgroundMode = "greenish";
            getContentPane().repaint();
        });
        JToggleButton checkBtn = createWinToggleButton("piss 2");
        checkBtn.setPreferredSize(new Dimension(120, 48));
        checkBtn.setMinimumSize(new Dimension(120, 48));
        checkBtn.setMaximumSize(new Dimension(200, 48));
        checkBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkBtn.addActionListener(e -> {
            backgroundMode = "checkered";
            getContentPane().repaint();
        });
        bgGroup.add(greenBtn);
        bgGroup.add(checkBtn);
        content.add(greenBtn);
        content.add(Box.createVerticalStrut(8));
        content.add(checkBtn);
        return content;
    }

    private JPanel createDialogPanel() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(WIN_GRAY);
        content.setOpaque(true);
        content.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createBevelBorder(BevelBorder.LOWERED),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));

        DocumentListener docListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { triggerDialogRedraw(); }
            @Override public void removeUpdate(DocumentEvent e) { triggerDialogRedraw(); }
            @Override public void changedUpdate(DocumentEvent e) { triggerDialogRedraw(); }
        };

        JCheckBox onOffCheck = new JCheckBox("ON / OFF", false);
        onOffCheck.setFont(WIN_FONT);
        onOffCheck.setForeground(Color.BLACK);
        onOffCheck.setOpaque(false);
        onOffCheck.addActionListener(e -> {
            dialogEnabled = onOffCheck.isSelected();
            triggerDialogRedraw();
        });

        ButtonGroup styleGroup = new ButtonGroup();
        JToggleButton jrpgBtn = createWinToggleButton("JRPG Box");
        jrpgBtn.setSelected(true);
        jrpgBtn.addActionListener(e -> { dialogStyleJRPG = true; triggerDialogRedraw(); });
        JToggleButton termBtn = createWinToggleButton("Terminal");
        termBtn.addActionListener(e -> { dialogStyleJRPG = false; triggerDialogRedraw(); });
        styleGroup.add(jrpgBtn);
        styleGroup.add(termBtn);

        JLabel nameLbl = new JLabel("Name (optional)");
        nameLbl.setFont(WIN_FONT);
        dialogNameField = new JTextField(14);
        dialogNameField.setFont(WIN_FONT);
        dialogNameField.setMaximumSize(new Dimension(200, 24));
        dialogNameField.getDocument().addDocumentListener(docListener);

        JLabel textLbl = new JLabel("Text");
        textLbl.setFont(WIN_FONT);
        dialogTextArea = new JTextArea(3, 14);
        dialogTextArea.setFont(WIN_FONT);
        dialogTextArea.setLineWrap(true);
        dialogTextArea.setWrapStyleWord(true);
        dialogTextArea.getDocument().addDocumentListener(docListener);

        JLabel vertLbl = new JLabel("Vertical Position");
        vertLbl.setFont(WIN_FONT);
        JSlider vertSlider = new JSlider(0, 100, 80);
        vertSlider.setPreferredSize(new Dimension(180, 28));
        vertSlider.addChangeListener(e -> {
            dialogVerticalPos = vertSlider.getValue();
            triggerDialogRedraw();
        });

        onOffCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        jrpgBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        termBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        dialogNameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        textLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        vertLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        vertSlider.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(onOffCheck);
        content.add(Box.createVerticalStrut(6));
        content.add(jrpgBtn);
        content.add(termBtn);
        content.add(Box.createVerticalStrut(6));
        content.add(nameLbl);
        content.add(dialogNameField);
        content.add(Box.createVerticalStrut(4));
        content.add(textLbl);
        JScrollPane textScroll = new JScrollPane(dialogTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        textScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(textScroll);
        content.add(Box.createVerticalStrut(4));
        content.add(vertLbl);
        content.add(vertSlider);

        return content;
    }

    private void triggerDialogRedraw() {
        if (canvas != null && dialogNameField != null && dialogTextArea != null) {
            canvas.updateDialogParams(dialogEnabled, dialogStyleJRPG,
                dialogNameField.getText(), dialogTextArea.getText(), dialogVerticalPos);
        }
    }

    private void addDockSeparator(JPanel dock) {
        JPanel sep = new PatternedPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        sep.setPreferredSize(new Dimension(2, 36));
        JLabel line = new JLabel();
        line.setPreferredSize(new Dimension(1, 24));
        line.setOpaque(true);
        line.setBackground(Color.GRAY);
        sep.add(line);
        dock.add(sep);
    }

    private JPanel createPaletteGrid() {
        JPanel grid = new PatternedPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        ButtonGroup palGroup = new ButtonGroup();
        for (int i = 0; i < PALETTES.length; i++) {
            JToggleButton btn = new Win95PaletteButton(PALETTE_NAMES[i], PALETTES[i]);
            final int idx = i;
            btn.addActionListener(e -> {
                paletteIndex = idx;
                scheduleProcess();
            });
            palGroup.add(btn);
            grid.add(btn);
            if (i == 0) btn.setSelected(true);
        }
        return grid;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILE I/O
    // ═══════════════════════════════════════════════════════════════════════════

    private void loadImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Images (png, jpg, bmp, gif)", "png", "jpg", "jpeg", "bmp", "gif"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage raw = ImageIO.read(fc.getSelectedFile());
            if (raw == null) throw new Exception("Unsupported format");
            originalImage = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = originalImage.createGraphics();
            g.drawImage(raw, 0, 0, null);
            g.dispose();
            statusLabel.setText(" Loaded: " + fc.getSelectedFile().getName() + "  (" + originalImage.getWidth() + " × " + originalImage.getHeight() + ")");
            scheduleProcess();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load image:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveImage() {
        if (processedImage == null) {
            JOptionPane.showMessageDialog(this, "No processed image to save.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("pixelated.png"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage toSave = processedImage;
                if (dialogEnabled && dialogNameField != null && dialogTextArea != null) {
                    toSave = applyDialogToImage(processedImage, dialogNameField.getText(), dialogTextArea.getText());
                }
                ImageIO.write(toSave, "png", fc.getSelectedFile());
                statusLabel.setText(" Saved to " + fc.getSelectedFile().getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private BufferedImage applyDialogToImage(BufferedImage src, String name, String text) {
        if (name == null) name = "";
        if (text == null) text = "";
        if (name.isEmpty() && text.isEmpty()) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(src, 0, 0, null);
        drawDialogOntoGraphics(g2, src.getWidth(), src.getHeight(), name, text);
        g2.dispose();
        return out;
    }

    private void drawDialogOntoGraphics(Graphics2D g2, int imgW, int imgH, String name, String text) {
        int margin = 20;
        int boxY = (int) (imgH * (dialogVerticalPos / 100.0));
        int boxW = imgW - 2 * margin;
        int lineH = Math.max(14, imgH / 25);
        int padding = lineH / 2;
        int boxH = padding * 2 + lineH * 3;
        boxY = Math.max(0, Math.min(boxY, imgH - boxH - padding));
        int boxX = margin;

        if (dialogStyleJRPG) {
            g2.setColor(Color.BLACK);
            g2.fillRect(boxX, boxY, boxW, boxH);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(boxX + 2, boxY + 2, boxW - 4, boxH - 4);
            g2.setStroke(new BasicStroke(1));
            int fontSize = Math.max(10, Math.min(24, imgH / 20));
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
            FontMetrics fm = g2.getFontMetrics();
            int textX = boxX + padding + 3;
            int textY = boxY + padding + fm.getAscent();
            if (!name.isEmpty()) {
                g2.setColor(Color.YELLOW);
                g2.drawString(name, textX, textY);
                textY += lineH;
            }
            g2.setColor(Color.WHITE);
            for (String line : text.split("\n")) {
                if (textY > boxY + boxH - padding) break;
                g2.drawString(line, textX, textY);
                textY += lineH;
            }
        } else {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            g2.setColor(Color.BLACK);
            g2.fillRect(boxX, boxY, boxW, boxH);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            int fontSize = Math.max(10, Math.min(20, imgH / 25));
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
            g2.setColor(new Color(0x00, 0xFF, 0x00));
            FontMetrics fm = g2.getFontMetrics();
            int textX = boxX + padding;
            int textY = boxY + padding + fm.getAscent();
            if (!name.isEmpty()) {
                g2.drawString(name, textX, textY);
                textY += lineH;
            }
            for (String line : text.split("\n")) {
                if (textY > boxY + boxH - padding) break;
                g2.drawString(line, textX, textY);
                textY += lineH;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROCESSING (unchanged — Redmean, block downsampling, Bayer dither)
    // ═══════════════════════════════════════════════════════════════════════════

    private void scheduleProcess() {
        if (originalImage == null) return;
        if (!processing.compareAndSet(false, true)) return;

        final int block = blockSize;
        final boolean dither = ditherEnabled;
        final boolean ghost = ghostEnabled;
        final boolean outline = outlineEnabled;
        final int palIdx = paletteIndex;

        SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                long t0 = System.nanoTime();
                BufferedImage downsampled = downsampleBlocks(originalImage, block);
                BufferedImage result = applyPaletteWithDither(downsampled, PALETTES[palIdx], dither);
                if (outline) result = applyOutline(result, block, PALETTES[palIdx]);
                if (ghost) result = applyGhost(result, 10, 10, 0.3f);
                long ms = (System.nanoTime() - t0) / 1_000_000;
                final String msg = " Processed in " + ms + " ms  |  Block: " + block + "px  |  Dither: " + (dither ? "ON" : "OFF") + "  |  Ghost: " + (ghost ? "ON" : "OFF") + "  |  Outline: " + (outline ? "ON" : "OFF") + "  |  Palette: " + PALETTE_NAMES[palIdx];
                SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
                return result;
            }

            @Override
            protected void done() {
                try {
                    processedImage = get();
                    canvas.setImage(processedImage);
                    triggerDialogRedraw();
                } catch (Exception ignored) {}
                processing.set(false);
            }
        };
        worker.execute();
    }

    private BufferedImage downsampleBlocks(BufferedImage src, int blockSize) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int by = 0; by < h; by += blockSize) {
            for (int bx = 0; bx < w; bx += blockSize) {
                int bw = Math.min(blockSize, w - bx);
                int bh = Math.min(blockSize, h - by);
                long rSum = 0, gSum = 0, bSum = 0;
                int count = bw * bh;

                for (int y = by; y < by + bh; y++) {
                    for (int x = bx; x < bx + bw; x++) {
                        int rgb = src.getRGB(x, y);
                        rSum += (rgb >> 16) & 0xFF;
                        gSum += (rgb >> 8) & 0xFF;
                        bSum += rgb & 0xFF;
                    }
                }

                int avgR = (int) (rSum / count);
                int avgG = (int) (gSum / count);
                int avgB = (int) (bSum / count);
                int avgRgb = (avgR << 16) | (avgG << 8) | avgB;

                for (int y = by; y < by + bh; y++) {
                    for (int x = bx; x < bx + bw; x++) {
                        out.setRGB(x, y, avgRgb);
                    }
                }
            }
        }
        return out;
    }

    private BufferedImage applyPaletteWithDither(BufferedImage src, Color[] palette, boolean dither) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int by = 0; by < h; by += blockSize) {
            for (int bx = 0; bx < w; bx += blockSize) {
                int bw = Math.min(blockSize, w - bx);
                int bh = Math.min(blockSize, h - by);
                long rSum = 0, gSum = 0, bSum = 0;
                int count = bw * bh;
                for (int y = by; y < by + bh; y++) {
                    for (int x = bx; x < bx + bw; x++) {
                        int rgb = src.getRGB(x, y);
                        rSum += (rgb >> 16) & 0xFF;
                        gSum += (rgb >> 8) & 0xFF;
                        bSum += rgb & 0xFF;
                    }
                }
                float avgR = (float) rSum / count;
                float avgG = (float) gSum / count;
                float avgB = (float) bSum / count;

                if (dither) {
                    float t = (BAYER_8X8[by % 8][bx % 8] / 64.0f) - 0.5f;
                    float bias = t * 48f;
                    avgR = clamp(avgR + bias);
                    avgG = clamp(avgG + bias);
                    avgB = clamp(avgB + bias);
                }

                Color nearest = findNearestRedmean(avgR, avgG, avgB, palette);
                int rgb = nearest.getRGB();

                for (int y = by; y < by + bh; y++) {
                    for (int x = bx; x < bx + bw; x++) {
                        out.setRGB(x, y, rgb);
                    }
                }
            }
        }
        return out;
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static Color findNearestRedmean(float r, float g, float b, Color[] palette) {
        Color best = palette[0];
        double bestDist = Double.MAX_VALUE;
        for (Color c : palette) {
            int cr = c.getRed(), cg = c.getGreen(), cb = c.getBlue();
            double dr = r - cr, dg = g - cg, db = b - cb;
            double rBar = (r + cr) / 2.0;
            double termR = (2.0 + rBar / 256.0) * dr * dr;
            double termG = 4.0 * dg * dg;
            double termB = (2.0 + (255.0 - rBar) / 256.0) * db * db;
            double dist = Math.sqrt(termR + termG + termB);
            if (dist < bestDist) {
                bestDist = dist;
                best = c;
            }
        }
        return best;
    }

    /** Ghost FX: blend image with offset semi-transparent copy (motion-trail). */
    private BufferedImage applyGhost(BufferedImage src, int offsetX, int offsetY, float alpha) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(src, offsetX, offsetY, null);
        g.dispose();
        BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(out, 0, 0, null);
        return rgb;
    }

    /** Outline FX: draw dark lines on edges between blocks with different colors (cel-shaded). */
    private BufferedImage applyOutline(BufferedImage src, int blockSize, Color[] palette) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        Color outlineColor = palette.length > 0 ? palette[0] : Color.BLACK;
        if (outlineColor.getRed() + outlineColor.getGreen() + outlineColor.getBlue() > 128) {
            outlineColor = new Color(
                Math.max(0, outlineColor.getRed() - 80),
                Math.max(0, outlineColor.getGreen() - 80),
                Math.max(0, outlineColor.getBlue() - 80)
            );
        }
        int outlineRgb = outlineColor.getRGB();
        int lineW = blockSize >= 16 ? 2 : 1;

        for (int by = 0; by < h; by += blockSize) {
            for (int bx = 0; bx < w; bx += blockSize) {
                int cur = src.getRGB(bx, by);
                int nRight = bx + blockSize < w ? src.getRGB(bx + blockSize, by) : cur;
                int nBottom = by + blockSize < h ? src.getRGB(bx, by + blockSize) : cur;

                if (cur != nRight) {
                    for (int dy = 0; dy < Math.min(blockSize, h - by); dy++) {
                        for (int d = 0; d < lineW && bx + blockSize + d < w; d++) {
                            out.setRGB(bx + blockSize + d, by + dy, outlineRgb);
                        }
                    }
                }
                if (cur != nBottom) {
                    for (int dx = 0; dx < Math.min(blockSize, w - bx); dx++) {
                        for (int d = 0; d < lineW && by + blockSize + d < h; d++) {
                            out.setRGB(bx + dx, by + blockSize + d, outlineRgb);
                        }
                    }
                }
            }
        }
        return out;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WIN95 COMPONENTS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Panel that switches between greenish (default) and checkered background. */
    private static class SwitchableBackgroundPanel extends JPanel {
        private static final int TILE = 12;
        private static final Color GREEN = new Color(0x00, 0x60, 0x44);
        private static final Color DARK = new Color(0x58, 0x58, 0x58);
        private static final Color LIGHT = new Color(0x48, 0x48, 0x48);
        private final java.util.function.Supplier<String> modeSupplier;
        private TexturePaint checkeredTexture;

        SwitchableBackgroundPanel(LayoutManager lm, java.util.function.Supplier<String> modeSupplier) {
            super(lm);
            this.modeSupplier = modeSupplier;
            setOpaque(true);
            setBackground(GREEN);
            BufferedImage tile = new BufferedImage(TILE * 2, TILE * 2, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = tile.createGraphics();
            g.setColor(DARK);
            g.fillRect(0, 0, TILE * 2, TILE * 2);
            g.setColor(LIGHT);
            g.fillRect(0, 0, TILE, TILE);
            g.fillRect(TILE, TILE, TILE, TILE);
            g.dispose();
            checkeredTexture = new TexturePaint(tile, new Rectangle2D.Float(0, 0, TILE * 2, TILE * 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            String mode = modeSupplier.get();
            if ("checkered".equals(mode)) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(checkeredTexture);
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else {
                g.setColor(GREEN);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            super.paintComponent(g);
        }
    }

    /** Container that paints a dark checkerboard behind the canvas and centers it. */
    private static class CheckerboardCanvasContainer extends JPanel implements Scrollable {
        private static final int CHECK_SIZE = 8;
        private static final Color DARK = new Color(0x60, 0x60, 0x60);
        private static final Color LIGHT = new Color(0x40, 0x40, 0x40);
        private final JComponent canvas;

        CheckerboardCanvasContainer(JComponent canvas) {
            super(new GridBagLayout());
            this.canvas = canvas;
            setOpaque(true);
            add(canvas, new GridBagConstraints());
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            for (int y = 0; y < h; y += CHECK_SIZE) {
                for (int x = 0; x < w; x += CHECK_SIZE) {
                    g.setColor(((x / CHECK_SIZE) + (y / CHECK_SIZE)) % 2 == 0 ? DARK : LIGHT);
                    g.fillRect(x, y, CHECK_SIZE, CHECK_SIZE);
                }
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension canvasSize = canvas.getPreferredSize();
            Container parent = getParent();
            if (parent instanceof javax.swing.JViewport) {
                javax.swing.JViewport vp = (javax.swing.JViewport) parent;
                return new Dimension(
                    Math.max(vp.getWidth(), canvasSize.width),
                    Math.max(vp.getHeight(), canvasSize.height)
                );
            }
            return canvasSize;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof javax.swing.JViewport
                && canvas.getPreferredSize().width <= ((javax.swing.JViewport) getParent()).getWidth();
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof javax.swing.JViewport
                && canvas.getPreferredSize().height <= ((javax.swing.JViewport) getParent()).getHeight();
        }
    }

    /** Panel with subtle repeating dot-matrix pattern. */
    private static class PatternedPanel extends JPanel {
        private static final int TILE = 16;
        private static final Color BASE = new Color(0xC4, 0xC4, 0xC4);
        private static final Color DOT  = new Color(0xD4, 0xD4, 0xD4);
        private TexturePaint texture;

        PatternedPanel(LayoutManager lm) {
            super(lm);
            setOpaque(true);
            BufferedImage tile = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = tile.createGraphics();
            g.setColor(BASE);
            g.fillRect(0, 0, TILE, TILE);
            g.setColor(DOT);
            for (int y = 0; y < TILE; y += 4) {
                for (int x = 0; x < TILE; x += 4) {
                    g.fillRect(x, y, 1, 1);
                }
            }
            g.dispose();
            texture = new TexturePaint(tile, new Rectangle2D.Float(0, 0, TILE, TILE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(texture);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /** Toggle button: RAISED when unselected, LOWERED when selected (pressed-in look). */
    private static class Win95ToggleButton extends JToggleButton {
        Win95ToggleButton(String text) {
            super(text);
            setFont(WIN_FONT);
            setBackground(WIN_GRAY);
            setForeground(Color.BLACK);
            setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            setFocusPainted(false);
        }

        @Override
        public void setSelected(boolean b) {
            super.setSelected(b);
            setBorder(BorderFactory.createBevelBorder(b ? BevelBorder.LOWERED : BevelBorder.RAISED));
        }
    }

    /** Palette button with color swatch, Win95 style (RAISED border, #C0C0C0). */
    private static class Win95PaletteButton extends JToggleButton {
        private final Color[] palette;

        Win95PaletteButton(String name, Color[] palette) {
            super(name);
            this.palette = palette;
            setFont(WIN_FONT);
            setPreferredSize(new Dimension(70, 48));
            setBackground(WIN_GRAY);
            setForeground(Color.BLACK);
            setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            setFocusPainted(false);
        }

        @Override
        public void setSelected(boolean b) {
            super.setSelected(b);
            setBorder(BorderFactory.createBevelBorder(b ? BevelBorder.LOWERED : BevelBorder.RAISED));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (palette == null || palette.length == 0) return;
            int sw = 5, sh = 4;
            int cols = Math.min(4, palette.length);
            int totalW = cols * sw;
            int x0 = (getWidth() - totalW) / 2;
            int y0 = 6;
            for (int i = 0; i < Math.min(16, palette.length); i++) {
                int cx = i % cols;
                int cy = i / cols;
                g.setColor(palette[i]);
                g.fillRect(x0 + cx * sw, y0 + cy * sh, sw, sh);
            }
        }
    }

    private static class ImageCanvas extends JPanel {
        private BufferedImage image;
        private boolean dialogEnabled;
        private boolean dialogStyleJRPG;
        private String dialogName = "";
        private String dialogText = "";
        private int dialogVerticalPos = 80;

        ImageCanvas() {
            setOpaque(true);
            setBackground(WIN_GRAY);
        }

        void setImage(BufferedImage img) {
            this.image = img;
            setPreferredSize(img != null ? new Dimension(img.getWidth(), img.getHeight()) : new Dimension(400, 300));
            revalidate();
            repaint();
        }

        void updateDialogParams(boolean enabled, boolean styleJRPG, String name, String text, int verticalPos) {
            this.dialogEnabled = enabled;
            this.dialogStyleJRPG = styleJRPG;
            this.dialogName = name != null ? name : "";
            this.dialogText = text != null ? text : "";
            this.dialogVerticalPos = verticalPos;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) {
                g.setColor(Color.GRAY);
                g.setFont(WIN_FONT);
                String msg = "Load an image to get started";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }
            int imgW = image.getWidth();
            int imgH = image.getHeight();
            int x = Math.max(0, (getWidth() - imgW) / 2);
            int y = Math.max(0, (getHeight() - imgH) / 2);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(image, x, y, null);

            if (dialogEnabled && (!dialogName.isEmpty() || !dialogText.isEmpty())) {
                g2.translate(x, y);
                drawDialogOverlay(g2, imgW, imgH);
                g2.translate(-x, -y);
            }
        }

        private void drawDialogOverlay(Graphics2D g2, int imgW, int imgH) {
            int margin = 20;
            int boxY = (int) (imgH * (dialogVerticalPos / 100.0));
            int boxW = imgW - 2 * margin;
            int lineH = Math.max(14, imgH / 25);
            int padding = lineH / 2;
            int boxH = padding * 2 + lineH * 3;
            boxY = Math.max(0, Math.min(boxY, imgH - boxH - padding));
            int boxX = margin;

            if (dialogStyleJRPG) {
                g2.setColor(Color.BLACK);
                g2.fillRect(boxX, boxY, boxW, boxH);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(boxX + 2, boxY + 2, boxW - 4, boxH - 4);
                g2.setStroke(new BasicStroke(1));

                int fontSize = Math.max(10, Math.min(24, imgH / 20));
                Font font = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics();
                int textX = boxX + padding + 3;
                int textY = boxY + padding + fm.getAscent();

                if (!dialogName.isEmpty()) {
                    g2.setColor(Color.YELLOW);
                    g2.drawString(dialogName, textX, textY);
                    textY += lineH;
                }
                g2.setColor(Color.WHITE);
                for (String line : dialogText.split("\n")) {
                    if (textY > boxY + boxH - padding) break;
                    g2.drawString(line, textX, textY);
                    textY += lineH;
                }
            } else {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                g2.setColor(Color.BLACK);
                g2.fillRect(boxX, boxY, boxW, boxH);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

                int fontSize = Math.max(10, Math.min(20, imgH / 25));
                Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
                g2.setFont(font);
                g2.setColor(new Color(0x00, 0xFF, 0x00));
                FontMetrics fm = g2.getFontMetrics();
                int textX = boxX + padding;
                int textY = boxY + padding + fm.getAscent();

                if (!dialogName.isEmpty()) {
                    g2.drawString(dialogName, textX, textY);
                    textY += lineH;
                }
                for (String line : dialogText.split("\n")) {
                    if (textY > boxY + boxH - padding) break;
                    g2.drawString(line, textX, textY);
                    textY += lineH;
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PolyPixelator().setVisible(true));
    }
}
