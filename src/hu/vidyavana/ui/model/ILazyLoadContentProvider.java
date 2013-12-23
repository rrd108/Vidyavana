package hu.vidyavana.ui.model;

public interface ILazyLoadContentProvider
{
	int getLength();

	LazyLoadContentRange getRange(int start, int end);
}
