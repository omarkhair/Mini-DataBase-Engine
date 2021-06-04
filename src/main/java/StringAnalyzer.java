
import java.util.Vector;

public class StringAnalyzer {

	private Vector<String> words;
	private int wordIndex;

	public StringAnalyzer(String s) {
		words = new Vector<String>();
		boolean isVarchar = false;
		boolean isChar = false;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '\'' && isVarchar) {
				sb.append('\'');
				words.add(sb.toString());
				isVarchar = false;
				sb = new StringBuilder();
			} else if (!isVarchar && s.charAt(i) == '\'') {
				sb.append('\'');
				isVarchar = true;
			} else if (isVarchar) {
				sb.append(s.charAt(i));
			} else if (isChar && isDelimiter(s.charAt(i))) {
				words.add(sb.toString());
				isChar = false;
				sb = new StringBuilder();
				if (isDelimiter2(s.charAt(i)))
					words.add(s.charAt(i) + "");
			} else if (!isDelimiter(s.charAt(i)) && !isOperator(s.charAt(i))) {
				sb.append(s.charAt(i));
				isChar = true;
			} else if (isDelimiter2(s.charAt(i))) {
				words.add(s.charAt(i) + "");
			} else if (isChar && isOperator(s.charAt(i))) {
				words.add(sb.toString());
				isChar = false;
				sb = new StringBuilder();
				if (isOperator(s.charAt(i + 1)))
					words.add(s.charAt(i++) + "" + s.charAt(i));
				else
					words.add(s.charAt(i) + "");
			} else if (!isChar && isOperator(s.charAt(i))) {
				isChar = false;
				sb = new StringBuilder();
				if (isOperator(s.charAt(i + 1)))
					words.add(s.charAt(i++) + "" + s.charAt(i));
				else
					words.add(s.charAt(i) + "");
			}
		}
		if (sb.length() != 0)
			words.add(sb.toString());
		wordIndex = 0;
	}

	private static boolean isOperator(char c) {
		return c == '>' || c == '<' || c == '=' || c == '!';
	}

	private static boolean isDelimiter(char c) {
		return c == ' ' || c == ',' || c == '(' || c == ')' || c == '\n' || c == '\r';
	}

	private static boolean isDelimiter2(char c) {
		return c == ',' || c == '(' || c == ')';
	}

	public String nextWord() throws DBAppException {
		if (wordIndex == words.size())
			throw new DBAppException("invalid SQL program");
		if (words.get(wordIndex).charAt(0) != '\'')
			return words.get(wordIndex++).toLowerCase();
		else
			return words.get(wordIndex++);
	}

	public String readNextWord() throws DBAppException {
		if (wordIndex == words.size())
			throw new DBAppException("invalid SQL program");
		if (words.get(wordIndex).charAt(0) != '\'')
			return words.get(wordIndex).toLowerCase();
		else
			return words.get(wordIndex);
	}

	public boolean hasMoreWords() {
		return !(wordIndex == words.size());
	}

	public static void main(String[] args) throws DBAppException {
		StringAnalyzer sa = new StringAnalyzer("nAme = 'Omar khAir'");
		while (sa.hasMoreWords())
			System.out.println(sa.nextWord());

	}
}
