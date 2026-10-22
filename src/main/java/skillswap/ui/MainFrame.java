package skillswap.ui;

import skillswap.matching.Exchange;
import skillswap.matching.MatchResult;
import skillswap.model.Proficiency;
import skillswap.model.Rating;
import skillswap.model.Session;
import skillswap.model.Skill;
import skillswap.model.Student;
import skillswap.service.SkillSwapService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Main responsive desktop interface. All displayed counts and actions use saved service data. */
public final class MainFrame extends JFrame {
    private static final int MAX_CONTENT_WIDTH = 1480;
    private final SkillSwapService service;
    private final JPanel pageHost = new JPanel(new BorderLayout());
    private final JPanel navigation = new JPanel();
    private final JComboBox<StudentChoice> profilePicker = new JComboBox<>();
    private final JLabel pageTitle = new JLabel();
    private final JLabel saveStatus = new JLabel();
    private final List<JButton> navButtons = new ArrayList<>();
    private JPanel sidebarPanel;
    private JPanel header;
    private JPanel titleBlock;
    private JPanel headerRight;
    private JLabel headerTagline;
    private JComboBox<String> compactNavigation;
    private String currentStudentId;
    private String activePage = "Dashboard";
    private String saveMessage = "Saved locally";
    private Color saveStatusColor = Theme.GREEN;
    private boolean updatingProfiles;
    private boolean updatingNavigation;
    private boolean compactMode;

