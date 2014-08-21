import javax.swing.JFrame;
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
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;

@SuppressWarnings("serial")
public class Launcher extends JFrame
		implements KeyListener, DocumentListener
{
	private List<DesktopEntry> desktopEntries;
	private SuggestionEngine suggestionEngine;

	private JTextField textField;
	private JList<DesktopEntry> suggestionList;
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
		this.initializeComponents();

		this.loadDesktopEntries();
		this.suggestionEngine = new SuggestionEngine(
				this.desktopEntries);
		this.updateSuggestions();

		this.pack();
		this.setLocationRelativeTo(null);
		
		this.setVisible(true);
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

		this.getContentPane().setLayout(new BoxLayout(
			this.getContentPane(), BoxLayout.Y_AXIS));
		this.getContentPane().add(this.textField);
		this.getContentPane().add(this.suggestionList);
	}

	protected void loadDesktopEntries()
	{
		long startTime = System.currentTimeMillis();
		
		this.desktopEntries = new ArrayList<DesktopEntry>();
		this.loadDesktopEntries("/usr/share/applications");
		this.loadDesktopEntries(System.getProperty("user.home") + 
			"/.local/share/applications");
		
		System.out.println("Loading entries took " + (System.currentTimeMillis() - startTime) + " ms");
	}

	protected void loadDesktopEntries(String root)
	{
		for (File file : new File(root).listFiles())
			if (file.isFile() && file.getName().endsWith(".desktop"))
			{
				DesktopEntry entry = DesktopEntry.fromDesktopFile(file.getPath());
				if (entry.showInMenu("Launcher"))
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
			int nameFontAscent = g.getFontMetrics(nameFont).getAscent();
			int nameFontHeight = g.getFontMetrics(nameFont).getHeight();
			Font commentFont = new Font(fontName, Font.PLAIN, 14);
			int commentFontAscent = g.getFontMetrics(commentFont).getAscent();

			Color selectedColor = new Color(1.0f, 1f, 1f, 0.9f);
			Color nameColor = Color.BLACK;
			Color commentColor = Color.GRAY;
			Color commandColor = Color.LIGHT_GRAY;
			
			if (this.selected)
			{
				g.setColor(selectedColor);
				g.fillRect(0, 0, this.getWidth(), 64);
			}

			try
			{
				String iconPath = entry.findIcon();
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
