package hu.vidyavana.ui.model.style;

import com.sleepycat.persist.model.Persistent;

@Persistent
public class StyleRange
{
	public short start;
	public short end;
	public byte styleCode;

	
	public StyleRange()
	{
	}


	public StyleRange(int start, int end, CharacterStyle style)
	{
		this.start = (short) start;
		this.end = (short) end;
		this.styleCode = style.code;
	}
}
