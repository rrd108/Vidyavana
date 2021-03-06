package hu.vidyavana.convert.ed;

import hu.vidyavana.convert.api.*;
import hu.vidyavana.convert.api.WriterInfo.SpecialFile;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static hu.vidyavana.convert.ed.EdPreviousEntity.*;

public class EdFileProcessor implements FileProcessor
{
    public static final boolean NO_SPACES_AFTER_WBW_DASH = false;

	private File destDir;
	private String srcFileName;
	private WriterInfo writerInfo;
	private String ebookPath;
	private List<String> manual;

	private boolean currentBookHasLeftAlignLeadParagraph = false;
	
	private int lineNumber;
	private Book book;
	private Chapter chapter;
	private Paragraph para;
	private EdTags currentTag;
	private EdTags currentAlias;
	private int nextPos;
	private EdPreviousEntity prev;
	private Stack<String> formatStack;
	private boolean skippingUnhandledTag;
	private int lastTextTag;
	private int fontCode;
	private int microspace;
	private int emspace;
	private boolean superscript;
	private StringBuilder deferredMarkup;
	private File tocFile;


	@Override
	public void init(File srcDir, File destDir)
	{
		this.destDir = destDir;
		writerInfo = new WriterInfo();
		writerInfo.fileNames = new ArrayList<>();
		//writerInfo.forEbook = !"false".equals(System.getProperty("for.ebook"));
		ebookPath = System.getProperty("ebook.path");
		manual = new ArrayList<String>();
		
		if(!writerInfo.forEbook)
		{
			// xml TOC file
			try
			{
				tocFile = new File(destDir.getAbsolutePath() + "/toc.xml");
				Writer toc = writerInfo.toc = new OutputStreamWriter(new FileOutputStream(tocFile), "UTF-8");
				toc.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
				toc.write("<toc>\r\n");
				toc.write("  <version>");
				toc.write(new SimpleDateFormat("yyMMdd").format(new Date()));
				toc.write("</version>\r\n");
				toc.write("  <entries>\r\n");
			}
			catch(IOException ex)
			{
				throw new RuntimeException("Error initializing TOC file.", ex);
			}
		}
	}


	@Override
	public void process(File srcFile, String fileName) throws Exception
	{
		srcFileName = fileName;
		writerInfo.specialFile = WriterInfo.SpecialFile.fnameMap.get(fileName);
		File destFile = new File(destDir.getAbsolutePath() + "/" + fileName + ".xml");
		process(srcFile, destFile);
		
		// reminders of manual work
		if(fileName.toLowerCase().indexOf("hund28xt") != -1)
		{
			manual.add("NOD: initials separate word in pf,1xt,3,7,8,10,11,17,20,21,26,30,32,36,38,45,46,48,51!");
			manual.add("hund28xt.h50: footnote to be merged!");
		}
		else if(fileName.toLowerCase().indexOf("huc219xt") != -1)
			manual.add("huc219xt.h60: para broken in two at footnote!");
		else if(fileName.toLowerCase().indexOf("hukb00dc.h09") != -1) {
			manual.add("hukb00dc.h09.xml: dedication broken lines");
			manual.add("*words<br/>* from George Harrison, toc-ban is");
		}
		else if(fileName.toLowerCase().indexOf("hubg00pf.h10") != -1)
			manual.add("Hubg00pf.h10: @signature speciális tartalma törlendő!");
	}


