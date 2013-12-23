package hu.vidyavana.db;

import hu.vidyavana.convert.api.ParagraphClass;
import hu.vidyavana.db.api.Db;
import hu.vidyavana.db.model.*;
import hu.vidyavana.ui.model.style.*;
import hu.vidyavana.util.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.*;
import org.w3c.dom.*;
import org.w3c.dom.Document;
import com.sleepycat.persist.PrimaryIndex;

public class AddBook
{
	public static Pattern XML_LINE = Pattern.compile("^\\s*(<p( class=\"(.*?)\")?.*?>)?(.*?)(</p>|$)");
	public static Pattern MARKUP = Pattern.compile("<(.*?)>");
	
	private int bookId;
	// private String bookPath;
	private File bookDir;
	private final IndexWriter iw;
	private FieldType txtFieldType;
	private ArrayList<String> bookFileNames;
	private ArrayList<Integer> paraEndDocIndexes;
	private int paraEndIx;
	private StringBuilder plainSB;

	
	public AddBook(int bookId, String bookPath, IndexWriter writer)
	{
		this.bookId = bookId;
		// this.bookPath = bookPath;
		bookDir = new File(bookPath);
		this.iw = writer;
		txtFieldType = new FieldType();
		txtFieldType.setIndexed(true);
		txtFieldType.setTokenized(true);
		txtFieldType.setStored(false);
		txtFieldType.setStoreTermVectors(false);
		txtFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		txtFieldType.freeze();
	}
	
	
	public void run()
	{
		Db.openForWrite();
		addToc();
		addChapters();
		Db.openForRead();
	}


	private void addToc()
	{
		Element docElem = getXmlRoot("toc.xml");
		// String xmlVersion = docElem.getElementsByTagName("version").item(0).getTextContent();

		PrimaryIndex<BookOrdinalKey, Contents> idx = Contents.pkIdx();
		NodeList entries = docElem.getElementsByTagName("entries");
		if(entries.getLength() > 0)
		{
			NodeList entryList = entries.item(0).getChildNodes();
			for(int i=0, len=entryList.getLength(); i<len; ++i)
			{
				Node entry = entryList.item(i);
				if(!"entry".equals(entry.getNodeName()))
					continue;
				NodeList children = entry.getChildNodes();
				int tocOrdinal = -1;
				Contents contents = new Contents();
				for(int j=0, len2 = children.getLength(); j<len2; ++j)
				{
					Node n = children.item(j);
					String txt = n.getTextContent().trim();
					if("level".equals(n.getNodeName()))
						contents.level = Integer.parseInt(txt);
					else if("division".equals(n.getNodeName()))
						contents.division = txt;
					else if("title".equals(n.getNodeName()))
						contents.title = txt;
					else if("toc_ordinal".equals(n.getNodeName()))
						tocOrdinal = Integer.parseInt(txt);
					else if("para_ordinal".equals(n.getNodeName()))
						contents.bookParaOrdinal = Integer.parseInt(txt);
				}
				contents.key = new BookOrdinalKey(bookId, tocOrdinal);
				if(contents.title == null)
					contents.title = "";
				idx.put(contents);
			}
		}
		
		bookFileNames = new ArrayList<>();
		NodeList files = docElem.getElementsByTagName("files");
		if(files.getLength() > 0)
		{
			NodeList fileList = files.item(0).getChildNodes();
			for(int i=0, len=fileList.getLength(); i<len; ++i)
			{
				Node entry = fileList.item(i);
				if("file".equals(entry.getNodeName()))
					bookFileNames.add(entry.getTextContent().trim());
			}
		}
	}


