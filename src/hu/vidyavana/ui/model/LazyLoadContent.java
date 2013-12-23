package hu.vidyavana.ui.model;

import javax.swing.text.AbstractDocument.Content;
import javax.swing.text.*;
import javax.swing.undo.UndoableEdit;
import org.apache.commons.collections4.list.TreeList;

public class LazyLoadContent implements Content
{
	private final ILazyLoadContentProvider contentProvider;
	private TreeList<LazyLoadContentRange> ranges;


	public LazyLoadContent(ILazyLoadContentProvider contentProvider)
	{
		this.contentProvider = contentProvider;
		ranges = new TreeList<LazyLoadContentRange>();
	}
	
	
	@Override
	public Position createPosition(int offset) throws BadLocationException
	{
		return null;
	}

	@Override
	public int length()
	{
		return 0;
	}

	@Override
	public UndoableEdit insertString(int where, String str) throws BadLocationException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public UndoableEdit remove(int where, int nitems) throws BadLocationException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getString(int where, int len) throws BadLocationException
	{
		return null;
	}

	@Override
	public void getChars(int where, int len, Segment txt) throws BadLocationException
	{
	}

}
