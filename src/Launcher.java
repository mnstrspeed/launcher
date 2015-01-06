import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.imageio.ImageIO;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

@SuppressWarnings("serial")
public class Launcher extends JWindow
		implements KeyListener, DocumentListener
{
	private static final int PORT = 62321;
	private static Launcher instance;

	public static void main(String[] args)
	{
		try
		{
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress("127.0.0.1", PORT), 50);
			socket.getOutputStream().write(42);
			socket.close();
		}
		catch (Exception ex)
		{
			// Start new
			JFrame owner = new JFrame();
			owner.setVisible(true);
			instance = new Launcher(owner);
			new Thread(new Runnable() {
				@Override
				public void run()
				{
					try
					{
						ServerSocket server = new ServerSocket(PORT);
						while (true)
						{
							Socket connection = server.accept();
							if (connection.getInputStream().read() == 42)
								instance.showLauncher();
							connection.close();
						}
					}
					catch (Exception ex)
					{
						throw new RuntimeException(ex);
					}
				}
			}).start();

			if (!Arrays.asList(args).contains("--silent"))
				instance.showLauncher();
		}
	}
	
	private List<DesktopEntry> desktopEntries;
	private SuggestionEngine suggestionEngine;

	private JTextField textField;
	private JList<DesktopEntry> suggestionList;
	private static final int SUGGESTION_LIMIT = 5;

	public Launcher(JFrame owner)
	{
		super(owner);

		this.setAlwaysOnTop(true);
		this.setFocusableWindowState(true);
		this.initializeComponents();

		this.loadDesktopEntries();
		this.suggestionEngine = new SuggestionEngine(
				this.desktopEntries);
	}

	public void showLauncher()
	{
		this.textField.setText("");
		this.updateSuggestions();
		
		this.setVisible(true);
		this.setLocationRelativeTo(null);
	}

	public void hideLauncher()
	{
		this.setVisible(false);
	}
	
	protected void initializeComponents()
	{
		this.setBackground(Color.WHITE);
		
		this.textField = new JTextField();
		this.textField.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		this.textField.setFont(new Font("FreeSans", Font.PLAIN, 28));
		this.textField.setOpaque(false);
		this.textField.addKeyListener(this);
		this.textField.getDocument().addDocumentListener(this);

		this.suggestionList = new JList<DesktopEntry>();
		this.suggestionList.setOpaque(false);
		this.suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.suggestionList.setLayoutOrientation(JList.VERTICAL);
		this.suggestionList.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
		this.suggestionList.setCellRenderer(new SuggestionListCell.Renderer());
		this.suggestionList.addMouseMotionListener(new MouseAdapter() {
			public void mouseMoved(MouseEvent e)
			{
				suggestionList.setSelectedIndex(suggestionList.locationToIndex(
						new Point(e.getX(), e.getY())));
			}
		});

		this.getContentPane().setLayout(new BoxLayout(
			this.getContentPane(), BoxLayout.Y_AXIS));
		this.getContentPane().add(this.textField);
		this.getContentPane().add(this.suggestionList);
	}

	protected void loadDesktopEntries()
	{
		this.desktopEntries = new ArrayList<DesktopEntry>();
		this.loadDesktopEntries("/usr/share/applications");
		this.loadDesktopEntries(System.getProperty("user.home") + 
			"/.local/share/applications");
	}

	protected void loadDesktopEntries(String root)
	{
		for (File file : new File(root).listFiles())
			if (file.isFile() && file.getName().endsWith(".desktop"))
			{
				DesktopEntry entry = DesktopEntry.fromDesktopFile(file.getPath());
				if (entry.showInMenu("Unity"))
					this.desktopEntries.add(entry);
			}
	}

	protected void updateSuggestions()
	{
		List<DesktopEntry> suggestions = this.suggestionEngine
				.getSuggestions(this.textField.getText());
		if (suggestions.size() > SUGGESTION_LIMIT)
			suggestions = suggestions.subList(0, SUGGESTION_LIMIT);
		
		this.suggestionList.setListData(new Vector<DesktopEntry>(suggestions));
		this.suggestionList.setSelectedIndex(0);
		this.pack();
	}

	protected void launchSelected()
	{
		DesktopEntry selected = this.suggestionList.getSelectedValue();
		if (selected != null)
		{
			this.suggestionEngine.registerSelection(selected);
			try
			{
				System.out.println(selected.exec);
				Runtime.getRuntime().exec(selected.exec);
			}
			catch (IOException ex)
			{
				JOptionPane.showMessageDialog(this, 
					"Unable to launch " + selected.name);
			}
			this.hideLauncher();
		}
	}

	@Override
	public void insertUpdate(DocumentEvent e)
	{
		this.updateSuggestions(); 
	}

	@Override
	public void removeUpdate(DocumentEvent e)
	{ 
		this.updateSuggestions();
	}

	@Override
	public void changedUpdate(DocumentEvent e)
	{
		this.updateSuggestions(); 
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			this.hideLauncher();
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
			this.launchSelected();
		if (e.getKeyCode() == KeyEvent.VK_UP)
			this.suggestionList.setSelectedIndex(this.suggestionList.getSelectedIndex() - 1);
		if (e.getKeyCode() == KeyEvent.VK_DOWN)
			this.suggestionList.setSelectedIndex(this.suggestionList.getSelectedIndex() + 1);
	}

	@Override public void keyReleased(KeyEvent e) { }
	@Override public void keyTyped(KeyEvent e) { }

	public static class SuggestionListCell extends JComponent
	{
		public static class Renderer implements ListCellRenderer<DesktopEntry>
		{
			@Override
			public Component getListCellRendererComponent(
					JList<? extends DesktopEntry> list, DesktopEntry value,
					int index, boolean isSelected, boolean cellHasFocus)
			{
				return new SuggestionListCell(value, isSelected);
			}
		}
		
		private DesktopEntry entry;
		private boolean selected;
		
		public SuggestionListCell(DesktopEntry entry, boolean selected)
		{
			this.entry = entry;
			this.selected = selected;
		}
		
		@Override
		public Dimension getMinimumSize()
		{
			return new Dimension(400, 48 + 5);
		}
		
		@Override
		public Dimension getPreferredSize()
		{
			return this.getMinimumSize();
		}

		@Override
		public Dimension getMaximumSize()
		{
			return this.getMinimumSize();
		}
		
		private final Color selectedColor = new Color(1.0f, 1f, 1f, 0.9f);
		private final Color nameColor = Color.BLACK;
		private final Color commentColor = Color.GRAY;
		private final Color commandColor = Color.LIGHT_GRAY;
		
		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			
			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING, 
					RenderingHints.VALUE_ANTIALIAS_ON);
			
			String fontName = g.getFont().getName();
			Font nameFont = new Font(fontName, Font.PLAIN, 16);
			Font commentFont = new Font(fontName, Font.PLAIN, 14);
			int nameFontAscent = g.getFontMetrics(nameFont).getAscent();
			int nameFontHeight = g.getFontMetrics(nameFont).getHeight();
			int commentFontAscent = g.getFontMetrics(commentFont).getAscent();
			
			if (this.selected)
			{
				g.setColor(selectedColor);
				g.fillRect(0, 0, this.getWidth(), 64);
			}

			try
			{
				String iconPath = entry.findIcon();
				System.out.println("Loading " + iconPath);
				if (iconPath == null)
					throw new IOException("No icon found");

				BufferedImage icon = ImageIO.read(new File(iconPath));
				g.drawImage(icon, 0, 0, 48, 48, null);
			}
			catch (IOException ex)
			{ }

			g.setFont(nameFont);
			g.setColor(nameColor);
			g.drawString(entry.name, 
				48 + 15, 
				4 + nameFontAscent);
			
			if (entry.exec != null)
			{
				g.setFont(commentFont);
				g.setColor(commandColor);	
				g.drawString(truncateText(entry.exec, 300, g.getFontMetrics()), 
					48 + 15 + g.getFontMetrics(nameFont).stringWidth(entry.name + " "),
					4 + nameFontAscent);
			}
			
			if (entry.comment != null)
			{
				g.setFont(commentFont);
				g.setColor(commentColor);
				g.drawString(truncateText(entry.comment, 300, g.getFontMetrics()),
					48 + 15,
					4 + nameFontHeight + commentFontAscent); 
			}
		}
		
		private String truncateText(String text, int availableWidth,
				FontMetrics metrics)
		{
			int currentChar, currentWidth = 0;
			for (currentChar = 0; currentChar < text.length() &&
				currentWidth < availableWidth; currentChar++)
			{
				currentWidth += metrics.charWidth(text.charAt(currentChar));
			}

			return currentChar >= text.length() ? text :
				text.substring(0, currentChar) + "...";
		}
	}
}
