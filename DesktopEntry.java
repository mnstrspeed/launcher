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
					String[] segments = line.split("=");
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
		return findIcon(this.icon, "hicolor");
	}

	private static String findIcon(String icon, String theme)
	{
		if (icon == null)
			return null;

		String maxIcon = "";
		int maxSize = -1;

		File themeFolder = new File("/usr/share/icons/" + theme);
		for (File sizeFolder : themeFolder.listFiles())
		{
			if (sizeFolder.isDirectory() && sizeFolder.getName().contains("x"))
			{
				int size = Integer.parseInt(sizeFolder.getName().split("x")[0]);
				String extension = icon.contains(".") ? "" : ".png";
				File iconFile = new File(sizeFolder, "apps/" + icon + extension);
				if (iconFile.exists() && size > maxSize)
				{
					maxIcon = iconFile.getPath();
					maxSize = size;
				}
			}
		}
		return maxIcon;
	}
}
