import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.imageio.ImageIO;
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
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class Launcher extends JFrame
		implements KeyListener, DocumentListener
{
	private List<DesktopEntry> desktopEntries;
	private SuggestionEngine suggestionEngine;

	private JTextField textField;
	private SuggestionList suggestionList;
	private static final int SUGGESTION_LIMIT = 5;

	public static void main(String[] args)
	{
		new Launcher();
	}

	public Launcher()
	{
		super();

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setUndecorated(true);

		this.setBackground(Color.WHITE);
		
		this.textField = new JTextField();
		this.textField.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		this.textField.setFont(new Font("FreeSans", Font.PLAIN, 28));
		this.textField.setOpaque(false);
		this.textField.addKeyListener(this);
		this.textField.getDocument().addDocumentListener(this);

		this.suggestionList = new SuggestionList(SUGGESTION_LIMIT);
		this.suggestionList.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));

		this.getContentPane().setLayout(new BoxLayout(
			this.getContentPane(), BoxLayout.Y_AXIS));
		this.getContentPane().add(this.textField);
		this.getContentPane().add(this.suggestionList);

		this.loadDesktopEntries();
		this.suggestionEngine = new SuggestionEngine(this.desktopEntries);
		this.updateSuggestions();

		this.pack();
		
		this.setLocationRelativeTo(null);
//		Point location = this.getLocation();
//		this.setLocation(
//			(int)location.getX(),
//			(int)(location.getY() - this.suggestionList.getMaximumSize().getHeight() / 2.0));

		this.setVisible(true);
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
				this.desktopEntries.add(DesktopEntry.fromDesktopFile(file.getPath()));
	}

	protected void updateSuggestions()
	{
		this.suggestionList.show(this.suggestionEngine.getSuggestions(
			this.textField.getText()));
		this.pack();
	}

	protected void launchSelected()
	{
		DesktopEntry selected = this.suggestionList.getSelected();
		if (selected != null)
		{
			try
			{
				Runtime.getRuntime().exec(selected.exec);
			}
			catch (IOException ex)
			{
				JOptionPane.showMessageDialog(this, 
					"Unable to launch " + selected.name);
				System.exit(1);
			}
			System.exit(0);
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
			System.exit(0);
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
			this.launchSelected();
		if (e.getKeyCode() == KeyEvent.VK_UP)
			this.suggestionList.moveSelectionUp();
		if (e.getKeyCode() == KeyEvent.VK_DOWN)
			this.suggestionList.moveSelectionDown();
	}

	@Override public void keyReleased(KeyEvent e) { }
	@Override public void keyTyped(KeyEvent e) { }

	public class SuggestionList extends JComponent
	{
		private List<DesktopEntry> suggestions;
		private int suggestionLimit;
		private int selected = 0;

		public SuggestionList(int suggestionLimit)
		{
			super();
			this.suggestions = new ArrayList<DesktopEntry>();
			this.suggestionLimit = suggestionLimit;
		}

		public void show(List<DesktopEntry> suggestions)
		{
			if (suggestions.size() > this.suggestionLimit)
				suggestions = suggestions.subList(0, this.suggestionLimit);
			this.suggestions = suggestions;
			this.selected = 0;

			System.out.println("");
			for (DesktopEntry entry : this.suggestions)
				System.out.println(entry.name + "(" + entry.icon + " at " +
					entry.findIcon() + ")");

			this.repaint();
		}

		public DesktopEntry getSelected()
		{
			if (this.selected >= this.suggestions.size())
				return null;
			return this.suggestions.get(this.selected);
		}
	
		public void moveSelectionUp()
		{
			this.selected--;
			if (this.selected < 0)
				this.selected = this.suggestions.size() - 1;

			this.repaint();
		}

		public void moveSelectionDown()
		{
			this.selected++;
			this.selected %= this.suggestions.size();
			
			this.repaint();
		}

		@Override
		public Dimension getPreferredSize()
		{
			return this.getSizeFor(this.suggestions.size());
		}

		@Override
		public Dimension getMinimumSize()
		{
			return this.getPreferredSize();
		}

		@Override
		public Dimension getMaximumSize()
		{
			return this.getSizeFor(this.suggestionLimit);
		}

		private Dimension getSizeFor(int nr)
		{
			Insets insets = this.getInsets();
			return new Dimension(
				400 + insets.left + insets.right,
				nr * 64 + insets.top + insets.bottom);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Insets insets = this.getInsets();

			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
			
			String fontName = g.getFont().getName();
			Font nameFont = new Font(fontName, Font.PLAIN, 16);
			int nameFontAscent = g.getFontMetrics(nameFont).getAscent();
			int nameFontHeight = g.getFontMetrics(nameFont).getHeight();
			Font commentFont = new Font(fontName, Font.PLAIN, 14);
			int commentFontAscent = g.getFontMetrics(commentFont).getAscent();
			int commentFontHeight = g.getFontMetrics(commentFont).getHeight();

			Color selectedColor = Color.WHITE;

			Color nameColor = Color.BLACK;
			Color commentColor = Color.GRAY;
			Color commandColor = Color.LIGHT_GRAY;

			for (int i = 0; i < this.suggestions.size(); i++)
			{
				if (i == this.selected)
				{
					g.setColor(selectedColor);
					g.fillRect(0, i * 64, this.getWidth(), 64);
				}

				DesktopEntry entry = this.suggestions.get(i);
				try
				{
					String iconPath = entry.findIcon();
					if (iconPath == null)
						throw new IOException("No icon found");

					BufferedImage icon = ImageIO.read(new File(iconPath));
					g.drawImage(icon, 
						insets.left + 0, 
						insets.top + i * 64, 
						64, 64, null);
				}
				catch (IOException ex)
				{ }

				g.setFont(nameFont);
				g.setColor(nameColor);
				g.drawString(entry.name, 
					insets.left + 64 + 15, 
					4 + insets.top + i * 64 + nameFontAscent);
				
				if (entry.comment != null)
				{
					g.setFont(commentFont);
					g.setColor(commentColor);
					g.drawString(truncateText(entry.comment, 300, g.getFontMetrics()),
						insets.left + 64 + 15,
						4 + insets.top + i * 64 + nameFontHeight + commentFontAscent); 
				}

				if (entry.exec != null)
				{
					g.setFont(commentFont);
					g.setColor(commandColor);	
					g.drawString(truncateText(entry.exec, 300, g.getFontMetrics()), 
						insets.left + 64 + 15,
						4 + insets.top + i * 64 + nameFontHeight 
							+ commentFontHeight + commentFontAscent);
				}
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