	private void addChapters()
	{
		PrimaryIndex<BookOrdinalKey, Para> idx = Para.pkIdx();
		plainSB = new StringBuilder(10000);
		int bookParaOrdinal = 0;
		paraEndDocIndexes = new ArrayList<>(5000);
		for(String fname : bookFileNames)
		{
			File f = new File(bookDir, fname);
			BufferedReader in = null;
			try
			{
				in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
				Para para = null;
				StringBuilder paraSB = new StringBuilder(10000);
				boolean inPara = false;
				while(true)
				{
					String line = in.readLine();
					if(line == null)
						break;
					Matcher m = XML_LINE.matcher(line);
					m.find();
					if(m.group(1) != null)
					{
						if(inPara)
							addPara(para, paraSB, idx);
						
						String className = m.group(3);
						ParagraphClass cls;
						try
						{
							cls = ParagraphClass.valueOf(className);
						}
						catch(Exception ex)
						{
							cls = ParagraphClass.TorzsKoveto;
						}
						
						para = new Para(bookId, ++bookParaOrdinal, cls.code);
						
						String txt = m.group(4);
						paraSB.setLength(0);
						paraSB.append(txt);
						
						inPara = true;
					}
					else if(inPara)
						paraSB.append(' ').append(m.group(4));

					if(inPara)
					{
						// if <p> is started or cont'd, look for </p>
						inPara = m.group(5).length() == 0;
						if(!inPara)
							addPara(para, paraSB, idx);
					}
				}
			}
			catch(Exception ex)
			{
				throw new RuntimeException(ex);
			}
			finally
			{
				if(in != null)
					try
					{
						in.close();
					}
					catch(IOException ex)
					{
						throw new RuntimeException(ex);
					}
			}
		}
		BookMeta bm = new BookMeta();
		bm.id = bookId;
		bm.paraEndDocIndexes = new int[paraEndDocIndexes.size()];
		int ix = 0;
		for(int val : paraEndDocIndexes)
			bm.paraEndDocIndexes[ix++] = val;
		BookMeta.pkIdx().put(bm);
	}


	private void addPara(Para para, StringBuilder paraSB, PrimaryIndex<BookOrdinalKey, Para> idx)
	{
		String paraTxt = parsePara(para, paraSB);
		paraEndIx += paraTxt.length()+1;
		paraEndDocIndexes.add(paraEndIx);
		para.text = Encrypt.getInstance().encrypt(paraTxt);
		idx.put(para);
		
		org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
		doc.add(new IntField("bid", bookId, Store.YES));
		doc.add(new IntField("ord", para.key.ordinal, Store.YES));
		doc.add(new Field("text", paraTxt, txtFieldType));
		try
		{
			// TODO transaction
			iw.addDocument(doc);
		}
		catch(IOException ex)
		{
			throw new RuntimeException(ex);
		}
	}


	/**
	 * Separates paragraph into text and markup positions. 
	 * 
	 * @return Plain text of para
	 */
	private String parsePara(Para para, StringBuilder paraSB)
	{
		int src=0, dest=0;
		plainSB.setLength(0);
		List<StyleRange> styleRanges = null;
		Stack<Integer> bold = null, italic = null, superscript = null, ref;
		CharacterStyle charStyle = null;
		Matcher m = MARKUP.matcher(paraSB);
		while(m.find(src))
		{
			if(styleRanges == null)
				styleRanges = new ArrayList<>();
			int start = m.start();
			if(start > 0)
				plainSB.append(paraSB.substring(src, start));
			src = m.end();
			dest += start;
			String mkp = m.group(1);
			String baseMkp = mkp.charAt(0)=='/' ? mkp.substring(1) : mkp;
			if("b".equals(baseMkp))
			{
				if(bold == null)
					bold = new Stack<>();
				ref = bold;
				charStyle = CharacterStyle.Bold;
			}
			else if("i".equals(baseMkp))
			{
				if(italic == null)
					italic = new Stack<>();
				ref = italic;
				charStyle = CharacterStyle.Italic;
			}
			else
			{
				if(superscript == null)
					superscript = new Stack<>();
				ref = superscript;
				charStyle = CharacterStyle.Superscript;
			}
			switch(mkp)
			{
				case "b":
				case "i":
				case "sup":
					ref.push(styleRanges.size());
					StyleRange r = new StyleRange(dest, -1, charStyle);
					styleRanges.add(r);
					break;
				case "/b":
				case "/i":
				case "/sup":
					try
					{
						styleRanges.get(ref.pop()).end = (short) dest;
					}
					catch(Exception ignore)
					{
					}
					break;
				case "br/":
					plainSB.append('\n');
					++dest;
					++para.softBreaks;
					break;
			}
		}
		if(src < paraSB.length())
			plainSB.append(paraSB.substring(src));
		if(styleRanges != null)
			para.styleRanges = styleRanges.toArray(new StyleRange[styleRanges.size()]);
		return plainSB.toString();
	}


	private Element getXmlRoot(String fileName)
	{
		File f = new File(bookDir, fileName);
		String xml = XmlUtil.readFromFile(f);
		Document doc = XmlUtil.domFromString(xml);
		return doc.getDocumentElement();
	}
}
