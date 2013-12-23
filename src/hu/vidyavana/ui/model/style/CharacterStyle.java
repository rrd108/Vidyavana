package hu.vidyavana.ui.model.style;

public enum CharacterStyle
{
	Bold(0),
	Italic(1),
	Superscript(2),
	Subscript(3);
	
	public byte code;
	
	CharacterStyle(int code)
	{
		this.code = (byte) code;
	}
}
