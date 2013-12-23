package hu.vidyavana.ui.model;

import hu.vidyavana.db.model.Para;
import java.util.List;

public class LazyLoadContentRange
{
	public int paraOffset;
	public int paraNum;		// 0 means unloaded content
	public int startPos;
	public int endPos;
	public List<Para> content;
}