    public MainFrame(SkillSwapService service) {
        super("Skill Swap | Learn together");
        this.service = service;
        Student preferred = service.getStudents().stream().filter(s -> s.getEmail().equals("asha@skillswap.demo")).findFirst()
                .orElseGet(() -> service.getStudents().stream().min(Comparator.comparing(Student::getName)).orElseThrow());
        currentStudentId = preferred.getId();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(360, 600));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.max(360, Math.min(1260, screen.width - 32)),
                Math.max(600, Math.min(800, screen.height - 80)));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());
        sidebarPanel = buildSidebar();
        add(sidebarPanel, BorderLayout.WEST);
        add(buildMainShell(), BorderLayout.CENTER);
        refreshProfilePicker();
        renderPage();
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) { updateResponsiveLayout(); }
        });
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.NAVY);
        side.setPreferredSize(new Dimension(224, 0));
        side.setBorder(new EmptyBorder(23, 15, 19, 15));

        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        JLabel mark = new BrandMark();
        mark.setPreferredSize(new Dimension(42, 42));
        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.add(label("SKILL SWAP", Color.WHITE, 17, Font.BOLD));
        brandText.add(Box.createVerticalStrut(3));
        brandText.add(label("Learn from each other", new Color(168, 186, 210), 11, Font.PLAIN));
        brand.add(mark, BorderLayout.WEST);
        brand.add(brandText, BorderLayout.CENTER);
        side.add(brand, BorderLayout.NORTH);

        navigation.setOpaque(false);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBorder(new EmptyBorder(39, 0, 0, 0));
        String[] items = {"Dashboard", "Find partners", "My skills", "Sessions", "Profile & join"};
        for (String item : items) {
            JButton button = navButton(item);
            navButtons.add(button);
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(7));
        }
        side.add(navigation, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.add(label("COLLEGE DEMO", new Color(146, 166, 191), 10, Font.BOLD));
        footer.add(Box.createVerticalStrut(8));
        footer.add(label("Profiles and sessions are saved", new Color(198, 210, 226), 11, Font.PLAIN));
        footer.add(Box.createVerticalStrut(3));
        footer.add(label("locally on this computer.", new Color(198, 210, 226), 11, Font.PLAIN));
        side.add(footer, BorderLayout.SOUTH);
        return side;
    }

    private JButton navButton(String text) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean selected = Boolean.TRUE.equals(getClientProperty("selected"));
                if (selected) {
                    g.setColor(Theme.NAVY_LIGHT);
                    g.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 13, 13);
                } else if (getModel().isRollover()) {
                    g.setColor(new Color(255, 255, 255, 20));
                    g.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 13, 13);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setForeground(new Color(205, 217, 233));
        button.setFont(Theme.font(13, Font.BOLD));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(12, 13, 12, 11));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event -> {
            activePage = text;
            renderPage();
        });
        return button;
    }

    private JPanel buildMainShell() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(Theme.BG);
        header = new JPanel(new BorderLayout());
        header.setBackground(Theme.CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), new EmptyBorder(14, 28, 14, 27)));

        titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        pageTitle.setFont(Theme.font(17, Font.BOLD));
        pageTitle.setForeground(Theme.TEXT);
        titleBlock.add(pageTitle);
        titleBlock.add(Box.createVerticalStrut(3));
        headerTagline = label("A campus community for sharing what you know", Theme.MUTED, 11, Font.PLAIN);
        titleBlock.add(headerTagline);
        header.add(titleBlock, BorderLayout.WEST);

        headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerRight.setOpaque(false);
        saveStatus.setFont(Theme.font(11, Font.BOLD));
        styleComboBox(profilePicker);
        profilePicker.setPreferredSize(new Dimension(205, 34));
        profilePicker.addActionListener(event -> {
            if (updatingProfiles) return;
            StudentChoice choice = (StudentChoice) profilePicker.getSelectedItem();
            if (choice != null && !choice.id.equals(currentStudentId)) {
                currentStudentId = choice.id;
                renderPage();
            }
        });
        compactNavigation = styleComboBox(new JComboBox<>(new String[]{"Dashboard", "Find partners", "My skills", "Sessions", "Profile & join"}));
        compactNavigation.setPreferredSize(new Dimension(150, 36));
        compactNavigation.addActionListener(event -> {
            if (updatingNavigation) return;
            String selected = (String) compactNavigation.getSelectedItem();
            if (selected != null && !selected.equals(activePage)) {
                activePage = selected;
                renderPage();
            }
        });
        headerRight.add(saveStatus);
        headerRight.add(profilePicker);
        header.add(headerRight, BorderLayout.EAST);
        refreshSaveStatus();

        pageHost.setBackground(Theme.BG);
        shell.add(header, BorderLayout.NORTH);
        shell.add(pageHost, BorderLayout.CENTER);
        return shell;
    }

    private void refreshProfilePicker() {
        String keep = currentStudentId;
        updatingProfiles = true;
        try {
            DefaultComboBoxModel<StudentChoice> model = new DefaultComboBoxModel<>();
            service.getStudents().stream().sorted(Comparator.comparing(Student::getName))
                    .forEach(student -> model.addElement(new StudentChoice(student.getId(), student.getName())));
            profilePicker.setModel(model);
            for (int i = 0; i < model.getSize(); i++) if (model.getElementAt(i).id.equals(keep)) profilePicker.setSelectedIndex(i);
        } finally {
            updatingProfiles = false;
        }
    }

    private void updateResponsiveLayout() {
        boolean compact = getWidth() < 900;
        if (compact == compactMode || header == null) return;
        compactMode = compact;
        header.removeAll();
        headerRight.removeAll();
        if (compact) {
            if (sidebarPanel.getParent() == getContentPane()) getContentPane().remove(sidebarPanel);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), new EmptyBorder(10, 15, 10, 15)));
            headerTagline.setVisible(false);
            compactNavigation.setPreferredSize(new Dimension(150, 36));
            profilePicker.setPreferredSize(new Dimension(150, 36));
            JPanel controls = new JPanel(new GridLayout(1, 2, 8, 0));
            controls.setOpaque(false);
            controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            controls.add(compactNavigation);
            controls.add(profilePicker);
            header.add(titleBlock);
            header.add(Box.createVerticalStrut(8));
            header.add(controls);
            header.add(Box.createVerticalStrut(4));
            saveStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(saveStatus);
        } else {
            if (sidebarPanel.getParent() != getContentPane()) getContentPane().add(sidebarPanel, BorderLayout.WEST);
            header.setLayout(new BorderLayout());
            header.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), new EmptyBorder(14, 28, 14, 27)));
            headerTagline.setVisible(true);
            profilePicker.setPreferredSize(new Dimension(205, 34));
            headerRight.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            headerRight.add(saveStatus);
            headerRight.add(profilePicker);
            header.add(titleBlock, BorderLayout.WEST);
            header.add(headerRight, BorderLayout.EAST);
        }
        refreshSaveStatus();
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private void refreshSaveStatus() {
        String displayed = compactMode && saveMessage.length() > 16 ? "Saved" : saveMessage;
        saveStatus.setText("●  " + displayed);
        saveStatus.setForeground(saveStatusColor);
        saveStatus.setToolTipText(saveMessage);
    }

    private void renderPage() {
        pageHost.removeAll();
        pageTitle.setText(activePage);
        updatingNavigation = true;
        try { compactNavigation.setSelectedItem(activePage); }
        finally { updatingNavigation = false; }
        for (JButton button : navButtons) {
            boolean selected = button.getText().equals(activePage);
            button.putClientProperty("selected", selected);
            button.setForeground(selected ? Color.WHITE : new Color(217, 211, 224));
            button.repaint();
        }
        switch (activePage) {
            case "Find partners" -> pageHost.add(buildFindPage(), BorderLayout.CENTER);
            case "My skills" -> pageHost.add(buildSkillsPage(), BorderLayout.CENTER);
            case "Sessions" -> pageHost.add(buildSessionsPage(), BorderLayout.CENTER);
            case "Profile & join" -> pageHost.add(buildProfilePage(), BorderLayout.CENTER);
            default -> pageHost.add(buildDashboard(), BorderLayout.CENTER);
        }
        pageHost.revalidate();
        pageHost.repaint();
    }

    private JPanel buildDashboard() {
        Student current = currentStudent();
        JPanel page = pageColumn();
        JPanel hero = new HeroPanel();
        hero.setLayout(new BorderLayout(20, 10));
        hero.setBorder(new EmptyBorder(23, 28, 23, 25));
        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        JLabel heroEyebrow = label("A LITTLE KNOWLEDGE GOES A LONG WAY", new Color(255, 249, 245), 10, Font.BOLD);
        heroEyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        heroText.add(heroEyebrow);
        heroText.add(Box.createVerticalStrut(7));
        JLabel heroTitle = label("Good to see you, " + firstName(current.getName()) + ".", Color.WHITE, 24, Font.BOLD);
        heroTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        heroText.add(heroTitle);
        heroText.add(Box.createVerticalStrut(7));
        JLabel heroSubtitle = label("Share a strength. Find your next learning partner.", new Color(255, 241, 232), 13, Font.PLAIN);
        heroSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        heroText.add(heroSubtitle);
        hero.add(heroText, BorderLayout.CENTER);
        JButton explore = styledButton("Find partners", Theme.CARD, Theme.BLUE, Theme.CARD);
        explore.addActionListener(e -> { activePage = "Find partners"; renderPage(); });
        hero.add(explore, BorderLayout.EAST);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        installResponsiveHero(hero, heroText, heroTitle, heroSubtitle, explore);
        page.add(hero);
        page.add(Box.createVerticalStrut(18));

        JPanel stats = new ResponsiveGridPanel(5, 13, 12);
        stats.add(statCard("Skills to teach", String.valueOf(current.getOfferedSkills().size()), "Your strengths", Theme.BLUE));
        stats.add(statCard("Learning goals", String.valueOf(current.getWantedSkills().size()), "What you want to learn", Theme.TEAL));
        stats.add(statCard("Open sessions", String.valueOf(service.countUpcoming(currentStudentId)), "Pending or accepted", Theme.AMBER));
        stats.add(statCard("Completed sessions", String.valueOf(service.countCompleted(currentStudentId)), "Learning exchanges completed", Theme.GREEN));
        int ratings = service.getRatingCount(currentStudentId);
        stats.add(statCard("Teaching rating", ratings == 0 ? "New" : String.format(Locale.ROOT, "%.1f ★", service.getAverageRating(currentStudentId)), ratings + " review" + (ratings == 1 ? "" : "s"), Theme.GREEN));
        page.add(stats);
        page.add(Box.createVerticalStrut(19));

        JPanel row = new ResponsiveGridPanel(2, 15, 12);
        JPanel recommendations = cardPanel();
        recommendations.setLayout(new BoxLayout(recommendations, BoxLayout.Y_AXIS));
        recommendations.add(sectionHeading("Recommended partners", "Based on requested skills and proficiency"));
        List<MatchResult> suggestions = service.findMatches(currentStudentId);
        if (suggestions.isEmpty()) {
            recommendations.add(emptyMessage("No match yet", "Add skills or invite classmates to join."));
        } else {
            for (MatchResult match : suggestions.stream().limit(3).toList()) {
                Student partner = match.getParticipantIds().stream().filter(id -> !id.equals(currentStudentId)).map(service::getStudent).findFirst().orElse(null);
                if (partner != null) {
                    String type = match.getType() == MatchResult.Type.DIRECT ? "DIRECT" : match.getParticipantIds().size() + "-PERSON CHAIN";
                    JPanel line = new JPanel(new BorderLayout(12, 2));
                    line.setOpaque(false);
                    line.setBorder(new EmptyBorder(7, 0, 7, 0));
                    JPanel name = new JPanel(); name.setOpaque(false); name.setLayout(new BoxLayout(name, BoxLayout.Y_AXIS));
                    name.add(label(partner.getName(), Theme.TEXT, 13, Font.BOLD));
                    name.add(label(type + " · " + partner.getOfferedSkills().stream().map(Skill::getName).limit(2).collect(Collectors.joining(", ")),
                            Theme.MUTED, 11, Font.PLAIN));
                    line.add(name, BorderLayout.WEST);
                    line.add(pill(service.getRatingCount(partner.getId()) == 0 ? "New" : String.format(Locale.ROOT, "%.1f ★", service.getAverageRating(partner.getId())), Theme.BLUE_PALE, Theme.BLUE), BorderLayout.EAST);
                    recommendations.add(line);
                }
            }
        }
        JPanel activity = cardPanel();
        activity.setLayout(new BoxLayout(activity, BoxLayout.Y_AXIS));
        activity.add(sectionHeading("Recent activity", "Your latest saved sessions"));
        List<Session> recent = service.getSessionsFor(currentStudentId).stream().limit(4).toList();
        if (recent.isEmpty()) activity.add(emptyMessage("Your activity will appear here", "Create a session from a match to get started."));
        for (Session session : recent) activity.add(sessionSummary(session));
        row.add(recommendations);
        row.add(activity);
        page.add(row);
        return scrollPage(page);
    }

    private void installResponsiveHero(JPanel hero, JPanel heroText, JLabel title, JLabel subtitle, JButton action) {
        hero.addComponentListener(new ComponentAdapter() {
            private Boolean stacked;
            @Override public void componentResized(ComponentEvent event) {
                boolean compact = hero.getWidth() < 560;
                if (stacked != null && stacked == compact) return;
                stacked = compact;
                hero.removeAll();
                if (compact) {
                    hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
                    hero.setBorder(new EmptyBorder(18, 20, 18, 20));
                    title.setFont(Theme.font(18, Font.BOLD));
                    subtitle.setText("<html>Share a strength.<br>Find your next learning partner.</html>");
                    heroText.setAlignmentX(Component.LEFT_ALIGNMENT);
                    action.setAlignmentX(Component.LEFT_ALIGNMENT);
                    hero.add(heroText);
                    hero.add(Box.createVerticalStrut(12));
                    hero.add(action);
                } else {
                    hero.setLayout(new BorderLayout(20, 10));
                    hero.setBorder(new EmptyBorder(23, 28, 23, 25));
                    title.setFont(Theme.font(24, Font.BOLD));
                    subtitle.setText("Share a strength. Find your next learning partner.");
                    hero.add(heroText, BorderLayout.CENTER);
                    hero.add(action, BorderLayout.EAST);
                }
                refreshAdaptiveHeight(hero);
            }
        });
    }

    private void refreshAdaptiveHeight(Component component) {
        Dimension preferred = component.getPreferredSize();
        component.setMaximumSize(new Dimension(MAX_CONTENT_WIDTH, preferred.height));
        Container parent = component.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private void refreshAdaptiveAncestors(Component source) {
        Component current = source;
        while (current.getParent() != null && !(current.getParent() instanceof PagePanel)) {
            current = current.getParent();
            if (current instanceof javax.swing.JComponent swingComponent) {
                Dimension preferred = current.getPreferredSize();
                swingComponent.setMaximumSize(new Dimension(MAX_CONTENT_WIDTH, preferred.height));
            }
        }
        if (current.getParent() instanceof PagePanel page) {
            if (current instanceof javax.swing.JComponent swingComponent) {
                swingComponent.setMaximumSize(new Dimension(MAX_CONTENT_WIDTH, current.getPreferredSize().height));
            }
            page.revalidate();
        }
    }

    private JPanel buildFindPage() {
        JPanel page = pageColumn();
        page.add(pageIntro("Find your learning partner", "We check real skill exchanges and the proficiency each learner requested."));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setOpaque(false);
        JButton find = primaryButton("Find matches");
        JButton chains = secondaryButton("Explore chain swaps");
        controls.add(find);
        controls.add(chains);
        page.add(controls);
        page.add(Box.createVerticalStrut(9));
        JLabel algorithm = label("<html>Direct swaps appear first.<br>Chain search finds complete<br>cycles of 3–4 students.</html>", Theme.MUTED, 11, Font.PLAIN);
        algorithm.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(algorithm);
        page.add(Box.createVerticalStrut(14));
        JPanel results = new JPanel();
        results.setOpaque(false);
        results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
        page.add(results);
        Runnable showDefault = () -> renderMatchResults(results, service.findMatches(currentStudentId));
        find.addActionListener(e -> showDefault.run());
        chains.addActionListener(e -> renderMatchResults(results, service.findChainMatches(currentStudentId)));
        showDefault.run();
        return scrollPage(page);
    }

    private void renderMatchResults(JPanel results, List<MatchResult> matches) {
        results.removeAll();
        if (matches.isEmpty()) {
            JPanel empty = cardPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.add(sectionHeading("No compatible partner found", "A valid match needs every requested skill to be taught at the requested level."));
            empty.add(Box.createVerticalStrut(7));
            empty.add(label("Try adding another skill under My skills, or add classmates using Profile & join.", Theme.MUTED, 12, Font.PLAIN));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, empty.getPreferredSize().height));
            results.add(empty);
        }
        for (MatchResult match : matches) {
            JPanel card = cardPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            String matchTitle = match.getType() == MatchResult.Type.DIRECT ? "Direct skill swap" : match.getParticipantIds().size() + "-student learning chain";
            JPanel titleRow = new JPanel(new BorderLayout(8, 5));
            titleRow.setOpaque(false);
            titleRow.add(label(matchTitle, Theme.TEXT, 17, Font.BOLD), BorderLayout.WEST);
            String type = match.getType() == MatchResult.Type.DIRECT ? "RECIPROCAL" : "COMPLETE CYCLE";
            titleRow.add(pill(type, match.getType() == MatchResult.Type.DIRECT ? Theme.BLUE_PALE : Theme.GREEN_PALE,
                    match.getType() == MatchResult.Type.DIRECT ? Theme.BLUE : Theme.GREEN), BorderLayout.EAST);
            card.add(titleRow);
            card.add(Box.createVerticalStrut(7));
            if (match.getType() == MatchResult.Type.CHAIN) {
                String cycle = match.getParticipantIds().stream().map(id -> service.getStudent(id).getName())
                        .collect(Collectors.joining("  →  ")) + "  →  " + service.getStudent(match.getParticipantIds().get(0)).getName();
                card.add(label("Cycle  " + cycle, Theme.MUTED, 12, Font.PLAIN));
                card.add(Box.createVerticalStrut(9));
            }
            for (Exchange exchange : match.getExchanges()) {
                String learner = service.getStudent(exchange.getLearnerId()).getName();
                String teacher = service.getStudent(exchange.getTeacherId()).getName();
                Skill offeredSkill = service.getStudent(exchange.getTeacherId()).getOffered(exchange.getSkillName());
                Skill requestedSkill = service.getStudent(exchange.getLearnerId()).getWanted(exchange.getSkillName());
                String line = learner.equals(currentStudent().getName()) ? "You learn " + exchange.getSkillName() + " from " + teacher
                        : teacher.equals(currentStudent().getName()) ? "You teach " + exchange.getSkillName() + " to " + learner
                        : learner + " learns " + exchange.getSkillName() + " from " + teacher;
                if (offeredSkill != null && requestedSkill != null) {
                    line += "  ·  " + offeredSkill.getProficiency().getLabel() + " offered / " + requestedSkill.getProficiency().getLabel() + " requested";
                }
                card.add(exchangeLine(line));
            }
            if (match.getType() == MatchResult.Type.DIRECT) {
                String partnerId = match.getParticipantIds().stream().filter(id -> !id.equals(currentStudentId)).findFirst().orElse("");
                Student partner = service.getStudent(partnerId);
                String offers = partner.getOfferedSkills().stream().map(skill -> skill.getName() + " · " + skill.getProficiency().getLabel()).collect(Collectors.joining(", "));
                String wants = partner.getWantedSkills().stream().map(skill -> skill.getName() + " · " + skill.getProficiency().getLabel()).collect(Collectors.joining(", "));
                card.add(Box.createVerticalStrut(7));
                card.add(wrapLabel(partner.getName() + " · Can teach: " + offers + " · Wants to learn: " + wants, Theme.MUTED, 11, Font.PLAIN));
            } else {
                String participantStats = match.getParticipantIds().stream().filter(id -> !id.equals(currentStudentId))
                        .map(id -> {
                            Student participant = service.getStudent(id);
                            int ratingCount = service.getRatingCount(id);
                            String rating = ratingCount == 0 ? "new rating" : String.format(Locale.ROOT, "%.1f ★ (%d reviews)", service.getAverageRating(id), ratingCount);
                            return participant.getName() + " · " + rating + " · " + service.countCompleted(id) + " completed";
                        }).collect(Collectors.joining("  |  "));
                card.add(wrapLabel("Partner history · " + participantStats, Theme.MUTED, 11, Font.PLAIN));
            }
            card.add(Box.createVerticalStrut(12));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
            actions.setOpaque(false);
            JButton schedule = primaryButton(match.getType() == MatchResult.Type.CHAIN ? "Schedule full chain" : "Request full exchange");
            schedule.addActionListener(e -> runAction(() -> {
                List<Session> created = match.getType() == MatchResult.Type.CHAIN
                        ? service.scheduleChain(match, currentStudentId)
                        : service.scheduleDirectExchange(currentStudentId, match);
                setSaveMessage("Saved " + created.size() + " session" + (created.size() == 1 ? "" : "s"));
                activePage = "Sessions";
                renderPage();
            }));
            actions.add(schedule);
            if (match.getType() == MatchResult.Type.DIRECT) {
                String partnerId = match.getParticipantIds().stream().filter(id -> !id.equals(currentStudentId)).findFirst().orElse("");
                Student partner = service.getStudent(partnerId);
                int ratingCount = service.getRatingCount(partnerId);
                String rating = ratingCount == 0 ? "No ratings yet" : String.format(Locale.ROOT, "%.1f average · %d review(s)", service.getAverageRating(partnerId), ratingCount);
                long completed = service.getSessionsFor(partnerId).stream().filter(session -> session.getStatus() == Session.Status.COMPLETED).count();
                actions.add(label(partner.getName() + " · " + rating + " · " + completed + " completed", Theme.MUTED, 11, Font.PLAIN));
            }
            card.add(actions);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
            results.add(card);
            results.add(Box.createVerticalStrut(13));
        }
        results.setMaximumSize(new Dimension(MAX_CONTENT_WIDTH, results.getPreferredSize().height));
        results.revalidate();
        results.repaint();
    }

    private JPanel buildSkillsPage() {
        Student student = currentStudent();
        JPanel page = pageColumn();
        page.add(pageIntro("Your skills", "Keep what you can teach separate from what you want to learn."));
        JPanel columns = new ResponsiveGridPanel(2, 15, 13);
        columns.add(skillListCard("I can teach", "Offered skills are compared with classmates' learning goals.", Skill.Direction.OFFERED, student));
        columns.add(skillListCard("I want to learn", "Your selected level is the minimum proficiency a partner should teach.", Skill.Direction.WANTED, student));
        page.add(columns);
        page.add(Box.createVerticalStrut(15));
        JPanel note = cardPanel();
        note.setLayout(new BoxLayout(note, BoxLayout.Y_AXIS));
        note.add(sectionHeading("How proficiency is checked", "A teacher must meet or exceed the level requested by the learner."));
        note.add(label("For example, an Advanced Java teacher can support a Beginner or Intermediate request.", Theme.MUTED, 12, Font.PLAIN));
        page.add(note);
        return scrollPage(page);
    }

    private JPanel skillListCard(String title, String subtitle, Skill.Direction direction, Student student) {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, 13));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(sectionHeading(title, subtitle));
        card.add(top, BorderLayout.NORTH);
        DefaultListModel<Skill> model = new DefaultListModel<>();
        (direction == Skill.Direction.OFFERED ? student.getOfferedSkills() : student.getWantedSkills()).forEach(model::addElement);
        JList<Skill> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(Theme.font(13, Font.PLAIN));
        list.setFixedCellHeight(37);
        list.setBackground(Theme.CARD);
        list.setSelectionBackground(Theme.BLUE_PALE);
        list.setSelectionForeground(Theme.TEXT);
        list.setBorder(new EmptyBorder(4, 8, 4, 8));
        JScrollPane skillScroll = new JScrollPane(list);
        skillScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        skillScroll.getViewport().setBackground(Theme.CARD);
        card.add(skillScroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false);
        JButton add = primaryButton("Add skill");
        add.addActionListener(e -> addSkillDialog(direction));
        JButton remove = secondaryButton("Remove selected");
        remove.addActionListener(e -> {
            Skill selected = list.getSelectedValue();
            if (selected == null) { showError("Select a skill first."); return; }
            runAction(() -> { service.removeSkill(currentStudentId, direction, selected.getName()); setSaveMessage("Skill removed and saved"); renderPage(); });
        });
        JButton edit = secondaryButton("Edit selected");
        edit.addActionListener(e -> {
            Skill selected = list.getSelectedValue();
            if (selected == null) { showError("Select a skill first."); return; }
            editSkillDialog(direction, selected);
        });
        actions.add(add); actions.add(edit); actions.add(remove);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void addSkillDialog(Skill.Direction direction) {
        JTextField name = textField("e.g. Data analysis");
        JTextField category = textField("e.g. Technology");
        JComboBox<Proficiency> level = styleComboBox(new JComboBox<>(Proficiency.values()));
        JPanel form = formPanel();
        addFormRow(form, 0, "Skill", name);
        addFormRow(form, 1, "Category", category);
        addFormRow(form, 2, direction == Skill.Direction.OFFERED ? "Teaching level" : "Desired level", level);
        int answer = JOptionPane.showConfirmDialog(this, form, direction == Skill.Direction.OFFERED ? "Add a skill to teach" : "Add a learning goal", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        runAction(() -> {
            service.addSkill(currentStudentId, direction, name.getText(), category.getText(), (Proficiency) level.getSelectedItem());
            setSaveMessage("Skill saved"); renderPage();
        });
    }

    private void editSkillDialog(Skill.Direction direction, Skill selected) {
        JTextField name = textField(selected.getName()); name.setText(selected.getName());
        JTextField category = textField(selected.getCategory()); category.setText(selected.getCategory());
        JComboBox<Proficiency> level = styleComboBox(new JComboBox<>(Proficiency.values())); level.setSelectedItem(selected.getProficiency());
        JPanel form = formPanel();
        addFormRow(form, 0, "Skill", name);
        addFormRow(form, 1, "Category", category);
        addFormRow(form, 2, "Proficiency", level);
        int answer = JOptionPane.showConfirmDialog(this, form, "Edit skill", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        runAction(() -> {
            service.updateSkill(currentStudentId, direction, selected.getName(), name.getText(), category.getText(), (Proficiency) level.getSelectedItem());
            setSaveMessage("Skill updated and saved"); renderPage();
        });
    }

    private JPanel buildSessionsPage() {
        JPanel page = pageColumn();
        page.add(pageIntro("Sessions", "Follow each request through acceptance, completion, and feedback."));
        List<Session> sessions = service.getSessionsFor(currentStudentId);
        if (sessions.isEmpty()) page.add(emptyMessage("No sessions yet", "Find a direct partner or a complete chain to schedule a learning exchange."));
        for (Session session : sessions) {
            page.add(sessionCard(session));
            page.add(Box.createVerticalStrut(12));
        }
        return scrollPage(page);
    }

    private JPanel sessionCard(Session session) {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(14, 0));
        JPanel details = new JPanel(); details.setOpaque(false); details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        String teacher = service.getStudent(session.getTeacherId()).getName();
        String learner = service.getStudent(session.getLearnerId()).getName();
        details.add(label(session.getSkillName() + " session", Theme.TEXT, 16, Font.BOLD));
        details.add(Box.createVerticalStrut(5));
        details.add(label("SESSION " + session.getId().substring(0, Math.min(8, session.getId().length())).toUpperCase(Locale.ROOT), Theme.MUTED, 10, Font.BOLD));
        details.add(Box.createVerticalStrut(4));
        details.add(wrapLabel(teacher + " teaches " + session.getSkillName() + " to " + learner, Theme.MUTED, 12, Font.PLAIN));
        details.add(Box.createVerticalStrut(4));
        details.add(label(session.getScheduledAt().format(DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", Locale.ENGLISH)), Theme.MUTED, 12, Font.PLAIN));
        if (!session.getNotes().isBlank()) {
            details.add(Box.createVerticalStrut(4));
            details.add(wrapLabel(session.getNotes(), Theme.MUTED, 11, Font.PLAIN));
        }
        Rating rating = session.getRating();
        if (rating != null) {
            details.add(Box.createVerticalStrut(7));
            details.add(wrapLabel("★ ".repeat(rating.getStars()) + "  " + (rating.getFeedback().isBlank() ? "No written note" : rating.getFeedback()), Theme.AMBER, 11, Font.PLAIN));
        }
        card.add(details, BorderLayout.CENTER);

        JPanel right = new JPanel(); right.setOpaque(false); right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setPreferredSize(new Dimension(216, 0));
        JLabel status = pill(session.getStatus().toString().toUpperCase(Locale.ROOT), statusBackground(session.getStatus()), statusForeground(session.getStatus()));
        status.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(status);
        right.add(Box.createVerticalStrut(10));
        addSessionActions(right, session);
        card.add(right, BorderLayout.EAST);
        installResponsiveSessionCard(card, details, right, status);
        return card;
    }

    private void installResponsiveSessionCard(JPanel card, JPanel details, JPanel actions, JLabel status) {
        card.addComponentListener(new ComponentAdapter() {
            private Boolean stacked;
            @Override public void componentResized(ComponentEvent event) {
                boolean compact = card.getWidth() < 650;
                if (stacked != null && stacked == compact) return;
                stacked = compact;
                card.removeAll();
                if (compact) {
                    card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
                    actions.setPreferredSize(null);
                    details.setAlignmentX(Component.LEFT_ALIGNMENT);
                    actions.setAlignmentX(Component.LEFT_ALIGNMENT);
                    status.setAlignmentX(Component.LEFT_ALIGNMENT);
                    actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, actions.getPreferredSize().height));
                    card.add(details);
                    card.add(Box.createVerticalStrut(12));
                    card.add(actions);
                } else {
                    card.setLayout(new BorderLayout(14, 0));
                    actions.setPreferredSize(new Dimension(216, 0));
                    status.setAlignmentX(Component.RIGHT_ALIGNMENT);
                    card.add(details, BorderLayout.CENTER);
                    card.add(actions, BorderLayout.EAST);
                }
                refreshAdaptiveHeight(card);
            }
        });
    }

    private void addSessionActions(JPanel right, Session session) {
        if (session.getStatus() == Session.Status.PENDING) {
            if (session.getTeacherId().equals(currentStudentId)) {
                JButton accept = primaryButton("Accept request");
                accept.addActionListener(e -> runAction(() -> { service.acceptSession(session.getId(), currentStudentId); setSaveMessage("Request accepted"); renderPage(); }));
                accept.setAlignmentX(Component.RIGHT_ALIGNMENT); right.add(accept);
            } else {
                JLabel waiting = label("Waiting for " + service.getStudent(session.getTeacherId()).getName(), Theme.MUTED, 10, Font.PLAIN);
                waiting.setAlignmentX(Component.RIGHT_ALIGNMENT); right.add(waiting);
            }
            addSmallAction(right, "Reschedule", () -> rescheduleDialog(session));
            addSmallAction(right, "Cancel", () -> runAction(() -> { service.cancelSession(session.getId(), currentStudentId); setSaveMessage("Session cancelled"); renderPage(); }));
        } else if (session.getStatus() == Session.Status.ACCEPTED) {
            JButton complete = primaryButton("Mark completed");
            complete.addActionListener(e -> runAction(() -> { service.completeSession(session.getId(), currentStudentId); setSaveMessage("Session completed"); renderPage(); }));
            complete.setAlignmentX(Component.RIGHT_ALIGNMENT); right.add(complete);
            addSmallAction(right, "Reschedule", () -> rescheduleDialog(session));
            addSmallAction(right, "Cancel", () -> runAction(() -> { service.cancelSession(session.getId(), currentStudentId); setSaveMessage("Session cancelled"); renderPage(); }));
        } else if (session.getStatus() == Session.Status.COMPLETED && session.getRating() == null && session.getLearnerId().equals(currentStudentId)) {
            JButton rate = primaryButton("Rate this session");
            rate.addActionListener(e -> ratingDialog(session));
            rate.setAlignmentX(Component.RIGHT_ALIGNMENT); right.add(rate);
        } else if (session.getStatus() == Session.Status.COMPLETED && session.getLearnerId().equals(currentStudentId) && session.getRating() != null) {
            JLabel rated = label("Your feedback saved", Theme.GREEN, 10, Font.BOLD);
            rated.setAlignmentX(Component.RIGHT_ALIGNMENT); right.add(rated);
        }
    }

    private void addSmallAction(JPanel parent, String text, Runnable action) {
        JButton button = textButton(text);
        button.setAlignmentX(Component.RIGHT_ALIGNMENT);
        button.addActionListener(e -> action.run());
        parent.add(button);
    }

    private void rescheduleDialog(Session session) {
        JTextField dateTime = textField(session.getScheduledAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        JPanel form = formPanel();
        addFormRow(form, 0, "New date and time", dateTime);
        JLabel hint = label("Use YYYY-MM-DD HH:MM, for example 2026-10-22 14:30.", Theme.MUTED, 11, Font.PLAIN);
        form.add(hint, new java.awt.GridBagConstraints());
        int answer = JOptionPane.showConfirmDialog(this, form, "Reschedule session", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        runAction(() -> {
            LocalDateTime parsed;
            try { parsed = LocalDateTime.parse(dateTime.getText().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")); }
            catch (DateTimeParseException ex) { throw new IllegalArgumentException("Use the date and time format shown in the dialog."); }
            service.rescheduleSession(session.getId(), currentStudentId, parsed);
            setSaveMessage("Session time saved"); renderPage();
        });
    }

    private void ratingDialog(Session session) {
        JComboBox<Integer> stars = styleComboBox(new JComboBox<>(new Integer[]{5, 4, 3, 2, 1}));
        JTextField learned = textField("e.g. I can now write a basic Python function");
        JTextArea feedback = new JTextArea(4, 29);
        feedback.setLineWrap(true); feedback.setWrapStyleWord(true); feedback.setFont(Theme.font(12, Font.PLAIN));
        feedback.setBackground(Theme.CARD); feedback.setForeground(Theme.TEXT);
        feedback.setBorder(new EmptyBorder(8, 9, 8, 9));
        JPanel form = formPanel();
        addFormRow(form, 0, "Rating", stars);
        addFormRow(form, 1, "What you learned", learned);
        addFormRow(form, 2, "Feedback", new JScrollPane(feedback));
        int answer = JOptionPane.showConfirmDialog(this, form, "Share session feedback", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        runAction(() -> { service.rateSession(session.getId(), currentStudentId, (Integer) stars.getSelectedItem(), feedback.getText(), learned.getText()); setSaveMessage("Feedback saved and rating updated"); renderPage(); });
    }

    private JPanel buildProfilePage() {
        Student student = currentStudent();
        JPanel page = pageColumn();
        page.add(pageIntro("Student profiles", "Select a demo profile above or register a classmate on this device."));
        JPanel profile = cardPanel();
        profile.setLayout(new BorderLayout(16, 0));
        JPanel identity = new JPanel(); identity.setOpaque(false); identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(label(student.getName(), Theme.TEXT, 22, Font.BOLD));
        identity.add(Box.createVerticalStrut(5));
        identity.add(label(student.getEmail(), Theme.MUTED, 13, Font.PLAIN));
        identity.add(Box.createVerticalStrut(12));
        identity.add(label(student.getOfferedSkills().size() + " offered skills  ·  " + student.getWantedSkills().size() + " learning goals", Theme.TEXT, 12, Font.PLAIN));
        profile.add(identity, BorderLayout.CENTER);
        JPanel ratingCard = statCard("Teaching rating", service.getRatingCount(currentStudentId) == 0 ? "New" : String.format(Locale.ROOT, "%.1f ★", service.getAverageRating(currentStudentId)), service.getRatingCount(currentStudentId) + " saved review(s)", Theme.GREEN);
        profile.add(ratingCard, BorderLayout.EAST);
        JButton editProfile = secondaryButton("Edit profile");
        editProfile.addActionListener(e -> editProfileDialog(student));
        identity.add(Box.createVerticalStrut(9));
        identity.add(editProfile);
        installResponsiveProfileCard(profile, identity, ratingCard);
        page.add(profile);
        page.add(Box.createVerticalStrut(14));

        JPanel skills = new ResponsiveGridPanel(2, 14, 12);
        skills.add(readOnlySkillCard("Can teach", student.getOfferedSkills()));
        skills.add(readOnlySkillCard("Wants to learn", student.getWantedSkills()));
        page.add(skills);
        page.add(Box.createVerticalStrut(14));

        List<Session> profileSessions = service.getSessionsFor(currentStudentId);
        long teachingSessions = profileSessions.stream().filter(session -> session.getTeacherId().equals(currentStudentId)).count();
        long learningSessions = profileSessions.stream().filter(session -> session.getLearnerId().equals(currentStudentId)).count();
        JPanel activity = new ResponsiveGridPanel(3, 14, 12);
        activity.add(statCard("Teaching sessions", String.valueOf(teachingSessions), "All statuses", Theme.BLUE));
        activity.add(statCard("Learning sessions", String.valueOf(learningSessions), "All statuses", Theme.TEAL));
        activity.add(statCard("Completed sessions", String.valueOf(service.countCompleted(currentStudentId)), "Saved learning history", Theme.GREEN));
        page.add(sectionHeading("Learning activity", "Your teaching, learning, and completed exchanges"));
        page.add(activity);
        page.add(Box.createVerticalStrut(14));

        JPanel registration = cardPanel();
        registration.setLayout(new BorderLayout(0, 14));
        registration.add(sectionHeading("Register a student", "Email addresses must be unique. Skills are saved to local storage."), BorderLayout.NORTH);
        JPanel form = new ResponsiveRegistrationForm();
        JTextField name = textField("Full name");
        JTextField email = textField("student@college.edu");
        JTextField offered = textField("Java, public speaking");
        JTextField wanted = textField("Python, SQL");
        JComboBox<Proficiency> teachLevel = styleComboBox(new JComboBox<>(Proficiency.values()));
        JComboBox<Proficiency> learnLevel = styleComboBox(new JComboBox<>(Proficiency.values()));
        form.add(formLabel("Name")); form.add(name); form.add(formLabel("Email")); form.add(email);
        form.add(formLabel("Can teach")); form.add(offered); form.add(formLabel("Teaching level")); form.add(teachLevel);
        form.add(formLabel("Wants to learn")); form.add(wanted); form.add(formLabel("Requested level")); form.add(learnLevel);
        registration.add(form, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false);
        footer.add(label("Separate multiple skill names with commas.", Theme.MUTED, 11, Font.PLAIN), BorderLayout.WEST);
        JButton register = primaryButton("Create student profile");
        register.addActionListener(e -> runAction(() -> {
            Student created = service.register(name.getText(), email.getText(), offered.getText(), wanted.getText(),
                    (Proficiency) teachLevel.getSelectedItem(), (Proficiency) learnLevel.getSelectedItem());
            currentStudentId = created.getId();
            refreshProfilePicker();
            setSaveMessage("Profile saved");
            renderPage();
        }));
        footer.add(register, BorderLayout.EAST);
        registration.add(footer, BorderLayout.SOUTH);
        page.add(registration);
        page.add(Box.createVerticalStrut(14));

        JPanel demo = cardPanel(); demo.setLayout(new BorderLayout(14, 0));
        JPanel demoText = new JPanel(); demoText.setOpaque(false); demoText.setLayout(new BoxLayout(demoText, BoxLayout.Y_AXIS));
        demoText.add(sectionHeading("Demo profiles", "Asha and Dev show a direct swap. Meera, Arjun, and Kabir form a three-person cycle. Tara has no match."));
        demoText.add(label("Adding demo data is safe to repeat; existing profiles and sessions are not overwritten.", Theme.MUTED, 11, Font.PLAIN));
        demo.add(demoText, BorderLayout.CENTER);
        JButton seed = secondaryButton("Add missing demo data");
        seed.addActionListener(e -> runAction(() -> { service.loadDemoData(); refreshProfilePicker(); setSaveMessage("Missing demo profiles added"); renderPage(); }));
        demo.add(seed, BorderLayout.EAST);
        page.add(demo);
        page.add(Box.createVerticalStrut(14));

        JPanel feedback = cardPanel(); feedback.setLayout(new BoxLayout(feedback, BoxLayout.Y_AXIS));
        feedback.add(sectionHeading("Recent feedback", "Saved ratings from completed teaching sessions"));
        List<Session> reviews = service.getRecentFeedback(currentStudentId, 3);
        if (reviews.isEmpty()) feedback.add(emptyMessage("No feedback yet", "A learner can rate a completed session."));
        for (Session review : reviews) {
            Rating rating = review.getRating();
            feedback.add(label("★ ".repeat(rating.getStars()) + "  " + service.getStudent(review.getLearnerId()).getName() + " · " + review.getSkillName(), Theme.AMBER, 12, Font.BOLD));
            if (!rating.getFeedback().isBlank()) feedback.add(wrapLabel("“" + rating.getFeedback() + "”", Theme.MUTED, 12, Font.PLAIN));
            if (!rating.getLearned().isBlank()) feedback.add(wrapLabel("Learned: " + rating.getLearned(), Theme.MUTED, 11, Font.PLAIN));
            feedback.add(Box.createVerticalStrut(7));
        }
        page.add(feedback);
        return scrollPage(page);
    }

    private void installResponsiveProfileCard(JPanel profile, JPanel identity, JPanel rating) {
        profile.addComponentListener(new ComponentAdapter() {
            private Boolean stacked;
            @Override public void componentResized(ComponentEvent event) {
                boolean compact = profile.getWidth() < 620;
                if (stacked != null && stacked == compact) return;
                stacked = compact;
                profile.removeAll();
                if (compact) {
                    profile.setLayout(new BoxLayout(profile, BoxLayout.Y_AXIS));
                    identity.setAlignmentX(Component.LEFT_ALIGNMENT);
                    rating.setAlignmentX(Component.LEFT_ALIGNMENT);
                    rating.setMaximumSize(new Dimension(Integer.MAX_VALUE, rating.getPreferredSize().height));
                    profile.add(identity);
                    profile.add(Box.createVerticalStrut(12));
                    profile.add(rating);
                } else {
                    profile.setLayout(new BorderLayout(16, 0));
                    rating.setMaximumSize(new Dimension(225, rating.getPreferredSize().height));
                    profile.add(identity, BorderLayout.CENTER);
                    profile.add(rating, BorderLayout.EAST);
                }
                refreshAdaptiveHeight(profile);
            }
        });
    }

    private JPanel readOnlySkillCard(String title, java.util.Collection<Skill> values) {
        JPanel card = cardPanel(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(sectionHeading(title, ""));
        if (values.isEmpty()) card.add(label("No skills added.", Theme.MUTED, 12, Font.PLAIN));
        for (Skill skill : values) card.add(exchangeLine(skill.getName() + " · " + skill.getProficiency().getLabel() + " · " + skill.getCategory()));
        return card;
    }

    private void editProfileDialog(Student student) {
        JTextField name = textField("Full name"); name.setText(student.getName());
        JTextField email = textField("student@college.edu"); email.setText(student.getEmail());
        JPanel form = formPanel();
        addFormRow(form, 0, "Name", name);
        addFormRow(form, 1, "Email", email);
        int answer = JOptionPane.showConfirmDialog(this, form, "Edit student profile", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        runAction(() -> {
            service.updateProfile(currentStudentId, name.getText(), email.getText());
            refreshProfilePicker(); setSaveMessage("Profile changes saved"); renderPage();
        });
    }

    private JPanel sessionSummary(Session session) {
        JPanel row = new JPanel(new BorderLayout(8, 3)); row.setOpaque(false); row.setBorder(new EmptyBorder(7, 0, 7, 0));
        String peer = session.getTeacherId().equals(currentStudentId) ? service.getStudent(session.getLearnerId()).getName() : service.getStudent(session.getTeacherId()).getName();
        JPanel text = new JPanel(); text.setOpaque(false); text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(wrapLabel(session.getSkillName() + " · with " + peer, Theme.TEXT, 12, Font.BOLD));
        text.add(label(session.getScheduledAt().format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH)), Theme.MUTED, 10, Font.PLAIN));
        row.add(text, BorderLayout.WEST);
        row.add(pill(session.getStatus().toString(), statusBackground(session.getStatus()), statusForeground(session.getStatus())), BorderLayout.EAST);
        return row;
    }

    private JPanel pageColumn() {
        JPanel page = new PagePanel(); page.setBackground(Theme.BG); page.setBorder(new EmptyBorder(23, 26, 28, 26));
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        return page;
    }
    private JPanel scrollPage(JPanel content) {
        for (Component child : content.getComponents()) {
            Dimension preferred = child.getPreferredSize();
            if (child instanceof javax.swing.JComponent swingChild) swingChild.setAlignmentX(Component.CENTER_ALIGNMENT);
            child.setMaximumSize(new Dimension(MAX_CONTENT_WIDTH, Math.max(0, preferred.height)));
        }
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(Theme.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return wrap(scroll);
    }
    private JPanel wrap(Component component) { JPanel holder = new JPanel(new BorderLayout()); holder.setBackground(Theme.BG); holder.add(component); return holder; }
    private JPanel cardPanel() {
        JPanel card = new SurfacePanel(null);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }
    private JPanel pageIntro(String title, String subtitle) {
        JPanel panel = new JPanel(); panel.setOpaque(false); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(0, 0, 17, 0));
        JLabel titleLabel = label(title, Theme.TEXT, 22, Font.BOLD);
        JLabel subtitleLabel = wrapLabel(subtitle, Theme.MUTED, 12, Font.PLAIN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(subtitleLabel);
        return panel;
    }
    private JPanel sectionHeading(String title, String subtitle) {
        JPanel panel = new JPanel(); panel.setOpaque(false); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel titleLabel = label(title, Theme.TEXT, 14, Font.BOLD);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        if (subtitle != null && !subtitle.isBlank()) {
            JLabel subtitleLabel = wrapLabel(subtitle, Theme.MUTED, 11, Font.PLAIN);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(Box.createVerticalStrut(4)); panel.add(subtitleLabel);
        }
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));
        return panel;
    }
    private JPanel statCard(String title, String value, String caption, Color accent) {
        JPanel card = new SurfacePanel(accent); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 20, 20, 17));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(label(title.toUpperCase(Locale.ROOT), Theme.MUTED, 10, Font.BOLD));
        card.add(Box.createVerticalStrut(8));
        card.add(label(value, accent, 25, Font.BOLD));
        card.add(Box.createVerticalStrut(4));
        card.add(label(caption, Theme.MUTED, 10, Font.PLAIN));
        return card;
    }
    private JPanel emptyMessage(String title, String detail) {
        JPanel panel = new JPanel(); panel.setOpaque(false); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel titleLabel = label(title, Theme.TEXT, 13, Font.BOLD);
        JLabel detailLabel = wrapLabel(detail, Theme.MUTED, 11, Font.PLAIN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel); panel.add(Box.createVerticalStrut(4)); panel.add(detailLabel);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT); return panel;
    }
    private JPanel exchangeLine(String text) {
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(true); row.setBackground(Theme.BG); row.setBorder(new EmptyBorder(9, 11, 9, 11));
        row.add(wrapLabel(text, Theme.TEXT, 12, Font.PLAIN), BorderLayout.WEST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70)); row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }
    private JLabel pill(String text, Color background, Color foreground) {
        JLabel pill = new JLabel("  " + text + "  ") {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getBackground());
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        pill.setOpaque(false); pill.setBackground(background); pill.setForeground(foreground);
        pill.setFont(Theme.font(10, Font.BOLD)); pill.setBorder(new EmptyBorder(5, 3, 5, 3));
        return pill;
    }
    private JButton primaryButton(String text) { return styledButton(text, Theme.BLUE, Color.WHITE, Theme.BLUE); }
    private JButton secondaryButton(String text) { return styledButton(text, Color.WHITE, Theme.BLUE, Theme.BORDER); }
    private JButton textButton(String text) { JButton button = new JButton(text); button.setFont(Theme.font(11, Font.BOLD)); button.setForeground(Theme.BLUE); button.setFocusPainted(false); button.setContentAreaFilled(false); button.setBorder(new EmptyBorder(6, 0, 6, 0)); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return button; }
    private JButton styledButton(String text, Color background, Color foreground, Color border) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int inset = 1;
                Color fill = getBackground();
                if (!isEnabled()) fill = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 125);
                else if (getModel().isPressed()) fill = fill.darker();
                else if (getModel().isRollover()) fill = blend(fill, Color.WHITE, 0.12f);
                g.setColor(fill);
                g.fillRoundRect(inset, inset, getWidth() - 2 * inset, getHeight() - 2 * inset, 13, 13);
                if (border != null && !border.equals(background)) {
                    g.setColor(border);
                    g.drawRoundRect(inset, inset, getWidth() - 2 * inset - 1, getHeight() - 2 * inset - 1, 13, 13);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setFont(Theme.font(12, Font.BOLD)); button.setForeground(foreground); button.setBackground(background);
        button.setFocusPainted(false); button.setContentAreaFilled(false); button.setOpaque(false);
        button.setBorder(new EmptyBorder(9, 15, 9, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return button;
    }
    private JLabel label(String text, Color color, int size, int style) { JLabel label = new JLabel(text); label.setForeground(color); label.setFont(Theme.font(size, style)); return label; }
    private JLabel wrapLabel(String text, Color color, int size, int style) {
        JLabel label = new ResponsiveTextLabel(text);
        label.setForeground(color);
        label.setFont(Theme.font(size, style));
        return label;
    }
    private JTextField textField(String hint) {
        JTextField field = new JTextField();
        field.setFont(Theme.font(12, Font.PLAIN)); field.setForeground(Theme.TEXT); field.setBackground(Theme.CARD);
        field.setCaretColor(Theme.BLUE); field.setToolTipText(hint); field.setMargin(new Insets(5, 9, 5, 9));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(2, 2, 2, 2)));
        field.setPreferredSize(new Dimension(205, 36));
        return field;
    }
    private <T> JComboBox<T> styleComboBox(JComboBox<T> combo) {
        combo.setFont(Theme.font(12, Font.PLAIN)); combo.setForeground(Theme.TEXT); combo.setBackground(Theme.CARD);
        combo.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        combo.setPreferredSize(new Dimension(205, 36));
        return combo;
    }
    private JLabel formLabel(String text) { return label(text, Theme.TEXT, 11, Font.BOLD); }
    private JPanel formPanel() { JPanel panel = new JPanel(new java.awt.GridBagLayout()); panel.setBorder(new EmptyBorder(9, 8, 9, 8)); return panel; }
    private void addFormRow(JPanel panel, int row, String name, Component input) {
        java.awt.GridBagConstraints left = new java.awt.GridBagConstraints(); left.gridx = 0; left.gridy = row; left.anchor = java.awt.GridBagConstraints.LINE_START; left.insets = new Insets(6, 5, 6, 12);
        java.awt.GridBagConstraints right = new java.awt.GridBagConstraints(); right.gridx = 1; right.gridy = row; right.fill = java.awt.GridBagConstraints.HORIZONTAL; right.weightx = 1; right.insets = new Insets(6, 0, 6, 5);
        panel.add(formLabel(name), left); panel.add(input, right);
    }
    private Color statusBackground(Session.Status status) {
        return switch (status) { case PENDING -> Theme.AMBER_PALE; case ACCEPTED -> Theme.BLUE_PALE; case COMPLETED -> Theme.GREEN_PALE; case CANCELLED -> Theme.RED_PALE; };
    }
    private Color statusForeground(Session.Status status) {
        return switch (status) { case PENDING -> Theme.AMBER; case ACCEPTED -> Theme.BLUE; case COMPLETED -> Theme.GREEN; case CANCELLED -> Theme.RED; };
    }
    private Student currentStudent() { return service.getStudent(currentStudentId); }
    private String firstName(String name) { return name.split("\\s+", 2)[0]; }
    private void setSaveMessage(String text) { saveMessage = text; saveStatusColor = Theme.GREEN; refreshSaveStatus(); }
    private void runAction(IORunnable action) {
        try { action.run(); }
        catch (IllegalArgumentException | IllegalStateException ex) { showError(ex.getMessage()); }
        catch (IOException ex) { saveMessage = "Save failed"; saveStatusColor = Theme.RED; refreshSaveStatus(); showError("The change could not be saved to local storage. Your previous saved data is still available. Check that the data folder is writable, then try again."); }
    }
    private void showError(String message) { JOptionPane.showMessageDialog(this, message, "Skill Swap", JOptionPane.WARNING_MESSAGE); }

    private record StudentChoice(String id, String name) {
        @Override public String toString() { return name; }
    }

    private static Color blend(Color from, Color to, float amount) {
        float inverse = 1 - amount;
        return new Color(Math.round(from.getRed() * inverse + to.getRed() * amount),
                Math.round(from.getGreen() * inverse + to.getGreen() * amount),
                Math.round(from.getBlue() * inverse + to.getBlue() * amount));
    }

    private static class SurfacePanel extends JPanel {
        private final Color accent;
        SurfacePanel(Color accent) {
            this.accent = accent;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(54, 42, 32, 15));
            g.fillRoundRect(2, 4, w - 4, h - 6, 18, 18);
            g.setColor(Theme.CARD);
            g.fillRoundRect(1, 1, w - 4, h - 6, 18, 18);
            g.setColor(Theme.BORDER);
            g.drawRoundRect(1, 1, w - 4, h - 6, 18, 18);
            if (accent != null && h > 34) {
                g.setColor(accent);
                g.fillRoundRect(8, 13, 4, h - 29, 4, 4);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class HeroPanel extends JPanel {
        HeroPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(54, 42, 32, 28));
            g.fillRoundRect(2, 5, w - 4, h - 7, 24, 24);
            g.setPaint(new GradientPaint(0, 0, new Color(179, 71, 65), w, h, new Color(82, 61, 105)));
            g.fillRoundRect(1, 1, w - 4, h - 7, 24, 24);
            g.setColor(new Color(255, 255, 255, 30));
            g.fillOval(w - 162, -46, 162, 162);
            g.setColor(new Color(255, 255, 255, 22));
            g.fillOval(w - 115, 37, 80, 80);
            g.setColor(new Color(255, 255, 255, 80));
            g.drawOval(w - 145, -29, 128, 128);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class BrandMark extends JLabel {
        BrandMark() {
            super("S", SwingConstants.CENTER);
            setForeground(Color.WHITE);
            setFont(Theme.font(21, Font.BOLD));
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.BLUE);
            g.fillOval(1, 1, getWidth() - 3, getHeight() - 3);
            g.setColor(new Color(255, 255, 255, 100));
            g.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private final class PagePanel extends JPanel implements javax.swing.Scrollable {
        private int horizontalPadding = -1;
        @Override public void doLayout() {
            int padding = getWidth() > 0 && getWidth() < 540 ? 14 : 26;
            if (horizontalPadding != padding) {
                horizontalPadding = padding;
                setBorder(new EmptyBorder(20, padding, 24, padding));
            }
            super.doLayout();
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return Math.max(18, visibleRect.height - 32); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private final class ResponsiveTextLabel extends JLabel {
        private final String plainText;
        private int lastWidth = -1;
        private boolean updatePending;
        ResponsiveTextLabel(String text) {
            super(text);
            plainText = text;
        }
        @Override public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            if (width > 40 && Math.abs(width - lastWidth) > 3) {
                lastWidth = width;
                String safe = plainText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                super.setText("<html><div style='width:" + Math.max(40, width - 8) + "px'>" + safe + "</div></html>");
                if (!updatePending) {
                    updatePending = true;
                    SwingUtilities.invokeLater(() -> {
                        updatePending = false;
                        refreshAdaptiveAncestors(this);
                    });
                }
            }
        }
    }

    private final class ResponsiveGridPanel extends JPanel {
        private final GridLayout grid;
        private final int maxColumns;
        private int columns = -1;
        ResponsiveGridPanel(int maxColumns, int horizontalGap, int verticalGap) {
            this.maxColumns = maxColumns;
            grid = new GridLayout(1, maxColumns, horizontalGap, verticalGap);
            setLayout(grid);
            setOpaque(false);
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent event) { updateColumns(); }
            });
        }
        private void updateColumns() {
            int width = getWidth();
            int nextColumns = width < 480 ? 1 : width < 1000 ? Math.min(2, maxColumns) : maxColumns;
            if (nextColumns == columns) return;
            columns = nextColumns;
            int rows = Math.max(1, (getComponentCount() + columns - 1) / columns);
            grid.setRows(rows);
            grid.setColumns(columns);
            revalidate();
            refreshAdaptiveHeight(this);
        }
    }

    private final class ResponsiveRegistrationForm extends JPanel {
        private final GridLayout grid = new GridLayout(3, 4, 10, 8);
        private boolean compact;
        ResponsiveRegistrationForm() {
            setLayout(grid);
            setOpaque(false);
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent event) {
                    boolean nextCompact = getWidth() < 680;
                    if (nextCompact == compact) return;
                    compact = nextCompact;
                    grid.setRows(compact ? 6 : 3);
                    grid.setColumns(compact ? 2 : 4);
                    revalidate();
                    if (getParent() != null) refreshAdaptiveHeight(getParent());
                }
            });
        }
    }

    @FunctionalInterface private interface IORunnable { void run() throws IOException; }
}
