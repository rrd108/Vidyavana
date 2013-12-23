package hu.vidyavana.ui.model;

import hu.vidyavana.db.api.Db;
import hu.vidyavana.db.model.*;
import java.util.Arrays;
import com.sleepycat.persist.EntityCursor;

public class VContentTextProvider implements ILazyLoadContentProvider
{
	private int bookId;
	private BookMeta bookMeta;

	
	public VContentTextProvider()
	{
		Db.openForRead();
	}
	
	
	public void initBook(int bookId)
	{
		this.bookId = bookId;
		bookMeta = BookMeta.pkIdx().get(bookId);
	}
	
	
	/** 
	 * @return Length of currently selected book
	 */
	@Override
	public int getLength()
	{
		return bookMeta.paraEndDocIndexes[bookMeta.paraEndDocIndexes.length-1];
	}

	
	@Override
	public LazyLoadContentRange getRange(int start, int end)
	{
		int startOrd = Arrays.binarySearch(bookMeta.paraEndDocIndexes, start);
		startOrd = startOrd<0 ? -startOrd-1 : startOrd+1;
		int endOrd = Arrays.binarySearch(bookMeta.paraEndDocIndexes, end);
		endOrd = endOrd<0 ? -endOrd-1 : endOrd+1;
		
		EntityCursor<Para> cursor = Para.pkIdx().entities(
			new BookOrdinalKey(bookId, startOrd), true, 
			new BookOrdinalKey(bookId, endOrd), false);
		
		
		
		LazyLoadContentRange range = new LazyLoadContentRange();
		range.startPos = start;
		range.endPos = end;
		return range;
	}

}
