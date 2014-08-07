import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Predicate;

public class SuggestionEngine
{
	private List<DesktopEntry> candidates;

	public SuggestionEngine(List<DesktopEntry> candidates)
	{
		this.candidates = candidates;
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