	@Override
	public void finish()
	{
		Writer toc = writerInfo.toc;
		if(toc != null)
			try
			{
				if(writerInfo.diacritics != null)
				{
					toc.close();
					toc = writerInfo.toc = new OutputStreamWriter(new FileOutputStream(tocFile), "UTF-8");
					for(Map.Entry<String, Object> e : writerInfo.diacritics.entrySet())
					{
						String value = "";
						if(e.getValue() instanceof String)
						{
							value = (String) e.getValue();
							if(e.getKey().equals(value))
								continue;
						}
						else
						{
							TreeSet<String> set = (TreeSet) e.getValue();
							for(String w : set)
								value += w + '|';
							value = value.substring(0, value.length()-1);
						}
						toc.write(e.getKey());
						toc.write("=");
						toc.write(value);
						toc.write("\r\n");
					}
					toc.close();
					return;
				}

				if(ProofreadWords.ACTIVE) {
					writerInfo.proofreadWords.writeFiles(destDir, writerInfo);
				}
				
				toc.write("  </entries>\r\n");
				toc.write("  <files>\r\n");
				for(String fname : writerInfo.fileNames)
				{
					toc.write("    <file>");
					toc.write(fname);
					toc.write("</file>\r\n");
				}
				toc.write("  </files>\r\n");
				toc.write("</toc>\r\n");
				toc.close();
			}
			catch(IOException ex)
			{
				throw new RuntimeException("Error writing TOC file.", ex);
			}
		
		for(String m : manual)
		{
			System.out.print("!!! ");
			System.out.println(m);
		}
	}


	public void process(File ed, File xml) throws Exception
	{
		lineNumber = 1;
		book = new Book();
		chapter = new Chapter();
		book.chapter.add(chapter);
		prev = Beginning;
		formatStack = new Stack<>();
		lastTextTag = -100;
		deferredMarkup = new StringBuilder();
		
		readEdFile(ed);
		
		File outDir = xml.getParentFile();
		if(outDir.mkdirs() && ebookPath != null)
		{
			/*
			Files.copy(new File(ebookPath, "ed.xsl").toPath(),
				new File(outDir, "ed.xsl").toPath(),
				StandardCopyOption.REPLACE_EXISTING);
			Files.copy(new File(ebookPath, "ed.css").toPath(),
				new File(outDir, "ed.css").toPath(),
				StandardCopyOption.REPLACE_EXISTING);
			 */
		}
		writerInfo.xmlFile = xml;
		writerInfo.fileNames.add(xml.getName());
		book.writeToFile(writerInfo);
	}


