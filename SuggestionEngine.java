import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.function.Predicate;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collections;

public class SuggestionEngine
{
	private List<DesktopEntry> candidates;
	private HashMap<String, Integer> history;

	public SuggestionEngine(List<DesktopEntry> candidates)
	{
		this.candidates = candidates;
		this.loadHistory();

		// Sort by frequency
		Collections.sort(this.candidates, (a, b) -> Integer.compare(
				history.containsKey(b.name) ? history.get(b.name) : 0,
				history.containsKey(a.name) ? history.get(a.name) : 0));
	}

	private void loadHistory()
	{
		this.history = new HashMap<String, Integer>();
		File directory = new File(new File(System.getProperty("user.home")), 
			".launcher");
		File historyFile = new File(directory, "history");
		if (historyFile.exists())
		{
			try (BufferedReader reader = new BufferedReader(
				new FileReader(historyFile)))
			{
				for (String line = reader.readLine(); line != null;
					line = reader.readLine())
				{
					int split = line.indexOf(':');
					if (split >= 0)
					{
						String name = line.substring(split + 1, line.length());
						String count = line.substring(0, split);

						this.history.put(name, Integer.parseInt(count));
					}
				}
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
	}

	public void registerSelection(DesktopEntry entry)
	{
		int count = this.history.containsKey(entry.name) ?
			this.history.get(entry.name) : 0;
		this.history.put(entry.name, count + 1);

		this.saveHistory();
	}

	private void saveHistory()
	{
		File directory = new File(new File(System.getProperty("user.home")), 
			".launcher");
		if (!directory.exists())
			directory.mkdir();
		
		File historyFile = new File(directory, "history");
		try (PrintWriter writer = new PrintWriter(
			new FileWriter(historyFile, false)))
		{
			for (Map.Entry<String, Integer> entry : this.history.entrySet())
			{
				writer.println(entry.getValue() + ":" + entry.getKey());
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	public List<DesktopEntry> getSuggestions(String partial)
	{
		ArrayList<DesktopEntry> suggestions = new ArrayList<>();
		partial = partial.toLowerCase();

		// TODO: sort individual sublists by frequency before merging

		// 1. Start of name
		for (DesktopEntry candidate : this.candidates)
			if (candidate.name.toLowerCase().startsWith(partial) && !suggestions.contains(candidate))
				suggestions.add(candidate);

		// 2. Start of name segment
		for (DesktopEntry candidate : this.candidates)
			for (String segment : candidate.name.split(" "))
				if (segment.toLowerCase().startsWith(partial) && !suggestions.contains(candidate))
					suggestions.add(candidate);

		// 3. Part of name
		for (DesktopEntry candidate : this.candidates)
			if (candidate.name.toLowerCase().contains(partial) && !suggestions.contains(candidate))
				suggestions.add(candidate);

		// 4. Start of command
		for (DesktopEntry candidate : this.candidates)
			if (candidate.exec != null && candidate.exec.toLowerCase().startsWith(partial) && !suggestions.contains(candidate))
				suggestions.add(candidate);

		// 5. Part of description
		for (DesktopEntry candidate : this.candidates)
			if (candidate.comment != null && candidate.comment.toLowerCase().contains(partial) && !suggestions.contains(candidate))
				suggestions.add(candidate);

		return suggestions;
	}

	private static <T> List<T> filter(List<T> list, Predicate<T> pred)
	{
		ArrayList<T> positive = new ArrayList<T>();
		Iterator<T> it = list.iterator();
		while (it.hasNext())
		{
			T current = it.next();
			if (pred.test(current))
			{
				positive.add(current);
				it.remove();
			}
		}
		return positive;
	}
}
