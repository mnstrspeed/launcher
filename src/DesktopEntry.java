import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DesktopEntry
{
	public String type;
	public String name;
	public String comment;
	public String exec;
	public String icon;
	public boolean noDisplay = false;
	private List<String> onlyShowIn;

	public DesktopEntry()
	{
		this.onlyShowIn = new ArrayList<String>();
	}

	public String getType()
	{
		return this.type;
	}

	public String getName()
	{
		return this.name;
	}

	public String getComment()
	{
		return this.comment;
	}

	public String getExec()
	{
		return this.exec;
	}
	
	public boolean showInMenu(String ownIdentifier)
	{
		return !this.noDisplay && (this.onlyShowIn.isEmpty() || 
			this.onlyShowIn.contains(ownIdentifier));
	}

	public static DesktopEntry fromDesktopFile(String path)
	{
		DesktopEntry entry = new DesktopEntry();
		boolean readingDesktopEntry = false;

		try (BufferedReader reader = new BufferedReader(
			new FileReader(path)))
		{
			for (String line = reader.readLine(); line != null;
				line = reader.readLine())
			{
				line = line.trim();
				if (line.contains("=") && readingDesktopEntry)
				{
					String[] segments = line.split("=", 2);
					if (segments.length == 2)
					{
						String key = segments[0].trim();
						String value = segments[1].trim();

						if (key.equals("Type"))
							entry.type = value;
						if (key.equals("Name"))
							entry.name = value;
						if (key.equals("Comment"))
							entry.comment = value;
						if (key.equals("Exec"))
							entry.exec = value;
						if (key.equals("Icon"))
							entry.icon = value;
						if (key.equals("NoDisplay"))
							entry.noDisplay = Boolean.parseBoolean(value);
						if (key.equals("OnlyShowIn"))
							for (String identifier : value.split(";"))
								entry.onlyShowIn.add(identifier);
					}
				}
				else if (line.startsWith("["))
				{
					readingDesktopEntry = line.equals("[Desktop Entry]");
				}
			}
		}
		catch (IOException ex)
		{
			throw new RuntimeException(ex);
		}

		return entry;
	}

	public String findIcon()
	{
		String[] themes = new String[] {
			"/usr/share/icons/hicolor",
			System.getProperty("user.home") + "/.local/share/icons/hicolor"
		};

		for (String theme : themes)
		{
			String path = findIcon(this.icon, theme, 48);
			if (path != null)
				return path;
		}
		return null;
	}

	private static String findIcon(String icon, String themeRoot, int targetSize)
	{
		if (icon == null)
			return null;

		String bestIcon = "";
		int bestSize = -1;

		File themeFolder = new File(themeRoot);
		for (File sizeFolder : themeFolder.listFiles())
		{
			if (sizeFolder.isDirectory() && sizeFolder.getName().contains("x"))
			{
				int size = Integer.parseInt(sizeFolder.getName().split("x")[0]);
				String extension = icon.contains(".") ? "" : ".png";
				File iconFile = new File(sizeFolder, "apps/" + icon + extension);
				if (iconFile.exists())
				{
					System.out.println(iconFile.getPath());
					// TODO: this doesn't work =(((((
					if ((bestSize < targetSize && size > bestSize) ||
						(bestSize > targetSize && size > targetSize && 
							size >= targetSize))
					{
						bestIcon = iconFile.getPath();
						bestSize = size;
					}
				}
			}
		}
		return bestIcon;
	}
}