	private void readEdFile(File ed) throws Exception
	{
		try(InputStream is = new BufferedInputStream(new FileInputStream(ed)))
		{
			// use short instead of byte to work around signed byte handling
			short[] line = new short[100000];
			int ptr = 0;
			boolean emptyLine = true;
			while(true)
			{
				int c = is.read();
				if(c=='\n' || c<0)
				{
					while(ptr>0 && line[ptr-1]==' ') --ptr;
					if(ptr > 0)
					{
						processLine(line, ptr, emptyLine);
						emptyLine = false;
					}
					else
						emptyLine = true;
					if(c < 0) break;
					ptr = 0;
					++lineNumber;
					continue;
				}
				if(c=='\r' || ptr==0 && (c==' ' || c=='\t')) continue;
				line[ptr++] = (short) c;
			}
			purgeFormatStack();
		}
	}
	
	
	private void processLine(short[] line, int length, boolean paraStartLine)
	{
		// handle tags at the beginning of lines
		nextPos = 0;
		if(line[0] == '@' && paraStartLine)
		{
			// close previous tag
			if(superscript)
			{
				para.text.append("</sup>");
				superscript = false;
			}
			purgeFormatStack();
			skippingUnhandledTag = false;
			
			// start processing new tag
			String tagStr = sequenceToString(line, 1, length, (short) '=').trim().toLowerCase();
			while(nextPos<length && line[nextPos]==' ') ++nextPos;
			
			currentTag = EdTags.find(tagStr);
			if(currentTag == null)
				throw new IllegalStateException(String.format("ERROR: Nem definialt tag '%s' a '%s' fajlban. Sor: %d.", tagStr, srcFileName, lineNumber));
			currentAlias = currentTag.alias;
			if(currentAlias == null)
				currentAlias = currentTag;
			
			// if it's an unhandled tag
			if(currentAlias == EdTags.unhandled)
			{
				skippingUnhandledTag = true;
				return;
			}
			
			if(currentAlias == EdTags.paraContAfterInitialLetter)
			{
				prev = Hyphen;
			}
			else
			{
				// info or content tags
				para = new Paragraph();
				para.srcStyle = tagStr;
				para.srcFileName = srcFileName;
				para.srcFileLine = lineNumber;
				String tagName = currentTag.name();
				switch(currentTag)
				{
					case book_title:
						tagName = "title";
						// no break!
					case lila:
						book.info.add(para);
						break;
					case chaptno:
					case chapt_no:
						tagName = "chapter_number";
						chapter.info.add(para);
						break;
					case chapter_head:
						tagName = "head";
						chapter.info.add(para);
						break;
					case textno:
						tagName = "text_number";
						// text precedes textno with a max. of 1 tag in between
						if(chapter.para.size()-lastTextTag < 2)
						{
							// insert text_number before text
							chapter.para.add(lastTextTag-1, para);
							break;
						}
						// no break: add text_number as last
					default:
						chapter.para.add(para);
				}
	
				// info is represented as xml tag
				if(currentAlias == EdTags.info)
				{
					para.isInfo = true;
					para.xmlTagName = tagName;
				}
	
				// book text is p tag with a class attribute
				else
					para.cls = currentAlias.cls;
				
				// after footnote or any other quote para, following purp para is changed to purport for line spacing
				if(currentAlias == EdTags.purp_para)
				{
					Paragraph prevPara = chapter.para.get(chapter.para.size()-2);
					if(prevPara.cls != null && prevPara.cls.name().startsWith("Megjegyzes"))
					{
						currentTag = currentAlias = EdTags.purport;
						para.cls = currentAlias.cls;
					}
				}
	
				// register index level
				else if(currentAlias == EdTags.index_level_0)
				{
					if(tagName.startsWith("index_level"))
						para.indexLevel = Integer.parseInt(tagName.substring(12));
					else if(tagName.startsWith("xi"))
						para.indexLevel = Integer.parseInt(tagName.substring(2, 3));
				}
	
				// remember position of text tag or chapter title tag
				else if(currentAlias == EdTags.text || currentAlias == EdTags.chapter_title)
					lastTextTag = chapter.para.size();
	
				// mark footnote paragraphs
				else if(currentTag == EdTags.footnote || currentTag == EdTags.small_foot)
					para.text.append("Lábjegyzet: ");
				
				else if(writerInfo.specialFile == SpecialFile.BG_PF && currentTag == EdTags.purport && line[nextPos]!='<')
					para.cls = ParagraphClass.Balra;

				if(currentBookHasLeftAlignLeadParagraph && para.cls == ParagraphClass.TorzsKezdet)
					para.cls = ParagraphClass.BalraKezdet;
	
				prev = Tag;
			}
		}

		if(skippingUnhandledTag || nextPos >= length)
			return;

		// microspace to start a line: prevent implied space
		if(line[nextPos] == 127)
			prev = Microspace;
		
		// implied space if the previous line ended with a letter or punctuation
		if(prev == Char)
		{
			para.text.append(' ');
			prev = Space;
		}

		// iterate the characters of the line after the optional tag
		fontCode = 0;
		microspace = 0;
		emspace = 0;
		for(int pos=nextPos; pos<length; ++pos)
		{
			int c = line[pos];
			
			// handle inline formatting
			if(c == '<')
			{
				int processed = -1; 
				StringBuilder number = null;	// %fpjk után
				while(true)
				{
					c = Character.toUpperCase(line[++pos]);
					
					// process numbers after specific letters
					if(number != null)
					{
						if(c>='0' && c<='9' || c=='-' && number.length()==0 || c=='.')
						{
							number.append((char) c);
							continue;
						}
						else
						{
							if(processed == 'J')
							{
								double num;
								if(number.length() == 0)
									num = 0;
								else
									num = Double.parseDouble(number.toString());
								if(num > 200d)
								{
									para.text.append("<sup>");
									superscript = true;
								}
								else if(superscript)
								{
									para.text.append("</sup>");
									superscript = false;
								}
							}
							else if(processed == 'F')
							{
								if(number.length() == 0)
									fontCode = 0;
								else
									fontCode = Integer.parseInt(number.toString());
								if(fontCode == 255)
									fontCode = 0;
							}
							
							processed = -1;
							number = null;
						}
					}
					
					if(c == '$')
					{
						StringBuilder collect = new StringBuilder();
						for(++pos; (c=line[pos]) != '>'; ++pos)
							collect.append((char) c);
						if(collect.charAt(0)=='!')
						{
							if(collect.length()==1)		// <$!>
								prev = Linebreak;
							else
							{
								int c1 = collect.charAt(1);
								if(c1 == ' ')
									prev = OptionalSpace;
								else if(c1 == 'N')
								{
									if(prev == OptionalSpace)
										prev = Char;
								}
								else if(c1 == '|')
								{
									para.text.append(' ');
									prev = Space;
								}
								else if(c1 == 127)
								{
									microspace += collect.length()-1;
								}
							}
						}
					}
					
					if(c == 'D') c = 'M';
					
					// M/MI
					if(c == 'M')
					{
						int c2 = Character.toUpperCase(line[pos+1]);
						if(c2 == 'I')
						{
							++pos;
							if(formatStack.size()==0 || !("</i>".equals(formatStack.peek())))
							{
								if(emspace > 0 || microspace > 0)
									deferredMarkup.append("<i>");
								else
									para.text.append("<i>");
								formatStack.push("</i>");
							}
						}
						else
							purgeFormatStack();
					}
					
					// B/BI/BR
					else if(c == 'B')
					{
						int c2 = Character.toUpperCase(line[pos+1]);
						if(c2 == 'I')
						{
							++pos;
							para.text.append("<b><i>");
							formatStack.push("</i></b>");
						}
						else if(c2 == 'R')
						{
							++pos;
						}
						else
						{
							if(formatStack.size()==0 || !("</b>".equals(formatStack.peek())))
							{
								if(emspace > 0 || microspace > 0)
									deferredMarkup.append("<b>");
								else
									para.text.append("<b>");
								formatStack.push("</b>");
							}
						}
					}
					
					// QC, QJ, QR
					else if(c == 'Q')
					{
						int c2 = Character.toUpperCase(line[++pos]);
						if(c2 == 'C')
							para.cls = ParagraphClass.Kozepen;
						else if(c2 == 'R')
							para.cls = ParagraphClass.Jobbra;
					}
					
					else if(c == '~')
					{
						// count and handle later: they may only serve right alignment purposes
						++emspace;
					}
					
					else if(c == 'R')
					{
						if(prev == OptionalSpace)
							prev = Char;
						else if(currentAlias != EdTags.chapter_title &&
							(writerInfo.specialFile != SpecialFile.BG_DS || currentTag != EdTags.dc_body_r))
						{
							para.text.append("<br/>");
							prev = Linebreak;
						}
					}
					
					else if(c == '-')
					{
						prev = Hyphen;
					}
					
					else if(c == '+')
					{
						para.text.append("<!--num-tab-->");
						prev = Space;
					}
					
					else if(c == '|')
					{
						if(prev != Space)
						{
							para.text.append(' ');
							prev = Space;
						}
					}
					
					else if(c == 'N')
					{
						para.text.append('\u00a0');
						prev = Space;
					}
					
					else if(c == '_')
					{
						para.text.append('\u2003');
						prev = Space;
					}
					
					else if(c == 'T')
					{
						para.text.append("<!--tab-->");
						prev = Space;
					}
					
					// numbered ones
					else if(number == null && "%FPJKS".indexOf(c) != -1)
					{
						processed = c;
						number = new StringBuilder();
						continue;
					}
					
					else if(c>='0' && c<='9')
					{
						// disregard, only expect this in script
						if(!currentTag.name().startsWith("sans") && 
							!currentTag.name().startsWith("ben"))
								throw new IllegalStateException(String.format("ERROR: <%c> formazas a '%s' fajlban. Sor: %d.", c, srcFileName, lineNumber));
					}
					
					if(c == '>' || pos>=length)
						break;
				}
				// sanity check
				if(pos >= length)
					throw new IllegalStateException(String.format("ERROR: Hibas karakter formazas a '%s' fajlban. Sor: %d.", c, srcFileName, lineNumber));
			}
			
			else if(c == 127)
				++microspace;

			// printable characters
			else
			{
				postProcessCountedSymbols(line);
				
				// convert ed encoding to unicode
				if(c >= 128)
				{
					if(fontCode == 299)
					{
						if(c == 0x8a)
							c = '+';
					}
					
					if(c >= 128)
					{
						if(c == 253)
							continue;
						int savedCode = c;
						c = EdCharacter.convert(c);
						if(c == 0)
							throw new IllegalStateException(String.format("ERROR: Nem definialt karakterkod '%d' a '%s' fajlban. Sor: %d.", savedCode, srcFileName, lineNumber));
						if(c == '—' && prev == Space && currentAlias == EdTags.word_by_word)
						{
							// replace space with m dash in wbw
							para.text.setCharAt(para.text.length()-1, (char) c);
							prev = Dash;
							continue;
						}
					}
					
					para.text.append((char) c);
					if(c == '–' || c=='—')
						prev = Dash;
					else
						prev = Char;
				}
				
				// own markup {`#unicode}
				else if(c == '{' && pos < length-4 && line[pos+1] == '`' && line[pos+2] == '#')
				{
					pos += 2;
					String s = "";
					while((c = line[++pos]) != '}')
						s += (char) c;
					try
					{
						c = Integer.valueOf(s);
						para.text.append((char) c);
						prev = Char;
					}
					catch(NumberFormatException ex)
					{
						throw new IllegalStateException(String.format("ERROR: Hibas unicode markup a '%s' fajlban. Sor: %d.", c, srcFileName, lineNumber));
					}
				}
				
				// plain ascii characters
				else if(c >= ' ')
				{
					if(c == ' ')
					{
						if(NO_SPACES_AFTER_WBW_DASH && currentAlias == EdTags.word_by_word && prev == Dash)
							continue;
						prev = Space;
					}
					else if(c == '-')
						prev = Hyphen;
					else if(c == '"')
					{
						c = '”';
						prev = Char;
					}
					else if(c == '\'')
					{
						c = '’';
						prev = Char;
					}
					else
						prev = Char;

					para.text.append((char) c);
				}
			}
		}
		postProcessCountedSymbols(line);
	}


