package hu.vidyavana.db.model;

import hu.vidyavana.db.api.Db;
import hu.vidyavana.ui.model.style.StyleRange;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.model.*;

@Entity
public class Para
{
	@PrimaryKey
	public BookOrdinalKey key;

	public byte style;
	public byte[] text;
	public byte softBreaks;
	public StyleRange[] styleRanges;


	public Para()
	{
	}
	
	
	public Para(int bookId, int bookParaOrdinal, int style)
	{
		this.key = new BookOrdinalKey(bookId, bookParaOrdinal);
		this.style = (byte) style;
	}
	
	
	public static PrimaryIndex<BookOrdinalKey, Para> pkIdx()
	{
		return Db.store().getPrimaryIndex(BookOrdinalKey.class, Para.class);
	}
}