	private String sequenceToString(short[] line, int pos, int length, short c)
	{
		StringBuilder sb = new StringBuilder();
		while(pos < length && line[pos] != c)
			sb.append((char) line[pos++]);
		nextPos = pos+1;
		return sb.toString();
	}


	private void purgeFormatStack()
	{
		while(formatStack.size() > 0)
			para.text.append(formatStack.pop());
	}


	private void postProcessCountedSymbols(short[] line)
	{
		if(emspace > 0)
		{
			// process first line indent
			if(prev == Tag)
			{
				// if style has 2 indents by default: disregard marks
				if(para.cls != null && para.cls.defaultIndent)
				{
					emspace -= 2;
					if(emspace < 0)
						emspace = 0;
				}
				
				// if many indent marks start a paragraph
				else if(emspace > 4)
				{
					if(para.cls == ParagraphClass.TorzsKoveto)
						para.cls = ParagraphClass.Jobbra;
					else if(para.cls == ParagraphClass.MegjegyzesKoveto)
						para.cls = ParagraphClass.MegjegyzesJobbra;
					emspace = 0;
				}
			}
			
//			if(writerInfo.forEbook && prev == Linebreak)
//				emspace = 0;

			if(emspace > 3)
			{
				if(currentAlias == EdTags.word_by_word)
					emspace = 2;
				else if(currentTag == EdTags.footnote)
					emspace = 0;
				else if(currentTag != EdTags.bengali && currentTag != EdTags.center_line)
				{
					System.out.println("Tab line: " + sequenceToString(line, 0, 200, (short) '\r'));
					para.text.append("<!--tab-->");
					emspace = 0;
				}
			}

			while(emspace > 0)
			{
				para.text.append('\u2002');
				--emspace;
			}
			prev = Space;

			if(deferredMarkup.length() > 0)
			{
				para.text.append(deferredMarkup);
				deferredMarkup.setLength(0);
			}
		}

		else if(microspace > 0)
		{
			if(microspace >= 4 || microspace >= 2 && currentAlias != EdTags.word_by_word)
			{
				para.text.append(' ');
			}
			microspace = 0;
			prev = Microspace;

			if(deferredMarkup.length() > 0)
			{
				para.text.append(deferredMarkup);
				deferredMarkup.setLength(0);
			}
		}
	}
}
